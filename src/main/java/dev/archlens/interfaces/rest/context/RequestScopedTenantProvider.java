package dev.archlens.interfaces.rest.context;

import dev.archlens.application.port.out.TenantProvider;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RequestScopedTenantProvider implements TenantProvider {

    private String tenantId;

    @Override
    public String getCurrentTenantId() {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID has not been set for this request");
        }
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
