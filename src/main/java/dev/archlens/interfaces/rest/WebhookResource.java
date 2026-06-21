package dev.archlens.interfaces.rest;

import java.util.List;
import java.util.UUID;

import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.application.service.WebhookService;
import dev.archlens.application.service.WebhookService.CreatedWebhook;
import dev.archlens.domain.model.TenantWebhook;
import dev.archlens.interfaces.rest.dto.request.CreateWebhookRequest;
import dev.archlens.interfaces.rest.dto.request.UpdateWebhookRequest;
import dev.archlens.interfaces.rest.dto.response.WebhookCreatedResponse;
import dev.archlens.interfaces.rest.dto.response.WebhookResponse;
import dev.archlens.interfaces.rest.mapper.PlatformDtoMapper;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/account/webhooks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WebhookResource {

    private final WebhookService webhookService;
    private final TenantProvider tenantProvider;
    private final PlatformDtoMapper mapper;

    @Inject
    public WebhookResource(WebhookService webhookService,
                           TenantProvider tenantProvider,
                           PlatformDtoMapper mapper) {
        this.webhookService = webhookService;
        this.tenantProvider = tenantProvider;
        this.mapper = mapper;
    }

    @GET
    public List<WebhookResponse> list() {
        String tenantId = tenantProvider.getCurrentTenantId();
        return mapper.toWebhookResponseList(webhookService.list(tenantId));
    }

    @POST
    public Response create(@Valid CreateWebhookRequest request) {
        String tenantId = tenantProvider.getCurrentTenantId();
        CreatedWebhook created = webhookService.create(tenantId, request.url(), request.events());
        WebhookCreatedResponse body = mapper.toWebhookCreatedResponse(created.webhook(), created.secret());
        return Response.status(Response.Status.CREATED).entity(body).build();
    }

    @PUT
    @Path("/{webhookId}")
    public WebhookResponse update(@PathParam("webhookId") UUID webhookId,
                                  @Valid UpdateWebhookRequest request) {
        String tenantId = tenantProvider.getCurrentTenantId();
        TenantWebhook webhook = webhookService.update(
                tenantId, webhookId, request.url(), request.events(), request.enabled());
        return mapper.toWebhookResponse(webhook);
    }

    @DELETE
    @Path("/{webhookId}")
    public Response delete(@PathParam("webhookId") UUID webhookId) {
        String tenantId = tenantProvider.getCurrentTenantId();
        webhookService.delete(tenantId, webhookId);
        return Response.noContent().build();
    }
}
