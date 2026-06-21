package dev.archlens.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.domain.model.OrgMember;

public interface OrgMemberRepositoryPort {

    OrgMember save(OrgMember member);

    Optional<OrgMember> findById(UUID id);

    Optional<OrgMember> findByIdAndTenantId(UUID id, String tenantId);

    Optional<OrgMember> findByTenantIdAndEmail(String tenantId, String email);

    List<OrgMember> findAllByTenantId(String tenantId);

    void deleteByIdAndTenantId(UUID id, String tenantId);
}
