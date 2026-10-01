package io.arrogantprogrammer.thoughts.domain;

import java.util.Objects;

public record Content(String value) {

    public Content {
        Objects.requireNonNull(value, "content is required");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        if (value.length() < 10 || value.length() > 500) {
            throw new IllegalArgumentException("content must be between 10 and 500 characters");
        }
    }
}
