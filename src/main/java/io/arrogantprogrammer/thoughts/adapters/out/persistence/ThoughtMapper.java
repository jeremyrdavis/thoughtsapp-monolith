package io.arrogantprogrammer.thoughts.adapters.out.persistence;

import io.arrogantprogrammer.thoughts.domain.Author;
import io.arrogantprogrammer.thoughts.domain.Content;
import io.arrogantprogrammer.thoughts.domain.Rating;
import io.arrogantprogrammer.thoughts.domain.Thought;
import io.arrogantprogrammer.thoughts.domain.ThoughtId;

final class ThoughtMapper {

    private ThoughtMapper() {
    }

    static ThoughtEntity toEntity(Thought thought) {
        ThoughtEntity entity = new ThoughtEntity();
        copyMutableFields(entity, thought);
        entity.id = thought.id().value();
        entity.createdAt = thought.createdAt();
        return entity;
    }

    static void copyMutableFields(ThoughtEntity entity, Thought thought) {
        entity.content = thought.content().value();
        entity.author = thought.author().name();
        entity.authorBio = thought.author().bio();
        entity.thumbsUp = thought.rating().thumbsUp();
        entity.thumbsDown = thought.rating().thumbsDown();
        entity.status = thought.status();
        entity.updatedAt = thought.updatedAt();
    }

    static Thought toDomain(ThoughtEntity entity) {
        return Thought.rehydrate(
                new ThoughtId(entity.id),
                new Content(entity.content),
                new Author(entity.author, entity.authorBio),
                new Rating(entity.thumbsUp, entity.thumbsDown),
                entity.status,
                entity.createdAt,
                entity.updatedAt);
    }
}
