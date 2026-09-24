package dev.cmreborn;

import java.util.Collection;
import java.util.LinkedHashSet;

/** Decisions shared by cold scans and the persistent cache. No Android dependencies. */
final class ResolutionPolicy {
    private ResolutionPolicy() {}

    static <T> T unique(Collection<T> candidates) {
        LinkedHashSet<T> distinct = new LinkedHashSet<>(candidates);
        return distinct.size() == 1 ? distinct.iterator().next() : null;
    }

    static boolean allowFallback(boolean discoveryOnly, boolean ambiguous, long version,
            String baseDigest) {
        return !discoveryOnly && !ambiguous && version == 322660063L
                && "30f8b15f2d27ecdadf23f10126abd714e3be857340de17237e231a3ac32690e1"
                .equals(baseDigest);
    }

    static String cacheKey(int schema, String moduleVersion, long hostVersion,
            Collection<String> apkDigests) {
        return schema + ":" + moduleVersion + ":" + hostVersion + ":"
                + String.join(":", apkDigests);
    }
}
