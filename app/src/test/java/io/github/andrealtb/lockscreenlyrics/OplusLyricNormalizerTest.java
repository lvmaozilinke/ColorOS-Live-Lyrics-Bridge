package io.github.andrealtb.lockscreenlyrics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class OplusLyricNormalizerTest {
    @Test
    public void sameTimestampTranslationKeepsOnlyPrimaryLine() {
        String lrc = "[00:01.20]Put your lips close to mine\n"
                + "[00:01.20]\u8bf7\u9760\u8fd1\u6211 \u8f7b\u543b\u6211\u7684\u53cc\u5507";

        String normalized = OplusLyricNormalizer.normalizeForOfficialList(lrc);

        assertEquals("[00:01.200]Put your lips close to mine\n"
                + "[00:09.200]\u200B", normalized);
    }

    @Test
    public void delayedFirstLineUsesPreRollInsteadOfAddingASecondItem() {
        String lrc = "[00:02.00]Hello";

        String normalized = OplusLyricNormalizer.normalizeForOfficialList(lrc);

        assertEquals("[00:00.000]Hello\n[00:10.000]\u200B", normalized);
    }

    @Test
    public void repeatedPrimaryTextKeepsOccurrenceOrder() {
        String lrc = "[00:01.00]Stay\n"
                + "[00:05.00]Run\n"
                + "[00:09.00]Stay";

        String normalized = OplusLyricNormalizer.normalizeForOfficialList(lrc);

        assertEquals("[00:01.000]Stay\n"
                + "[00:05.000]Run\n"
                + "[00:09.000]Stay\n"
                + "[00:17.000]\u200B", normalized);
    }

    @Test
    public void inlineWordTimingTagsDoNotBecomeOfficialText() {
        String lrc = "[00:01.00]<00:01.20>Hello <00:01.70>world";

        String normalized = OplusLyricNormalizer.normalizeForOfficialList(lrc);

        assertEquals("[00:01.000]Hello world\n[00:09.000]\u200B", normalized);
    }

    @Test
    public void zeroWidthSpacerDoesNotBecomeOfficialItem() {
        String lrc = "[00:00.00]Before\n"
                + "[00:38.13]\u200B\n"
                + "[00:38.15]And all of the foes and all of the friends\n"
                + "[00:38.15]\u6240\u6709\u7684\u5bf9\u624b "
                + "\u6240\u6709\u7684\u670b\u53cb\u200B";

        String normalized = OplusLyricNormalizer.normalizeForOfficialList(lrc);

        assertEquals("[00:00.000]Before\n"
                + "[00:38.150]And all of the foes and all of the friends\n"
                + "[00:46.150]\u200B", normalized);
    }

}
