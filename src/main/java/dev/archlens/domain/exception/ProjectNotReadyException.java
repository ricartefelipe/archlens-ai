package dev.archlens.domain.exception;

import java.util.UUID;

import dev.archlens.domain.model.ProjectStatus;

public class ProjectNotReadyException extends RuntimeException {

    public ProjectNotReadyException(UUID projectId, ProjectStatus currentStatus) {
        super("Project " + projectId + " is not ready for analysis (status: " + currentStatus + ")");
    }
}
