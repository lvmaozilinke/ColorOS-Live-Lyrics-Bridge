package io.github.andrealtb.lockscreenlyrics;

/**
 * Describes what a lyric source can prove when it emits a lyric document.
 *
 * <p>The reducer uses this declaration instead of inferring source semantics from a player
 * package name. DexKit adapters, public integrations and future native providers can therefore
 * share the same session pipeline.</p>
 */
final class LyricProviderCapabilities {
    enum AssociationStrategy {
        /** The document contains a stable track identity such as songId/title/artist. */
        EXPLICIT_TRACK,
        /** The callback belongs to the next track observation, not necessarily the current one. */
        NEXT_TRACK_OBSERVATION,
        /** The provider guarantees that its callback belongs to the current published track. */
        CURRENT_TRACK
    }

    static final LyricProviderCapabilities PASSIVE_PARSER =
            new LyricProviderCapabilities(
                    AssociationStrategy.NEXT_TRACK_OBSERVATION,
                    false);

    static final LyricProviderCapabilities ACTIVE_INTEGRATION =
            new LyricProviderCapabilities(
                    AssociationStrategy.EXPLICIT_TRACK,
                    true);

    final AssociationStrategy associationStrategy;
    final boolean stableTrackIdentity;

    LyricProviderCapabilities(
            AssociationStrategy associationStrategy,
            boolean stableTrackIdentity) {
        this.associationStrategy = associationStrategy;
        this.stableTrackIdentity = stableTrackIdentity;
    }
}
