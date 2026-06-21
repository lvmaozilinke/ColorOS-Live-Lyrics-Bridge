package io.github.andrealtb.lockscreenlyrics;

interface PlayerAdapter {
    String packageName();

    String displayName();

    LyricProviderCapabilities lyricCapabilities();

    void installLyricSourceHooks(LockscreenLyricsModule module, ClassLoader classLoader);
}
