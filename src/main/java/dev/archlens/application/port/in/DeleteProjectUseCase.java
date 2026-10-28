package dev.archlens.application.port.in;

import java.util.UUID;

public interface DeleteProjectUseCase {

    void delete(UUID projectId);
}
