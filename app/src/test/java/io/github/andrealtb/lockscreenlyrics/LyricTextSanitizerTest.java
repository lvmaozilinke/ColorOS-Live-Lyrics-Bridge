package io.github.andrealtb.lockscreenlyrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LyricTextSanitizerTest {
    @Test
    public void removesInvisibleFormattingCharacters() {
        assertEquals(
                "main translation",
                LyricTextSanitizer.removeIgnorableCharacters(
                        "\uFEFFmain\u200B translation\u2060"));
    }

    @Test
    public void returnsOriginalStringWhenNoCleanupIsNeeded() {
        String lyric = "And all of the foes and all of the friends";

        assertTrue(lyric == LyricTextSanitizer.removeIgnorableCharacters(lyric));
    }
}
