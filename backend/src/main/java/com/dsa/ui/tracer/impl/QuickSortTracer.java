package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Quick Sort with Lomuto partitioning (pivot = arr[low]): partition the range so everything
 * less than the pivot ends up before it, then recurse on the two sides independently. Unlike
 * Merge Sort, the recursion here is NOT symmetric: how the range splits - and whether one
 * side is empty - depends entirely on where the pivot lands, so a well-chosen pivot on an
 * unsorted array balances the recursion while an already-sorted array (this pivot rule's
 * worst case) skews it all the way to one side, with every "less than pivot" swap skipped.
 */
@Component
public class QuickSortTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "quick-sort";
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
                        .help("Pivot = the range's first element. Partition around it, then recurse both sides.")
                        .length(2, 20).values(-999, 999)
                        .defaultValue(List.of(5, 2, 8, 1, 9))
                        .build());
    }

    /**
     * Already sorted: the pivot (always the range's leftmost, smallest-remaining element) is
     * never beaten by anything to its right, so "swapLess" never fires at all and every
     * partition puts the pivot at its own index unmoved - the classic Lomuto worst case,
     * where recursion skews to one side instead of the default's roughly balanced split.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 3, 4, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public void quickSort(int[] arr, int low, int high) {
                   if (low < high) {
                       // @a partition
                       int pIndex = partition(arr, low, high);
                       quickSort(arr, low, pIndex - 1);
                       quickSort(arr, pIndex + 1, high);
                   } else {
                       // @a base
                   }
                   // @a done
               }

               private int partition(int[] arr, int low, int high) {
                   // @a pivotChoice
                   int pivot = arr[low];
                   int i = low;
                   for (int j = low + 1; j <= high; j++) {
                       // @a compare
                       if (arr[j] < pivot) {
                           i++;
                           // @a swapLess
                           swap(arr, i, j);
                       }
                   }
                   // @a placePivot
                   swap(arr, low, i);
                   return i;
               }""";
    }

    /** Outside [low,high] is "visited" (already settled by an ancestor call); primary/secondary mark the active indices. */
    private List<ArrayElement> window(int[] arr, int low, int high, int primary, int secondary) {
        List<ArrayElement> state = new ArrayList<>(arr.length);
        for (int idx = 0; idx < arr.length; idx++) {
            String s;
            if (idx == primary) {
                s = "current";
            } else if (idx == secondary) {
                s = "swapping";
            } else if (idx < low || idx > high) {
                s = "visited";
            } else {
                s = "target";
            }
            state.add(new ArrayElement(idx, arr[idx], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("nums");
        quickSort(arr, 0, arr.length - 1, emit);

        emit.at("done")
                .say("Recursion complete. Sorted array: %s.", java.util.Arrays.toString(arr))
                .var("result", java.util.Arrays.toString(arr))
                .arrayState(window(arr, 0, arr.length - 1, -1, -1)).step();
    }

    private void quickSort(int[] arr, int low, int high, StepEmitter emit) {
        if (low < high) {
            emit.push("quickSort(" + low + ".." + high + ")");
            emit.at("partition")
                    .say("Range [%d,%d] has more than one element - partition it around a pivot, then recurse on both sides.",
                            low, high)
                    .var("low", low).var("high", high)
                    .arrayState(window(arr, low, high, -1, -1)).step();
            int pIndex = partition(arr, low, high, emit);
            quickSort(arr, low, pIndex - 1, emit);
            quickSort(arr, pIndex + 1, high, emit);
            emit.pop();
        } else {
            emit.at("base")
                    .say("Range [%d,%d] holds %s - nothing to partition.",
                            low, high, low == high ? "a single element" : "no elements")
                    .var("low", low).var("high", high)
                    .arrayState(window(arr, Math.min(low, high), Math.max(low, high), -1, -1)).step();
        }
    }

    private int partition(int[] arr, int low, int high, StepEmitter emit) {
        int pivot = arr[low];
        int i = low;
        emit.at("pivotChoice")
                .say("Partitioning [%d,%d]: pivot = arr[%d] = %d. i starts at %d.",
                        low, high, low, pivot, i)
                .var("low", low).var("high", high).var("pivot", pivot).var("i", i)
                .arrayState(window(arr, low, high, low, -1)).step();

        for (int j = low + 1; j <= high; j++) {
            emit.at("compare")
                    .say("Compare arr[j=%d]=%d against pivot(%d).", j, arr[j], pivot)
                    .var("j", j).var("i", i).var("pivot", pivot)
                    .arrayState(window(arr, low, high, i, j)).step();

            if (arr[j] < pivot) {
                i++;
                emit.at("swapLess")
                        .say("%d < pivot(%d) - advance i to %d and swap arr[%d] with arr[%d].",
                                arr[j], pivot, i, i, j)
                        .var("i", i).var("j", j)
                        .arrayState(window(arr, low, high, i, j)).step();
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }

        emit.at("placePivot")
                .say("Scan done - place the pivot at its final index %d by swapping arr[%d] and arr[%d].",
                        i, low, i)
                .var("low", low).var("i", i)
                .arrayState(window(arr, low, high, low, i)).step();
        int temp = arr[low];
        arr[low] = arr[i];
        arr[i] = temp;

        return i;
    }
}
