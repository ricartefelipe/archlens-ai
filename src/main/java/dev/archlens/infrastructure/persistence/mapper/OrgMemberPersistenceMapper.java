package dev.archlens.infrastructure.persistence.mapper;

import dev.archlens.domain.model.OrgMember;
import dev.archlens.domain.model.OrgMemberRole;
import dev.archlens.domain.model.OrgMemberStatus;
import dev.archlens.infrastructure.persistence.entity.OrgMemberEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrgMemberPersistenceMapper {

    public OrgMemberEntity toEntity(OrgMember member) {
        OrgMemberEntity entity = new OrgMemberEntity();
        entity.id = member.getId();
        entity.tenantId = member.getTenantId();
        entity.email = member.getEmail();
        entity.role = member.getRole().name();
        entity.status = member.getStatus().name();
        entity.createdAt = member.getCreatedAt();
        entity.updatedAt = member.getUpdatedAt();
        return entity;
    }

    public OrgMember toDomain(OrgMemberEntity entity) {
        OrgMember member = new OrgMember();
        member.setId(entity.id);
        member.setTenantId(entity.tenantId);
        member.setEmail(entity.email);
        member.setRole(OrgMemberRole.valueOf(entity.role));
        member.setStatus(OrgMemberStatus.valueOf(entity.status));
        member.setCreatedAt(entity.createdAt);
        member.setUpdatedAt(entity.updatedAt);
        return member;
    }
}
