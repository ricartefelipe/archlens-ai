package dev.archlens.interfaces.rest.exception;

import java.time.Instant;

import dev.archlens.domain.exception.AnalysisNotComparableException;
import dev.archlens.interfaces.rest.dto.response.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

@Provider
public class AnalysisNotComparableExceptionMapper implements ExceptionMapper<AnalysisNotComparableException> {

    @Override
    public Response toResponse(AnalysisNotComparableException exception) {
        String correlationId = MDC.get("correlationId") != null
                ? MDC.get("correlationId").toString()
                : null;

        ErrorResponse error = new ErrorResponse(
                correlationId,
                422,
                "Unprocessable Entity",
                exception.getMessage(),
                Instant.now());

        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
