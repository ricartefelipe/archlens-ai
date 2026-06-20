package dev.archlens.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpgradeTenantPlanRequest(
        @NotBlank String plan,
        String notes) {
}
