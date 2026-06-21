package io.github.andrealtb.lockscreenlyrics;

import java.util.LinkedHashMap;

/**
 * Player-independent reducer that associates lyric documents with track observations.
 *
 * <p>Passive parser callbacks are deliberately not bound to whatever metadata happened to be
 * published most recently. They remain pending until a compatible track observation arrives.
 * This prevents a new track's lyric from being written into the previous track during an
 * asynchronous MediaSession handoff.</p>
 */
final class LyricSessionReducer {
    // Salt emits result/source constructor callbacks back-to-back. A later identical lyric can
    // belong to the next media item and must go through normal track binding again.
    private static final long DUPLICATE_CAPTURE_COALESCE_WINDOW_MS = 300L;

    enum ObservationKind {
        STABLE_METADATA,
        RELAY_METADATA
    }

    static final class TrackSnapshot {
        final String title;
        final String artist;
        final long durationMillis;
        final String songId;
        final String key;

        TrackSnapshot(String title, String artist, long durationMillis, String songId) {
            this.title = nullToEmpty(title);
            this.artist = nullToEmpty(artist);
            this.durationMillis = durationMillis;
            this.songId = nullToEmpty(songId);
            this.key = TrackIdentity.buildKey(this.title, this.artist);
        }
    }

    static final class LyricDocument {
        final String source;
        final String lyric;
        final String rawLyric;
        final String trackHintKey;
        final long capturedAtMillis;
        final LyricProviderCapabilities capabilities;
        final String baselineTrackKey;
        String boundTrackKey = "";

        LyricDocument(
                String source,
                String lyric,
                String rawLyric,
                String trackHintKey,
                long capturedAtMillis,
                LyricProviderCapabilities capabilities,
                String baselineTrackKey) {
            this.source = nullToEmpty(source);
            this.lyric = nullToEmpty(lyric);
            this.rawLyric = nullToEmpty(rawLyric);
            this.trackHintKey = nullToEmpty(trackHintKey);
            this.capturedAtMillis = capturedAtMillis;
            this.capabilities = capabilities == null
                    ? LyricProviderCapabilities.PASSIVE_PARSER
                    : capabilities;
            this.baselineTrackKey = nullToEmpty(baselineTrackKey);
        }
    }

    static final class CaptureUpdate {
        final LyricDocument document;
        final boolean boundToCurrentTrack;

        CaptureUpdate(LyricDocument document, boolean boundToCurrentTrack) {
            this.document = document;
            this.boundToCurrentTrack = boundToCurrentTrack;
        }
    }

    static final class TrackUpdate {
        final TrackSnapshot track;
        final LyricDocument document;
        final long generation;
        final boolean trackChanged;

        TrackUpdate(
                TrackSnapshot track,
                LyricDocument document,
                long generation,
                boolean trackChanged) {
            this.track = track;
            this.document = document;
            this.generation = generation;
            this.trackChanged = trackChanged;
        }
    }

    private final long documentMaxAgeMillis;
    private final int cacheMaxEntries;
    private final LinkedHashMap<String, LyricDocument> documentsByTrack =
            new LinkedHashMap<>(16, 0.75f, true);

    private TrackSnapshot currentTrack;
    private TrackSnapshot awaitingDocumentTrack;
    private LyricDocument pendingDocument;
    private LyricDocument mostRecentDocument;
    private long generation;

    LyricSessionReducer(long documentMaxAgeMillis, int cacheMaxEntries) {
        this.documentMaxAgeMillis = documentMaxAgeMillis;
        this.cacheMaxEntries = Math.max(1, cacheMaxEntries);
    }

    synchronized CaptureUpdate capture(
            String source,
            String lyric,
            String rawLyric,
            String trackHintKey,
            long capturedAtMillis,
            LyricProviderCapabilities capabilities) {
        pruneExpired(capturedAtMillis);
        LyricDocument duplicate = findEquivalentCurrentCapture(
                source,
                lyric,
                rawLyric,
                trackHintKey,
                capturedAtMillis);
        if (duplicate != null) {
            mostRecentDocument = duplicate;
            return new CaptureUpdate(
                    duplicate,
                    currentTrack != null
                            && currentTrack.key.equals(duplicate.boundTrackKey));
        }
        LyricDocument document = new LyricDocument(
                source,
                lyric,
                rawLyric,
                trackHintKey,
                capturedAtMillis,
                capabilities,
                currentTrack == null ? "" : currentTrack.key);
        mostRecentDocument = document;

        boolean bindCurrent = false;
        if (currentTrack != null) {
            if (!document.trackHintKey.isEmpty()
                    && TrackIdentity.matchesHintKey(document.trackHintKey, currentTrack.key)) {
                bindCurrent = true;
            } else if (document.capabilities.associationStrategy
                    == LyricProviderCapabilities.AssociationStrategy.CURRENT_TRACK) {
                bindCurrent = true;
            } else if (document.capabilities.associationStrategy
                    == LyricProviderCapabilities.AssociationStrategy.NEXT_TRACK_OBSERVATION
                    && awaitingDocumentTrack != null
                    && awaitingDocumentTrack.key.equals(currentTrack.key)
                    && document.trackHintKey.isEmpty()) {
                // Track and lyric callbacks may arrive in either order. Once a stable track
                // transition has explicitly declared that it is awaiting a document, the next
                // unlabelled passive capture completes that transaction without relying on a
                // time window. A mismatching advisory hint remains pending because the player
                // may already be preloading the following track.
                bindCurrent = true;
            }
        }

        if (bindCurrent) {
            bind(document, currentTrack.key);
            pendingDocument = null;
            awaitingDocumentTrack = null;
        } else {
            pendingDocument = document;
        }
        trimCache();
        return new CaptureUpdate(document, bindCurrent);
    }

    synchronized TrackUpdate observeTrack(
            TrackSnapshot nextTrack,
            ObservationKind observationKind,
            long observedAtMillis) {
        pruneExpired(observedAtMillis);
        if (nextTrack == null || nextTrack.key.isEmpty()) {
            return new TrackUpdate(currentTrack, null, generation, false);
        }

        TrackSnapshot previousTrack = currentTrack;
        boolean trackChanged = previousTrack == null || !previousTrack.key.equals(nextTrack.key);
        currentTrack = nextTrack;
        if (trackChanged) {
            generation++;
        }

        LyricDocument selected = findDocumentForTrack(nextTrack.key);
        LyricDocument pending = pendingDocument;
        if (isFresh(pending, observedAtMillis)
                && shouldBindPending(
                pending,
                nextTrack,
                previousTrack,
                trackChanged,
                observationKind)) {
            bind(pending, nextTrack.key);
            pendingDocument = null;
            selected = pending;
        }
        if (selected != null) {
            awaitingDocumentTrack = null;
        } else if (trackChanged
                && observationKind == ObservationKind.STABLE_METADATA) {
            awaitingDocumentTrack = nextTrack;
        }
        return new TrackUpdate(nextTrack, selected, generation, trackChanged);
    }

    synchronized TrackSnapshot currentTrack() {
        return currentTrack;
    }

    synchronized long generation() {
        return generation;
    }

    synchronized LyricDocument recentDocument(long nowMillis) {
        pruneExpired(nowMillis);
        return isFresh(mostRecentDocument, nowMillis) ? mostRecentDocument : null;
    }

    synchronized LyricDocument documentForTrack(String trackKey, long nowMillis) {
        pruneExpired(nowMillis);
        return findDocumentForTrack(trackKey);
    }

    private boolean shouldBindPending(
            LyricDocument pending,
            TrackSnapshot nextTrack,
            TrackSnapshot previousTrack,
            boolean trackChanged,
            ObservationKind observationKind) {
        if (!pending.trackHintKey.isEmpty()) {
            if (TrackIdentity.matchesHintKey(pending.trackHintKey, nextTrack.key)) {
                return true;
            }
            if (pending.capabilities.stableTrackIdentity) {
                return false;
            }
        }

        LyricProviderCapabilities.AssociationStrategy strategy =
                pending.capabilities.associationStrategy;
        if (strategy == LyricProviderCapabilities.AssociationStrategy.CURRENT_TRACK) {
            return true;
        }
        if (strategy == LyricProviderCapabilities.AssociationStrategy.EXPLICIT_TRACK) {
            return false;
        }

        // A passive parser callback is a candidate for the next observed track. Relay metadata
        // for the already-playing track is not a transaction boundary and must never consume it.
        if (previousTrack == null) {
            return true;
        }
        if (!nextTrack.key.equals(pending.baselineTrackKey)) {
            return true;
        }
        return trackChanged && observationKind == ObservationKind.STABLE_METADATA;
    }

    private LyricDocument findDocumentForTrack(String trackKey) {
        if (trackKey == null || trackKey.isEmpty()) {
            return null;
        }
        LyricDocument exact = documentsByTrack.get(trackKey);
        if (exact != null && trackKey.equals(exact.boundTrackKey)) {
            return exact;
        }

        LyricDocument mostRecent = null;
        for (LyricDocument candidate : documentsByTrack.values()) {
            if (trackKey.equals(candidate.boundTrackKey)) {
                mostRecent = candidate;
            }
        }
        return mostRecent;
    }

    private LyricDocument findEquivalentCurrentCapture(
            String source,
            String lyric,
            String rawLyric,
            String trackHintKey,
            long capturedAtMillis) {
        LyricDocument recent = mostRecentDocument;
        if (recent == null
                || !recent.source.equals(nullToEmpty(source))
                || !recent.lyric.equals(nullToEmpty(lyric))
                || !recent.rawLyric.equals(nullToEmpty(rawLyric))
                || !recent.trackHintKey.equals(nullToEmpty(trackHintKey))) {
            return null;
        }
        long age = capturedAtMillis - recent.capturedAtMillis;
        if (age < 0L || age > DUPLICATE_CAPTURE_COALESCE_WINDOW_MS) {
            return null;
        }
        String currentKey = currentTrack == null ? "" : currentTrack.key;
        if (!recent.boundTrackKey.isEmpty()) {
            return recent.boundTrackKey.equals(currentKey) ? recent : null;
        }
        return recent.baselineTrackKey.equals(currentKey) ? recent : null;
    }

    private void bind(LyricDocument document, String trackKey) {
        document.boundTrackKey = trackKey;
        documentsByTrack.put(trackKey, document);
        trimCache();
    }

    private void pruneExpired(long nowMillis) {
        if (pendingDocument != null && !isFresh(pendingDocument, nowMillis)) {
            pendingDocument = null;
        }
        if (mostRecentDocument != null && !isFresh(mostRecentDocument, nowMillis)) {
            mostRecentDocument = null;
        }
        documentsByTrack.entrySet().removeIf(
                entry -> !isFresh(entry.getValue(), nowMillis));
    }

    private boolean isFresh(LyricDocument document, long nowMillis) {
        if (document == null) {
            return false;
        }
        long age = nowMillis - document.capturedAtMillis;
        return age >= 0L && age <= documentMaxAgeMillis;
    }

    private void trimCache() {
        while (documentsByTrack.size() > cacheMaxEntries) {
            String eldest = documentsByTrack.keySet().iterator().next();
            documentsByTrack.remove(eldest);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
