package io.github.andrealtb.lockscreenlyrics;

final class LyricTextSanitizer {
    private LyricTextSanitizer() {
    }

    static String removeIgnorableCharacters(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder cleaned = null;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!isIgnorableCharacter(character)) {
                if (cleaned != null) {
                    cleaned.append(character);
                }
                continue;
            }
            if (cleaned == null) {
                cleaned = new StringBuilder(value.length());
                cleaned.append(value, 0, index);
            }
        }
        return cleaned == null ? value : cleaned.toString();
    }

    static boolean isIgnorableCharacter(char character) {
        return character == '\u200B'
                || character == '\u2060'
                || character == '\uFEFF';
    }
}
