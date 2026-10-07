package io.arrogantprogrammer.thoughts.adapters.in.rest;

import io.arrogantprogrammer.thoughts.application.ThoughtApplicationService;
import io.arrogantprogrammer.thoughts.application.ThoughtDTO;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Tests for {@code /api/thoughts}, the JSON REST API exposed by {@code ThoughtsRestResource}.
 * The application service is mocked so each test isolates the resource's HTTP-layer
 * behavior (status codes, body shape, path/query handling) from business logic.
 */
@QuarkusTest
class ThoughtsRestResourceTest {

    @InjectMock
    ThoughtApplicationService thoughtApplicationService;

    private static final ThoughtDTO SAMPLE = new ThoughtDTO(
            UUID.randomUUID(), "A thought worth sharing with everyone.", "Ada Lovelace", "Mathematician", 3, 1, "APPROVED");

    // GET /api/thoughts with no query params uses page 0, size 20.
    @Test
    void listReturnsThoughtsWithDefaultPaging() {
        Mockito.when(thoughtApplicationService.list(0, 20)).thenReturn(List.of(SAMPLE));

        given()
            .when().get("/api/thoughts")
            .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].authorName", equalTo("Ada Lovelace"));
    }

    // GET /api/thoughts forwards explicit page/size query params straight through to the service.
    @Test
    void listHonoursPageAndSizeQueryParams() {
        Mockito.when(thoughtApplicationService.list(2, 5)).thenReturn(List.of(SAMPLE));

        given()
            .queryParam("page", 2)
            .queryParam("size", 5)
            .when().get("/api/thoughts")
            .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    // GET /api/thoughts/{id} returns the thought's JSON body when the service finds it.
    @Test
    void getReturnsThoughtWhenFound() {
        Mockito.when(thoughtApplicationService.findById(SAMPLE.id())).thenReturn(Optional.of(SAMPLE));

        given()
            .when().get("/api/thoughts/" + SAMPLE.id())
            .then()
                .statusCode(200)
                .body("content", equalTo(SAMPLE.content()));
    }

    // GET /api/thoughts/{id} maps an empty Optional from the service to 404.
    @Test
    void getReturns404WhenMissing() {
        UUID missing = UUID.randomUUID();
        Mockito.when(thoughtApplicationService.findById(missing)).thenReturn(Optional.empty());

        given()
            .when().get("/api/thoughts/" + missing)
            .then()
                .statusCode(404);
    }

    // GET /api/thoughts/random returns the thought the service picked.
    @Test
    void randomReturnsApprovedThought() {
        Mockito.when(thoughtApplicationService.randomApprovedThought()).thenReturn(Optional.of(SAMPLE));

        given()
            .when().get("/api/thoughts/random")
            .then()
                .statusCode(200)
                .body("id", equalTo(SAMPLE.id().toString()));
    }

    // GET /api/thoughts/random maps an empty Optional (no approved thoughts) to 404.
    @Test
    void randomReturns404WhenNoneApproved() {
        Mockito.when(thoughtApplicationService.randomApprovedThought()).thenReturn(Optional.empty());

        given()
            .when().get("/api/thoughts/random")
            .then()
                .statusCode(404);
    }

    // POST /api/thoughts returns 201 with a Location header pointing at the new resource,
    // and the created thought's JSON in the body.
    @Test
    void createReturns201WithLocationHeader() {
        Mockito.when(thoughtApplicationService.create(eq("A new thought worth sharing."), eq("Author"), eq("Bio")))
                .thenReturn(SAMPLE);

        given()
            .contentType("application/json")
            .body(new ThoughtRequest("A new thought worth sharing.", "Author", "Bio"))
            .when().post("/api/thoughts")
            .then()
                .statusCode(201)
                .header("Location", org.hamcrest.Matchers.containsString("/api/thoughts/" + SAMPLE.id()))
                .body("authorName", equalTo("Ada Lovelace"));
    }

    // POST /api/thoughts maps an IllegalArgumentException from domain validation to 400.
    @Test
    void createReturns400OnValidationFailure() {
        Mockito.when(thoughtApplicationService.create(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("content must be between 10 and 500 characters"));

        given()
            .contentType("application/json")
            .body(new ThoughtRequest("short", "Author", null))
            .when().post("/api/thoughts")
            .then()
                .statusCode(400);
    }

    // PUT /api/thoughts/{id} returns the updated thought's JSON when the service finds it.
    @Test
    void updateReturnsUpdatedThoughtWhenFound() {
        Mockito.when(thoughtApplicationService.update(eq(SAMPLE.id()), any(), any(), any()))
                .thenReturn(Optional.of(SAMPLE));

        given()
            .contentType("application/json")
            .body(new ThoughtRequest("An edited thought worth sharing.", "Author", "Bio"))
            .when().put("/api/thoughts/" + SAMPLE.id())
            .then()
                .statusCode(200)
                .body("authorName", equalTo("Ada Lovelace"));
    }

    // PUT /api/thoughts/{id} maps an empty Optional from the service to 404.
    @Test
    void updateReturns404WhenMissing() {
        UUID missing = UUID.randomUUID();
        Mockito.when(thoughtApplicationService.update(eq(missing), any(), any(), any()))
                .thenReturn(Optional.empty());

        given()
            .contentType("application/json")
            .body(new ThoughtRequest("An edited thought worth sharing.", "Author", "Bio"))
            .when().put("/api/thoughts/" + missing)
            .then()
                .statusCode(404);
    }

    // DELETE /api/thoughts/{id} returns 204 with no body when the service reports success.
    @Test
    void deleteReturns204WhenDeleted() {
        Mockito.when(thoughtApplicationService.delete(SAMPLE.id())).thenReturn(true);

        given()
            .when().delete("/api/thoughts/" + SAMPLE.id())
            .then()
                .statusCode(204);
    }

    // DELETE /api/thoughts/{id} maps a false result from the service (nothing to delete) to 404.
    @Test
    void deleteReturns404WhenMissing() {
        UUID missing = UUID.randomUUID();
        Mockito.when(thoughtApplicationService.delete(missing)).thenReturn(false);

        given()
            .when().delete("/api/thoughts/" + missing)
            .then()
                .statusCode(404);
    }

    // POST /api/thoughts/{id}/thumbs-up returns the updated vote count in the JSON body.
    @Test
    void thumbsUpReturnsUpdatedThought() {
        ThoughtDTO voted = new ThoughtDTO(SAMPLE.id(), SAMPLE.content(), SAMPLE.authorName(), SAMPLE.authorBio(), 4, 1, SAMPLE.status());
        Mockito.when(thoughtApplicationService.thumbsUp(SAMPLE.id())).thenReturn(Optional.of(voted));

        given()
            .when().post("/api/thoughts/" + SAMPLE.id() + "/thumbs-up")
            .then()
                .statusCode(200)
                .body("thumbsUp", equalTo(4));
    }

    // POST /api/thoughts/{id}/thumbs-down maps an empty Optional (thought not approved) to 404.
    @Test
    void thumbsDownReturns404WhenNotApproved() {
        UUID notApproved = UUID.randomUUID();
        Mockito.when(thoughtApplicationService.thumbsDown(notApproved)).thenReturn(Optional.empty());

        given()
            .when().post("/api/thoughts/" + notApproved + "/thumbs-down")
            .then()
                .statusCode(404);
    }
}
