package io.arrogantprogrammer.thoughts.adapters.out.persistence;

import io.arrogantprogrammer.thoughts.domain.Author;
import io.arrogantprogrammer.thoughts.domain.Content;
import io.arrogantprogrammer.thoughts.domain.Thought;
import io.arrogantprogrammer.thoughts.domain.ThoughtId;
import io.arrogantprogrammer.thoughts.domain.ThoughtRepository;
import io.arrogantprogrammer.thoughts.domain.ThoughtStatus;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Integration tests for {@code PanacheThoughtRepository} against the Dev Services
 * PostgreSQL database, verifying the {@link ThoughtRepository} port is implemented
 * correctly end to end (not against a mock).
 */
@QuarkusTest
class PanacheThoughtRepositoryTest {

    @Inject
    ThoughtRepository repository;

    private static Thought newThought() {
        return Thought.create(
                new Content("A thought worth sharing with everyone."),
                new Author("Ada Lovelace", "Mathematician"));
    }

    // Saving a thought and reading it back by id must reproduce every field exactly.
    @Test
    @TestTransaction
    void roundTripsThoughtById() {
        Thought thought = newThought();
        repository.save(thought);

        Thought found = repository.findById(thought.id()).orElseThrow();

        assertEquals(thought.id(), found.id());
        assertEquals(thought.content(), found.content());
        assertEquals(thought.author(), found.author());
        assertEquals(thought.rating(), found.rating());
        assertEquals(thought.status(), found.status());
        assertEquals(thought.createdAt(), found.createdAt());
        assertEquals(thought.updatedAt(), found.updatedAt());
    }

    // Saving a thought that was already persisted (same id) must update the existing
    // row rather than inserting a duplicate.
    @Test
    @TestTransaction
    void savingAnExistingThoughtUpdatesInPlace() {
        Thought thought = newThought();
        repository.save(thought);

        thought.thumbsUp();
        thought.approve();
        repository.save(thought);

        Thought found = repository.findById(thought.id()).orElseThrow();
        assertEquals(1, found.rating().thumbsUp());
        assertEquals(ThoughtStatus.APPROVED, found.status());
    }

    // Looking up an id that was never persisted must return an empty Optional, not throw.
    @Test
    @TestTransaction
    void findByIdReturnsEmptyWhenMissing() {
        assertTrue(repository.findById(Thought.create(
                new Content("Another thought worth sharing here."),
                new Author("Author", null)).id()).isEmpty());
    }

    // Seeds a mix of APPROVED, IN_REVIEW, and REMOVED thoughts, then draws random
    // thoughts repeatedly to check the query's WHERE clause never leaks a non-approved row.
    @Test
    @TestTransaction
    void findRandomApprovedNeverReturnsNonApprovedThoughts() {
        ThoughtEntity.deleteAll();

        Thought approvedOne = newThought();
        approvedOne.approve();
        Thought approvedTwo = newThought();
        approvedTwo.approve();
        Thought inReview = newThought();
        Thought removed = newThought();
        removed.remove();

        repository.save(approvedOne);
        repository.save(approvedTwo);
        repository.save(inReview);
        repository.save(removed);

        for (int i = 0; i < 50; i++) {
            Thought drawn = repository.findRandomApproved().orElseThrow();
            assertEquals(ThoughtStatus.APPROVED, drawn.status());
        }
    }

    // When only non-approved thoughts exist, the random-approved query must return empty, not an arbitrary row.
    @Test
    @TestTransaction
    void findRandomApprovedIsEmptyWhenNoneApproved() {
        ThoughtEntity.deleteAll();

        Thought inReview = newThought();
        repository.save(inReview);

        assertFalse(repository.findRandomApproved().isPresent());
    }

    // count() must reflect exactly the number of rows persisted, no more and no less.
    @Test
    @TestTransaction
    void countReflectsPersistedThoughts() {
        ThoughtEntity.deleteAll();
        repository.save(newThought());
        repository.save(newThought());

        assertEquals(2, repository.count());
    }

    // With more rows available than the requested page size, page() must still cap the result at that size.
    @Test
    @TestTransaction
    void pageReturnsAtMostRequestedSize() {
        ThoughtEntity.deleteAll();
        for (int i = 0; i < 5; i++) {
            repository.save(newThought());
        }

        assertEquals(3, repository.page(0, 3).size());
    }

    // Deleting a thought that exists returns true and the row is actually gone afterward.
    @Test
    @TestTransaction
    void deleteRemovesTheThoughtAndReturnsTrue() {
        ThoughtEntity.deleteAll();
        Thought thought = newThought();
        repository.save(thought);

        assertTrue(repository.delete(thought.id()));
        assertTrue(repository.findById(thought.id()).isEmpty());
    }

    // Deleting an id that doesn't exist must return false rather than throwing.
    @Test
    @TestTransaction
    void deleteOnMissingThoughtReturnsFalse() {
        ThoughtEntity.deleteAll();
        assertFalse(repository.delete(ThoughtId.generate()));
    }
}
