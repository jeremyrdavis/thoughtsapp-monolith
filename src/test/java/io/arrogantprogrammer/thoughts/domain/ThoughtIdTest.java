package io.arrogantprogrammer.thoughts.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThoughtIdTest {

    @Test
    void wrapsGivenUuid() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, new ThoughtId(uuid).value());
    }

    @Test
    void generateProducesDistinctIds() {
        assertNotEquals(ThoughtId.generate(), ThoughtId.generate());
    }

    @Test
    void rejectsNullValue() {
        assertThrows(NullPointerException.class, () -> new ThoughtId(null));
    }
}
