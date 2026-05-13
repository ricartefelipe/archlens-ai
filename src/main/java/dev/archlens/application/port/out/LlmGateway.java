package dev.archlens.application.port.out;

public interface LlmGateway {

    LlmAnalysisResult analyzeProject(String projectContext);

    String answerQuestion(String question, String analysisContext);
}
