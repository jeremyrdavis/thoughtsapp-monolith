package io.arrogantprogrammer.thoughts.adapters.in.web;

import io.arrogantprogrammer.thoughts.application.ThoughtApplicationService;
import io.arrogantprogrammer.thoughts.application.ThoughtDTO;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
}
