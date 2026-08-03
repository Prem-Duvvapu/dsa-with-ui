package com.dsa.ui.algorithm.tree;

import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.*;

/**
 * Problem: Tree Preorder Traversal (Root -> Left -> Right)
 */
public class TreePreorderTraversal {

    public static class Node {
        public int val;
        public Node left;
        public Node right;

        public Node(int val) {
            this.val = val;
        }
    }

    public List<Integer> solve(Node root, TraceRecorder recorder) {
        List<Integer> result = new ArrayList<>();
        Map<Integer, String> nodeStates = new HashMap<>();

        recorder.record(new TraceEvent(
            "start", 10,
            "Start Tree Preorder Traversal (Root -> Left -> Right).",
            Map.of("rootVal", root != null ? String.valueOf(root.val) : "null"),
            "Tree", null, List.of("preorder(root)"), nodeStates, null
        ));

        traverse(root, result, recorder, nodeStates);

        recorder.record(new TraceEvent(
            "complete", 25,
            String.format("Preorder Traversal Complete! Result sequence: %s", result.toString()),
            Map.of("preorderResult", result.toString()),
            "Tree", null, List.of(), nodeStates, null
        ));

        return result;
    }

    private void traverse(Node node, List<Integer> result, TraceRecorder recorder, Map<Integer, String> nodeStates) {
        if (node == null) return;

        nodeStates.put(node.val, "calling");
        recorder.record(new TraceEvent(
            "visit_node", 15,
            String.format("Preorder Visit: Process Root Node %d FIRST -> add %d to result. Total visited = %d.", node.val, node.val, result.size() + 1),
            Map.of("visitedVal", String.valueOf(node.val)),
            "Tree", null, List.of("preorder(" + node.val + ")"), new HashMap<>(nodeStates), null
        ));

        result.add(node.val);
        nodeStates.put(node.val, "visited");

        traverse(node.left, result, recorder, nodeStates);
        traverse(node.right, result, recorder, nodeStates);
    }
}
