package dev.archlens.infrastructure.persistence.panache;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.infrastructure.persistence.entity.OrgInviteEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrgInvitePanacheRepository implements PanacheRepositoryBase<OrgInviteEntity, UUID> {

    public Optional<OrgInviteEntity> findByTokenHash(String tokenHash) {
        return find("tokenHash", tokenHash).firstResultOptional();
    }

    public List<OrgInviteEntity> findPendingByTenantId(String tenantId) {
        return list("tenantId = ?1 and acceptedAt is null", tenantId);
    }
}
