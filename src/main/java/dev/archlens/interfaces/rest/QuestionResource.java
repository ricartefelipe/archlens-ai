package dev.archlens.interfaces.rest;

import java.util.UUID;

import dev.archlens.application.port.in.AskQuestionUseCase;
import dev.archlens.domain.model.Question;
import dev.archlens.interfaces.rest.dto.request.AskQuestionRequest;
import dev.archlens.interfaces.rest.dto.response.QuestionResponse;
import dev.archlens.interfaces.rest.mapper.QuestionDtoMapper;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path("/v1/projects/{projectId}/analyses/{analysisId}/questions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QuestionResource {

    private static final Logger LOG = Logger.getLogger(QuestionResource.class);

    private final AskQuestionUseCase askUseCase;
    private final QuestionDtoMapper mapper;

    @Inject
    public QuestionResource(AskQuestionUseCase askUseCase,
                            QuestionDtoMapper mapper) {
        this.askUseCase = askUseCase;
        this.mapper = mapper;
    }

    @POST
    public Response ask(@PathParam("projectId") UUID projectId,
                        @PathParam("analysisId") UUID analysisId,
                        @Valid AskQuestionRequest request) {
        LOG.infof("Asking question on analysis: analysisId=%s", analysisId);
        Question question = askUseCase.ask(analysisId, request.question());
        QuestionResponse response = mapper.toResponse(question);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}
