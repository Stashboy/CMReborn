package dev.cmreborn;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class ResolutionPolicyTest {
    private static final String VERIFIED =
            "30f8b15f2d27ecdadf23f10126abd714e3be857340de17237e231a3ac32690e1";

    @Test public void missingAndAmbiguousMatchesNeverChooseTheFirstCandidate() {
        assertNull(ResolutionPolicy.unique(Collections.emptyList()));
        assertNull(ResolutionPolicy.unique(Arrays.asList("first", "second")));
        assertEquals("same", ResolutionPolicy.unique(Arrays.asList("same", "same")));
    }

    @Test public void fallbackRequiresBothTheVerifiedVersionAndTheExactApk() {
        assertTrue(ResolutionPolicy.allowFallback(false, false, 322660063, VERIFIED));
        assertFalse(ResolutionPolicy.allowFallback(false, false, 322660064, VERIFIED));
        assertFalse(ResolutionPolicy.allowFallback(false, false, 322660063, "changed"));
        assertFalse(ResolutionPolicy.allowFallback(false, false, 322660063, ""));
    }

    @Test public void ambiguousFingerprintsCannotBeOverruledByKnownNames() {
        assertFalse(ResolutionPolicy.allowFallback(false, true, 322660063, VERIFIED));
    }

    @Test public void discoveryOnlyTestingDisablesValidatedFallbacks() {
        assertFalse(ResolutionPolicy.allowFallback(true, false, 322660063, VERIFIED));
    }

    @Test public void cacheInvalidatesForModuleRulesHostVersionAndEveryApk() {
        String original = ResolutionPolicy.cacheKey(1, "1.1.29", 322660063, Arrays.asList("base", "split"));
        assertNotEquals(original, ResolutionPolicy.cacheKey(2, "1.1.29", 322660063, Arrays.asList("base", "split")));
        assertNotEquals(original, ResolutionPolicy.cacheKey(1, "1.1.30", 322660063, Arrays.asList("base", "split")));
        assertNotEquals(original, ResolutionPolicy.cacheKey(1, "1.1.29", 322660064, Arrays.asList("base", "split")));
        assertNotEquals(original, ResolutionPolicy.cacheKey(1, "1.1.29", 322660063, Arrays.asList("newBase", "split")));
        assertNotEquals(original, ResolutionPolicy.cacheKey(1, "1.1.29", 322660063, Arrays.asList("base", "newSplit")));
    }
}
