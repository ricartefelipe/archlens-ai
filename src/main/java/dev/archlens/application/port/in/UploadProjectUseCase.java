package dev.archlens.application.port.in;

import java.io.InputStream;
import java.util.UUID;

import dev.archlens.domain.model.Project;

public interface UploadProjectUseCase {

    Project upload(UUID projectId, String fileName, InputStream zipStream);
}
