package dev.archlens.infrastructure.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import dev.archlens.application.port.out.FileStoragePort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LocalFileStorageAdapter implements FileStoragePort {

    private static final Logger LOG = Logger.getLogger(LocalFileStorageAdapter.class);

    @ConfigProperty(name = "archlens.storage.base-path", defaultValue = "/tmp/archlens/projects")
    String basePath;

    @Override
    public Path storeZip(UUID projectId, String fileName, InputStream zipStream) {
        try {
            Path dir = getProjectDirectory(projectId);
            Files.createDirectories(dir);
            Path zipFile = dir.resolve(fileName);
            Files.copy(zipStream, zipFile, StandardCopyOption.REPLACE_EXISTING);
            LOG.infof("Stored zip at %s", zipFile);
            return zipFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store zip file: " + e.getMessage(), e);
        }
    }

    @Override
    public Path getProjectDirectory(UUID projectId) {
        return Path.of(basePath, projectId.toString());
    }

    @Override
    public void deleteProjectFiles(UUID projectId) {
        Path dir = getProjectDirectory(projectId);
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        LOG.warnf("Failed to delete %s: %s", p, e.getMessage());
                    }
                });
            } catch (IOException e) {
                LOG.warnf("Failed to walk directory %s: %s", dir, e.getMessage());
            }
        }
    }
}
