package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.StepEmitter;

import java.util.Arrays;

/**
 * The owner's own KMP prefix-function loop, as they wrote it in their Rotate String submission:
 * {@code j} walks the pattern from 1, {@code currLength} is the length of the prefix matched so far,
 * and on a mismatch it falls back to {@code lps[currLength - 1]}. Shared by the tracers whose
 * answer is read straight off the lps table.
 */
final class OwnerLps {

    private OwnerLps() {
    }

    /** Runs the loop, emitting one step per iteration at {@code anchor}; returns the table. */
    static int[] build(String pattern, StepEmitter emit, String anchor) {
        int m = pattern.length();
        int[] lps = new int[m];
        int j = 1;
        int currLength = 0;
        while (j < m) {
            String what;
            int at = j;
            if (pattern.charAt(j) == pattern.charAt(currLength)) {
                currLength++;
                lps[j] = currLength;
                what = String.format("pattern[%d] = '%c' continues the matched prefix: lps[%d] = %d.",
                        j, pattern.charAt(j), j, currLength);
                j++;
            } else if (currLength == 0) {
                lps[j] = 0;
                what = String.format("pattern[%d] = '%c' starts no prefix: lps[%d] = 0.", j, pattern.charAt(j), j);
                j++;
            } else {
                int was = currLength;
                currLength = lps[currLength - 1];
                what = String.format("pattern[%d] = '%c' breaks the %d-letter match; try the next shorter "
                        + "candidate, length %d (lps[%d]).", j, pattern.charAt(j), was, currLength, was - 1);
            }
            emit.at(anchor).say(what)
                    .var("j", at).var("currLength", currLength).var("lps", Arrays.toString(lps))
                    .chars(pattern, at, currLength == 0 ? -1 : currLength - 1).step();
        }
        return lps;
    }
}
