package dev.archlens.infrastructure.security;

import dev.archlens.application.service.ApiKeyService;
import dev.archlens.domain.model.ApiKeyRecord;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism("api-key")
@Priority(401)
public class ApiKeyAuthenticationMechanism
        implements io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism {

    private static final String HEADER_NAME = "X-Api-Key";
    private static final String TENANT_ID_ATTRIBUTE = "tenant_id";

    private final ApiKeyService apiKeyService;

    @Inject
    public ApiKeyAuthenticationMechanism(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context,
                                              io.quarkus.security.identity.IdentityProviderManager identityProviderManager) {
        String apiKey = context.request().getHeader(HEADER_NAME);
        if (apiKey == null || apiKey.isBlank()) {
            return Uni.createFrom().nullItem();
        }

        return Uni.createFrom().item(() -> apiKeyService.authenticate(apiKey))
                .onItem().transform(opt -> opt.map(this::toIdentity).orElse(null));
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().nullItem();
    }

    private SecurityIdentity toIdentity(ApiKeyRecord record) {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(() -> "api-key:" + record.getName());
        builder.addRole("viewer");
        if (record.hasWriteScope()) {
            builder.addRole("architect");
        }
        builder.addAttribute(TENANT_ID_ATTRIBUTE, record.getTenantId());
        return builder.build();
    }
}
