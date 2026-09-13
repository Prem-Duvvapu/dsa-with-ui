package com.dsa.ui.tracer.impl;

/**
 * Number/noun agreement for narration.
 *
 * <p>"at most 1 replacements", "the smallest of the 1 live heads", "only 1 nodes remain":
 * every trace that counts something eventually prints the count 1, and a format string with
 * a hardcoded "s" gets it wrong on exactly that step. Spread across the catalogue this was
 * 31 steps in 12 problems, found by checking every golden file rather than by reading them.
 *
 * <p>Cheap to fix, easy to reintroduce, and worth a shared helper so the fix is one call
 * rather than a ternary written out at each site.
 */
final class Narration {

    private Narration() {
    }

    /** "" when there is one of them, "s" otherwise. */
    static String s(long n) {
        return n == 1 ? "" : "s";
    }

    /** Verb agreement to match. */
    static String is(long n) {
        return n == 1 ? "is" : "are";
    }

    /**
     * "1st", "2nd", "3rd", "4th" - and "11th", "12th", "13th", which are the three the
     * naive last-digit rule gets wrong.
     *
     * <p>Several tracers printed "%d-th" instead: "the 2-th largest", "the 3-th root",
     * "the 5-th missing positive integer". Twelve steps across seven problems, and it reads
     * like a format placeholder that never got finished.
     */
    static String ordinal(long n) {
        long abs = Math.abs(n);
        String suffix = "th";
        if (abs % 100 < 11 || abs % 100 > 13) {
            suffix = switch ((int) (abs % 10)) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
        }
        return n + suffix;
    }

    /** For nouns that do not simply take an "s" - entry/entries, is/are, has/have. */
    static String plural(long n, String one, String many) {
        return n == 1 ? one : many;
    }
}
