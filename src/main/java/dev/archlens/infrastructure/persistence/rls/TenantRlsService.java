package dev.archlens.infrastructure.persistence.rls;

import dev.archlens.application.port.out.TenantProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TenantRlsService {

    private static final Logger LOG = Logger.getLogger(TenantRlsService.class);

    private final EntityManager entityManager;
    private final TenantProvider tenantProvider;

    @ConfigProperty(name = "archlens.rls.enabled", defaultValue = "false")
    boolean rlsEnabled;

    @Inject
    public TenantRlsService(EntityManager entityManager, TenantProvider tenantProvider) {
        this.entityManager = entityManager;
        this.tenantProvider = tenantProvider;
    }

    @Transactional
    public void applyCurrentTenant() {
        applyTenant(tenantProvider.getCurrentTenantId());
    }

    @Transactional
    public void applyTenant(String tenantId) {
        if (!rlsEnabled) {
            return;
        }
        entityManager.createNativeQuery("SELECT set_config('app.current_tenant', :tenant, true)")
                .setParameter("tenant", tenantId)
                .getSingleResult();
        LOG.debugf("RLS tenant applied: %s", tenantId);
    }

    @Transactional
    public void bypassForMaintenance() {
        if (!rlsEnabled) {
            return;
        }
        entityManager.createNativeQuery("SELECT set_config('app.rls_bypass', 'on', true)")
                .getSingleResult();
    }
}
