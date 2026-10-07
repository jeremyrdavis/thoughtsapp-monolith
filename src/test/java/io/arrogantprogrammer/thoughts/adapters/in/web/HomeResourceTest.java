package io.arrogantprogrammer.thoughts.adapters.in.web;

import io.arrogantprogrammer.thoughts.application.ThoughtApplicationService;
import io.arrogantprogrammer.thoughts.application.ThoughtDTO;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests for the htmx-driven Qute pages served by {@code HomeResource}: the home page
 * and the voting partials. The application service is mocked so each test isolates
 * template rendering from business logic.
 */
@QuarkusTest
class HomeResourceTest {

    @InjectMock
    ThoughtApplicationService thoughtApplicationService;

    private static final ThoughtDTO SAMPLE = new ThoughtDTO(
            UUID.randomUUID(), "A thought worth sharing with everyone.", "Ada Lovelace", "Mathematician", 3, 1, "APPROVED");

    // Most tests want a thought available by default; individual tests override this stub as needed.
    @BeforeEach
    void setUp() {
        Mockito.when(thoughtApplicationService.randomApprovedThought()).thenReturn(Optional.of(SAMPLE));
    }

    // GET / renders the full page with the random thought's content, author, and card markup.
    @Test
    void indexRendersThoughtCard() {
        given()
            .when().get("/")
            .then()
                .statusCode(200)
                .body(containsString("A thought worth sharing with everyone."))
                .body(containsString("Ada Lovelace"))
                .body(containsString("thought-card"));
    }

    // GET / falls back to a friendly empty-state message rather than erroring when no
    // approved thought exists to show.
    @Test
    void indexShowsFriendlyEmptyStateWhenNoThoughtsExist() {
        Mockito.when(thoughtApplicationService.randomApprovedThought()).thenReturn(Optional.empty());

        given()
            .when().get("/")
            .then()
                .statusCode(200)
                .body(containsString("No thoughts to show yet"));
    }

    // GET /thoughts/random renders only the card fragment, for htmx to swap in without a full page reload.
    @Test
    void randomCardEndpointRendersJustTheCardFragment() {
        given()
            .when().get("/thoughts/random")
            .then()
                .statusCode(200)
                .body(containsString("A thought worth sharing with everyone."));
    }

    // POST /thoughts/{id}/thumbs-up re-renders the card fragment reflecting the new vote.
    @Test
    void thumbsUpRendersUpdatedCard() {
        ThoughtDTO voted = new ThoughtDTO(SAMPLE.id(), SAMPLE.content(), SAMPLE.authorName(), SAMPLE.authorBio(), 4, 1, SAMPLE.status());
        Mockito.when(thoughtApplicationService.thumbsUp(SAMPLE.id())).thenReturn(Optional.of(voted));

        given()
            .when().post("/thoughts/" + SAMPLE.id() + "/thumbs-up")
            .then()
                .statusCode(200)
                .body(containsString("A thought worth sharing with everyone."));
    }

    // POST /thoughts/{id}/thumbs-down re-renders the card fragment reflecting the new vote.
    @Test
    void thumbsDownRendersUpdatedCard() {
        ThoughtDTO voted = new ThoughtDTO(SAMPLE.id(), SAMPLE.content(), SAMPLE.authorName(), SAMPLE.authorBio(), 3, 2, SAMPLE.status());
        Mockito.when(thoughtApplicationService.thumbsDown(SAMPLE.id())).thenReturn(Optional.of(voted));

        given()
            .when().post("/thoughts/" + SAMPLE.id() + "/thumbs-down")
            .then()
                .statusCode(200)
                .body(containsString("A thought worth sharing with everyone."));
    }

    // POST /thoughts/{id}/thumbs-up maps an empty Optional (thought not found) to 404.
    @Test
    void thumbsUpOnMissingThoughtReturns404() {
        UUID missing = UUID.randomUUID();
        Mockito.when(thoughtApplicationService.thumbsUp(missing)).thenReturn(Optional.empty());

        given()
            .when().post("/thoughts/" + missing + "/thumbs-up")
            .then()
                .statusCode(404);
    }

    // POST /thoughts/{id}/thumbs-down maps an empty Optional (thought not approved) to 404.
    @Test
    void thumbsDownOnNonApprovedThoughtReturns404() {
        UUID notApproved = UUID.randomUUID();
        Mockito.when(thoughtApplicationService.thumbsDown(notApproved)).thenReturn(Optional.empty());

        given()
            .when().post("/thoughts/" + notApproved + "/thumbs-down")
            .then()
                .statusCode(404);
    }
}
