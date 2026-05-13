package dev.archlens.infrastructure.gateway;

import java.util.List;

import org.jboss.logging.Logger;

import dev.archlens.application.port.out.LlmAnalysisResult;
import dev.archlens.application.port.out.LlmGateway;
import dev.archlens.application.port.out.LlmRiskFinding;
import dev.archlens.domain.model.RiskCategory;
import dev.archlens.domain.model.RiskSeverity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LocalLlmGateway implements LlmGateway {

    private static final Logger LOG = Logger.getLogger(LocalLlmGateway.class);

    @Override
    public LlmAnalysisResult analyzeProject(String projectContext) {
        LOG.debug("LocalLlmGateway: análise simulada para desenvolvimento local");

        String summary = "Análise arquitetural do projeto concluída. Foram identificados riscos relacionados "
                + "a observabilidade, acoplamento e separação de camadas. Recomenda-se revisão dos pontos "
                + "críticos listados.";

        List<LlmRiskFinding> findings = List.of(
                new LlmRiskFinding(
                        RiskCategory.LACK_OF_OBSERVABILITY,
                        RiskSeverity.HIGH,
                        "Ausência de métricas customizadas",
                        "O projeto não possui métricas de negócio instrumentadas. Apenas métricas padrão do "
                                + "framework estão disponíveis, impossibilitando a monitoração de indicadores críticos.",
                        "src/main/java",
                        "Nenhuma anotação @Timed ou @Counted encontrada",
                        "Adicionar métricas de negócio usando MicroProfile Metrics ou Micrometer"),
                new LlmRiskFinding(
                        RiskCategory.EXCESSIVE_COUPLING,
                        RiskSeverity.MEDIUM,
                        "Controller com lógica de negócio",
                        "Foram encontrados controllers que contêm regras de validação complexas e lógica de "
                                + "negócio embutida, violando o princípio de separação de responsabilidades.",
                        "src/main/java/controller",
                        "Regras de validação complexas no controller",
                        "Extrair lógica de negócio para use case dedicado"),
                new LlmRiskFinding(
                        RiskCategory.MISSING_CORRELATION_ID,
                        RiskSeverity.HIGH,
                        "Ausência de correlation-id nos logs",
                        "Os logs da aplicação não incluem um identificador de correlação, dificultando o "
                                + "rastreamento de requisições em ambiente distribuído.",
                        null,
                        "Logs não incluem X-Correlation-Id",
                        "Implementar filtro de correlation-id e propagá-lo via MDC"),
                new LlmRiskFinding(
                        RiskCategory.LAYER_SEPARATION_ISSUE,
                        RiskSeverity.MEDIUM,
                        "Entidade JPA exposta na API REST",
                        "Entidades de persistência estão sendo utilizadas diretamente como objetos de "
                                + "resposta da API, criando acoplamento entre a camada de infraestrutura e a interface.",
                        "src/main/java/resource",
                        "Retorno direto de entidades JPA em endpoints REST",
                        "Criar DTOs específicos para a camada de apresentação"));

        LOG.debugf("LocalLlmGateway: %d achados simulados", findings.size());
        return new LlmAnalysisResult(summary, findings);
    }

    @Override
    public String answerQuestion(String question, String analysisContext) {
        LOG.debugf("LocalLlmGateway: pergunta recebida — %s", question);

        return "Com base na análise do projeto, a arquitetura atual apresenta pontos de atenção "
                + "que devem ser endereçados para garantir a evolução sustentável do sistema. "
                + "Em ambiente de desenvolvimento local, utilize um serviço de inferência configurável "
                + "(ver documentação do projeto e variáveis de ambiente).";
    }
}
