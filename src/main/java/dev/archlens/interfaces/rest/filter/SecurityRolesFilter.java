package dev.archlens.interfaces.rest.filter;

import java.io.IOException;
import java.util.Set;

import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.application.service.OrgService;
import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class SecurityRolesFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(SecurityRolesFilter.class);

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    private final OrgService orgService;
    private final TenantProvider tenantProvider;

    @Inject
    public SecurityRolesFilter(OrgService orgService, TenantProvider tenantProvider) {
        this.orgService = orgService;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var securityContext = requestContext.getSecurityContext();
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return;
        }

        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();

        if (!path.startsWith("v1/")) {
            return;
        }

        if (path.startsWith("v1/admin/")) {
            if (!securityContext.isUserInRole("admin")) {
                LOG.warnf("User %s lacks admin role for %s %s",
                        securityContext.getUserPrincipal().getName(), method, path);
                abort(requestContext);
            }
            return;
        }

        if (path.startsWith("v1/org/") && WRITE_METHODS.contains(method)) {
            if ("v1/org/invites/accept".equals(path)) {
                return;
            }
            String email = extractEmail(requestContext);
            String tenantId = tenantProvider.getCurrentTenantId();
            if (!orgService.isOrgAdmin(tenantId, email)
                    && !securityContext.isUserInRole("admin")
                    && !securityContext.isUserInRole("architect")) {
                LOG.warnf("User %s lacks ORG_ADMIN for %s %s",
                        securityContext.getUserPrincipal().getName(), method, path);
                abort(requestContext);
            }
            return;
        }

        if (WRITE_METHODS.contains(method)) {
            boolean hasWriteRole = securityContext.isUserInRole("admin")
                    || securityContext.isUserInRole("architect");

            if (!hasWriteRole) {
                LOG.warnf("User %s lacks write permission for %s %s",
                        securityContext.getUserPrincipal().getName(), method, path);
                abort(requestContext);
            }
        }
    }

    private String extractEmail(ContainerRequestContext requestContext) {
        var principal = requestContext.getSecurityContext().getUserPrincipal();
        if (principal instanceof OidcJwtCallerPrincipal jwtPrincipal) {
            String email = jwtPrincipal.getClaims().getClaimValueAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
            return jwtPrincipal.getClaims().getClaimValueAsString("preferred_username");
        }
        return principal.getName();
    }

    private static void abort(ContainerRequestContext requestContext) {
        requestContext.abortWith(
                Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\":\"Insufficient permissions\"}")
                        .build());
    }
}
