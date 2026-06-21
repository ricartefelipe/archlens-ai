package dev.archlens.interfaces.rest.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateWebhookRequest(
        @NotBlank String url,
        @NotEmpty List<String> events) {
}
