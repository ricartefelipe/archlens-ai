package dev.archlens.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.domain.model.Analysis;

public interface AnalysisRepositoryPort {

    Analysis save(Analysis analysis);

    Optional<Analysis> findById(UUID id);

    Optional<Analysis> findByProjectIdAndId(UUID projectId, UUID analysisId);

    List<Analysis> findByProjectIdAndTenantId(UUID projectId, String tenantId);
}
