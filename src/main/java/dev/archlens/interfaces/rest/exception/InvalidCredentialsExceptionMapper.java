package dev.archlens.interfaces.rest.exception;

import java.time.Instant;

import dev.archlens.domain.exception.InvalidCredentialsException;
import dev.archlens.interfaces.rest.dto.response.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

@Provider
public class InvalidCredentialsExceptionMapper implements ExceptionMapper<InvalidCredentialsException> {

    @Override
    public Response toResponse(InvalidCredentialsException exception) {
        String correlationId = MDC.get("correlationId") != null
                ? MDC.get("correlationId").toString()
                : null;

        ErrorResponse error = new ErrorResponse(
                correlationId,
                Response.Status.UNAUTHORIZED.getStatusCode(),
                "Unauthorized",
                exception.getMessage(),
                Instant.now());

        return Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
