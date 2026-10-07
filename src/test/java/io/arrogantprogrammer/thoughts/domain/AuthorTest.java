package io.arrogantprogrammer.thoughts.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the {@link Author} class.
 */
class AuthorTest {

    // A null bio is allowed; the author is still constructed with just a name.
    @Test
    void acceptsNameWithoutBio() {
        Author author = new Author("Ada Lovelace", null);
        assertEquals("Ada Lovelace", author.name());
        assertNull(author.bio());
    }

    // A non-null bio is stored as given.
    @Test
    void acceptsNameWithBio() {
        Author author = new Author("Ada Lovelace", "Mathematician");
        assertEquals("Mathematician", author.bio());
    }

    // 200 characters is the upper bound for name length, so it must be accepted.
    @Test
    void acceptsNameAtTwoHundredCharacters() {
        String name = "a".repeat(200);
        Author author = new Author(name, null);
        assertEquals(200, author.name().length());
    }

    // 200 characters is the upper bound for bio length, so it must be accepted.
    @Test
    void acceptsBioAtTwoHundredCharacters() {
        String bio = "b".repeat(200);
        Author author = new Author("Name", bio);
        assertEquals(200, author.bio().length());
    }

    // One character past the 200-character name limit must be rejected.
    @Test
    void rejectsNameLongerThanTwoHundredCharacters() {
        String name = "a".repeat(201);
        assertThrows(IllegalArgumentException.class, () -> new Author(name, null));
    }

    // One character past the 200-character bio limit must be rejected.
    @Test
    void rejectsBioLongerThanTwoHundredCharacters() {
        String bio = "b".repeat(201);
        assertThrows(IllegalArgumentException.class, () -> new Author("Name", bio));
    }

    // A name that is only whitespace carries no real content and must be rejected.
    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Author("   ", null));
    }

    // A null name has no sensible default and must fail fast with an NPE.
    @Test
    void rejectsNullName() {
        assertThrows(NullPointerException.class, () -> new Author(null, null));
    }
}
