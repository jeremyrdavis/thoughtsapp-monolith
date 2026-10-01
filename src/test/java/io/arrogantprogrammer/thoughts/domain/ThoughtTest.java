package io.arrogantprogrammer.thoughts.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThoughtTest {

    private static final Content CONTENT = new Content("A thought worth sharing with everyone.");
    private static final Author AUTHOR = new Author("Ada Lovelace", "Mathematician");

    @Test
    void createStartsInReviewWithZeroRating() {
        Thought thought = Thought.create(CONTENT, AUTHOR);

        assertNotNull(thought.id());
        assertEquals(ThoughtStatus.IN_REVIEW, thought.status());
        assertEquals(0, thought.rating().thumbsUp());
        assertEquals(0, thought.rating().thumbsDown());
        assertEquals(CONTENT, thought.content());
        assertEquals(AUTHOR, thought.author());
        assertNotNull(thought.createdAt());
        assertEquals(thought.createdAt(), thought.updatedAt());
    }

    @Test
    void editReplacesContentAndAuthorAndUpdatesTimestamp() throws InterruptedException {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        Instant createdAt = thought.createdAt();
        Thread.sleep(2);

        Content newContent = new Content("An edited thought worth sharing instead.");
        Author newAuthor = new Author("Grace Hopper", "Rear Admiral");
        thought.edit(newContent, newAuthor);

        assertEquals(newContent, thought.content());
        assertEquals(newAuthor, thought.author());
        assertTrue(thought.updatedAt().isAfter(createdAt));
    }

    @Test
    void thumbsUpIncrementsRatingAndUpdatesTimestamp() throws InterruptedException {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        Instant createdAt = thought.createdAt();
        Thread.sleep(2);

        thought.thumbsUp();

        assertEquals(1, thought.rating().thumbsUp());
        assertEquals(0, thought.rating().thumbsDown());
        assertTrue(thought.updatedAt().isAfter(createdAt));
    }

    @Test
    void thumbsDownIncrementsRatingAndUpdatesTimestamp() throws InterruptedException {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        Instant createdAt = thought.createdAt();
        Thread.sleep(2);

        thought.thumbsDown();

        assertEquals(0, thought.rating().thumbsUp());
        assertEquals(1, thought.rating().thumbsDown());
        assertTrue(thought.updatedAt().isAfter(createdAt));
    }

    @Test
    void approveFromInReviewSucceeds() {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        thought.approve();
        assertEquals(ThoughtStatus.APPROVED, thought.status());
    }

    @Test
    void approveFromApprovedFails() {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        thought.approve();
        assertThrows(IllegalThoughtStatusTransitionException.class, thought::approve);
    }

    @Test
    void approveFromRemovedFails() {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        thought.remove();
        assertThrows(IllegalThoughtStatusTransitionException.class, thought::approve);
    }

    @Test
    void removeFromInReviewSucceeds() {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        thought.remove();
        assertEquals(ThoughtStatus.REMOVED, thought.status());
    }

    @Test
    void removeFromApprovedSucceeds() {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        thought.approve();
        thought.remove();
        assertEquals(ThoughtStatus.REMOVED, thought.status());
    }

    @Test
    void removeFromRemovedFails() {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        thought.remove();
        assertThrows(IllegalThoughtStatusTransitionException.class, thought::remove);
    }

    @Test
    void restoreFromRemovedSucceeds() {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        thought.remove();
        thought.restore();
        assertEquals(ThoughtStatus.IN_REVIEW, thought.status());
    }

    @Test
    void restoreFromInReviewFails() {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        assertThrows(IllegalThoughtStatusTransitionException.class, thought::restore);
    }

    @Test
    void restoreFromApprovedFails() {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        thought.approve();
        assertThrows(IllegalThoughtStatusTransitionException.class, thought::restore);
    }

    @Test
    void sendBackToReviewFromApprovedSucceeds() {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        thought.approve();
        thought.sendBackToReview();
        assertEquals(ThoughtStatus.IN_REVIEW, thought.status());
    }

    @Test
    void sendBackToReviewFromInReviewFails() {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        assertThrows(IllegalThoughtStatusTransitionException.class, thought::sendBackToReview);
    }

    @Test
    void sendBackToReviewFromRemovedFails() {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        thought.remove();
        assertThrows(IllegalThoughtStatusTransitionException.class, thought::sendBackToReview);
    }

    @Test
    void illegalTransitionMessageNamesFromAndToStatus() {
        Thought thought = Thought.create(CONTENT, AUTHOR);
        IllegalThoughtStatusTransitionException exception =
                assertThrows(IllegalThoughtStatusTransitionException.class, thought::restore);
        assertTrue(exception.getMessage().contains("IN_REVIEW"));
    }

    @Test
    void rehydrateReconstructsExactState() {
        ThoughtId id = ThoughtId.generate();
        Rating rating = new Rating(4, 1);
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");

        Thought thought = Thought.rehydrate(id, CONTENT, AUTHOR, rating, ThoughtStatus.APPROVED, createdAt, updatedAt);

        assertEquals(id, thought.id());
        assertEquals(CONTENT, thought.content());
        assertEquals(AUTHOR, thought.author());
        assertEquals(rating, thought.rating());
        assertEquals(ThoughtStatus.APPROVED, thought.status());
        assertEquals(createdAt, thought.createdAt());
        assertEquals(updatedAt, thought.updatedAt());
    }

    @Test
    void rehydratedThoughtStillEnforcesTransitionRules() {
        Thought thought = Thought.rehydrate(ThoughtId.generate(), CONTENT, AUTHOR, Rating.zero(),
                ThoughtStatus.REMOVED, Instant.now(), Instant.now());

        assertFalse(thought.status() == ThoughtStatus.IN_REVIEW);
        thought.restore();
        assertEquals(ThoughtStatus.IN_REVIEW, thought.status());
    }
}
