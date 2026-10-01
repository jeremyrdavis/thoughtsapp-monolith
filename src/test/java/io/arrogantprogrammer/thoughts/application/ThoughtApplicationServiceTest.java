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

    @Test
    @TestTransaction
    void createPersistsANewInReviewThought() {
        ThoughtEntity.deleteAll();

        ThoughtDTO dto = thoughtApplicationService.create("A brand new thought worth sharing.", "Author", "Bio");

        assertEquals("IN_REVIEW", dto.status());
        assertEquals(1, thoughtRepository.count());
        assertEquals(dto.id(), thoughtApplicationService.findById(dto.id()).orElseThrow().id());
    }

    @Test
    @TestTransaction
    void findByIdReturnsEmptyWhenMissing() {
        assertFalse(thoughtApplicationService.findById(UUID.randomUUID()).isPresent());
    }

    @Test
    @TestTransaction
    void updateReplacesContentAndAuthor() {
        ThoughtEntity.deleteAll();
        Thought thought = Thought.create(new Content("The original thought worth sharing."), new Author("Original", null));
        thoughtRepository.save(thought);

        ThoughtDTO updated = thoughtApplicationService
                .update(thought.id().value(), "An edited thought worth sharing now.", "Edited Author", "Edited Bio")
                .orElseThrow();

        assertEquals("An edited thought worth sharing now.", updated.content());
        assertEquals("Edited Author", updated.authorName());
        assertEquals("Edited Bio", updated.authorBio());

        ThoughtDTO reloaded = thoughtApplicationService.findById(thought.id().value()).orElseThrow();
        assertEquals("An edited thought worth sharing now.", reloaded.content());
        assertEquals("Edited Author", reloaded.authorName());
        assertEquals("Edited Bio", reloaded.authorBio());
    }

    @Test
    @TestTransaction
    void updateOnMissingThoughtReturnsEmpty() {
        assertFalse(thoughtApplicationService.update(UUID.randomUUID(), "An edited thought worth sharing now.", "Author", null)
                .isPresent());
    }

    @Test
    @TestTransaction
    void deleteRemovesTheThought() {
        ThoughtEntity.deleteAll();
        Thought thought = Thought.create(new Content("A thought that will be deleted."), new Author("Author", null));
        thoughtRepository.save(thought);

        assertTrue(thoughtApplicationService.delete(thought.id().value()));
        assertFalse(thoughtApplicationService.findById(thought.id().value()).isPresent());
    }

    @Test
    @TestTransaction
    void deleteOnMissingThoughtReturnsFalse() {
        assertFalse(thoughtApplicationService.delete(UUID.randomUUID()));
    }

    @Test
    @TestTransaction
    void listReturnsRequestedPageSize() {
        ThoughtEntity.deleteAll();
        for (int i = 0; i < 5; i++) {
            thoughtRepository.save(Thought.create(
                    new Content("A listed thought number " + i + " worth sharing."), new Author("Author", null)));
        }

        assertEquals(3, thoughtApplicationService.list(0, 3).size());
    }
}
