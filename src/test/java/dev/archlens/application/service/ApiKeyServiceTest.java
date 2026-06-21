package dev.archlens.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.archlens.application.port.out.ApiKeyRepositoryPort;
import dev.archlens.domain.exception.ApiKeyNotFoundException;
import dev.archlens.domain.model.ApiKeyRecord;
import dev.archlens.infrastructure.security.SecureTokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiKeyServiceTest {

    private InMemoryApiKeyRepository repository;
    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryApiKeyRepository();
        apiKeyService = new ApiKeyService(repository, new SecureTokenHasher());
    }

    @Test
    @DisplayName("criação retorna chave plain text uma vez")
    void createReturnsPlainKeyOnce() {
        ApiKeyService.CreatedApiKey created = apiKeyService.create("tenant-a", "CI", "read,write");

        assertNotNull(created.plainKey());
        assertTrue(created.plainKey().startsWith("alk_"));
        assertEquals("read,write", created.record().getScopes());
    }

    @Test
    @DisplayName("autenticação válida retorna registro ativo")
    void authenticateValidKey() {
        ApiKeyService.CreatedApiKey created = apiKeyService.create("tenant-a", "bot", "write");
        Optional<ApiKeyRecord> auth = apiKeyService.authenticate(created.plainKey());

        assertTrue(auth.isPresent());
        assertEquals("tenant-a", auth.get().getTenantId());
        assertNotNull(auth.get().getLastUsedAt());
    }

    @Test
    @DisplayName("revogação impede autenticação")
    void revokeBlocksAuthentication() {
        ApiKeyService.CreatedApiKey created = apiKeyService.create("tenant-a", "bot", "read");
        apiKeyService.revoke("tenant-a", created.record().getId());

        assertTrue(apiKeyService.authenticate(created.plainKey()).isEmpty());
    }

    @Test
    @DisplayName("revogar chave inexistente lança exceção")
    void revokeMissingKeyThrows() {
        assertThrows(ApiKeyNotFoundException.class,
                () -> apiKeyService.revoke("tenant-a", UUID.randomUUID()));
    }

    @Test
    @DisplayName("listagem retorna chaves do tenant")
    void listReturnsTenantKeys() {
        apiKeyService.create("tenant-a", "k1", "read");
        apiKeyService.create("tenant-a", "k2", "read");
        apiKeyService.create("tenant-b", "k3", "read");

        List<ApiKeyRecord> keys = apiKeyService.list("tenant-a");
        assertEquals(2, keys.size());
    }

    @Test
    @DisplayName("scope write habilita role architect")
    void writeScopeDetected() {
        ApiKeyService.CreatedApiKey created = apiKeyService.create("tenant-a", "writer", "read,write");
        assertTrue(created.record().hasWriteScope());

        ApiKeyService.CreatedApiKey readOnly = apiKeyService.create("tenant-a", "reader", "read");
        assertFalse(readOnly.record().hasWriteScope());
    }

    private static final class InMemoryApiKeyRepository implements ApiKeyRepositoryPort {
        private final List<ApiKeyRecord> store = new ArrayList<>();

        @Override
        public ApiKeyRecord save(ApiKeyRecord record) {
            store.removeIf(r -> r.getId().equals(record.getId()));
            store.add(record);
            return record;
        }

        @Override
        public Optional<ApiKeyRecord> findById(UUID id) {
            return store.stream().filter(r -> r.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<ApiKeyRecord> findByIdAndTenantId(UUID id, String tenantId) {
            return store.stream()
                    .filter(r -> r.getId().equals(id) && tenantId.equals(r.getTenantId()))
                    .findFirst();
        }

        @Override
        public Optional<ApiKeyRecord> findByKeyHash(String keyHash) {
            return store.stream().filter(r -> keyHash.equals(r.getKeyHash())).findFirst();
        }

        @Override
        public List<ApiKeyRecord> findAllByTenantId(String tenantId) {
            return store.stream().filter(r -> tenantId.equals(r.getTenantId())).toList();
        }
    }
}
