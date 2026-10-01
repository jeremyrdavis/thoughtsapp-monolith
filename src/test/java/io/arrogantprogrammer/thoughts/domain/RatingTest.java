package io.arrogantprogrammer.thoughts.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RatingTest {

    @Test
    void zeroHasNoVotes() {
        Rating rating = Rating.zero();
        assertEquals(0, rating.thumbsUp());
        assertEquals(0, rating.thumbsDown());
    }

    @Test
    void approvalRateIsZeroWhenThereAreNoVotes() {
        assertEquals(0.0, Rating.zero().approvalRate());
    }

    @Test
    void approvalRateIsComputedFromBothCounters() {
        Rating rating = new Rating(3, 1);
        assertEquals(0.75, rating.approvalRate());
    }

    @Test
    void approvalRateIsOneWhenOnlyThumbsUp() {
        Rating rating = new Rating(5, 0);
        assertEquals(1.0, rating.approvalRate());
    }

    @Test
    void approvalRateIsZeroWhenOnlyThumbsDown() {
        Rating rating = new Rating(0, 5);
        assertEquals(0.0, rating.approvalRate());
    }

    @Test
    void incrementThumbsUpReturnsNewInstanceWithIncrementedCounter() {
        Rating rating = Rating.zero().incrementThumbsUp();
        assertEquals(1, rating.thumbsUp());
        assertEquals(0, rating.thumbsDown());
    }

    @Test
    void incrementThumbsDownReturnsNewInstanceWithIncrementedCounter() {
        Rating rating = Rating.zero().incrementThumbsDown();
        assertEquals(0, rating.thumbsUp());
        assertEquals(1, rating.thumbsDown());
    }

    @Test
    void rejectsNegativeThumbsUp() {
        assertThrows(IllegalArgumentException.class, () -> new Rating(-1, 0));
    }

    @Test
    void rejectsNegativeThumbsDown() {
        assertThrows(IllegalArgumentException.class, () -> new Rating(0, -1));
    }
}
