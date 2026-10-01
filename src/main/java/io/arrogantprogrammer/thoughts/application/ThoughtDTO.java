package io.arrogantprogrammer.thoughts.application;

import io.arrogantprogrammer.thoughts.domain.Thought;

import java.util.UUID;

public record ThoughtDTO(UUID id, String content, String authorName, String authorBio, int thumbsUp, int thumbsDown) {

    public static ThoughtDTO from(Thought thought) {
        return new ThoughtDTO(
                thought.id().value(),
                thought.content().value(),
                thought.author().name(),
                thought.author().bio(),
                thought.rating().thumbsUp(),
                thought.rating().thumbsDown());
    }
}
