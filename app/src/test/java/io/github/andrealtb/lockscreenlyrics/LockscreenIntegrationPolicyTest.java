package io.github.andrealtb.lockscreenlyrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class LockscreenIntegrationPolicyTest {
    @Test
    public void repeatedLyricTextStillMatchesTheCurrentlyActiveLine() {
        String dorothea = "Hey Dorothea do you ever stop and think about me";

        assertTrue(LockscreenIntegrationPolicy.activeTextMatches(dorothea, dorothea));
    }

    @Test
    public void translationActionMovesAheadOfTransportActionsByIdentity() {
        Object previous = new Object();
        Object next = new Object();
        Object translation = new Object();
        List<Object> source = Arrays.asList(previous, next, translation);

        ArrayList<Object> ordered =
                LockscreenIntegrationPolicy.promoteActionIdentity(source, translation);

        assertSame(translation, ordered.get(0));
        assertEquals(Arrays.asList(translation, previous, next), ordered);
        assertEquals(Arrays.asList(previous, next, translation), source);
    }

    @Test
    public void actionOrderIsLeftUntouchedWhenTranslationIsAlreadyFirst() {
        Object translation = new Object();

        assertNull(LockscreenIntegrationPolicy.promoteActionIdentity(
                Arrays.asList(translation, new Object()), translation));
    }
}
