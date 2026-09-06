package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Selection Sort: for each position i, scan the remaining unsorted suffix for its minimum,
 * then swap it into place. Exactly one swap per pass (or none, when arr[i] is already the
 * minimum) - the fewest writes of any O(N^2) sort, at the cost of always scanning to the end
 * regardless of how sorted the input already is.
 */
@Component
public class SelectionSortTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "selection-sort";
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
                        .help("Selection Sort always scans the whole unsorted suffix, no matter how ordered it already is.")
                        .length(2, 20).values(-999, 999)
                        .defaultValue(List.of(5, 2, 8, 1, 9))
                        .build());
    }

    /**
     * Already sorted: mini never moves away from i, so every pass takes the "no swap" branch
     * instead of the default's two real swaps - a different branch profile, not a permutation.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 3, 4, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public void selectionSort(int[] arr, int n) {
                   for (int i = 0; i < n - 1; i++) {
                       // @a passStart
                       int mini = i;
                       for (int j = i + 1; j < n; j++) {
                           // @a compare
                           if (arr[j] < arr[mini]) {
                               // @a newMin
                               mini = j;
                           }
                       }
                       if (mini != i) {
                           // @a swap
                           int temp = arr[i];
                           arr[i] = arr[mini];
                           arr[mini] = temp;
                       } else {
                           // @a noSwap
                       }
                   }
                   // @a done
               }""";
    }

    /** Sorted prefix [0,sortedUpTo) is "sorted"; i and the running mini are "pivot"; j is "comparing". */
    private List<ArrayElement> window(int[] arr, int sortedUpTo, int i, int mini, int j) {
        List<ArrayElement> state = new ArrayList<>(arr.length);
        for (int idx = 0; idx < arr.length; idx++) {
            String s;
            if (idx < sortedUpTo) {
                s = "sorted";
            } else if (idx == mini || idx == i) {
                s = "pivot";
            } else if (idx == j) {
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

        for (int i = 0; i < n - 1; i++) {
            int mini = i;
            emit.at("passStart")
                    .say("Pass %d: scan indices [%d..%d] for the minimum, starting from arr[%d]=%d.",
                            i + 1, i, n - 1, i, arr[i])
                    .var("i", i).var("mini", mini)
                    .arrayState(window(arr, i, i, mini, -1)).step();

            for (int j = i + 1; j < n; j++) {
                emit.at("compare")
                        .say("Compare arr[j=%d]=%d against current minimum arr[mini=%d]=%d.",
                                j, arr[j], mini, arr[mini])
                        .var("j", j).var("mini", mini)
                        .arrayState(window(arr, i, i, mini, j)).step();

                if (arr[j] < arr[mini]) {
                    int prevMiniVal = arr[mini];
                    mini = j;
                    emit.at("newMin")
                            .say("%d < %d - mini becomes %d.", arr[j], prevMiniVal, mini)
                            .var("j", j).var("mini", mini)
                            .arrayState(window(arr, i, i, mini, j)).step();
                }
            }

            if (mini != i) {
                int temp = arr[i];
                arr[i] = arr[mini];
                arr[mini] = temp;
                emit.at("swap")
                        .say("Minimum found at index %d - swap arr[%d] and arr[%d]. Index %d is now sorted.",
                                mini, i, mini, i)
                        .var("i", i).var("mini", mini)
                        .arrayState(window(arr, i + 1, -1, -1, -1)).step();
            } else {
                emit.at("noSwap")
                        .say("arr[%d]=%d was already the minimum of its suffix - no swap needed. Index %d is now sorted.",
                                i, arr[i], i)
                        .var("i", i)
                        .arrayState(window(arr, i + 1, -1, -1, -1)).step();
            }
        }

        emit.at("done")
                .say("All passes complete. Sorted array: %s.", java.util.Arrays.toString(arr))
                .var("result", java.util.Arrays.toString(arr))
                .arrayState(window(arr, n, -1, -1, -1)).step();
    }
}
