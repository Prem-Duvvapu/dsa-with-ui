package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Counts pairs (i, j) with i &lt; j and a[i] &gt; 2 * a[j], via merge sort. Unlike
 * {@link CountInversionsTracer}'s inversion count, this predicate cannot be tallied inside
 * the merge itself - a[i] &gt; 2 * a[j] is not monotone in the same way a[i] &gt; a[j] is once
 * the merge has started reordering elements - so counting runs as its own pass over the two
 * already-sorted halves, using a second pointer that only ever moves forward, before the
 * ordinary merge happens.
 */
@Component
public class ReversePairsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "reverse-pairs";
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
                        .help("Counts i < j with nums[i] > 2 * nums[j], not the plain inversions of count-inversions.")
                        .length(1, 24).values(-999, 999)
                        .defaultValue(List.of(1, 3, 2, 3, 1))
                        .build());
    }

    /** Strictly increasing: a[i] < a[j] for every i < j, so a[i] > 2*a[j] never holds. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 3, 4, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public int reversePairs(int[] a, int n) {
                   int total = mergeSort(a, 0, n - 1);
                   // @a done
                   return total;
               }

               private int mergeSort(int[] a, int low, int high) {
                   // @a base
                   if (low >= high) return 0;
                   // @a split
                   int mid = (low + high) / 2;
                   int cnt = mergeSort(a, low, mid);
                   cnt += mergeSort(a, mid + 1, high);
                   cnt += countPairs(a, low, mid, high);
                   merge(a, low, mid, high);
                   return cnt;
               }

               private int countPairs(int[] a, int low, int mid, int high) {
                   int cnt = 0, right = mid + 1;
                   for (int i = low; i <= mid; i++) {
                       // @a advanceRight
                       while (right <= high && a[i] > 2 * a[right]) right++;
                       // @a tally
                       cnt += (right - (mid + 1));
                   }
                   return cnt;
               }

               private void merge(int[] a, int low, int mid, int high) {
                   List<Integer> temp = new ArrayList<>();
                   int left = low, right = mid + 1;
                   while (left <= mid && right <= high) {
                       // @a mergeCompare
                       if (a[left] <= a[right]) {
                           // @a mergeTakeLeft
                           temp.add(a[left++]);
                       } else {
                           // @a mergeTakeRight
                           temp.add(a[right++]);
                       }
                   }
                   while (left <= mid) temp.add(a[left++]);
                   while (right <= high) temp.add(a[right++]);
                   for (int i = low; i <= high; i++) a[i] = temp.get(i - low);
                   // @a mergeWriteback
               }""";
    }

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
        int total = mergeSort(a, 0, n - 1, emit);

        emit.at("done")
                .say("Recursion complete. Total pairs with a[i] > 2*a[j]: %d.", total)
                .var("answer", total)
                .arrayState(window(a, 0, n - 1, -1, -1)).step();
    }

    private int mergeSort(int[] a, int low, int high, StepEmitter emit) {
        if (low >= high) {
            emit.at("base")
                    .say("Range [%d,%d] holds %s - nothing to pair.",
                            low, high, low == high ? "a single element" : "no elements")
                    .var("low", low).var("high", high)
                    .arrayState(window(a, low, high, low, -1)).step();
            return 0;
        }

        int mid = (low + high) / 2;
        emit.push("reversePairs(" + low + ".." + high + ")");
        emit.at("split")
                .say("Split [%d,%d] at mid=%d into two already-independent halves [%d,%d] and [%d,%d].",
                        low, high, mid, low, mid, mid + 1, high)
                .var("low", low).var("mid", mid).var("high", high)
                .arrayState(window(a, low, high, mid, -1)).step();

        int cnt = mergeSort(a, low, mid, emit);
        cnt += mergeSort(a, mid + 1, high, emit);
        cnt += countPairs(a, low, mid, high, emit);
        merge(a, low, mid, high, emit);
        emit.pop();
        return cnt;
    }

    private int countPairs(int[] a, int low, int mid, int high, StepEmitter emit) {
        int cnt = 0;
        int right = mid + 1;
        for (int i = low; i <= mid; i++) {
            int rightBefore = right;
            while (right <= high && a[i] > 2 * a[right]) {
                right++;
            }
            emit.at("advanceRight")
                    .say(right == rightBefore
                            ? "a[%d]=%d beats none of the right half yet at index %d (a[%d]=%d), so right stays put."
                                    .formatted(i, a[i], right, right, right <= high ? a[right] : 0)
                            : "a[%d]=%d beats indices %d..%d of the right half (each more than half of a[%d]=%d), so right advances to %d."
                                    .formatted(i, a[i], rightBefore, right - 1, i, a[i], right))
                    .var("i", i).var("right", right)
                    .arrayState(window(a, low, high, i, right <= high ? right : -1)).step();

            int contributed = right - (mid + 1);
            cnt += contributed;
            emit.at("tally")
                    .say("a[%d]=%d pairs with %d element%s on the right (indices %d..%d) - running total %d.",
                            i, a[i], contributed, contributed == 1 ? "" : "s", mid + 1, right - 1, cnt)
                    .var("contributed", contributed).var("runningCount", cnt)
                    .arrayState(window(a, low, high, i, -1)).step();
        }
        return cnt;
    }

    private void merge(int[] a, int low, int mid, int high, StepEmitter emit) {
        List<Integer> temp = new ArrayList<>();
        int left = low;
        int right = mid + 1;

        while (left <= mid && right <= high) {
            emit.at("mergeCompare")
                    .say("Merging [%d,%d]: compare a[%d]=%d against a[%d]=%d.",
                            low, high, left, a[left], right, a[right])
                    .var("left", left).var("right", right)
                    .arrayState(window(a, low, high, left, right)).step();

            if (a[left] <= a[right]) {
                emit.at("mergeTakeLeft")
                        .say("%d <= %d, take a[%d]=%d from the left.", a[left], a[right], left, a[left])
                        .var("taken", a[left])
                        .arrayState(window(a, low, high, left, right)).step();
                temp.add(a[left]);
                left++;
            } else {
                emit.at("mergeTakeRight")
                        .say("%d > %d, take a[%d]=%d from the right (already counted above).",
                                a[left], a[right], right, a[right])
                        .var("taken", a[right])
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

        emit.at("mergeWriteback")
                .say("[%d,%d] merged in sorted order.", low, high)
                .arrayState(window(a, low, high, -1, -1)).step();
    }
}
