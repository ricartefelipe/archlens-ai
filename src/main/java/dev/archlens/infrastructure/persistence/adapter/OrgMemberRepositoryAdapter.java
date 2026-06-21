package dev.archlens.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.application.port.out.OrgMemberRepositoryPort;
import dev.archlens.domain.model.OrgMember;
import dev.archlens.infrastructure.persistence.entity.OrgMemberEntity;
import dev.archlens.infrastructure.persistence.mapper.OrgMemberPersistenceMapper;
import dev.archlens.infrastructure.persistence.panache.OrgMemberPanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OrgMemberRepositoryAdapter implements OrgMemberRepositoryPort {

    private final OrgMemberPanacheRepository repository;
    private final OrgMemberPersistenceMapper mapper;

    @Inject
    public OrgMemberRepositoryAdapter(OrgMemberPanacheRepository repository,
                                        OrgMemberPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public OrgMember save(OrgMember member) {
        OrgMemberEntity entity = mapper.toEntity(member);
        Optional<OrgMemberEntity> existing = repository.findByIdOptional(member.getId());
        if (existing.isPresent()) {
            OrgMemberEntity current = existing.get();
            current.email = entity.email;
            current.role = entity.role;
            current.status = entity.status;
            current.updatedAt = entity.updatedAt;
            return mapper.toDomain(current);
        }
        repository.persist(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<OrgMember> findById(UUID id) {
        return repository.findByIdOptional(id).map(mapper::toDomain);
    }

    @Override
    public Optional<OrgMember> findByIdAndTenantId(UUID id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId).map(mapper::toDomain);
    }

    @Override
    public Optional<OrgMember> findByTenantIdAndEmail(String tenantId, String email) {
        return repository.findByTenantIdAndEmail(tenantId, email).map(mapper::toDomain);
    }

    @Override
    public List<OrgMember> findAllByTenantId(String tenantId) {
        return repository.findAllByTenantId(tenantId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByIdAndTenantId(UUID id, String tenantId) {
        repository.deleteByIdAndTenantId(id, tenantId);
    }
}
