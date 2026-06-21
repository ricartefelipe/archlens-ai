package dev.archlens.infrastructure.persistence.panache;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.infrastructure.persistence.entity.OrgMemberEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrgMemberPanacheRepository implements PanacheRepositoryBase<OrgMemberEntity, UUID> {

    public List<OrgMemberEntity> findAllByTenantId(String tenantId) {
        return list("tenantId", tenantId);
    }

    public Optional<OrgMemberEntity> findByTenantIdAndEmail(String tenantId, String email) {
        return find("tenantId = ?1 and email = ?2", tenantId, email).firstResultOptional();
    }

    public Optional<OrgMemberEntity> findByIdAndTenantId(UUID id, String tenantId) {
        return find("id = ?1 and tenantId = ?2", id, tenantId).firstResultOptional();
    }

    public void deleteByIdAndTenantId(UUID id, String tenantId) {
        delete("id = ?1 and tenantId = ?2", id, tenantId);
    }
}
