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

    /**
     * Análises ainda em andamento (PENDING/PROCESSING) sem progresso desde
     * {@code cutoff}. Consulta de manutenção: percorre todos os tenants, então
     * o chamador precisa ativar o bypass de RLS na mesma transação.
     */
    List<Analysis> findInProgressOlderThan(java.time.Instant cutoff);

    /**
     * Status atual lido direto do banco, ignorando o cache de primeiro nível.
     * Dentro de uma transação longa, {@code findById} devolve a instância
     * gerenciada com o estado antigo — inútil para detectar um cancelamento
     * commitado por outra transação.
     */
    Optional<dev.archlens.domain.model.AnalysisStatus> findCurrentStatusFresh(UUID id);

    /**
     * Cancela de forma condicional e atômica: só transiciona se a análise
     * ainda está PENDING/PROCESSING. Devolve a análise já cancelada, ou vazio
     * se ela chegou a um estado terminal — o chamador decide como reportar.
     * Retornar o resultado evita uma releitura que poderia ver estado obsoleto.
     */
    Optional<Analysis> cancelIfInProgress(UUID id, java.time.Instant when, String summary);
}
