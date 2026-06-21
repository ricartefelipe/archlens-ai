package dev.archlens.interfaces.rest;

import java.util.List;
import java.util.UUID;

import dev.archlens.application.port.out.TenantProvider;
import dev.archlens.application.service.OrgService;
import dev.archlens.application.service.OrgService.InviteCreated;
import dev.archlens.domain.model.OrgMember;
import dev.archlens.domain.model.OrgMemberRole;
import dev.archlens.interfaces.rest.dto.request.AcceptOrgInviteRequest;
import dev.archlens.interfaces.rest.dto.request.CreateOrgInviteRequest;
import dev.archlens.interfaces.rest.dto.request.CreateOrgMemberRequest;
import dev.archlens.interfaces.rest.dto.response.OrgInviteCreatedResponse;
import dev.archlens.interfaces.rest.dto.response.OrgInviteResponse;
import dev.archlens.interfaces.rest.dto.response.OrgMemberResponse;
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

@Path("/v1/org")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrgResource {

    private final OrgService orgService;
    private final TenantProvider tenantProvider;
    private final PlatformDtoMapper mapper;

    @Inject
    public OrgResource(OrgService orgService, TenantProvider tenantProvider, PlatformDtoMapper mapper) {
        this.orgService = orgService;
        this.tenantProvider = tenantProvider;
        this.mapper = mapper;
    }

    @GET
    @Path("/members")
    public List<OrgMemberResponse> listMembers() {
        String tenantId = tenantProvider.getCurrentTenantId();
        return mapper.toMemberResponseList(orgService.listMembers(tenantId));
    }

    @POST
    @Path("/members")
    public Response addMember(@Valid CreateOrgMemberRequest request) {
        String tenantId = tenantProvider.getCurrentTenantId();
        OrgMemberRole role = OrgMemberRole.valueOf(request.role());
        OrgMember member = orgService.addMember(tenantId, request.email(), role);
        return Response.status(Response.Status.CREATED)
                .entity(mapper.toMemberResponse(member))
                .build();
    }

    @DELETE
    @Path("/members/{memberId}")
    public Response removeMember(@PathParam("memberId") UUID memberId) {
        String tenantId = tenantProvider.getCurrentTenantId();
        orgService.removeMember(tenantId, memberId);
        return Response.noContent().build();
    }

    @GET
    @Path("/invites")
    public List<OrgInviteResponse> listInvites() {
        String tenantId = tenantProvider.getCurrentTenantId();
        return mapper.toInviteResponseList(orgService.listPendingInvites(tenantId));
    }

    @POST
    @Path("/invites")
    public Response createInvite(@Valid CreateOrgInviteRequest request) {
        String tenantId = tenantProvider.getCurrentTenantId();
        OrgMemberRole role = OrgMemberRole.valueOf(request.role());
        InviteCreated created = orgService.createInvite(tenantId, request.email(), role);
        OrgInviteCreatedResponse body = mapper.toInviteCreatedResponse(created.invite(), created.token());
        return Response.status(Response.Status.CREATED).entity(body).build();
    }

    @POST
    @Path("/invites/accept")
    public Response acceptInvite(@Valid AcceptOrgInviteRequest request) {
        OrgMember member = orgService.acceptInvite(request.token(), request.email());
        return Response.ok(mapper.toMemberResponse(member)).build();
    }
}
