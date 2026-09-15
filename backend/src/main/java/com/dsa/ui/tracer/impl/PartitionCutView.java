package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;

import java.util.ArrayList;
import java.util.List;

/**
 * The cells row shared by {@link MedianTwoSortedArraysTracer} and
 * {@link KthElementTwoSortedArraysTracer}.
 *
 * <p>Two things it fixes, both of which were wrong when these tracers emitted a bare
 * {@code concat(a, b)}:
 *
 * <p><b>Order is the caller's, not the algorithm's.</b> Both tracers may swap which array
 * they cut, and concatenating post-swap silently reordered the row halfway through the
 * trace - cell 0 meant {@code nums1[0]} on the first step and {@code nums2[0]} on the
 * second, and the row stopped being sorted in any direction. The viewer sees the two
 * arrays they typed, in the order they typed them, for the whole run.
 *
 * <p><b>Only four elements decide a partition.</b> That is the entire insight of the
 * algorithm - it never reads either array's interior - and a row where every cell looked
 * identical said the opposite. The two elements left of the cuts and the two right of them
 * carry live states, so {@code SearchSpaceCanvas} draws exactly the four values the
 * narration is comparing.
 *
 * <p>Marking them also keeps the canvas out of index mode. {@code SearchSpaceCanvas} reads
 * {@code [low, high]} as indices into the cells when nothing live sits outside that range,
 * which would frame the first few merged values as "the surviving search space". They are
 * not: {@code low} and {@code high} count cut positions in the smaller array. Whenever
 * {@code nums1} is the longer array - true of both tracers' defaults and alternates - the
 * cut array's highlighted cells sit past {@code high}, and the canvas draws the answer-space
 * view that is actually correct here.
 */
final class PartitionCutView {

    private PartitionCutView() {
    }

    /** Both arrays, caller's order, nothing highlighted: for steps before a cut exists. */
    static List<ArrayElement> plain(int[] nums1, int[] nums2) {
        List<ArrayElement> cells = new ArrayList<>(nums1.length + nums2.length);
        for (int i = 0; i < nums1.length; i++) {
            cells.add(new ArrayElement(i, nums1[i], "default"));
        }
        for (int j = 0; j < nums2.length; j++) {
            cells.add(new ArrayElement(nums1.length + j, nums2[j], "default"));
        }
        return cells;
    }

    /**
     * Both arrays with the four boundary elements marked.
     *
     * @param cutArray  the array being cut - {@code nums2} once the tracer swapped, else {@code nums1}
     * @param cut1      elements of {@code cutArray} left of its cut
     * @param cut2      elements of the other array left of its cut
     */
    static List<ArrayElement> withCuts(int[] nums1, int[] nums2, int[] cutArray, int cut1, int cut2) {
        boolean swapped = cutArray != nums1;
        int cutBase = swapped ? nums1.length : 0;
        int otherBase = swapped ? 0 : nums1.length;
        int cutLength = swapped ? nums2.length : nums1.length;
        int otherLength = swapped ? nums1.length : nums2.length;

        List<ArrayElement> cells = plain(nums1, nums2);
        mark(cells, cutBase, cutLength, cut1);
        mark(cells, otherBase, otherLength, cut2);
        return cells;
    }

    /**
     * "target" on the element the cut sits just after, "probe" on the one it sits just
     * before. Both are states SearchSpaceCanvas treats as live. A cut at 0 or at the end
     * has no element on that side - that is the +/-infinity the narration names, and there
     * is correspondingly nothing to highlight.
     */
    private static void mark(List<ArrayElement> cells, int base, int length, int cut) {
        if (cut > 0 && cut <= length) {
            cells.get(base + cut - 1).setState("target");
        }
        if (cut < length) {
            cells.get(base + cut).setState("probe");
        }
    }
}
