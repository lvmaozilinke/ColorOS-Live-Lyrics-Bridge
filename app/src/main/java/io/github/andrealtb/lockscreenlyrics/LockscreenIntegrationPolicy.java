package io.github.andrealtb.lockscreenlyrics;

import java.util.ArrayList;
import java.util.List;

final class LockscreenIntegrationPolicy {
    private LockscreenIntegrationPolicy() {
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
