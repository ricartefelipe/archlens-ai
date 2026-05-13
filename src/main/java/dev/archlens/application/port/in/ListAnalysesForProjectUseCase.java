package dev.archlens.application.port.in;

import java.util.List;
import java.util.UUID;

import dev.archlens.domain.model.Analysis;

public interface ListAnalysesForProjectUseCase {

    List<Analysis> listByProject(UUID projectId);
}
