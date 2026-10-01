package io.arrogantprogrammer.thoughts.domain;

import java.util.Objects;

public record Author(String name, String bio) {

    public Author {
        Objects.requireNonNull(name, "author name is required");
        if (name.isBlank()) {
            throw new IllegalArgumentException("author name must not be blank");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("author name must be at most 200 characters");
        }
        if (bio != null && bio.length() > 200) {
            throw new IllegalArgumentException("author bio must be at most 200 characters");
        }
    }
}
