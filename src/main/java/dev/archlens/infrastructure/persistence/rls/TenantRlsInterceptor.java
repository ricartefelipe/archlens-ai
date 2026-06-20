package dev.archlens.infrastructure.persistence.rls;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@TenantScopedRls
@Interceptor
@Priority(Interceptor.Priority.APPLICATION - 10)
public class TenantRlsInterceptor {

    private final TenantRlsService tenantRlsService;

    @Inject
    public TenantRlsInterceptor(TenantRlsService tenantRlsService) {
        this.tenantRlsService = tenantRlsService;
    }

    @AroundInvoke
    public Object applyTenant(InvocationContext context) throws Exception {
        tenantRlsService.applyCurrentTenant();
        return context.proceed();
    }
}
