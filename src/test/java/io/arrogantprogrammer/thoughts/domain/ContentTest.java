package io.arrogantprogrammer.thoughts.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the {@link Content} class.
 */
class ContentTest {

    // Content within the 10-500 character range is stored unchanged.
    @Test
    void acceptsValidContent() {
        Content content = new Content("This is a perfectly valid thought.");
        assertEquals("This is a perfectly valid thought.", content.value());
    }

    // Surrounding whitespace is stripped before the value is stored.
    @Test
    void trimsLeadingAndTrailingWhitespace() {
        Content content = new Content("  padded content here  ");
        assertEquals("padded content here", content.value());
    }

    // 10 characters is the lower length bound, so it must be accepted.
    @Test
    void acceptsExactlyTenCharacters() {
        Content content = new Content("1234567890");
        assertEquals(10, content.value().length());
    }

    // 500 characters is the upper length bound, so it must be accepted.
    @Test
    void acceptsExactlyFiveHundredCharacters() {
        String value = "a".repeat(500);
        Content content = new Content(value);
        assertEquals(500, content.value().length());
    }

    // Fewer than 10 characters is below the minimum and must be rejected.
    @Test
    void rejectsShorterThanTenCharacters() {
        assertThrows(IllegalArgumentException.class, () -> new Content("short"));
    }

    // The length check applies after trimming, so padding can't disguise 9 real characters as valid.
    @Test
    void rejectsNineCharactersAfterTrim() {
        assertThrows(IllegalArgumentException.class, () -> new Content("  123456789  "));
    }

    // More than 500 characters is above the maximum and must be rejected.
    @Test
    void rejectsLongerThanFiveHundredCharacters() {
        String value = "a".repeat(501);
        assertThrows(IllegalArgumentException.class, () -> new Content(value));
    }

    // Content made up of only whitespace carries no real text and must be rejected.
    @Test
    void rejectsBlankContent() {
        assertThrows(IllegalArgumentException.class, () -> new Content("    "));
    }

    // An empty string is trivially shorter than the 10-character minimum and must be rejected.
    @Test
    void rejectsEmptyContent() {
        assertThrows(IllegalArgumentException.class, () -> new Content(""));
    }

    // A null value has no sensible default and must fail fast with an NPE.
    @Test
    void rejectsNullContent() {
        assertThrows(NullPointerException.class, () -> new Content(null));
    }
}
