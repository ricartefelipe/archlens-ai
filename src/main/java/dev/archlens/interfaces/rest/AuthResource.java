package dev.archlens.interfaces.rest;

import dev.archlens.application.service.AuthService;
import dev.archlens.interfaces.rest.dto.request.LoginRequest;
import dev.archlens.interfaces.rest.dto.response.LoginResponse;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/public/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class AuthResource {

    private final AuthService authService;

    @Inject
    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @POST
    @Path("/login")
    @PermitAll
    public Uni<Response> login(@Valid LoginRequest request) {
        return Uni.createFrom().item(() -> authService.login(request))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .map(AuthResource::ok);
    }

    private static Response ok(LoginResponse response) {
        return Response.ok(response).build();
    }
}
