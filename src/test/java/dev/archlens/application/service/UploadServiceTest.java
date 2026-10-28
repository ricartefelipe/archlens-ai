package dev.archlens.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import dev.archlens.application.port.out.FileStoragePort;
import dev.archlens.application.port.out.IngestGateway;
import dev.archlens.application.port.out.ProjectFileRepositoryPort;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantAccountRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.ArchiveLimitExceededException;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.ProjectFile;
import dev.archlens.domain.model.ProjectStatus;
import dev.archlens.domain.model.TenantAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Limites de ingestão do arquivo compactado: ZIP slip, número de entradas,
 * total descomprimido e razão de expansão (bomba de descompressão).
 */
class UploadServiceTest {

    private static final String TENANT = "tenant-1";

    @TempDir
    Path storageRoot;

    private UUID projectId;
    private RecordingProjectRepository projectRepository;
    private RecordingFileRepository fileRepository;
    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        projectRepository = new RecordingProjectRepository(projectId, TENANT);
        fileRepository = new RecordingFileRepository();

        uploadService = new UploadService(
                projectRepository,
                fileRepository,
                new TempDirStorage(storageRoot),
                new FileClassifier(),
                () -> TENANT,
                new IngestOrchestrationService(new NoOpIngestGateway(), projectRepository),
                new QuotaService(new UnlimitedAccountRepository(), projectRepository, false));
    }

    @Test
    @DisplayName("extrai arquivos relevantes de um ZIP comum")
    void extractsRelevantFiles() {
        byte[] zip = zip(Map.of(
                "src/Main.java", "class Main {}",
                "Dockerfile", "FROM alpine",
                "notes.bin", "ignorado"));

        uploadService.upload(projectId, "ok.zip", new ByteArrayInputStream(zip));

        List<String> stored = fileRepository.paths();
        assertTrue(stored.contains("Main.java") || stored.contains("src/Main.java"),
                "arquivo Java deveria ser extraído, veio: " + stored);
        // Sem o flag de ingestão assíncrona (teste unitário), o ingest conclui
        // no mesmo fluxo e o projeto chega a READY.
        assertEquals(ProjectStatus.READY, projectRepository.current().getStatus());
    }

    @Test
    @DisplayName("ignora entrada que tenta escapar do diretório do projeto (ZIP slip)")
    void ignoresZipSlipEntry() {
        byte[] zip = zip(Map.of(
                "../../evil.java", "class Evil {}",
                "src/Ok.java", "class Ok {}"));

        uploadService.upload(projectId, "slip.zip", new ByteArrayInputStream(zip));

        assertFalse(Files.exists(storageRoot.resolve("evil.java")),
                "arquivo não deve ser escrito fora do diretório do projeto");
        assertFalse(Files.exists(storageRoot.getParent().resolve("evil.java")));
        assertTrue(fileRepository.paths().stream().noneMatch(p -> p.contains("evil")));
    }

    @Test
    @DisplayName("recusa arquivo com número de entradas acima do limite")
    void refusesTooManyEntries() {
        Map<String, String> entries = new HashMap<>();
        for (int i = 0; i <= UploadService.MAX_ENTRIES; i++) {
            entries.put("src/F" + i + ".java", "class F" + i + " {}");
        }

        ArchiveLimitExceededException error = assertThrows(ArchiveLimitExceededException.class,
                () -> uploadService.upload(projectId, "many.zip",
                        new ByteArrayInputStream(zip(entries))));

        assertTrue(error.getMessage().contains("entradas"), error.getMessage());
        assertEquals(ProjectStatus.FAILED, projectRepository.current().getStatus());
    }

    @Test
    @DisplayName("recusa bomba de descompressão por razão de expansão")
    void refusesExpansionBomb() {
        // ~64 MB de zeros em um ZIP de poucos KB: muito acima do piso e da razão.
        byte[] payload = new byte[16 * 1024 * 1024];
        Map<String, byte[]> entries = new HashMap<>();
        for (int i = 0; i < 4; i++) {
            entries.put("src/Bomb" + i + ".java", payload);
        }

        ArchiveLimitExceededException error = assertThrows(ArchiveLimitExceededException.class,
                () -> uploadService.upload(projectId, "bomb.zip",
                        new ByteArrayInputStream(zipBytes(entries))));

        assertTrue(error.getMessage().contains("expansão") || error.getMessage().contains("descomprimido"),
                error.getMessage());
    }

    @Test
    @DisplayName("razão de expansão só conta acima do piso de bytes")
    void expansionRatioIgnoresSmallArchives() {
        // Um ZIP pequeno pode ter razão altíssima sem ser uma bomba.
        assertFalse(UploadService.isExpansionBomb(1024L * 1024, 1024));
        assertTrue(UploadService.isExpansionBomb(
                UploadService.RATIO_CHECK_FLOOR_BYTES * 2,
                UploadService.RATIO_CHECK_FLOOR_BYTES * 2 / (UploadService.MAX_EXPANSION_RATIO + 1)));
    }

    // --- fixtures ---------------------------------------------------------

    private static byte[] zip(Map<String, String> entries) {
        Map<String, byte[]> bytes = new HashMap<>();
        entries.forEach((name, content) -> bytes.put(name, content.getBytes(StandardCharsets.UTF_8)));
        return zipBytes(bytes);
    }

    private static byte[] zipBytes(Map<String, byte[]> entries) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }

    private record TempDirStorage(Path root) implements FileStoragePort {

        @Override
        public Path storeZip(UUID projectId, String fileName, InputStream zipStream) {
            return root.resolve(fileName);
        }

        @Override
        public Path getProjectDirectory(UUID projectId) {
            return root.resolve(projectId.toString());
        }

        @Override
        public void deleteProjectFiles(UUID projectId) {
            // nada a fazer: o diretório temporário é descartado pelo JUnit
        }
    }

    private static final class RecordingProjectRepository implements ProjectRepositoryPort {

        private final Project project = new Project();

        private RecordingProjectRepository(UUID projectId, String tenantId) {
            project.setId(projectId);
            project.setTenantId(tenantId);
            project.setName("demo");
            project.setStatus(ProjectStatus.CREATED);
        }

        private Project current() {
            return project;
        }

        @Override
        public Project save(Project p) {
            return p;
        }

        @Override
        public Optional<Project> findById(UUID id) {
            return project.getId().equals(id) ? Optional.of(project) : Optional.empty();
        }

        @Override
        public Optional<Project> findByIdAndTenantId(UUID id, String tenantId) {
            return project.getId().equals(id) && project.getTenantId().equals(tenantId)
                    ? Optional.of(project)
                    : Optional.empty();
        }

        @Override
        public List<Project> findAllByTenantId(String tenantId) {
            return project.getTenantId().equals(tenantId) ? List.of(project) : List.of();
        }

        @Override
        public boolean existsById(UUID id) {
            return project.getId().equals(id);
        }

        @Override
        public boolean existsByIdAndTenantId(UUID id, String tenantId) {
            return project.getId().equals(id) && project.getTenantId().equals(tenantId);
        }
    }

    private static final class RecordingFileRepository implements ProjectFileRepositoryPort {

        private final List<ProjectFile> saved = new ArrayList<>();

        private List<String> paths() {
            return saved.stream().map(ProjectFile::getFilePath).toList();
        }

        @Override
        public ProjectFile save(ProjectFile file) {
            saved.add(file);
            return file;
        }

        @Override
        public List<ProjectFile> saveAll(List<ProjectFile> files) {
            saved.addAll(files);
            return files;
        }

        @Override
        public List<ProjectFile> findByProjectId(UUID projectId) {
            return saved;
        }

        @Override
        public void deleteByProjectId(UUID projectId) {
            saved.clear();
        }
    }

    /** Sem enforcement de quota: este teste cobre limites de extração. */
    private static final class UnlimitedAccountRepository implements TenantAccountRepositoryPort {

        private final Map<String, TenantAccount> accounts = new HashMap<>();

        @Override
        public TenantAccount save(TenantAccount account) {
            accounts.put(account.getTenantId(), account);
            return account;
        }

        @Override
        public Optional<TenantAccount> findByTenantId(String tenantId) {
            return Optional.ofNullable(accounts.get(tenantId));
        }

        @Override
        public List<TenantAccount> findAll() {
            return List.copyOf(accounts.values());
        }
    }

    private static final class NoOpIngestGateway implements IngestGateway {

        @Override
        public IngestJobStatus triggerIngest(UUID projectId, String tenantId, List<String> filePaths) {
            return new IngestJobStatus(projectId, "COMPLETED", filePaths.size(), filePaths.size(), 0);
        }

        @Override
        public IngestJobStatus getStatus(UUID projectId) {
            return new IngestJobStatus(projectId, "COMPLETED", 0, 0, 0);
        }
    }
}
