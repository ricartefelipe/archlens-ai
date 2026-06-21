package dev.archlens.interfaces.rest;

import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal;
import jakarta.ws.rs.core.SecurityContext;

final class OrgResourceSupport {

    private OrgResourceSupport() {
    }

    static String extractEmail(SecurityContext securityContext) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return null;
        }

        var principal = securityContext.getUserPrincipal();
        if (principal instanceof OidcJwtCallerPrincipal jwtPrincipal) {
            String email = jwtPrincipal.getClaims().getClaimValueAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
            return jwtPrincipal.getClaims().getClaimValueAsString("preferred_username");
        }
        return principal.getName();
    }
}
