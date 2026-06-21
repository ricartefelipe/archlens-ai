package dev.archlens.interfaces.rest.filter;

import java.io.IOException;

import dev.archlens.interfaces.rest.context.RequestScopedTenantProvider;
import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

@Provider
@Priority(Priorities.AUTHENTICATION + 1)
public class TenantFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(TenantFilter.class);
    private static final String HEADER_NAME = "X-Tenant-Id";
    private static final String TENANT_ID_ATTRIBUTE = "tenant_id";
    private static final String DEFAULT_TENANT = "default";

    private final RequestScopedTenantProvider tenantProvider;
    private final SecurityIdentity securityIdentity;

    @Inject
    public TenantFilter(RequestScopedTenantProvider tenantProvider, SecurityIdentity securityIdentity) {
        this.tenantProvider = tenantProvider;
        this.securityIdentity = securityIdentity;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String tenantId = extractTenantFromJwt(requestContext);

        if (tenantId == null || tenantId.isBlank()) {
            tenantId = extractTenantFromSecurityIdentity();
        }

        if (tenantId == null || tenantId.isBlank()) {
            tenantId = requestContext.getHeaderString(HEADER_NAME);
        } else {
            String headerTenant = requestContext.getHeaderString(HEADER_NAME);
            if (headerTenant != null && !headerTenant.isBlank() && !headerTenant.equals(tenantId)) {
                LOG.warnf("Ignoring spoofed %s header (JWT/API key tenant=%s, header=%s)",
                        HEADER_NAME, tenantId, headerTenant);
            }
        }

        if (tenantId == null || tenantId.isBlank()) {
            LOG.warnf("No tenant found in JWT or %s header, falling back to '%s'", HEADER_NAME, DEFAULT_TENANT);
            tenantId = DEFAULT_TENANT;
        }

        tenantProvider.setTenantId(tenantId);
        MDC.put("tenantId", tenantId);
    }

    private String extractTenantFromJwt(ContainerRequestContext requestContext) {
        var securityContext = requestContext.getSecurityContext();
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return null;
        }

        var principal = securityContext.getUserPrincipal();
        if (principal instanceof OidcJwtCallerPrincipal jwtPrincipal) {
            try {
                return jwtPrincipal.getClaims().getClaimValueAsString(TENANT_ID_ATTRIBUTE);
            } catch (Exception e) {
                LOG.debugf("Could not extract tenant_id from JWT: %s", e.getMessage());
            }
        }

        return null;
    }

    private String extractTenantFromSecurityIdentity() {
        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            return null;
        }
        Object tenantId = securityIdentity.getAttribute(TENANT_ID_ATTRIBUTE);
        return tenantId != null ? tenantId.toString() : null;
    }
}
