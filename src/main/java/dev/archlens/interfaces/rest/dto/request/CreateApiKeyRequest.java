package dev.archlens.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateApiKeyRequest(
        @NotBlank String name,
        String scopes) {
}
