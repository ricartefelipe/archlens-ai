package dev.archlens.domain.exception;

/**
 * Arquivo compactado recusado por exceder os limites de ingestão: total
 * descomprimido, número de entradas ou razão de expansão (bomba de
 * descompressão). A mensagem é segura para exibição ao cliente — descreve o
 * limite atingido, sem caminhos internos nem conteúdo do arquivo.
 */
public class ArchiveLimitExceededException extends RuntimeException {

    public ArchiveLimitExceededException(String message) {
        super(message);
    }
}
