package dev.archlens.interfaces.rest.exception;

import java.time.Instant;

import dev.archlens.domain.exception.ArchiveLimitExceededException;
import dev.archlens.interfaces.rest.dto.response.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

@Provider
public class ArchiveLimitExceededExceptionMapper implements ExceptionMapper<ArchiveLimitExceededException> {

    private static final int PAYLOAD_TOO_LARGE = 413;

    @Override
    public Response toResponse(ArchiveLimitExceededException exception) {
        String correlationId = MDC.get("correlationId") != null
                ? MDC.get("correlationId").toString()
                : null;

        ErrorResponse error = new ErrorResponse(
                correlationId,
                PAYLOAD_TOO_LARGE,
                "Payload Too Large",
                exception.getMessage(),
                Instant.now());

        return Response.status(PAYLOAD_TOO_LARGE)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
