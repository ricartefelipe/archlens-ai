package dev.archlens.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateTenantStatusRequest(
        @NotBlank String status) {
}
