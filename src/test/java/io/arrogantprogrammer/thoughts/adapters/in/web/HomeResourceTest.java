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
}
