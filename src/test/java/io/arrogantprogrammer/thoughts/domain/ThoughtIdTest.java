package io.arrogantprogrammer.thoughts.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the {@link ThoughtId} class.
 */
class ThoughtIdTest {

    // The constructor wraps whatever UUID it is given without altering it.
    @Test
    void wrapsGivenUuid() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, new ThoughtId(uuid).value());
    }

    // generate() must produce a fresh, unique identifier on each call.
    @Test
    void generateProducesDistinctIds() {
        assertNotEquals(ThoughtId.generate(), ThoughtId.generate());
    }

    // An identifier has no sensible default and must fail fast with an NPE when null.
    @Test
    void rejectsNullValue() {
        assertThrows(NullPointerException.class, () -> new ThoughtId(null));
    }
}
