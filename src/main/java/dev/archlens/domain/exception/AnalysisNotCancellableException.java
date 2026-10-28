package dev.archlens.domain.exception;

import java.util.UUID;

import dev.archlens.domain.model.AnalysisStatus;

/**
 * Cancelamento recusado porque a análise já chegou a um estado terminal.
 * A mensagem é segura para exibição: informa o estado atual, sem detalhe interno.
 */
public class AnalysisNotCancellableException extends RuntimeException {

    public AnalysisNotCancellableException(UUID analysisId, AnalysisStatus currentStatus) {
        super("Análise " + analysisId + " não pode ser cancelada: status atual é "
                + currentStatus + ".");
    }
}
