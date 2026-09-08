package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.TreeNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Executes real min-heap insert and extract-min operations on caller-provided values. */
@Component
public class ImplementMinHeapTracer implements AlgorithmTracer {
    @Override public String id() { return "implement-min-heap"; }
    @Override public DsType dsType() { return DsType.TREE; }

    @Override public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("values", FieldType.INT_ARRAY).label("Values to insert")
                        .length(1, 40).values(-1000, 1000)
                        .defaultValue(List.of(5, 3, 8, 1, 2)).build(),
                InputField.of("extractCount", FieldType.INT).label("Extract minimum count")
                        .help("Must not exceed the number of inserted values.")
                        .range(0, 20).defaultValue(2).build());
    }

    @Override public Map<String, Object> alternateInput() {
        return Map.of("values", List.of(9, 4, 7, 1, 0, 3), "extractCount", 3);
    }

    @Override public String annotatedCode() {
        return """
               public List<Integer> runMinHeap(int[] values, int extractCount) {
                   // @a init
                   List<Integer> heap = new ArrayList<>();
                   for (int value : values) {
                       // @a insert
                       heap.add(value);
                       int index = heap.size() - 1;
                       while (index > 0 && heap.get(index) < heap.get((index - 1) / 2)) {
                           // @a siftUp
                           swap(heap, index, (index - 1) / 2);
                           index = (index - 1) / 2;
                       }
                   }
                   for (int operation = 0; operation < extractCount; operation++) {
                       // @a extract
                       int minimum = heap.get(0);
                       int last = heap.remove(heap.size() - 1);
                       if (!heap.isEmpty()) {
                           // @a replace
                           heap.set(0, last);
                           int index = 0;
                           while (hasSmallerChild(heap, index)) {
                               // @a siftDown
                               int child = smallerChild(heap, index);
                               swap(heap, index, child);
                               index = child;
                           }
                       }
                   }
                   // @a done
                   return heap;
               }""";
    }

    @Override public void run(Inputs in, StepEmitter emit) {
        int[] values = in.getIntArray("values");
        int extractCount = in.getInt("extractCount");
        if (extractCount > values.length) {
            throw new InputValidationException(Map.of("extractCount",
                    "Must not exceed values.length (" + values.length + ")."));
        }

        List<Integer> heap = new ArrayList<>();
        List<Integer> extracted = new ArrayList<>();
        emit.at("init").say("Start with an empty min-heap.")
                .var("size", 0).tree(List.of()).step();

        for (int value : values) {
            heap.add(value);
            int child = heap.size() - 1;
            emit.at("insert").say("Append %d at index %d to preserve the complete-tree shape.", value, child)
                    .var("inserted", value).var("size", heap.size())
                    .tree(render(heap, child, -1)).step();
            while (child > 0) {
                int parent = (child - 1) / 2;
                if (heap.get(parent) <= heap.get(child)) break;
                Collections.swap(heap, parent, child);
                emit.at("siftUp").say("Swap with the larger parent at index %d; the minimum rises toward the root.", parent)
                        .var("child", child).var("parent", parent)
                        .tree(render(heap, parent, child)).step();
                child = parent;
            }
        }

        for (int operation = 0; operation < extractCount; operation++) {
            int minimum = heap.get(0);
            extracted.add(minimum);
            emit.at("extract").say("Extract root %d, the minimum of every value in the heap.", minimum)
                    .var("minimum", minimum).var("operation", operation + 1)
                    .tree(render(heap, 0, -1)).step();

            int last = heap.remove(heap.size() - 1);
            if (heap.isEmpty()) continue;
            heap.set(0, last);
            emit.at("replace").say("Move last value %d to the root so the tree remains complete.", last)
                    .var("replacement", last).tree(render(heap, 0, -1)).step();

            int parent = 0;
            while (true) {
                int left = 2 * parent + 1, right = left + 1, smallest = parent;
                if (left < heap.size() && heap.get(left) < heap.get(smallest)) smallest = left;
                if (right < heap.size() && heap.get(right) < heap.get(smallest)) smallest = right;
                if (smallest == parent) break;
                Collections.swap(heap, parent, smallest);
                emit.at("siftDown").say("Swap the replacement with smaller child %d to restore heap order.", heap.get(parent))
                        .var("parent", parent).var("child", smallest)
                        .tree(render(heap, smallest, parent)).step();
                parent = smallest;
            }
        }

        emit.at("done").say("Operations complete. Extracted %s; remaining heap %s.", extracted, heap)
                .var("extracted", extracted).var("remaining", heap.size())
                .tree(heap.isEmpty() ? List.of() : render(heap, 0, -1)).step();
    }

    private static List<TreeNode> render(List<Integer> heap, int current, int target) {
        Map<Integer, String> states = new LinkedHashMap<>();
        if (target >= 0) states.put(target, "target");
        if (current >= 0) states.put(current, "current");
        return new BinaryTreeLayout(heap.toArray(Integer[]::new)).render(states);
    }
}
