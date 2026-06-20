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
import dev.archlens.domain.exception.ProjectNotFoundException;
import dev.archlens.domain.model.Project;
import dev.archlens.domain.model.ProjectFile;
import dev.archlens.domain.model.ProjectStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ApplicationScoped
public class UploadService implements UploadProjectUseCase {

    private static final Logger LOG = Logger.getLogger(UploadService.class);
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    private final ProjectRepositoryPort projectRepository;
    private final ProjectFileRepositoryPort fileRepository;
    private final FileStoragePort fileStorage;
    private final FileClassifier fileClassifier;
    private final TenantProvider tenantProvider;
    private final IngestOrchestrationService ingestOrchestrationService;

    public UploadService(ProjectRepositoryPort projectRepository,
                         ProjectFileRepositoryPort fileRepository,
                         FileStoragePort fileStorage,
                         FileClassifier fileClassifier,
                         TenantProvider tenantProvider,
                         IngestOrchestrationService ingestOrchestrationService) {
        this.projectRepository = projectRepository;
        this.fileRepository = fileRepository;
        this.fileStorage = fileStorage;
        this.fileClassifier = fileClassifier;
        this.tenantProvider = tenantProvider;
        this.ingestOrchestrationService = ingestOrchestrationService;
    }

    @Override
    @Transactional
    public Project upload(UUID projectId, String fileName, InputStream zipStream) {
        String tenantId = tenantProvider.getCurrentTenantId();
        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        LOG.infof("Starting upload for project %s: %s", projectId, fileName);

        project.setStatus(ProjectStatus.UPLOADING);
        projectRepository.save(project);

        try {
            fileRepository.deleteByProjectId(projectId);
            fileStorage.deleteProjectFiles(projectId);

            Path projectDir = fileStorage.getProjectDirectory(projectId);
            Files.createDirectories(projectDir);

            List<ProjectFile> extractedFiles = extractZip(zipStream, projectDir, projectId, tenantId);

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

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryPath = sanitizePath(entry.getName());
                if (entryPath == null || !fileClassifier.isRelevant(entryPath)) {
                    continue;
                }

                Path targetFile = projectDir.resolve(entryPath);
                Files.createDirectories(targetFile.getParent());

                byte[] content = zis.readNBytes((int) Math.min(entry.getSize() > 0 ? entry.getSize() : MAX_FILE_SIZE, MAX_FILE_SIZE));
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
