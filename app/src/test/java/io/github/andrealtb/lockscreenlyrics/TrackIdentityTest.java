package io.github.andrealtb.lockscreenlyrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TrackIdentityTest {
    @Test
    public void lrcTitleTagCanContainCompositeTitleAndArtist() {
        assertEquals(
                TrackIdentity.buildKey("Alma Mater [Explicit]", "Bleachers"),
                TrackIdentity.buildLrcHintKey(
                        "Alma Mater (Explicit) - Bleachers",
                        ""));
    }

    @Test
    public void saltRelayArtistRestoresStableTrackIdentity() {
        TrackIdentity.SaltRelayIdentity identity =
                TrackIdentity.parseSaltRelayArtist(
                        "William Black/Fairlane - Broken");

        assertEquals("Broken", identity.title);
        assertEquals("William Black/Fairlane", identity.artist);
    }

    @Test
    public void saltRelayArtistKeepsAdditionalTitleSeparators() {
        TrackIdentity.SaltRelayIdentity identity =
                TrackIdentity.parseSaltRelayArtist(
                        "Porter Robinson - Kitsune Maison Freestyle - Live");

        assertEquals("Kitsune Maison Freestyle - Live", identity.title);
        assertEquals("Porter Robinson", identity.artist);
    }

    @Test
    public void saltRelayIdentityMatchesExplicitLrcHintDuringMetadataHandoff() {
        TrackIdentity.SaltRelayIdentity identity =
                TrackIdentity.parseSaltRelayArtist("Adele - All I Ask");

        assertTrue(TrackIdentity.relayIdentityMatchesHint(
                identity,
                TrackIdentity.buildKey("All I Ask", "Adele")));
        assertFalse(TrackIdentity.relayIdentityMatchesHint(
                identity,
                TrackIdentity.buildKey("Actually Romantic", "Taylor Swift")));
    }

    @Test
    public void explicitSuffixDoesNotChangeTrackIdentity() {
        assertEquals(
                TrackIdentity.buildKey("Modern Girl", "Bleachers"),
                TrackIdentity.buildKey(
                        "Modern Girl [Explicit]",
                        "Bleachers"));
        assertEquals(
                TrackIdentity.buildKey("Jesus Is Dead", "Bleachers"),
                TrackIdentity.buildKey(
                        "Jesus Is Dead (Explicit)",
                        "Bleachers"));
    }

    @Test
    public void ordinaryBracketedTitleTextIsPreserved() {
        assertEquals(
                "song [live]|artist",
                TrackIdentity.buildKey("Song [Live]", "Artist"));
    }

    @Test
    public void missingLrcArtistStillMatchesTheSameTitle() {
        assertTrue(TrackIdentity.matchesHintKey(
                "alma mater|",
                TrackIdentity.buildKey("Alma Mater [Explicit]", "Bleachers")));
        assertFalse(TrackIdentity.matchesHintKey(
                "alma mater|",
                TrackIdentity.buildKey("Tiny Moves", "Bleachers")));
    }

}
