package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Insertion Sort: build a sorted prefix one element at a time by holding the next element
 * as {@code key} and shifting every larger element in the sorted prefix one slot to the
 * right, until {@code key} lands in its correct spot. Unlike Selection or Bubble Sort, the
 * inner loop here runs BACKWARD through an already-sorted region, and how far it goes
 * (zero shifts to the whole prefix) depends entirely on how out of place {@code key} is.
 */
@Component
public class InsertionSortTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "insertion-sort";
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
                        .help("Each key shifts backward through the sorted prefix until it finds its spot.")
                        .length(2, 20).values(-999, 999)
                        .defaultValue(List.of(5, 2, 8, 1, 9))
                        .build());
    }

    /**
     * All duplicates: arr[j] > key is never true (equal values never shift past each other),
     * so every key is inserted with zero shifts - a materially different branch profile from
     * the default, whose key=1 pass shifts three elements in one go.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(4, 4, 4, 4, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               public void insertionSort(int[] arr, int n) {
                   for (int i = 1; i < n; i++) {
                       // @a pickKey
                       int key = arr[i];
                       int j = i - 1;
                       while (j >= 0 && arr[j] > key) {
                           // @a shift
                           arr[j + 1] = arr[j];
                           j--;
                       }
                       // @a place
                       arr[j + 1] = key;
                   }
                   // @a done
               }""";
    }

    /** [0,i) is the settled sorted prefix; j is the shifting cursor, hole is key's empty slot. */
    private List<ArrayElement> window(int[] arr, int sortedPrefix, int j, int hole) {
        List<ArrayElement> state = new ArrayList<>(arr.length);
        for (int idx = 0; idx < arr.length; idx++) {
            String s;
            if (idx == hole) {
                s = "pivot";
            } else if (idx == j) {
                s = "comparing";
            } else if (idx < sortedPrefix) {
                s = "sorted";
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

        for (int i = 1; i < n; i++) {
            int key = arr[i];
            int j = i - 1;
            emit.at("pickKey")
                    .say("Pass %d: hold key = arr[%d] = %d, and walk backward through the sorted prefix [0..%d].",
                            i, i, key, i - 1)
                    .var("i", i).var("key", key).var("j", j)
                    .arrayState(window(arr, i, -1, i)).step();

            while (j >= 0 && arr[j] > key) {
                emit.at("shift")
                        .say("arr[%d]=%d > key(%d) - shift it right into arr[%d].",
                                j, arr[j], key, j + 1)
                        .var("j", j).var("key", key)
                        .arrayState(window(arr, i, j, j + 1)).step();
                arr[j + 1] = arr[j];
                j--;
            }

            arr[j + 1] = key;
            emit.at("place")
                    .say("Place key(%d) into arr[%d]. Sorted prefix now covers [0..%d].",
                            key, j + 1, i)
                    .var("key", key).var("insertedAt", j + 1)
                    .arrayState(window(arr, i + 1, -1, j + 1)).step();
        }

        emit.at("done")
                .say("Sorted array: %s.", java.util.Arrays.toString(arr))
                .var("result", java.util.Arrays.toString(arr))
                .arrayState(window(arr, n, -1, -1)).step();
    }
}
