package dev.archlens.application.service;

import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;

import dev.archlens.application.port.in.UploadProjectUseCase;
import dev.archlens.application.port.out.FileStoragePort;
import dev.archlens.application.port.out.ProjectFileRepositoryPort;
import dev.archlens.application.port.out.ProjectRepositoryPort;
import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.domain.exception.ArchiveLimitExceededException;
import dev.archlens.domain.exception.ProjectNotFoundException;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.ProjectFile;
import dev.archlens.domain.model.ProjectStatus;
import dev.archlens.infrastructure.persistence.rls.TenantScopedRls;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@TenantScopedRls
@ApplicationScoped
public class UploadService implements UploadProjectUseCase {

    private static final Logger LOG = Logger.getLogger(UploadService.class);
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    /** Orçamento total de bytes descomprimidos por upload. */
    static final long MAX_TOTAL_UNCOMPRESSED_BYTES = 500L * 1024 * 1024;

    /** Número máximo de entradas lidas do arquivo, incluindo as ignoradas. */
    static final int MAX_ENTRIES = 20_000;

    /**
     * Razão máxima entre bytes descomprimidos e o tamanho do arquivo recebido.
     * Só é aplicada acima de {@link #RATIO_CHECK_FLOOR_BYTES}, para não rejeitar
     * arquivos pequenos — em que uma razão alta é normal e inofensiva.
     */
    static final long MAX_EXPANSION_RATIO = 200;

    static final long RATIO_CHECK_FLOOR_BYTES = 8L * 1024 * 1024;

    private final ProjectRepositoryPort projectRepository;
    private final ProjectFileRepositoryPort fileRepository;
    private final FileStoragePort fileStorage;
    private final FileClassifier fileClassifier;
    private final TenantProvider tenantProvider;
    private final IngestOrchestrationService ingestOrchestrationService;
    private final QuotaService quotaService;

    public UploadService(ProjectRepositoryPort projectRepository,
                         ProjectFileRepositoryPort fileRepository,
                         FileStoragePort fileStorage,
                         FileClassifier fileClassifier,
                         TenantProvider tenantProvider,
                         IngestOrchestrationService ingestOrchestrationService,
                         QuotaService quotaService) {
        this.projectRepository = projectRepository;
        this.fileRepository = fileRepository;
        this.fileStorage = fileStorage;
        this.fileClassifier = fileClassifier;
        this.tenantProvider = tenantProvider;
        this.ingestOrchestrationService = ingestOrchestrationService;
        this.quotaService = quotaService;
    }

    @Override
    @Transactional
    public Project upload(UUID projectId, String fileName, InputStream zipStream) {
        String tenantId = tenantProvider.getCurrentTenantId();
        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        LOG.infof("Starting upload for project %s: %s", projectId, fileName);

        quotaService.checkCanUpload(tenantId, MAX_FILE_SIZE);

        project.setStatus(ProjectStatus.UPLOADING);
        projectRepository.save(project);

        try {
            fileRepository.deleteByProjectId(projectId);
            fileStorage.deleteProjectFiles(projectId);

            Path projectDir = fileStorage.getProjectDirectory(projectId);
            Files.createDirectories(projectDir);

            List<ProjectFile> extractedFiles = extractZip(zipStream, projectDir, projectId, tenantId);

            long totalBytes = extractedFiles.stream().mapToLong(ProjectFile::getSizeBytes).sum();
            quotaService.recordUpload(tenantId, totalBytes);

            fileRepository.saveAll(extractedFiles);

            project.setStatus(ProjectStatus.UPLOADED);
            project.setFileCount(extractedFiles.size());
            projectRepository.save(project);

            List<String> filePaths = extractedFiles.stream()
                    .map(ProjectFile::getFilePath)
                    .toList();
            ingestOrchestrationService.startIngest(projectId, tenantId, filePaths);

            LOG.infof("Upload completed for project %s: %d files extracted, ingest started", projectId, extractedFiles.size());
            return project;
        } catch (ArchiveLimitExceededException e) {
            // Recusa de limite é resposta do cliente (413), não falha interna.
            LOG.warnf("Upload recusado para o projeto %s: %s", projectId, e.getMessage());
            project.setStatus(ProjectStatus.FAILED);
            projectRepository.save(project);
            throw e;
        } catch (Exception e) {
            LOG.errorf(e, "Upload failed for project %s", projectId);
            project.setStatus(ProjectStatus.FAILED);
            projectRepository.save(project);
            throw new RuntimeException("Failed to process upload: " + e.getMessage(), e);
        }
    }

    private List<ProjectFile> extractZip(InputStream zipStream, Path projectDir,
                                          UUID projectId, String tenantId) throws IOException {
        List<ProjectFile> files = new ArrayList<>();
        Path extractionRoot = projectDir.toAbsolutePath().normalize();

        // Conta os bytes recebidos para calcular a razão de expansão; sem isso,
        // um arquivo pequeno poderia escrever um volume ilimitado em disco.
        CountingInputStream counting = new CountingInputStream(zipStream);
        int entryCount = 0;
        long totalUncompressed = 0;

        try (ZipInputStream zis = new ZipInputStream(counting)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (++entryCount > MAX_ENTRIES) {
                    throw new ArchiveLimitExceededException(
                            "Arquivo recusado: mais de " + MAX_ENTRIES + " entradas.");
                }

                if (entry.isDirectory()) {
                    continue;
                }

                String entryPath = sanitizePath(entry.getName());
                if (entryPath == null || !fileClassifier.isRelevant(entryPath)) {
                    continue;
                }

                Path targetFile = projectDir.resolve(entryPath).toAbsolutePath().normalize();
                if (!targetFile.startsWith(extractionRoot)) {
                    // Defesa final de ZIP slip: o caminho resolvido tem de ficar
                    // dentro do diretório do projeto.
                    LOG.warnf("Entrada de arquivo fora do diretório do projeto %s ignorada", projectId);
                    continue;
                }
                Files.createDirectories(targetFile.getParent());

                long budgetLeft = MAX_TOTAL_UNCOMPRESSED_BYTES - totalUncompressed;
                if (budgetLeft <= 0) {
                    throw new ArchiveLimitExceededException(totalLimitMessage());
                }

                // Lê 1 byte além do permitido para distinguir "no limite" de "acima".
                long readLimit = Math.min(MAX_FILE_SIZE, budgetLeft) + 1;
                byte[] content = zis.readNBytes((int) Math.min(readLimit, Integer.MAX_VALUE));

                if (content.length > budgetLeft) {
                    throw new ArchiveLimitExceededException(totalLimitMessage());
                }
                if (content.length > MAX_FILE_SIZE) {
                    throw new ArchiveLimitExceededException(
                            "Arquivo recusado: entrada maior que o limite de "
                                    + (MAX_FILE_SIZE / (1024 * 1024)) + " MB por arquivo.");
                }

                totalUncompressed += content.length;
                if (isExpansionBomb(totalUncompressed, counting.count())) {
                    throw new ArchiveLimitExceededException(
                            "Arquivo recusado: razão de expansão acima de " + MAX_EXPANSION_RATIO + ":1.");
                }

                Files.write(targetFile, content);

                String hash = sha256(content);

                ProjectFile pf = new ProjectFile();
                pf.setId(UUID.randomUUID());
                pf.setProjectId(projectId);
                pf.setTenantId(tenantId);
                pf.setFilePath(entryPath);
                pf.setFileType(fileClassifier.classify(entryPath));
                pf.setSizeBytes(content.length);
                pf.setContentHash(hash);

                files.add(pf);
                zis.closeEntry();
            }
        }
        return files;
    }

    private static String totalLimitMessage() {
        return "Arquivo recusado: conteúdo descomprimido acima do limite de "
                + (MAX_TOTAL_UNCOMPRESSED_BYTES / (1024 * 1024)) + " MB.";
    }

    /**
     * Razão de expansão só vale como sinal depois de um volume mínimo: um ZIP
     * de poucos KB que expande muito é normal (texto comprime bem).
     */
    static boolean isExpansionBomb(long uncompressedBytes, long compressedBytesRead) {
        if (uncompressedBytes <= RATIO_CHECK_FLOOR_BYTES || compressedBytesRead <= 0) {
            return false;
        }
        return uncompressedBytes / compressedBytesRead > MAX_EXPANSION_RATIO;
    }

    /** Conta os bytes consumidos do stream de entrada (dados comprimidos). */
    private static final class CountingInputStream extends java.io.FilterInputStream {

        private long count;

        private CountingInputStream(InputStream in) {
            super(in);
        }

        private long count() {
            return count;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) {
                count++;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int read = super.read(buffer, offset, length);
            if (read > 0) {
                count += read;
            }
            return read;
        }
    }

    private String sanitizePath(String entryName) {
        if (entryName.contains("..") || entryName.startsWith("/")) {
            return null;
        }
        int slashIndex = entryName.indexOf('/');
        if (slashIndex > 0 && slashIndex < entryName.length() - 1) {
            return entryName.substring(slashIndex + 1);
        }
        return entryName;
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return null;
        }
    }
}
