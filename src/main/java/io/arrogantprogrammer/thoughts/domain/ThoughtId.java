package io.arrogantprogrammer.thoughts.domain;

import java.util.Objects;
import java.util.UUID;

public record ThoughtId(UUID value) {

    public ThoughtId {
        Objects.requireNonNull(value, "id is required");
    }

    public static ThoughtId generate() {
        return new ThoughtId(UUID.randomUUID());
    }
}
