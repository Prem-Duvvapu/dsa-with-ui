package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bubble Sort: repeatedly walk the unsorted prefix comparing adjacent elements, swapping
 * whenever they are out of order so the largest remaining element "bubbles" to the end of
 * the range each pass. The optimization flag ({@code didSwap}) breaks out the moment a whole
 * pass makes no swap - the array is already sorted, so later passes would be wasted work.
 */
@Component
public class BubbleSortTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bubble-sort";
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
                        .help("Adjacent elements swap whenever they are out of order.")
                        .length(2, 20).values(-999, 999)
                        .defaultValue(List.of(5, 2, 8, 1, 9))
                        .build());
    }

    /**
     * Reverse-sorted: every adjacent pair is out of order every single pass, so the
     * "didSwap" optimization never fires early - the full O(N^2) worst case, unlike the
     * default's early break after its third pass.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(9, 7, 5, 3, 1));
    }

    @Override
    public String annotatedCode() {
        return """
               public void bubbleSort(int[] arr, int n) {
                   for (int i = n - 1; i >= 0; i--) {
                       // @a passStart
                       boolean didSwap = false;
                       for (int j = 0; j <= i - 1; j++) {
                           // @a compare
                           if (arr[j] > arr[j + 1]) {
                               // @a swap
                               int temp = arr[j];
                               arr[j] = arr[j + 1];
                               arr[j + 1] = temp;
                               didSwap = true;
                           }
                       }
                       if (!didSwap) {
                           // @a earlyExit
                           break;
                       }
                   }
                   // @a done
               }""";
    }

    /** [sortedFrom, n) is the settled suffix; j and j+1 are the pair under comparison. */
    private List<ArrayElement> window(int[] arr, int sortedFrom, int j, int jPlus1) {
        List<ArrayElement> state = new ArrayList<>(arr.length);
        for (int idx = 0; idx < arr.length; idx++) {
            String s;
            if (idx >= sortedFrom) {
                s = "sorted";
            } else if (idx == j || idx == jPlus1) {
                s = "comparing";
            } else {
                s = "default";
            }
            state.add(new ArrayElement(idx, arr[idx], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("nums");
        int n = arr.length;

        for (int i = n - 1; i >= 0; i--) {
            boolean didSwap = false;
            emit.at("passStart")
                    .say("Pass %d: bubble the largest of indices [0..%d] up to index %d.",
                            n - i, i, i)
                    .var("i", i)
                    .arrayState(window(arr, i + 1, -1, -1)).step();

            for (int j = 0; j <= i - 1; j++) {
                emit.at("compare")
                        .say("Compare adjacent arr[%d]=%d and arr[%d]=%d.",
                                j, arr[j], j + 1, arr[j + 1])
                        .var("j", j)
                        .arrayState(window(arr, i + 1, j, j + 1)).step();

                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    didSwap = true;
                    emit.at("swap")
                            .say("%d > %d - out of order, so swap them.", arr[j + 1], arr[j])
                            .var("j", j).var("didSwap", true)
                            .arrayState(window(arr, i + 1, j, j + 1)).step();
                }
            }

            if (!didSwap) {
                emit.at("earlyExit")
                        .say("Pass %d made no swaps - the array is already sorted. Breaking out early.",
                                n - i)
                        .var("didSwap", false)
                        .arrayState(window(arr, 0, -1, -1)).step();
                break;
            }
        }

        emit.at("done")
                .say("Sorted array: %s.", java.util.Arrays.toString(arr))
                .var("result", java.util.Arrays.toString(arr))
                .arrayState(window(arr, 0, -1, -1)).step();
    }
}
