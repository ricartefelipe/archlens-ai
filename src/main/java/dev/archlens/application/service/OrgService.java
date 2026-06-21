package dev.archlens.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import dev.archlens.application.port.out.OrgInviteRepositoryPort;
import dev.archlens.application.port.out.OrgMemberRepositoryPort;
import dev.archlens.domain.exception.InvalidOrgInviteException;
import dev.archlens.domain.exception.OrgMemberNotFoundException;
import dev.archlens.domain.model.OrgInvite;
import dev.archlens.domain.model.OrgMember;
import dev.archlens.domain.model.OrgMemberRole;
import dev.archlens.domain.model.OrgMemberStatus;
import dev.archlens.infrastructure.persistence.rls.TenantScopedRls;
import dev.archlens.infrastructure.security.SecureTokenHasher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@TenantScopedRls
@ApplicationScoped
public class OrgService {

    private final OrgMemberRepositoryPort memberRepository;
    private final OrgInviteRepositoryPort inviteRepository;
    private final SecureTokenHasher tokenHasher;

    @Inject
    public OrgService(OrgMemberRepositoryPort memberRepository,
                      OrgInviteRepositoryPort inviteRepository,
                      SecureTokenHasher tokenHasher) {
        this.memberRepository = memberRepository;
        this.inviteRepository = inviteRepository;
        this.tokenHasher = tokenHasher;
    }

    public List<OrgMember> listMembers(String tenantId) {
        return memberRepository.findAllByTenantId(tenantId);
    }

    public List<OrgInvite> listPendingInvites(String tenantId) {
        return inviteRepository.findPendingByTenantId(tenantId);
    }

    @Transactional
    public OrgMember addMember(String tenantId, String email, OrgMemberRole role) {
        memberRepository.findByTenantIdAndEmail(tenantId, email).ifPresent(existing -> {
            if (existing.getStatus() != OrgMemberStatus.REMOVED) {
                throw new IllegalArgumentException("Member already exists: " + email);
            }
        });

        Instant now = Instant.now();
        OrgMember member = new OrgMember();
        member.setId(UUID.randomUUID());
        member.setTenantId(tenantId);
        member.setEmail(email.toLowerCase().trim());
        member.setRole(role);
        member.setStatus(OrgMemberStatus.ACTIVE);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        return memberRepository.save(member);
    }

    @Transactional
    public void removeMember(String tenantId, UUID memberId) {
        OrgMember member = memberRepository.findByIdAndTenantId(memberId, tenantId)
                .orElseThrow(() -> new OrgMemberNotFoundException(memberId));
        member.setStatus(OrgMemberStatus.REMOVED);
        member.setUpdatedAt(Instant.now());
        memberRepository.save(member);
    }

    public record InviteCreated(OrgInvite invite, String token) {
    }

    @Transactional
    public InviteCreated createInvite(String tenantId, String email, OrgMemberRole role) {
        String token = tokenHasher.randomToken(32);
        String tokenHash = tokenHasher.sha256(token);
        Instant now = Instant.now();

        OrgInvite invite = new OrgInvite();
        invite.setId(UUID.randomUUID());
        invite.setTenantId(tenantId);
        invite.setEmail(email.toLowerCase().trim());
        invite.setRole(role);
        invite.setTokenHash(tokenHash);
        invite.setExpiresAt(now.plus(7, ChronoUnit.DAYS));
        invite.setCreatedAt(now);

        OrgInvite saved = inviteRepository.save(invite);
        return new InviteCreated(saved, token);
    }

    @Transactional
    public OrgMember acceptInvite(String token, String email) {
        String tokenHash = tokenHasher.sha256(token);
        OrgInvite invite = inviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidOrgInviteException("Invite token inválido"));

        if (invite.isAccepted()) {
            throw new InvalidOrgInviteException("Invite já aceito");
        }
        if (invite.isExpired()) {
            throw new InvalidOrgInviteException("Invite expirado");
        }

        String normalizedEmail = email.toLowerCase().trim();
        if (!invite.getEmail().equals(normalizedEmail)) {
            throw new InvalidOrgInviteException("Email não corresponde ao convite");
        }

        invite.setAcceptedAt(Instant.now());
        inviteRepository.save(invite);

        return memberRepository.findByTenantIdAndEmail(invite.getTenantId(), normalizedEmail)
                .map(existing -> {
                    existing.setRole(invite.getRole());
                    existing.setStatus(OrgMemberStatus.ACTIVE);
                    existing.setUpdatedAt(Instant.now());
                    return memberRepository.save(existing);
                })
                .orElseGet(() -> addMember(invite.getTenantId(), normalizedEmail, invite.getRole()));
    }

    public boolean isOrgAdmin(String tenantId, String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return memberRepository.findByTenantIdAndEmail(tenantId, email.toLowerCase().trim())
                .map(m -> m.getStatus() == OrgMemberStatus.ACTIVE
                        && m.getRole() == OrgMemberRole.ORG_ADMIN)
                .orElse(false);
    }

    @Transactional
    public OrgMember ensureBootstrapAdmin(String tenantId, String email) {
        boolean hasActiveMember = memberRepository.findAllByTenantId(tenantId).stream()
                .anyMatch(m -> m.getStatus() == OrgMemberStatus.ACTIVE);
        if (hasActiveMember) {
            return null;
        }

        String bootstrapEmail = (email != null && !email.isBlank())
                ? email.toLowerCase().trim()
                : "admin@" + tenantId + ".local";
        return addMember(tenantId, bootstrapEmail, OrgMemberRole.ORG_ADMIN);
    }
}
