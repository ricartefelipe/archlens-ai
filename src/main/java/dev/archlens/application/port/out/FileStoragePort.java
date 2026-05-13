package dev.archlens.application.port.out;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

public interface FileStoragePort {

    Path storeZip(UUID projectId, String fileName, InputStream zipStream);

    Path getProjectDirectory(UUID projectId);

    void deleteProjectFiles(UUID projectId);
}
