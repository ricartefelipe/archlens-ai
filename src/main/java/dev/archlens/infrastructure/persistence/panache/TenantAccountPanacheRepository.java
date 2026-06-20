package dev.archlens.infrastructure.persistence.panache;

import dev.archlens.infrastructure.persistence.entity.TenantAccountEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TenantAccountPanacheRepository implements PanacheRepositoryBase<TenantAccountEntity, String> {
}
