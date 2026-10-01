package io.arrogantprogrammer.thoughts.application;

import io.arrogantprogrammer.thoughts.adapters.out.persistence.ThoughtEntity;
import io.arrogantprogrammer.thoughts.domain.Author;
import io.arrogantprogrammer.thoughts.domain.Content;
import io.arrogantprogrammer.thoughts.domain.Thought;
import io.arrogantprogrammer.thoughts.domain.ThoughtRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ThoughtApplicationServiceTest {

    @Inject
    ThoughtApplicationService thoughtApplicationService;

    @Inject
    ThoughtRepository thoughtRepository;

    @Test
    @TestTransaction
    void mapsRandomApprovedThoughtToDto() {
        ThoughtEntity.deleteAll();
        Thought thought = Thought.create(
                new Content("A thought worth sharing with everyone."),
                new Author("Ada Lovelace", "Mathematician"));
        thought.approve();
        thought.thumbsUp();
        thoughtRepository.save(thought);

        ThoughtDTO dto = thoughtApplicationService.randomApprovedThought().orElseThrow();

        assertEquals(thought.id().value(), dto.id());
        assertEquals(thought.content().value(), dto.content());
        assertEquals(thought.author().name(), dto.authorName());
        assertEquals(thought.author().bio(), dto.authorBio());
        assertEquals(1, dto.thumbsUp());
        assertEquals(0, dto.thumbsDown());
    }

    @Test
    @TestTransaction
    void isEmptyWhenNoThoughtsAreApproved() {
        ThoughtEntity.deleteAll();
        thoughtRepository.save(Thought.create(
                new Content("A thought still waiting for review."),
                new Author("Author", null)));

        assertFalse(thoughtApplicationService.randomApprovedThought().isPresent());
    }

    @Test
    @TestTransaction
    void thumbsUpIncrementsCountOfApprovedThought() {
        ThoughtEntity.deleteAll();
        Thought thought = Thought.create(new Content("A thought worth voting on today."), new Author("Author", null));
        thought.approve();
        thoughtRepository.save(thought);

        ThoughtDTO dto = thoughtApplicationService.thumbsUp(thought.id().value()).orElseThrow();

        assertEquals(1, dto.thumbsUp());
        assertEquals(0, dto.thumbsDown());
    }

    @Test
    @TestTransaction
    void thumbsDownIncrementsCountOfApprovedThought() {
        ThoughtEntity.deleteAll();
        Thought thought = Thought.create(new Content("A thought worth voting on today."), new Author("Author", null));
        thought.approve();
        thoughtRepository.save(thought);

        ThoughtDTO dto = thoughtApplicationService.thumbsDown(thought.id().value()).orElseThrow();

        assertEquals(0, dto.thumbsUp());
        assertEquals(1, dto.thumbsDown());
    }

    @Test
    @TestTransaction
    void thumbsUpOnMissingThoughtReturnsEmpty() {
        assertFalse(thoughtApplicationService.thumbsUp(UUID.randomUUID()).isPresent());
    }

    @Test
    @TestTransaction
    void thumbsUpOnNonApprovedThoughtReturnsEmpty() {
        ThoughtEntity.deleteAll();
        Thought thought = Thought.create(new Content("A thought still waiting for review."), new Author("Author", null));
        thoughtRepository.save(thought);

        assertFalse(thoughtApplicationService.thumbsUp(thought.id().value()).isPresent());
    }

    @Test
    @TestTransaction
    void thumbsDownOnRemovedThoughtReturnsEmpty() {
        ThoughtEntity.deleteAll();
        Thought thought = Thought.create(new Content("A thought that has been removed."), new Author("Author", null));
        thought.remove();
        thoughtRepository.save(thought);

        assertFalse(thoughtApplicationService.thumbsDown(thought.id().value()).isPresent());
    }

    @Test
    @TestTransaction
    void thumbsUpOnMissingThoughtDoesNotCreateARow() {
        ThoughtEntity.deleteAll();
        assertTrue(thoughtApplicationService.thumbsUp(UUID.randomUUID()).isEmpty());
        assertEquals(0, thoughtRepository.count());
    }
}
