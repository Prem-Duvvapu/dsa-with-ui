package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Maintains the kth largest stream value with a size-k min-heap. */
@Component
public class KthLargestStreamTracer implements AlgorithmTracer {
    @Override public String id() { return "kth-largest-stream"; }
    @Override public DsType dsType() { return DsType.HEAP; }

    @Override public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("initial", FieldType.INT_ARRAY).label("Initial stream values")
                        .length(1, 30).values(-1000, 1000).defaultValue(List.of(4, 5, 8, 2)).build(),
                InputField.of("additions", FieldType.INT_ARRAY).label("Values to add")
                        .length(1, 30).values(-1000, 1000).defaultValue(List.of(3, 5, 10, 9, 4)).build(),
                InputField.of("k", FieldType.INT).label("K").range(1, 30).defaultValue(3).build());
    }

    @Override public Map<String, Object> alternateInput() {
        return Map.of("initial", List.of(7, 1, 6, 4), "additions", List.of(8, 2, 10, 5, 11, 3), "k", 2);
    }

    @Override public String annotatedCode() {
        return """
               public int add(int value) {
                   // @a push
                   heap.offer(value);
                   if (heap.size() > k) {
                       // @a evict
                       heap.poll();
                   }
                   // @a report
                   return heap.peek();
               }

               // @a done
               // heap contains the k largest values seen
               """;
    }

    @Override public void run(Inputs in, StepEmitter emit) {
        int[] initial = in.getIntArray("initial");
        int[] additions = in.getIntArray("additions");
        int k = in.getInt("k");
        if (k > initial.length) {
            throw new InputValidationException(Map.of("k", "Must not exceed initial.length (" + initial.length + ")."));
        }

        PriorityQueue<Integer> heap = new PriorityQueue<>();
        for (int value : initial) add(value, k, heap, emit, false);
        List<Integer> answers = new ArrayList<>();
        for (int value : additions) {
            add(value, k, heap, emit, true);
            answers.add(heap.peek());
            emit.at("report").say("After adding %d, the %d-th largest stream value is %d.", value, k, heap.peek())
                    .var("added", value).var("answer", heap.peek()).arrayState(render(heap)).step();
        }
        emit.at("done").say("Kth-largest answers for all additions: %s.", answers)
                .var("answers", answers).arrayState(render(heap)).step();
    }

    private static void add(int value, int k, PriorityQueue<Integer> heap, StepEmitter emit, boolean live) {
        heap.offer(value);
        emit.at("push").say("%s %d; the min-heap temporarily contains %d value(s).",
                        live ? "Add" : "Seed", value, heap.size())
                .var("value", value).var("size", heap.size()).arrayState(render(heap)).step();
        if (heap.size() > k) {
            int removed = heap.poll();
            emit.at("evict").say("Remove smallest %d so only the %d largest values seen remain.", removed, k)
                    .var("removed", removed).var("size", heap.size()).arrayState(render(heap)).step();
        }
    }

    private static List<ArrayElement> render(PriorityQueue<Integer> heap) {
        List<Integer> values = new ArrayList<>(heap);
        Collections.sort(values);
        List<ArrayElement> out = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            out.add(new ArrayElement(i, values.get(i), i == 0 ? "target" : "default", i == 0 ? "kth largest" : null));
        }
        return out;
    }
}
