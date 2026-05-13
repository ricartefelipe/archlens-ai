package dev.archlens.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.domain.model.Project;

public interface ProjectRepositoryPort {

    Project save(Project project);

    Optional<Project> findById(UUID id);

    List<Project> findAllByTenantId(String tenantId);

    boolean existsById(UUID id);
}
