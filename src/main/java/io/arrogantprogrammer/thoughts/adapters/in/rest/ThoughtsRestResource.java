package io.arrogantprogrammer.thoughts.adapters.in.rest;

import io.arrogantprogrammer.thoughts.application.ThoughtApplicationService;
import io.arrogantprogrammer.thoughts.application.ThoughtDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/thoughts")
@Produces(MediaType.APPLICATION_JSON)
public class ThoughtsRestResource {

    @Inject
    ThoughtApplicationService thoughtApplicationService;

    @GET
    public List<ThoughtDTO> list(@QueryParam("page") @DefaultValue("0") int page,
                                  @QueryParam("size") @DefaultValue("20") int size) {
        return thoughtApplicationService.list(page, size);
    }

    @GET
    @Path("/random")
    public Response random() {
        return thoughtApplicationService.randomApprovedThought()
                .map(dto -> Response.ok(dto).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") UUID id) {
        return thoughtApplicationService.findById(id)
                .map(dto -> Response.ok(dto).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(ThoughtRequest request, @Context UriInfo uriInfo) {
        ThoughtDTO created = thoughtApplicationService.create(request.content(), request.authorName(), request.authorBio());
        URI location = uriInfo.getAbsolutePathBuilder().path(created.id().toString()).build();
        return Response.created(location).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") UUID id, ThoughtRequest request) {
        return thoughtApplicationService.update(id, request.content(), request.authorName(), request.authorBio())
                .map(dto -> Response.ok(dto).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        boolean deleted = thoughtApplicationService.delete(id);
        return deleted ? Response.noContent().build() : Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path("/{id}/thumbs-up")
    public Response thumbsUp(@PathParam("id") UUID id) {
        return thoughtApplicationService.thumbsUp(id)
                .map(dto -> Response.ok(dto).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("/{id}/thumbs-down")
    public Response thumbsDown(@PathParam("id") UUID id) {
        return thoughtApplicationService.thumbsDown(id)
                .map(dto -> Response.ok(dto).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
