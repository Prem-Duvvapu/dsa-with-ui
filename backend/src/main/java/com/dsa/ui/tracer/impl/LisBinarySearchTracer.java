package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;
import java.util.Map;

/**
 * LIS length via patience sorting: tails[k] is the smallest possible tail of any
 * increasing subsequence of length k+1. The array stays sorted, so each element
 * finds its slot with a binary search (lower_bound).
 */
@Component
public class LisBinarySearchTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "lis-binary-search";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("tails[k] = smallest tail of an increasing subsequence of length k+1.")
                        .length(1, 30).values(-999, 999)
                        .defaultValue(List.of(10, 9, 2, 5, 3, 7, 101, 18))
                        .build());
    }

    /** Strictly decreasing: every element replaces tails[0], nothing ever appends. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(9, 6, 4, 2));
    }

    @Override
    public String annotatedCode() {
        return """
               public int lengthOfLIS(int[] nums) {
                   // @a init
                   int[] tails = new int[nums.length];
                   int size = 0;
                   for (int x : nums) {
                       // @a probe
                       int lo = 0, hi = size;
                       while (lo < hi) {
                           int mid = (lo + hi) >>> 1;
                           if (tails[mid] < x) lo = mid + 1;
                           else hi = mid;
                       }
                       // @a place
                       tails[lo] = x;
                       if (lo == size) size++;
                   }
                   // @a done
                   return size;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int[] tails = new int[nums.length];
        int size = 0;

        emit.at("init").say("tails[] is empty. tails[k] will always hold the smallest value that can end a rising run of length k+1 - that is what keeps it sorted.")
                .var("size", 0).array(new int[0]).step();

        for (int x : nums) {
            int lo = 0, hi = size;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (tails[mid] < x) {
                    lo = mid + 1;
                    emit.at("probe").say("x=%d: tails[%d]=%d < x, so x belongs to the right of %d. Search [%d,%d).",
                                    x, mid, tails[mid], mid, lo, hi)
                            .var("x", x).var("lo", lo).var("hi", hi).var("size", size)
                            .array(java.util.Arrays.copyOf(tails, size), Math.min(mid, Math.max(size - 1, 0))).step();
                } else {
                    hi = mid;
                    emit.at("probe").say("x=%d: tails[%d]=%d >= x, so x could take %d's slot or an earlier one. Search [%d,%d).",
                                    x, mid, tails[mid], mid, lo, hi)
                            .var("x", x).var("lo", lo).var("hi", hi).var("size", size)
                            .array(java.util.Arrays.copyOf(tails, size), Math.min(mid, Math.max(size - 1, 0))).step();
                }
            }
            boolean appended = lo == size;
            tails[lo] = x;
            if (appended) {
                size++;
                emit.at("place").say("x=%d is above every tail, so it extends the longest run: append at index %d. Length grows to %d.",
                                x, lo, size)
                        .var("x", x).var("pos", lo).var("size", size)
                        .array(java.util.Arrays.copyOf(tails, size), lo).step();
            } else {
                emit.at("place").say("x=%d lands at index %d: a smaller tail can now end a run of length %d. Length stays %d.",
                                x, lo, lo + 1, size)
                        .var("x", x).var("pos", lo).var("size", size)
                        .array(java.util.Arrays.copyOf(tails, size), lo).step();
            }
        }

        emit.at("done").say("size never shrank and tails stayed sorted the whole way - LIS length is %d.", size)
                .var("size", size).array(java.util.Arrays.copyOf(tails, size)).step();
    }
}
