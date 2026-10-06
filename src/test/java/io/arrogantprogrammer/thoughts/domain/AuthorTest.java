package io.arrogantprogrammer.thoughts.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the {@link Author} class.
 */
class AuthorTest {

    @Test
    void acceptsNameWithoutBio() {
        Author author = new Author("Ada Lovelace", null);
        assertEquals("Ada Lovelace", author.name());
        assertNull(author.bio());
    }

    @Test
    void acceptsNameWithBio() {
        Author author = new Author("Ada Lovelace", "Mathematician");
        assertEquals("Mathematician", author.bio());
    }

    @Test
    void acceptsNameAtTwoHundredCharacters() {
        String name = "a".repeat(200);
        Author author = new Author(name, null);
        assertEquals(200, author.name().length());
    }

    @Test
    void acceptsBioAtTwoHundredCharacters() {
        String bio = "b".repeat(200);
        Author author = new Author("Name", bio);
        assertEquals(200, author.bio().length());
    }

    @Test
    void rejectsNameLongerThanTwoHundredCharacters() {
        String name = "a".repeat(201);
        assertThrows(IllegalArgumentException.class, () -> new Author(name, null));
    }

    @Test
    void rejectsBioLongerThanTwoHundredCharacters() {
        String bio = "b".repeat(201);
        assertThrows(IllegalArgumentException.class, () -> new Author("Name", bio));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Author("   ", null));
    }

    @Test
    void rejectsNullName() {
        assertThrows(NullPointerException.class, () -> new Author(null, null));
    }
}
