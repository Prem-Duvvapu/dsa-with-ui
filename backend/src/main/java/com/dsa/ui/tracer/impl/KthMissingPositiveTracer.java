package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * At index i, {@code arr[i] - (i + 1)} counts exactly how many positive integers are
 * missing up to that point — every gap before arr[i] contributes one. That count only ever
 * grows as i increases, so the first index whose missing-count reaches k is found with an
 * ordinary lower-bound search, and the answer falls out of that index directly.
 */
@Component
public class KthMissingPositiveTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "kth-missing-positive";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("arr", FieldType.INT_ARRAY)
                        .label("Sorted array (strictly increasing, positive)")
                        .length(1, 30).values(1, 2000).sorted().distinct()
                        .defaultValue(List.of(2, 3, 4, 7, 11))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("K")
                        .range(1, 2000)
                        .defaultValue(5)
                        .build());
    }

    /** A consecutive run 1..4 has zero missing numbers inside it, so low always advances past the whole array. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("arr", List.of(1, 2, 3, 4), "k", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               public int findKthPositive(int[] arr, int k) {
                   // @a init
                   int low = 0, high = arr.length - 1;
                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       int missing = arr[mid] - (mid + 1);
                       if (missing < k) {
                           // @a tooFew
                           low = mid + 1;
                       } else {
                           // @a tooMany
                           high = mid - 1;
                       }
                   }
                   // @a done
                   return low + k;
               }""";
    }

    private List<ArrayElement> window(int[] arr, int low, int high, int mid) {
        List<ArrayElement> state = new ArrayList<>(arr.length);
        for (int i = 0; i < arr.length; i++) {
            String s = i == mid ? "current" : (i < low || i > high) ? "visited" : "target";
            state.add(new ArrayElement(i, arr[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("arr");
        int k = in.getInt("k");
        int low = 0, high = arr.length - 1;

        emit.at("init")
                .say("Find the %d-th missing positive integer. arr[i] - (i+1) counts how many are missing up to index i.", k)
                .var("k", k).var("low", low).var("high", high)
                .arrayState(window(arr, low, high, -1)).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            int missing = arr[mid] - (mid + 1);
            emit.at("mid")
                    .say("Probe [%d..%d]: arr[%d]=%d, so %d positives are missing up to here.",
                            low, high, mid, arr[mid], missing)
                    .var("low", low).var("high", high).var("mid", mid).var("missing", missing)
                    .arrayState(window(arr, low, high, mid)).step();

            if (missing < k) {
                emit.at("tooFew")
                        .say("%d < %d — not enough missing yet, look right.", missing, k)
                        .var("low", mid + 1)
                        .arrayState(window(arr, mid + 1, high, -1)).step();
                low = mid + 1;
            } else {
                emit.at("tooMany")
                        .say("%d >= %d — already enough missing, look left for an earlier crossing.", missing, k)
                        .var("high", mid - 1)
                        .arrayState(window(arr, low, mid - 1, -1)).step();
                high = mid - 1;
            }
        }

        int answer = low + k;
        emit.at("done")
                .say("low passed high at %d. The %d-th missing positive integer is %d.", low, k, answer)
                .var("answer", answer)
                .arrayState(window(arr, 0, -1, -1)).step();
    }
}
