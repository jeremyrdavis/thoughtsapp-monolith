package io.arrogantprogrammer.thoughts.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentTest {

    @Test
    void acceptsValidContent() {
        Content content = new Content("This is a perfectly valid thought.");
        assertEquals("This is a perfectly valid thought.", content.value());
    }

    @Test
    void trimsLeadingAndTrailingWhitespace() {
        Content content = new Content("  padded content here  ");
        assertEquals("padded content here", content.value());
    }

    @Test
    void acceptsExactlyTenCharacters() {
        Content content = new Content("1234567890");
        assertEquals(10, content.value().length());
    }

    @Test
    void acceptsExactlyFiveHundredCharacters() {
        String value = "a".repeat(500);
        Content content = new Content(value);
        assertEquals(500, content.value().length());
    }

    @Test
    void rejectsShorterThanTenCharacters() {
        assertThrows(IllegalArgumentException.class, () -> new Content("short"));
    }

    @Test
    void rejectsNineCharactersAfterTrim() {
        assertThrows(IllegalArgumentException.class, () -> new Content("  123456789  "));
    }

    @Test
    void rejectsLongerThanFiveHundredCharacters() {
        String value = "a".repeat(501);
        assertThrows(IllegalArgumentException.class, () -> new Content(value));
    }

    @Test
    void rejectsBlankContent() {
        assertThrows(IllegalArgumentException.class, () -> new Content("    "));
    }

    @Test
    void rejectsEmptyContent() {
        assertThrows(IllegalArgumentException.class, () -> new Content(""));
    }

    @Test
    void rejectsNullContent() {
        assertThrows(NullPointerException.class, () -> new Content(null));
    }
}
