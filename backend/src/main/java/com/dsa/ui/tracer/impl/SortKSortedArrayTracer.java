package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Sorts a k-sorted array with a min-heap containing at most k+1 candidates. */
@Component
public class SortKSortedArrayTracer implements AlgorithmTracer {
    @Override public String id() { return "sort-k-sorted-array"; }
    @Override public DsType dsType() { return DsType.HEAP; }

    @Override public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("values", FieldType.INT_ARRAY).label("K-sorted array")
                        .length(1, 50).values(-1000, 1000)
                        .defaultValue(List.of(6, 5, 3, 2, 8, 10, 9)).build(),
                InputField.of("k", FieldType.INT).label("Maximum displacement K")
                        .range(0, 49).defaultValue(3).build());
    }

    @Override public Map<String, Object> alternateInput() {
        return Map.of("values", List.of(3, 1, 2, 5, 4, 7, 6, 8), "k", 2);
    }

    @Override public String annotatedCode() {
        return """
               public int[] sortKSorted(int[] values, int k) {
                   ArrayHeap<Integer> heap = ArrayHeap.minHeap();
                   int write = 0;
                   for (int value : values) {
                       // @a offer
                       heap.offer(value);
                       if (heap.size() > k) {
                           // @a emit
                           values[write++] = heap.poll();
                       }
                   }
                   while (!heap.isEmpty()) {
                       // @a drain
                       values[write++] = heap.poll();
                   }
                   // @a done
                   return values;
               }""";
    }

    @Override public void run(Inputs in, StepEmitter emit) {
        int[] values = in.getIntArray("values");
        int k = in.getInt("k");
        if (k >= values.length) {
            throw new InputValidationException(Map.of("k", "Must be smaller than values.length (" + values.length + ")."));
        }
        ArrayHeap<Integer> heap = ArrayHeap.minHeap();
        int[] sorted = new int[values.length];
        int write = 0;
        for (int i = 0; i < values.length; i++) {
            heap.offer(values[i]);
            emit.at("offer").say("Add %d from index %d. The next answer lies among these at most %d candidates.",
                            values[i], i, k + 1)
                    .var("read", i).var("write", write).arrayState(renderHeap(heap)).step();
            if (heap.size() > k) {
                sorted[write] = heap.poll();
                emit.at("emit").say("Remove minimum %d and place it at sorted index %d.", sorted[write], write)
                        .var("write", write).arrayState(renderHeap(heap)).step();
                write++;
            }
        }
        while (!heap.isEmpty()) {
            sorted[write] = heap.poll();
            emit.at("drain").say("Input exhausted; drain %d into sorted index %d.", sorted[write], write)
                    .var("write", write).arrayState(renderHeap(heap)).step();
            write++;
        }
        emit.at("done").say("Every candidate emitted in order: %s.", Arrays.toString(sorted))
                .var("result", Arrays.toString(sorted)).array(sorted).step();
    }

    /** The heap's own array - see ArrayHeap for why this must not be sorted first. */
    private static List<ArrayElement> renderHeap(ArrayHeap<Integer> heap) {
        List<Integer> snapshot = heap.slots();
        List<ArrayElement> out = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) {
            out.add(new ArrayElement(i, snapshot.get(i), i == 0 ? "target" : "default",
                    i == 0 ? "next out" : "candidate"));
        }
        return out;
    }
}
