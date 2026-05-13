package dev.archlens.application.port.in;

import dev.archlens.domain.model.Project;

public interface CreateProjectUseCase {

    Project create(String name, String description);
}
