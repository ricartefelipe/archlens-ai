package dev.archlens.application.port.in;

import java.util.List;
import java.util.UUID;

import dev.archlens.domain.model.Adr;

public interface GetAdrsUseCase {

    List<Adr> getByAnalysisId(UUID analysisId);
}
