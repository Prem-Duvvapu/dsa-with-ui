package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Merge Sort: recursively split the range in half until each half is a single element, then
 * merge each pair of sorted halves back together with two pointers. Unlike the three
 * comparison-based sorts in this batch, every call always recurses into BOTH halves before
 * doing any comparison work at all - the shape of the recursion never depends on the data,
 * only its length; only the merge step (and which side runs out first) depends on the values.
 */
@Component
public class MergeSortTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "merge-sort";
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
                        .help("Split in half recursively, then merge sorted halves back together.")
                        .length(2, 20).values(-999, 999)
                        .defaultValue(List.of(5, 2, 8, 1, 9))
                        .build());
    }

    /**
     * Reverse-sorted: at every single merge, the left pointer's value is always greater than
     * the right pointer's, so the merge always takes from the right and the "take left"
     * branch never fires at all - the exact opposite branch profile from the default, whose
     * merges take from both sides.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(9, 7, 5, 3, 1));
    }

    @Override
    public String annotatedCode() {
        return """
               public void mergeSort(int[] arr, int low, int high) {
                   // @a base
                   if (low >= high) return;
                   // @a split
                   int mid = (low + high) / 2;
                   mergeSort(arr, low, mid);
                   mergeSort(arr, mid + 1, high);
                   merge(arr, low, mid, high);
                   // @a done
               }

               private void merge(int[] arr, int low, int mid, int high) {
                   List<Integer> temp = new ArrayList<>();
                   int left = low, right = mid + 1;
                   while (left <= mid && right <= high) {
                       // @a compare
                       if (arr[left] <= arr[right]) {
                           // @a takeLeft
                           temp.add(arr[left++]);
                       } else {
                           // @a takeRight
                           temp.add(arr[right++]);
                       }
                   }
                   // @a drain
                   while (left <= mid) temp.add(arr[left++]);
                   while (right <= high) temp.add(arr[right++]);

                   for (int i = low; i <= high; i++) {
                       // @a writeback
                       arr[i] = temp.get(i - low);
                   }
               }""";
    }

    /** Outside [low,high] is "visited" (already settled by an ancestor merge); primary/secondary mark the active pointers. */
    private List<ArrayElement> window(int[] arr, int low, int high, int primary, int secondary) {
        List<ArrayElement> state = new ArrayList<>(arr.length);
        for (int i = 0; i < arr.length; i++) {
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
            state.add(new ArrayElement(i, arr[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("nums");
        mergeSort(arr, 0, arr.length - 1, emit);

        emit.at("done")
                .say("Recursion complete. Sorted array: %s.", java.util.Arrays.toString(arr))
                .var("result", java.util.Arrays.toString(arr))
                .arrayState(window(arr, 0, arr.length - 1, -1, -1)).step();
    }

    private void mergeSort(int[] arr, int low, int high, StepEmitter emit) {
        if (low >= high) {
            emit.at("base")
                    .say("Range [%d,%d] holds %s - nothing left to split.",
                            low, high, low == high ? "a single element" : "no elements")
                    .var("low", low).var("high", high)
                    .arrayState(window(arr, low, high, low, -1)).step();
            return;
        }

        int mid = (low + high) / 2;
        emit.push("mergeSort(" + low + ".." + high + ")");
        emit.at("split")
                .say("Split [%d,%d] at mid=%d into [%d,%d] and [%d,%d], and recurse on each half.",
                        low, high, mid, low, mid, mid + 1, high)
                .var("low", low).var("mid", mid).var("high", high)
                .arrayState(window(arr, low, high, mid, -1)).step();

        mergeSort(arr, low, mid, emit);
        mergeSort(arr, mid + 1, high, emit);
        merge(arr, low, mid, high, emit);
        emit.pop();
    }

    private void merge(int[] arr, int low, int mid, int high, StepEmitter emit) {
        List<Integer> temp = new ArrayList<>();
        int left = low;
        int right = mid + 1;

        while (left <= mid && right <= high) {
            emit.at("compare")
                    .say("Merging [%d,%d]: compare left pointer arr[%d]=%d against right pointer arr[%d]=%d.",
                            low, high, left, arr[left], right, arr[right])
                    .var("left", left).var("right", right)
                    .arrayState(window(arr, low, high, left, right)).step();

            if (arr[left] <= arr[right]) {
                emit.at("takeLeft")
                        .say("%d <= %d - take the left element arr[%d]=%d.",
                                arr[left], arr[right], left, arr[left])
                        .var("taken", arr[left])
                        .arrayState(window(arr, low, high, left, right)).step();
                temp.add(arr[left]);
                left++;
            } else {
                emit.at("takeRight")
                        .say("%d > %d - the left side is not ready yet, take the right element arr[%d]=%d.",
                                arr[left], arr[right], right, arr[right])
                        .var("taken", arr[right])
                        .arrayState(window(arr, low, high, left, right)).step();
                temp.add(arr[right]);
                right++;
            }
        }

        if (left <= mid || right <= high) {
            int drainedFrom = left <= mid ? left : right;
            emit.at("drain")
                    .say("One side is exhausted - copy whatever remains of the other side (starting at index %d) straight into place.",
                            drainedFrom)
                    .var("left", left).var("right", right)
                    .arrayState(window(arr, low, high, left <= mid ? left : -1, right <= high ? right : -1)).step();
        }
        while (left <= mid) {
            temp.add(arr[left]);
            left++;
        }
        while (right <= high) {
            temp.add(arr[right]);
            right++;
        }

        for (int i = low; i <= high; i++) {
            arr[i] = temp.get(i - low);
        }

        emit.at("writeback")
                .say("[%d,%d] merged in sorted order: %s.",
                        low, high, java.util.Arrays.toString(java.util.Arrays.copyOfRange(arr, low, high + 1)))
                .var("range", "[" + low + "," + high + "]")
                .arrayState(window(arr, low, high, -1, -1)).step();
    }
}
