package dev.archlens.interfaces.rest;

import java.util.List;
import java.util.UUID;

import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.application.service.ApiKeyService;
import dev.archlens.application.service.ApiKeyService.CreatedApiKey;
import dev.archlens.interfaces.rest.dto.request.CreateApiKeyRequest;
import dev.archlens.interfaces.rest.dto.response.ApiKeyCreatedResponse;
import dev.archlens.interfaces.rest.dto.response.ApiKeyResponse;
import dev.archlens.interfaces.rest.mapper.PlatformDtoMapper;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/account/api-keys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApiKeyResource {

    private final ApiKeyService apiKeyService;
    private final TenantProvider tenantProvider;
    private final PlatformDtoMapper mapper;

    @Inject
    public ApiKeyResource(ApiKeyService apiKeyService,
                          TenantProvider tenantProvider,
                          PlatformDtoMapper mapper) {
        this.apiKeyService = apiKeyService;
        this.tenantProvider = tenantProvider;
        this.mapper = mapper;
    }

    @GET
    public List<ApiKeyResponse> list() {
        String tenantId = tenantProvider.getCurrentTenantId();
        return mapper.toApiKeyResponseList(apiKeyService.list(tenantId));
    }

    @POST
    public Response create(@Valid CreateApiKeyRequest request) {
        String tenantId = tenantProvider.getCurrentTenantId();
        CreatedApiKey created = apiKeyService.create(tenantId, request.name(), request.scopes());
        ApiKeyCreatedResponse body = mapper.toApiKeyCreatedResponse(created.record(), created.plainKey());
        return Response.status(Response.Status.CREATED).entity(body).build();
    }

    @DELETE
    @Path("/{keyId}")
    public Response revoke(@PathParam("keyId") UUID keyId) {
        String tenantId = tenantProvider.getCurrentTenantId();
        apiKeyService.revoke(tenantId, keyId);
        return Response.noContent().build();
    }
}
