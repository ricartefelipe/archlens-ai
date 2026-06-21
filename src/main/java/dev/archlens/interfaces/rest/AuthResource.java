package dev.archlens.interfaces.rest;

import dev.archlens.application.service.AuthService;
import dev.archlens.interfaces.rest.dto.request.LoginRequest;
import dev.archlens.interfaces.rest.dto.response.LoginResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService authService;

    @Inject
    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Response.ok(response).build();
    }
}
