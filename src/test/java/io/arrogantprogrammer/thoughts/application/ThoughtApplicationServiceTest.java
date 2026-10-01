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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
