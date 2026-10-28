package dev.archlens.interfaces.rest.exception;

import java.time.Instant;

import dev.archlens.interfaces.rest.dto.response.ErrorResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        String correlationId = MDC.get("correlationId") != null
                ? MDC.get("correlationId").toString()
                : null;

        // Um mapper de Exception também captura as exceções de status do
        // JAX-RS: sem este ramo, um ForbiddenException virava 500 e o cliente
        // não distinguia falta de permissão de falha do servidor.
        if (exception instanceof WebApplicationException webException) {
            Response original = webException.getResponse();
            Response.StatusType status = original.getStatusInfo();

            if (status.getFamily() == Response.Status.Family.SERVER_ERROR) {
                LOG.error("Unhandled server error", exception);
            } else {
                LOG.debugf("Request rejected with %d: %s", status.getStatusCode(), exception.getMessage());
            }

            if (original.hasEntity()) {
                return original;
            }

            return Response.status(status.getStatusCode())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorResponse(
                            correlationId,
                            status.getStatusCode(),
                            status.getReasonPhrase(),
                            clientSafeMessage(status, exception),
                            Instant.now()))
                    .build();
        }

        LOG.error("Unhandled exception", exception);

        ErrorResponse error = new ErrorResponse(
                correlationId,
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                Instant.now());

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }

    /**
     * Mensagem de erro de status: a de 5xx é genérica, para não revelar
     * detalhe interno; a de 4xx descreve o que o cliente precisa corrigir.
     */
    private static String clientSafeMessage(Response.StatusType status, Exception exception) {
        if (status.getFamily() == Response.Status.Family.SERVER_ERROR) {
            return "An unexpected error occurred. Please try again later.";
        }
        String message = exception.getMessage();
        return message != null && !message.isBlank() ? message : status.getReasonPhrase();
    }
}
