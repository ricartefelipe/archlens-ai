package dev.archlens.infrastructure.persistence.mapper;

import dev.archlens.domain.model.OrgInvite;
import dev.archlens.domain.model.OrgMemberRole;
import dev.archlens.infrastructure.persistence.entity.OrgInviteEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrgInvitePersistenceMapper {

    public OrgInviteEntity toEntity(OrgInvite invite) {
        OrgInviteEntity entity = new OrgInviteEntity();
        entity.id = invite.getId();
        entity.tenantId = invite.getTenantId();
        entity.email = invite.getEmail();
        entity.role = invite.getRole().name();
        entity.tokenHash = invite.getTokenHash();
        entity.expiresAt = invite.getExpiresAt();
        entity.acceptedAt = invite.getAcceptedAt();
        entity.createdAt = invite.getCreatedAt();
        return entity;
    }

    public OrgInvite toDomain(OrgInviteEntity entity) {
        OrgInvite invite = new OrgInvite();
        invite.setId(entity.id);
        invite.setTenantId(entity.tenantId);
        invite.setEmail(entity.email);
        invite.setRole(OrgMemberRole.valueOf(entity.role));
        invite.setTokenHash(entity.tokenHash);
        invite.setExpiresAt(entity.expiresAt);
        invite.setAcceptedAt(entity.acceptedAt);
        invite.setCreatedAt(entity.createdAt);
        return invite;
    }
}
