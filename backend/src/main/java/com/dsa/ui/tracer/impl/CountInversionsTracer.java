package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Counts inversions - pairs (i, j) with i &lt; j and a[i] &gt; a[j] - via the merge-sort
 * counting trick: every time a merge takes from the right half, every remaining element in
 * the left half forms one new inversion with it, so the count can be read straight off
 * {@code mid - left + 1} without ever comparing every pair.
 */
@Component
public class CountInversionsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "count-inversions";
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
                        .help("Not sorted - the number of inversions is exactly how far it is from sorted order.")
                        .length(1, 24).values(-999, 999)
                        .defaultValue(List.of(5, 3, 2, 4, 1))
                        .build());
    }

    /** Fully reverse-sorted: every merge takes from the right, so "take" never fires. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(5, 4, 3, 2, 1));
    }

    @Override
    public String annotatedCode() {
        return """
               public int numberOfInversions(int[] a, int n) {
                   int total = countInversions(a, 0, n - 1);
                   // @a done
                   return total;
               }

               private int countInversions(int[] a, int low, int high) {
                   // @a base
                   if (low >= high) return 0;
                   // @a split
                   int mid = (low + high) / 2;
                   int cnt = countInversions(a, low, mid);
                   cnt += countInversions(a, mid + 1, high);
                   cnt += merge(a, low, mid, high);
                   return cnt;
               }

               private int merge(int[] a, int low, int mid, int high) {
                   List<Integer> temp = new ArrayList<>();
                   int left = low, right = mid + 1, cnt = 0;
                   while (left <= mid && right <= high) {
                       // @a compare
                       if (a[left] <= a[right]) {
                           // @a take
                           temp.add(a[left++]);
                       } else {
                           // @a inversion
                           cnt += (mid - left + 1);
                           temp.add(a[right++]);
                       }
                   }
                   while (left <= mid) temp.add(a[left++]);
                   while (right <= high) temp.add(a[right++]);
                   for (int i = low; i <= high; i++) a[i] = temp.get(i - low);
                   // @a writeback
                   return cnt;
               }""";
    }

    /** In-range indices stay visible; the two active pointers get distinct colors. */
    private List<ArrayElement> window(int[] a, int low, int high, int primary, int secondary) {
        List<ArrayElement> state = new ArrayList<>(a.length);
        for (int i = 0; i < a.length; i++) {
            String s;
            if (i == primary) {
                s = "current";
            } else if (i == secondary) {
                s = "swapping";
            } else if (i < low || i > high) {
                s = "visited";
            } else {
                s = "target";
            }
            state.add(new ArrayElement(i, a[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] a = in.getIntArray("nums");
        int n = a.length;
        int total = mergeSortCount(a, 0, n - 1, emit);

        emit.at("done")
                .say("Recursion complete. Total inversions across the whole array: %d.", total)
                .var("answer", total)
                .arrayState(window(a, 0, n - 1, -1, -1)).step();
    }

    private int mergeSortCount(int[] a, int low, int high, StepEmitter emit) {
        if (low >= high) {
            emit.at("base")
                    .say("Range [%d,%d] holds %s - nothing to compare, so this call "
                            + "contributes 0 inversions.",
                            low, high, low == high ? "a single element" : "no elements")
                    .var("low", low).var("high", high)
                    .arrayState(window(a, low, high, low, -1)).step();
            return 0;
        }

        int mid = (low + high) / 2;
        emit.push("count(" + low + ".." + high + ")");
        emit.at("split")
                .say("Split [%d,%d] at mid=%d into [%d,%d] and [%d,%d], then recurse on each half.",
                        low, high, mid, low, mid, mid + 1, high)
                .var("low", low).var("mid", mid).var("high", high)
                .arrayState(window(a, low, high, mid, -1)).step();

        int cnt = mergeSortCount(a, low, mid, emit);
        cnt += mergeSortCount(a, mid + 1, high, emit);
        cnt += merge(a, low, mid, high, emit);
        emit.pop();
        return cnt;
    }

    private int merge(int[] a, int low, int mid, int high, StepEmitter emit) {
        List<Integer> temp = new ArrayList<>();
        int left = low;
        int right = mid + 1;
        int cnt = 0;

        while (left <= mid && right <= high) {
            emit.at("compare")
                    .say("Merging [%d,%d]: compare a[%d]=%d against a[%d]=%d.",
                            low, high, left, a[left], right, a[right])
                    .var("left", left).var("right", right)
                    .arrayState(window(a, low, high, left, right)).step();

            if (a[left] <= a[right]) {
                emit.at("take")
                        .say("%d <= %d, so the left side is not inverted here - take a[%d]=%d.",
                                a[left], a[right], left, a[left])
                        .var("taken", a[left])
                        .arrayState(window(a, low, high, left, right)).step();
                temp.add(a[left]);
                left++;
            } else {
                int contributed = mid - left + 1;
                cnt += contributed;
                emit.at("inversion")
                        .say("%d > %d, so a[%d] is out of order against every remaining "
                                + "left element (indices %d..%d) - that is %d new inversion%s.",
                                a[left], a[right], right, left, mid, contributed,
                                contributed == 1 ? "" : "s")
                        .var("contributed", contributed).var("runningCount", cnt)
                        .arrayState(window(a, low, high, left, right)).step();
                temp.add(a[right]);
                right++;
            }
        }
        while (left <= mid) {
            temp.add(a[left]);
            left++;
        }
        while (right <= high) {
            temp.add(a[right]);
            right++;
        }
        for (int i = low; i <= high; i++) {
            a[i] = temp.get(i - low);
        }

        emit.at("writeback")
                .say("[%d,%d] merged in sorted order, contributing %d inversion%s from this merge.",
                        low, high, cnt, cnt == 1 ? "" : "s")
                .var("cnt", cnt)
                .arrayState(window(a, low, high, -1, -1)).step();
        return cnt;
    }
}
