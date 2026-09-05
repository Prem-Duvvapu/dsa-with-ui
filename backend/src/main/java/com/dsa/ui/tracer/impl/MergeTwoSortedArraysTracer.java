package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Merges two sorted arrays in place, without the O(N+M) extra buffer an ordinary merge
 * step would need. The gap method (a Shell-sort-derived trick) treats both arrays as one
 * combined buffer and repeatedly compares pairs a fixed distance ("gap") apart, swapping
 * out-of-order pairs and halving the gap each pass until it reaches 1 - at which point the
 * whole buffer is sorted, arr1's prefix and arr2's suffix included.
 */
@Component
public class MergeTwoSortedArraysTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "merge-two-sorted-arrays";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("arr1", FieldType.INT_ARRAY)
                        .label("Array 1 (sorted)")
                        .help("Already sorted ascending; merges in place with array 2.")
                        .length(1, 10).values(-50, 50).sorted()
                        .defaultValue(java.util.List.of(1, 3, 5, 7))
                        .build(),
                InputField.of("arr2", FieldType.INT_ARRAY)
                        .label("Array 2 (sorted)")
                        .help("Already sorted ascending; merges in place with array 1.")
                        .length(1, 10).values(-50, 50).sorted()
                        .defaultValue(java.util.List.of(0, 2, 6, 8))
                        .build());
    }

    /** Very different shape: 2 elements against 6, rather than an equal 4-and-4 split. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "arr1", java.util.List.of(10, 20),
                "arr2", java.util.List.of(1, 2, 3, 4, 5, 30));
    }

    @Override
    public String annotatedCode() {
        return """
               public void merge(int[] arr1, int[] arr2, int n, int m) {
                   int len = n + m;
                   // @a initGap
                   int gap = (len / 2) + (len % 2);

                   while (gap > 0) {
                       int left = 0, right = left + gap;
                       while (right < len) {
                           // @a compare
                           int leftVal = get(arr1, arr2, n, left);
                           int rightVal = get(arr1, arr2, n, right);
                           if (leftVal > rightVal) {
                               // @a swap
                               set(arr1, arr2, n, left, rightVal);
                               set(arr1, arr2, n, right, leftVal);
                           } else {
                               // @a noSwap
                           }
                           left++; right++;
                       }
                       if (gap == 1) break;
                       // @a shrinkGap
                       gap = (gap / 2) + (gap % 2);
                   }
                   // @a done
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr1 = in.getIntArray("arr1");
        int[] arr2 = in.getIntArray("arr2");
        int n = arr1.length, m = arr2.length;
        int len = n + m;
        int[] combined = new int[len];
        System.arraycopy(arr1, 0, combined, 0, n);
        System.arraycopy(arr2, 0, combined, n, m);

        int gap = (len / 2) + (len % 2);
        emit.at("initGap")
                .say("Combined buffer (arr1 then arr2): %s. Initial gap = ceil(%d/2) = %d.",
                        java.util.Arrays.toString(combined), len, gap)
                .var("gap", gap)
                .array(combined).step();

        while (gap > 0) {
            int left = 0, right = left + gap;
            while (right < len) {
                emit.at("compare")
                        .say("Gap=%d: compare position %d (%d) against position %d (%d).",
                                gap, left, combined[left], right, combined[right])
                        .var("left", left).var("right", right)
                        .array(combined, left, right).step();

                if (combined[left] > combined[right]) {
                    int leftVal = combined[left], rightVal = combined[right];
                    combined[left] = rightVal;
                    combined[right] = leftVal;
                    emit.at("swap")
                            .say("%d > %d: swap. Buffer now %s.",
                                    leftVal, rightVal, java.util.Arrays.toString(combined))
                            .var("swapped", leftVal + " <-> " + rightVal)
                            .array(combined, left, right).step();
                } else {
                    emit.at("noSwap")
                            .say("%d <= %d: already in order, no swap.",
                                    combined[left], combined[right])
                            .array(combined, left, right).step();
                }
                left++;
                right++;
            }
            if (gap == 1) {
                break;
            }
            gap = (gap / 2) + (gap % 2);
            emit.at("shrinkGap")
                    .say("Pass complete. Gap shrinks to %d.", gap)
                    .var("gap", gap)
                    .array(combined).step();
        }

        emit.at("done")
                .say("Gap reached 1 and its pass finished - buffer fully merged: %s.",
                        java.util.Arrays.toString(combined))
                .array(combined).step();
    }
}
