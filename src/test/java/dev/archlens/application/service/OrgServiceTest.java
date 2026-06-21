package dev.archlens.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.application.port.out.OrgInviteRepositoryPort;
import dev.archlens.application.port.out.OrgMemberRepositoryPort;
import dev.archlens.domain.exception.InvalidOrgInviteException;
import dev.archlens.domain.exception.OrgMemberNotFoundException;
import dev.archlens.domain.model.OrgInvite;
import dev.archlens.domain.model.OrgMember;
import dev.archlens.domain.model.OrgMemberRole;
import dev.archlens.domain.model.OrgMemberStatus;
import dev.archlens.infrastructure.security.SecureTokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrgServiceTest {

    private InMemoryOrgMemberRepository memberRepository;
    private InMemoryOrgInviteRepository inviteRepository;
    private OrgService orgService;
    private SecureTokenHasher tokenHasher;

    @BeforeEach
    void setUp() {
        memberRepository = new InMemoryOrgMemberRepository();
        inviteRepository = new InMemoryOrgInviteRepository();
        tokenHasher = new SecureTokenHasher();
        orgService = new OrgService(memberRepository, inviteRepository, tokenHasher);
    }

    @Test
    @DisplayName("adiciona membro ativo ao tenant")
    void addMemberCreatesActiveMember() {
        OrgMember member = orgService.addMember("tenant-a", "user@example.com", OrgMemberRole.ORG_MEMBER);

        assertEquals("user@example.com", member.getEmail());
        assertEquals(OrgMemberStatus.ACTIVE, member.getStatus());
        assertEquals(1, orgService.listMembers("tenant-a").size());
    }

    @Test
    @DisplayName("remove membro marca status REMOVED")
    void removeMemberMarksRemoved() {
        OrgMember member = orgService.addMember("tenant-a", "user@example.com", OrgMemberRole.ORG_MEMBER);
        orgService.removeMember("tenant-a", member.getId());

        OrgMember updated = memberRepository.findById(member.getId()).orElseThrow();
        assertEquals(OrgMemberStatus.REMOVED, updated.getStatus());
    }

    @Test
    @DisplayName("remover membro inexistente lança exceção")
    void removeMissingMemberThrows() {
        assertThrows(OrgMemberNotFoundException.class,
                () -> orgService.removeMember("tenant-a", UUID.randomUUID()));
    }

    @Test
    @DisplayName("criar invite retorna token plain text")
    void createInviteReturnsToken() {
        OrgService.InviteCreated created = orgService.createInvite(
                "tenant-a", "invite@example.com", OrgMemberRole.ORG_VIEWER);

        assertEquals("invite@example.com", created.invite().getEmail());
        assertTrue(created.token().length() > 20);
        assertEquals(1, orgService.listPendingInvites("tenant-a").size());
    }

    @Test
    @DisplayName("aceitar invite cria membro ativo")
    void acceptInviteCreatesMember() {
        OrgService.InviteCreated created = orgService.createInvite(
                "tenant-a", "new@example.com", OrgMemberRole.ORG_ADMIN);

        OrgMember member = orgService.acceptInvite(created.token(), "new@example.com");

        assertEquals(OrgMemberRole.ORG_ADMIN, member.getRole());
        assertEquals(OrgMemberStatus.ACTIVE, member.getStatus());
        assertTrue(orgService.isOrgAdmin("tenant-a", "new@example.com"));
    }

    @Test
    @DisplayName("aceitar invite com email errado falha")
    void acceptInviteWrongEmailFails() {
        OrgService.InviteCreated created = orgService.createInvite(
                "tenant-a", "new@example.com", OrgMemberRole.ORG_MEMBER);

        assertThrows(InvalidOrgInviteException.class,
                () -> orgService.acceptInvite(created.token(), "other@example.com"));
    }

    @Test
    @DisplayName("isOrgAdmin retorna false para membro comum")
    void isOrgAdminFalseForMember() {
        orgService.addMember("tenant-a", "member@example.com", OrgMemberRole.ORG_MEMBER);
        assertTrue(!orgService.isOrgAdmin("tenant-a", "member@example.com"));
    }

    private static final class InMemoryOrgMemberRepository implements OrgMemberRepositoryPort {
        private final List<OrgMember> store = new ArrayList<>();

        @Override
        public OrgMember save(OrgMember member) {
            store.removeIf(m -> m.getId().equals(member.getId()));
            store.add(member);
            return member;
        }

        @Override
        public Optional<OrgMember> findById(UUID id) {
            return store.stream().filter(m -> m.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<OrgMember> findByIdAndTenantId(UUID id, String tenantId) {
            return store.stream()
                    .filter(m -> m.getId().equals(id) && tenantId.equals(m.getTenantId()))
                    .findFirst();
        }

        @Override
        public Optional<OrgMember> findByTenantIdAndEmail(String tenantId, String email) {
            return store.stream()
                    .filter(m -> tenantId.equals(m.getTenantId()) && email.equals(m.getEmail()))
                    .findFirst();
        }

        @Override
        public List<OrgMember> findAllByTenantId(String tenantId) {
            return store.stream().filter(m -> tenantId.equals(m.getTenantId())).toList();
        }

        @Override
        public void deleteByIdAndTenantId(UUID id, String tenantId) {
            store.removeIf(m -> m.getId().equals(id) && tenantId.equals(m.getTenantId()));
        }
    }

    private static final class InMemoryOrgInviteRepository implements OrgInviteRepositoryPort {
        private final List<OrgInvite> store = new ArrayList<>();

        @Override
        public OrgInvite save(OrgInvite invite) {
            store.removeIf(i -> i.getId().equals(invite.getId()));
            store.add(invite);
            return invite;
        }

        @Override
        public Optional<OrgInvite> findById(UUID id) {
            return store.stream().filter(i -> i.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<OrgInvite> findByTokenHash(String tokenHash) {
            return store.stream().filter(i -> tokenHash.equals(i.getTokenHash())).findFirst();
        }

        @Override
        public List<OrgInvite> findPendingByTenantId(String tenantId) {
            return store.stream()
                    .filter(i -> tenantId.equals(i.getTenantId()) && i.getAcceptedAt() == null)
                    .toList();
        }
    }
}
