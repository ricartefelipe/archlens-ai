package dev.archlens.interfaces.rest.exception;

import java.time.Instant;

import dev.archlens.domain.exception.AnalysisNotCancellableException;
import dev.archlens.interfaces.rest.dto.response.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

@Provider
public class AnalysisNotCancellableExceptionMapper implements ExceptionMapper<AnalysisNotCancellableException> {

    @Override
    public Response toResponse(AnalysisNotCancellableException exception) {
        String correlationId = MDC.get("correlationId") != null
                ? MDC.get("correlationId").toString()
                : null;

        ErrorResponse error = new ErrorResponse(
                correlationId,
                Response.Status.CONFLICT.getStatusCode(),
                "Conflict",
                exception.getMessage(),
                Instant.now());

        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
