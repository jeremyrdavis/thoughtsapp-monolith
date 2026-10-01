package io.arrogantprogrammer.thoughts.adapters.in.web;

import io.arrogantprogrammer.thoughts.application.ThoughtApplicationService;
import io.arrogantprogrammer.thoughts.application.ThoughtDTO;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Optional;
import java.util.UUID;

@Path("/")
public class HomeResource {

    @Inject
    ThoughtApplicationService thoughtApplicationService;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(ThoughtDTO thought);

        public static native TemplateInstance card(ThoughtDTO thought);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return Templates.index(thoughtApplicationService.randomApprovedThought().orElse(null));
    }

    @GET
    @Path("/thoughts/random")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance randomCard() {
        return Templates.card(thoughtApplicationService.randomApprovedThought().orElse(null));
    }

    @POST
    @Path("/thoughts/{id}/thumbs-up")
    @Produces(MediaType.TEXT_HTML)
    public Response thumbsUp(@PathParam("id") UUID id) {
        return cardResponse(thoughtApplicationService.thumbsUp(id));
    }

    @POST
    @Path("/thoughts/{id}/thumbs-down")
    @Produces(MediaType.TEXT_HTML)
    public Response thumbsDown(@PathParam("id") UUID id) {
        return cardResponse(thoughtApplicationService.thumbsDown(id));
    }

    private Response cardResponse(Optional<ThoughtDTO> thought) {
        return thought
                .map(dto -> Response.ok(Templates.card(dto)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
