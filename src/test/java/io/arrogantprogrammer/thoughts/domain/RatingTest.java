package io.arrogantprogrammer.thoughts.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the {@link Rating} class.
 */
class RatingTest {

    // The zero() factory starts both counters at 0.
    @Test
    void zeroHasNoVotes() {
        Rating rating = Rating.zero();
        assertEquals(0, rating.thumbsUp());
        assertEquals(0, rating.thumbsDown());
    }

    // With no votes cast, approvalRate() must not divide by zero and should read as 0.
    @Test
    void approvalRateIsZeroWhenThereAreNoVotes() {
        assertEquals(0.0, Rating.zero().approvalRate());
    }

    // approvalRate() is thumbsUp / (thumbsUp + thumbsDown) across a mix of votes.
    @Test
    void approvalRateIsComputedFromBothCounters() {
        Rating rating = new Rating(3, 1);
        assertEquals(0.75, rating.approvalRate());
    }

    // With only upvotes, the approval rate is 100%.
    @Test
    void approvalRateIsOneWhenOnlyThumbsUp() {
        Rating rating = new Rating(5, 0);
        assertEquals(1.0, rating.approvalRate());
    }

    // With only downvotes, the approval rate is 0%.
    @Test
    void approvalRateIsZeroWhenOnlyThumbsDown() {
        Rating rating = new Rating(0, 5);
        assertEquals(0.0, rating.approvalRate());
    }

    // Rating is immutable, so incrementing returns a new instance with thumbsUp bumped and thumbsDown untouched.
    @Test
    void incrementThumbsUpReturnsNewInstanceWithIncrementedCounter() {
        Rating rating = Rating.zero().incrementThumbsUp();
        assertEquals(1, rating.thumbsUp());
        assertEquals(0, rating.thumbsDown());
    }

    // Rating is immutable, so incrementing returns a new instance with thumbsDown bumped and thumbsUp untouched.
    @Test
    void incrementThumbsDownReturnsNewInstanceWithIncrementedCounter() {
        Rating rating = Rating.zero().incrementThumbsDown();
        assertEquals(0, rating.thumbsUp());
        assertEquals(1, rating.thumbsDown());
    }

    // Vote counters model real tallies and can never go negative.
    @Test
    void rejectsNegativeThumbsUp() {
        assertThrows(IllegalArgumentException.class, () -> new Rating(-1, 0));
    }

    // Vote counters model real tallies and can never go negative.
    @Test
    void rejectsNegativeThumbsDown() {
        assertThrows(IllegalArgumentException.class, () -> new Rating(0, -1));
    }
}
