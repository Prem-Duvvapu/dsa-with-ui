package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.TreeNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Builds a binary min-heap while explaining its complete-tree array relationships. */
@Component
public class HeapsTheoryTracer implements AlgorithmTracer {
    @Override public String id() { return "heaps-theory"; }
    @Override public DsType dsType() { return DsType.TREE; }

    @Override public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("values", FieldType.INT_ARRAY).label("Values to insert")
                .help("Values are inserted left to right into a complete binary min-heap.")
                .length(1, 40).values(-1000, 1000)
                .defaultValue(List.of(7, 2, 9, 1, 5, 3, 8)).build());
    }

    @Override public Map<String, Object> alternateInput() {
        return Map.of("values", List.of(4, 6, 2, 9, 1, 7, 3, 0));
    }

    @Override public String annotatedCode() {
        return """
               public List<Integer> buildMinHeap(int[] values) {
                   List<Integer> heap = new ArrayList<>();
                   for (int value : values) {
                       // @a insert
                       heap.add(value);
                       int child = heap.size() - 1;
                       while (child > 0) {
                           int parent = (child - 1) / 2;
                           if (heap.get(parent) <= heap.get(child)) break;
                           // @a sift
                           Collections.swap(heap, parent, child);
                           child = parent;
                       }
                   }
                   // @a done
                   return heap;
               }""";
    }

    @Override public void run(Inputs in, StepEmitter emit) {
        int[] values = in.getIntArray("values");
        List<Integer> heap = new ArrayList<>();
        for (int value : values) {
            heap.add(value);
            int child = heap.size() - 1;
            int parent = child == 0 ? -1 : (child - 1) / 2;
            emit.at("insert")
                    .say(child == 0
                            ? "Insert %d at root index 0. A heap is filled level by level."
                            : "Insert %d at index %d. Its parent is floor((%d - 1) / 2) = %d.",
                            child == 0 ? new Object[]{value} : new Object[]{value, child, child, parent})
                    .var("inserted", value).var("index", child)
                    .tree(render(heap, child, parent)).step();

            while (child > 0) {
                parent = (child - 1) / 2;
                if (heap.get(parent) <= heap.get(child)) break;
                int parentValue = heap.get(parent);
                Collections.swap(heap, parent, child);
                emit.at("sift")
                        .say("%d is smaller than parent %d, so swap indices %d and %d.",
                                value, parentValue, child, parent)
                        .var("child", child).var("parent", parent)
                        .tree(render(heap, parent, child)).step();
                child = parent;
            }
        }
        emit.at("done")
                .say("Complete tree built. Every parent is no greater than either child: %s.", heap)
                .var("size", heap.size()).var("root", heap.get(0))
                .tree(render(heap, 0, -1)).step();
    }

    private static List<TreeNode> render(List<Integer> heap, int current, int target) {
        Map<Integer, String> states = new LinkedHashMap<>();
        if (target >= 0) states.put(target, "target");
        if (current >= 0) states.put(current, "current");
        return new BinaryTreeLayout(heap.toArray(Integer[]::new)).render(states);
    }
}
