package dev.archlens.infrastructure.jobs;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.application.service.WebhookService;
import dev.archlens.domain.model.Analysis;
import dev.archlens.domain.model.AnalysisStatus;
import dev.archlens.infrastructure.persistence.rls.TenantRlsService;
import dev.archlens.interfaces.rest.context.RequestScopedTenantProvider;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Libera análises abandonadas. Uma interrupção fora do fluxo normal — fila
 * recriada, mensagem perdida, processo derrubado sem reentrega — deixava a
 * análise em PENDING/PROCESSING para sempre, com o cliente vendo progresso
 * eterno. O varredor marca como FAILED, com motivo legível, tudo que está em
 * andamento sem progresso há mais tempo que {@code archlens.analysis.stuck-after}.
 *
 * O prazo precisa ser maior que a análise legítima mais longa: expirar cedo
 * demais derruba trabalho vivo; a reentrega normal do broker continua sendo o
 * caminho primário de retomada.
 */
@ApplicationScoped
public class StuckAnalysisSweeper {

    private static final Logger LOG = Logger.getLogger(StuckAnalysisSweeper.class);

    private final AnalysisRepositoryPort analysisRepository;
    private final TenantRlsService tenantRlsService;
    private final WebhookService webhookService;
    private final RequestScopedTenantProvider requestTenantProvider;

    @ConfigProperty(name = "archlens.analysis.stuck-after", defaultValue = "PT30M")
    Duration stuckAfter;

    @Inject
    public StuckAnalysisSweeper(AnalysisRepositoryPort analysisRepository,
                                TenantRlsService tenantRlsService,
                                WebhookService webhookService,
                                RequestScopedTenantProvider requestTenantProvider) {
        this.analysisRepository = analysisRepository;
        this.tenantRlsService = tenantRlsService;
        this.webhookService = webhookService;
        this.requestTenantProvider = requestTenantProvider;
    }

    @Scheduled(every = "{archlens.analysis.sweep-every}",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void scheduledSweep() {
        sweep(Instant.now());
    }

    /** Executa uma varredura; separado do gatilho para teste direto. */
    @ActivateRequestContext
    public int sweep(Instant now) {
        List<Analysis> expired = expireInTransaction(now.minus(stuckAfter));

        // Webhooks fora da transação: falha de entrega não desfaz a liberação.
        for (Analysis analysis : expired) {
            // WebhookService é tenant-scoped: cada notificação roda no tenant
            // dono da análise, não num contexto de request que não existe aqui.
            requestTenantProvider.setTenantId(analysis.getTenantId());
            Map<String, Object> payload = new HashMap<>();
            payload.put("analysisId", analysis.getId().toString());
            payload.put("projectId", analysis.getProjectId().toString());
            payload.put("tenantId", analysis.getTenantId());
            payload.put("status", AnalysisStatus.FAILED.name());
            payload.put("summary", analysis.getSummary());
            webhookService.dispatch(analysis.getTenantId(),
                    WebhookService.EVENT_ANALYSIS_FAILED, payload);
        }

        if (!expired.isEmpty()) {
            LOG.warnf("Varredura liberou %d análise(s) abandonada(s) há mais de %s",
                    expired.size(), stuckAfter);
        }
        return expired.size();
    }

    @Transactional
    List<Analysis> expireInTransaction(Instant cutoff) {
        // Varredura de manutenção cruza tenants; o bypass vale só nesta transação.
        tenantRlsService.bypassForMaintenance();

        List<Analysis> stuck = analysisRepository.findInProgressOlderThan(cutoff);
        for (Analysis analysis : stuck) {
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setSummary(timeoutSummary(stuckAfter));
            analysis.setUpdatedAt(Instant.now());
            analysisRepository.save(analysis);
            LOG.warnf("Analysis %s (tenant %s) marcada FAILED por abandono",
                    analysis.getId(), analysis.getTenantId());
        }
        return stuck;
    }

    static String timeoutSummary(Duration stuckAfter) {
        long minutes = Math.max(1, stuckAfter.toMinutes());
        return "Análise interrompida: sem progresso por mais de " + minutes
                + " minuto(s). O processamento foi liberado; execute uma nova análise. "
                + "Se o problema persistir, verifique worker e fila de mensagens.";
    }
}
