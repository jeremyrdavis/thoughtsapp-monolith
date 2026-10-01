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

@QuarkusTest
class HomeResourceTest {

    @InjectMock
    ThoughtApplicationService thoughtApplicationService;

    private static final ThoughtDTO SAMPLE = new ThoughtDTO(
            UUID.randomUUID(), "A thought worth sharing with everyone.", "Ada Lovelace", "Mathematician", 3, 1);

    @BeforeEach
    void setUp() {
        Mockito.when(thoughtApplicationService.randomApprovedThought()).thenReturn(Optional.of(SAMPLE));
    }

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

    @Test
    void indexShowsFriendlyEmptyStateWhenNoThoughtsExist() {
        Mockito.when(thoughtApplicationService.randomApprovedThought()).thenReturn(Optional.empty());

        given()
            .when().get("/")
            .then()
                .statusCode(200)
                .body(containsString("No thoughts to show yet"));
    }

    @Test
    void randomCardEndpointRendersJustTheCardFragment() {
        given()
            .when().get("/thoughts/random")
            .then()
                .statusCode(200)
                .body(containsString("A thought worth sharing with everyone."));
    }

    @Test
    void thumbsUpRendersUpdatedCard() {
        ThoughtDTO voted = new ThoughtDTO(SAMPLE.id(), SAMPLE.content(), SAMPLE.authorName(), SAMPLE.authorBio(), 4, 1);
        Mockito.when(thoughtApplicationService.thumbsUp(SAMPLE.id())).thenReturn(Optional.of(voted));

        given()
            .when().post("/thoughts/" + SAMPLE.id() + "/thumbs-up")
            .then()
                .statusCode(200)
                .body(containsString("A thought worth sharing with everyone."));
    }

    @Test
    void thumbsDownRendersUpdatedCard() {
        ThoughtDTO voted = new ThoughtDTO(SAMPLE.id(), SAMPLE.content(), SAMPLE.authorName(), SAMPLE.authorBio(), 3, 2);
        Mockito.when(thoughtApplicationService.thumbsDown(SAMPLE.id())).thenReturn(Optional.of(voted));

        given()
            .when().post("/thoughts/" + SAMPLE.id() + "/thumbs-down")
            .then()
                .statusCode(200)
                .body(containsString("A thought worth sharing with everyone."));
    }

    @Test
    void thumbsUpOnMissingThoughtReturns404() {
        UUID missing = UUID.randomUUID();
        Mockito.when(thoughtApplicationService.thumbsUp(missing)).thenReturn(Optional.empty());

        given()
            .when().post("/thoughts/" + missing + "/thumbs-up")
            .then()
                .statusCode(404);
    }

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
