package dev.archlens.application.port.out;

import java.util.List;
import java.util.UUID;

import dev.archlens.domain.model.Adr;

public interface AdrRepositoryPort {

    Adr save(Adr adr);

    List<Adr> saveAll(List<Adr> adrs);

    List<Adr> findByAnalysisId(UUID analysisId);
}
