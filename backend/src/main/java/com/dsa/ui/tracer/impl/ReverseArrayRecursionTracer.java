package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.TreeNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Two inward-moving pointers swap and recurse: {@code reverseArray(l, r)} swaps
 * {@code arr[l]}/{@code arr[r]} then calls {@code reverseArray(l + 1, r - 1)}, until the
 * pointers meet or cross. The call chain doubles as the recursion tree, and the array
 * itself is the live state riding alongside it.
 */
@Component
public class ReverseArrayRecursionTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "reverse-array-recursion";
    }

    @Override
    public DsType dsType() {
        return DsType.RECURSION_TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .length(1, 12).values(-999, 999)
                        .defaultValue(List.of(1, 2, 3, 4, 5))
                        .build());
    }

    /** A different length (even instead of odd) and different values — no leftover middle element. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(10, 20, 30, 40, 50, 60));
    }

    @Override
    public String annotatedCode() {
        return """
               public void reverseArray(int l, int r, int[] arr) {
                   if (l >= r) {
                       // @a base
                       return;
                   }
                   int temp = arr[l];
                   // @a swap
                   arr[l] = arr[r];
                   arr[r] = temp;
                   // @a recurse
                   reverseArray(l + 1, r - 1, arr);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("nums");
        int frameCount = arr.length / 2 + 1;
        List<TreeNode> nodes = buildChain(frameCount);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (TreeNode node : nodes) {
            states.put(node.getId(), "unvisited");
        }

        recurse(0, arr.length - 1, arr, 0, nodes, states, emit);
    }

    private void recurse(int l, int r, int[] arr, int depth, List<TreeNode> nodes, Map<Integer, String> states,
                          StepEmitter emit) {
        int id = depth + 1;
        emit.push("reverseArray(l=" + l + ", r=" + r + ")");

        if (l >= r) {
            states.put(id, "visited");
            emit.at("base")
                    .say(l == r
                            ? "l == r == %d — the middle element needs no swap. Reversal complete."
                            : "l (%d) has crossed r — reversal complete.", l)
                    .var("l", l).var("r", r)
                    .tree(nodes).nodes(states)
                    .array(arr, l == r ? l : -1).step();
            emit.pop();
            return;
        }

        states.put(id, "calling");
        int temp = arr[l];
        arr[l] = arr[r];
        arr[r] = temp;
        emit.at("swap")
                .say("Swap arr[%d] and arr[%d]: they are now %d and %d.", l, r, arr[l], arr[r])
                .var("l", l).var("r", r)
                .tree(nodes).nodes(states)
                .array(arr, l, r).step();

        recurse(l + 1, r - 1, arr, depth + 1, nodes, states, emit);

        states.put(id, "visited");
        emit.at("recurse")
                .say("reverseArray(l=%d, r=%d) returns now that its recursive call has finished.", l, r)
                .var("l", l).var("r", r)
                .tree(nodes).nodes(states)
                .array(arr, l, r).step();
        emit.pop();
    }

    /** One node per (l, r) call frame, chained top to bottom. */
    private static List<TreeNode> buildChain(int frameCount) {
        List<TreeNode> nodes = new ArrayList<>();
        for (int id = 1; id <= frameCount; id++) {
            Integer child = id < frameCount ? id + 1 : null;
            nodes.add(new TreeNode(id, "reverseArray(#" + id + ")", 200, 40 + 55.0 * (id - 1), child, null,
                    "unvisited"));
        }
        return nodes;
    }
}
