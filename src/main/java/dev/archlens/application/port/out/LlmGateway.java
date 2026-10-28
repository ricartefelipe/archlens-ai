package dev.archlens.application.port.out;

public interface LlmGateway {

    LlmAnalysisResult analyzeProject(String projectContext);

    String answerQuestion(String question, String analysisContext);

    /**
     * Indica se este gateway realiza inferência de modelo externo/real.
     * Implementações simuladas ou determinísticas locais devem retornar
     * {@code false}, para que a origem de cada conclusão possa ser registrada
     * e para que o perfil de produção recuse apresentar saída simulada como real.
     */
    default boolean performsExternalInference() {
        return true;
    }
}
