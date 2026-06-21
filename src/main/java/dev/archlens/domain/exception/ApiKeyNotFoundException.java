package dev.archlens.domain.exception;

import java.util.UUID;

public class ApiKeyNotFoundException extends RuntimeException {

    public ApiKeyNotFoundException(UUID id) {
        super("API key not found: " + id);
    }
}
