package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * A direct generalization of {@link MedianTwoSortedArraysTracer}: the same four-boundary
 * partition check, but the cut is driven by k instead of always bisecting to the halfway
 * point. The median is exactly this algorithm's k = (n1+n2+1)/2 case - finding "the middle
 * element" and "the k-th element" are the same search with a different target cut.
 */
@Component
public class KthElementTwoSortedArraysTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "kth-element-2-sorted-arrays";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums1", FieldType.INT_ARRAY)
                        .label("Array 1 (sorted)")
                        .length(1, 10).values(-1000, 1000).sorted()
                        .defaultValue(java.util.List.of(-13, -11, 14, 16))
                        .build(),
                InputField.of("nums2", FieldType.INT_ARRAY)
                        .label("Array 2 (sorted)")
                        .length(1, 10).values(-1000, 1000).sorted()
                        .defaultValue(java.util.List.of(-14, -9, -1, 15, 17))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("K (1-indexed)")
                        .help("Must be between 1 and the combined length of both arrays.")
                        .range(1, 20)
                        .defaultValue(4)
                        .build());
    }

    /** Different arrays: exercises the swap and the shrinkHigh/shrinkLow branches this default misses. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "nums1", java.util.List.of(-7, -5, -3, 0, 6, 12),
                "nums2", java.util.List.of(-13, 0, 2, 9, 12),
                "k", 4);
    }

    @Override
    public String annotatedCode() {
        return """
               public int kthElement(int[] nums1, int[] nums2, int k) {
                   int[] a = nums1, b = nums2;
                   if (a.length > b.length) {
                       // @a swapToSmaller
                       a = nums2; b = nums1;
                   }
                   int n1 = a.length, n2 = b.length;
                   int low = Math.max(0, k - n2), high = Math.min(k, n1);

                   while (low <= high) {
                       // @a partition
                       int cut1 = (low + high) / 2;
                       int cut2 = k - cut1;

                       // @a boundaries
                       int l1 = cut1 == 0 ? Integer.MIN_VALUE : a[cut1 - 1];
                       int l2 = cut2 == 0 ? Integer.MIN_VALUE : b[cut2 - 1];
                       int r1 = cut1 == n1 ? Integer.MAX_VALUE : a[cut1];
                       int r2 = cut2 == n2 ? Integer.MAX_VALUE : b[cut2];

                       if (l1 <= r2 && l2 <= r1) {
                           // @a found
                           return Math.max(l1, l2);
                       } else if (l1 > r2) {
                           // @a shrinkHigh
                           high = cut1 - 1;
                       } else {
                           // @a shrinkLow
                           low = cut1 + 1;
                       }
                   }
                   return -1;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums1 = in.getIntArray("nums1");
        int[] nums2 = in.getIntArray("nums2");
        int k = in.getInt("k");
        int[] a = nums1, b = nums2;

        if (a.length > b.length) {
            emit.at("swapToSmaller")
                    .say("Array 1 (length %d) is longer than array 2 (length %d) - always "
                            + "partition the smaller one, so swap roles.", a.length, b.length)
                    .array(concat(a, b)).step();
            a = nums2;
            b = nums1;
        }

        int n1 = a.length, n2 = b.length;
        int low = Math.max(0, k - n2), high = Math.min(k, n1);

        while (low <= high) {
            int cut1 = (low + high) / 2;
            int cut2 = k - cut1;
            emit.at("partition")
                    .say("Try cutting the smaller array after %d element%s. That forces the "
                            + "larger array's cut after %d element%s, so the left side holds "
                            + "exactly %d elements - the first %d of the merged order.",
                            cut1, cut1 == 1 ? "" : "s", cut2, cut2 == 1 ? "" : "s", k, k)
                    .var("low", low).var("high", high).var("cut1", cut1).var("cut2", cut2)
                    .array(concat(a, b)).step();

            int l1 = cut1 == 0 ? Integer.MIN_VALUE : a[cut1 - 1];
            int l2 = cut2 == 0 ? Integer.MIN_VALUE : b[cut2 - 1];
            int r1 = cut1 == n1 ? Integer.MAX_VALUE : a[cut1];
            int r2 = cut2 == n2 ? Integer.MAX_VALUE : b[cut2];

            emit.at("boundaries")
                    .say("Left boundaries: %s, %s. Right boundaries: %s, %s.",
                            fmt(l1), fmt(l2), fmt(r1), fmt(r2))
                    .var("l1", fmt(l1)).var("l2", fmt(l2)).var("r1", fmt(r1)).var("r2", fmt(r2))
                    .array(concat(a, b)).step();

            if (l1 <= r2 && l2 <= r1) {
                int answer = Math.max(l1, l2);
                emit.at("found")
                        .say("Every left element is <= every right element in both arrays - "
                                + "valid partition. The %d-th element is %d.", k, answer)
                        .var("answer", answer)
                        .array(concat(a, b)).step();
                return;
            } else if (l1 > r2) {
                emit.at("shrinkHigh")
                        .say("%d > %d - the smaller array's cut is too far right. Move it left.",
                                l1, r2)
                        .var("high", cut1 - 1)
                        .array(concat(a, b)).step();
                high = cut1 - 1;
            } else {
                emit.at("shrinkLow")
                        .say("%d > %d - the smaller array's cut is too far left. Move it right.",
                                l2, r1)
                        .var("low", cut1 + 1)
                        .array(concat(a, b)).step();
                low = cut1 + 1;
            }
        }
    }

    private static String fmt(int boundary) {
        if (boundary == Integer.MIN_VALUE) {
            return "-inf";
        }
        if (boundary == Integer.MAX_VALUE) {
            return "+inf";
        }
        return String.valueOf(boundary);
    }

    private static int[] concat(int[] a, int[] b) {
        int[] out = new int[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
