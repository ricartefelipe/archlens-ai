package dev.archlens.interfaces.rest.filter;

import java.io.IOException;
import java.util.Set;

import jakarta.annotation.Priority;
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

        if (WRITE_METHODS.contains(method)) {
            boolean hasWriteRole = securityContext.isUserInRole("admin")
                    || securityContext.isUserInRole("architect");

            if (!hasWriteRole) {
                LOG.warnf("User %s lacks write permission for %s %s",
                        securityContext.getUserPrincipal().getName(), method, path);
                requestContext.abortWith(
                        Response.status(Response.Status.FORBIDDEN)
                                .entity("{\"error\":\"Insufficient permissions\"}")
                                .build());
            }
        }
    }
}
