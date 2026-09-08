package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.TreeNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Converts the caller's heap array to a max-heap using bottom-up sift-down. */
@Component
public class MinToMaxHeapTracer implements AlgorithmTracer {
    @Override public String id() { return "min-to-max-heap"; }
    @Override public DsType dsType() { return DsType.TREE; }

    @Override public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("values", FieldType.INT_ARRAY).label("Min-heap array")
                .help("Bottom-up max-heapify is valid for any array; a min-heap makes the transformation easiest to see.")
                .length(1, 63).values(-1000, 1000)
                .defaultValue(List.of(1, 3, 5, 7, 9, 8, 10)).build());
    }

    @Override public Map<String, Object> alternateInput() {
        return Map.of("values", List.of(0, 2, 1, 6, 4, 3, 5, 9, 8));
    }

    @Override public String annotatedCode() {
        return """
               public void convertToMaxHeap(int[] values) {
                   for (int parent = values.length / 2 - 1; parent >= 0; parent--) {
                       // @a parent
                       int root = parent;
                       while (true) {
                           // @a choose
                           int largest = largestOfRootAndChildren(values, root);
                           if (largest == root) {
                               // @a settled
                               break;
                           }
                           // @a swap
                           swap(values, root, largest);
                           root = largest;
                       }
                   }
                   // @a done
               }""";
    }

    @Override public void run(Inputs in, StepEmitter emit) {
        int[] values = in.getIntArray("values");
        for (int parent = values.length / 2 - 1; parent >= 0; parent--) {
            emit.at("parent").say("Heapify subtree rooted at index %d (value %d).", parent, values[parent])
                    .var("parent", parent).tree(render(values, parent, -1)).step();
            int root = parent;
            while (true) {
                int left = 2 * root + 1, right = left + 1, largest = root;
                if (left < values.length && values[left] > values[largest]) largest = left;
                if (right < values.length && values[right] > values[largest]) largest = right;
                emit.at("choose").say(largest == root
                                ? "Neither child exceeds %d; this root is already settled."
                                : "Largest among the root and its children is %d at index %d.",
                                largest == root ? new Object[]{values[root]} : new Object[]{values[largest], largest})
                        .var("root", root).var("largest", largest)
                        .tree(render(values, root, largest == root ? -1 : largest)).step();
                if (largest == root) {
                    emit.at("settled").say("Subtree at index %d now satisfies max-heap order.", root)
                            .var("root", root).tree(render(values, root, -1)).step();
                    break;
                }
                int rootValue = values[root], childValue = values[largest];
                int tmp = values[root]; values[root] = values[largest]; values[largest] = tmp;
                emit.at("swap").say("Swap %d with larger child %d.", rootValue, childValue)
                        .var("root", root).var("child", largest)
                        .tree(render(values, largest, root)).step();
                root = largest;
            }
        }
        emit.at("done").say("Conversion complete. Every parent is at least as large as its children: %s.",
                        Arrays.toString(values))
                .var("root", values[0]).tree(render(values, 0, -1)).step();
    }

    private static List<TreeNode> render(int[] values, int current, int target) {
        Integer[] boxed = Arrays.stream(values).boxed().toArray(Integer[]::new);
        Map<Integer, String> states = new LinkedHashMap<>();
        if (target >= 0) states.put(target, "target");
        if (current >= 0) states.put(current, "current");
        return new BinaryTreeLayout(boxed).render(states);
    }
}
