package dev.archlens.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.domain.model.OrgInvite;

public interface OrgInviteRepositoryPort {

    OrgInvite save(OrgInvite invite);

    Optional<OrgInvite> findById(UUID id);

    Optional<OrgInvite> findByTokenHash(String tokenHash);

    List<OrgInvite> findPendingByTenantId(String tenantId);
}
