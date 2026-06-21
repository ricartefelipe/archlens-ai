package dev.archlens.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.application.port.out.OrgInviteRepositoryPort;
import dev.archlens.domain.model.OrgInvite;
import dev.archlens.infrastructure.persistence.entity.OrgInviteEntity;
import dev.archlens.infrastructure.persistence.mapper.OrgInvitePersistenceMapper;
import dev.archlens.infrastructure.persistence.panache.OrgInvitePanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OrgInviteRepositoryAdapter implements OrgInviteRepositoryPort {

    private final OrgInvitePanacheRepository repository;
    private final OrgInvitePersistenceMapper mapper;

    @Inject
    public OrgInviteRepositoryAdapter(OrgInvitePanacheRepository repository,
                                        OrgInvitePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public OrgInvite save(OrgInvite invite) {
        OrgInviteEntity entity = mapper.toEntity(invite);
        Optional<OrgInviteEntity> existing = repository.findByIdOptional(invite.getId());
        if (existing.isPresent()) {
            OrgInviteEntity current = existing.get();
            current.acceptedAt = entity.acceptedAt;
            return mapper.toDomain(current);
        }
        repository.persist(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<OrgInvite> findById(UUID id) {
        return repository.findByIdOptional(id).map(mapper::toDomain);
    }

    @Override
    public Optional<OrgInvite> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public List<OrgInvite> findPendingByTenantId(String tenantId) {
        return repository.findPendingByTenantId(tenantId).stream().map(mapper::toDomain).toList();
    }
}
