package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * A min-heap of stick lengths: always connect the two currently-shortest sticks, since
 * joining any longer stick first would make it (and every stick it later joins) pay that
 * length's cost again on every subsequent connection. The heap is shown as the plain array
 * it really is - only its two smallest entries change identity each step, not its shape.
 */
@Component
public class MinCostConnectSticksTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "min-cost-connect-sticks";
    }

    @Override
    public DsType dsType() {
        return DsType.HEAP;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("sticks", FieldType.INT_ARRAY)
                        .label("Stick lengths")
                        .help("At least two sticks to connect.")
                        .length(2, 30).values(1, 1000)
                        .defaultValue(List.of(2, 4, 3))
                        .build());
    }

    /** A longer, differently-shaped set of sticks - a different total cost, not a permutation. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("sticks", List.of(1, 8, 3, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public int connectSticks(int[] sticks) {
                   PriorityQueue<Integer> heap = new PriorityQueue<>();
                   for (int s : sticks) heap.add(s);

                   int totalCost = 0;
                   while (heap.size() > 1) {
                       // @a popTwo
                       int a = heap.poll();
                       int b = heap.poll();
                       // @a connect
                       int cost = a + b;
                       totalCost += cost;
                       heap.add(cost);
                   }
                   // @a done
                   return totalCost;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] sticks = in.getIntArray("sticks");

        // A real binary heap, not a sorted list. This used to keep the sticks fully sorted
        // and call remove(0) / insert-in-place, which gives the same answer and draws a
        // perfectly ordered tree at every step - while the code panel beside it says
        // PriorityQueue.poll(). See ArrayHeap.
        ArrayHeap<Integer> heap = ArrayHeap.minHeap();
        for (int s : sticks) heap.offer(s);

        emit.at("popTwo")
                .say("Start with sticks %s.", heap.slots())
                .var("heapSize", heap.size())
                .array(toArray(heap)).step();

        int totalCost = 0;
        while (heap.size() > 1) {
            int a = heap.poll();
            int b = heap.poll();
            emit.at("popTwo")
                    .say("Two shortest sticks are %d and %d.", a, b)
                    .var("a", a).var("b", b)
                    .array(toArray(heap)).step();

            int cost = a + b;
            totalCost += cost;
            int insertAt = heap.offer(cost);
            emit.at("connect")
                    .say("Connect %d + %d for cost %d (running total %d) - the joined stick, "
                                    + "length %d, rejoins the heap.",
                            a, b, cost, totalCost, cost)
                    .var("cost", cost).var("totalCost", totalCost)
                    .array(toArray(heap), insertAt).step();
        }

        emit.at("done")
                .say("One stick remains, length %d. Total cost to connect them all: %d.",
                        heap.peek(), totalCost)
                .var("answer", totalCost)
                .array(toArray(heap), 0).step();
    }

    private static int[] toArray(ArrayHeap<Integer> heap) {
        List<Integer> slots = heap.slots();
        int[] out = new int[slots.size()];
        for (int i = 0; i < out.length; i++) out[i] = slots.get(i);
        return out;
    }
}
