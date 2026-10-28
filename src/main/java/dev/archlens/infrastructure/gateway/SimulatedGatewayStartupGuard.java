package dev.archlens.infrastructure.gateway;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import dev.archlens.application.port.out.LlmGateway;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Impede que um lançamento de produção (NORMAL) inicie com um gateway de LLM
 * simulado alimentando resultados reais via fallback, a menos que o modo demo
 * seja habilitado explicitamente. Garante que saída simulada nunca seja
 * apresentada como análise de modelo real sem opção explícita do operador.
 */
@ApplicationScoped
public class SimulatedGatewayStartupGuard {

    private static final Logger LOG = Logger.getLogger(SimulatedGatewayStartupGuard.class);

    @Inject
    LlmGateway llmGateway;

    @ConfigProperty(name = "archlens.analysis.llm-fallback-enabled", defaultValue = "false")
    boolean llmFallbackEnabled;

    @Inject
    dev.archlens.infrastructure.config.ArchLensLlmConfig llmConfig;

    void onStart(@Observes StartupEvent event) {
        boolean normalLaunch = LaunchMode.current() == LaunchMode.NORMAL;
        boolean externalInference = llmGateway.performsExternalInference();

        if (mustRefuseBoot(normalLaunch, llmFallbackEnabled, externalInference, llmConfig.allowSimulatedDemo())) {
            throw new IllegalStateException(
                    "Inicialização recusada: fallback de análise está habilitado com um gateway de LLM "
                            + "simulado (sem inferência externa) em execução de produção. Configure um provedor "
                            + "real (archlens.llm.provider=openai|ollama) ou habilite explicitamente o modo demo "
                            + "com archlens.llm.allow-simulated-demo=true.");
        }

        if (!externalInference) {
            LOG.warn("LLM gateway sem inferência de modelo externo (modo local/determinístico). "
                    + "As conclusões são registradas com essa origem; o fallback simulado está "
                    + (llmFallbackEnabled ? "HABILITADO" : "desabilitado") + ".");
        }
    }

    /**
     * Regra pura de decisão (sem dependências de runtime), para teste direto.
     * Recusa o boot quando um lançamento de produção usaria um gateway simulado
     * para produzir resultados reais sem o operador optar pelo modo demo.
     */
    static boolean mustRefuseBoot(boolean normalLaunch, boolean fallbackEnabled,
                                  boolean externalInference, boolean allowSimulatedDemo) {
        return normalLaunch && fallbackEnabled && !externalInference && !allowSimulatedDemo;
    }
}
