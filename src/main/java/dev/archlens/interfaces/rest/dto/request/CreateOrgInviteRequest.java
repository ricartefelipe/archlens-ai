package dev.archlens.interfaces.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateOrgInviteRequest(
        @NotBlank @Email String email,
        @NotBlank String role) {
}
