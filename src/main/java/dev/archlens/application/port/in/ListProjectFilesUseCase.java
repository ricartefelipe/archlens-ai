package dev.archlens.application.port.in;

import java.util.List;
import java.util.UUID;

import dev.archlens.domain.model.ProjectFile;

public interface ListProjectFilesUseCase {

    List<ProjectFile> listByProjectId(UUID projectId);
}
