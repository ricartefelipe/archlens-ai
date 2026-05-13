package dev.archlens.application.port.in;

import java.util.UUID;

import dev.archlens.domain.model.Project;

public interface GetProjectUseCase {

    Project getById(UUID id);
}
