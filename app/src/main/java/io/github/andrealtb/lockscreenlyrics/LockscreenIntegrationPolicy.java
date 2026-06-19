package io.github.andrealtb.lockscreenlyrics;

import java.util.ArrayList;
import java.util.List;

final class LockscreenIntegrationPolicy {
    enum LyricInfoSource {
        PLAYER_INTEGRATION,
        MODULE_CAPTURE,
        PLAYER_FALLBACK,
        NONE
    }

    private LockscreenIntegrationPolicy() {
    }

    static LyricInfoSource chooseLyricInfoSource(
            boolean hasUsablePlayerLyricInfo,
            boolean hasPlayerIntegrationData,
            boolean hasCapturedLyricForCurrentTrack) {
        if (hasPlayerIntegrationData) {
            return LyricInfoSource.PLAYER_INTEGRATION;
        }
        if (hasCapturedLyricForCurrentTrack) {
            return LyricInfoSource.MODULE_CAPTURE;
        }
        return hasUsablePlayerLyricInfo ? LyricInfoSource.PLAYER_FALLBACK : LyricInfoSource.NONE;
    }

    static boolean activeTextMatches(String renderedText, String activeText) {
        return renderedText != null
                && !renderedText.isEmpty()
                && renderedText.equals(activeText);
    }

    static ArrayList<Object> promoteActionIdentity(List<?> actions, Object target) {
        if (actions == null || actions.isEmpty() || target == null || actions.get(0) == target) {
            return null;
        }

        int targetIndex = -1;
        for (int index = 0; index < actions.size(); index++) {
            if (actions.get(index) == target) {
                targetIndex = index;
                break;
            }
        }
        if (targetIndex < 0) {
            return null;
        }

        ArrayList<Object> ordered = new ArrayList<>(actions);
        ordered.remove(targetIndex);
        ordered.add(0, target);
        return ordered;
    }
}
