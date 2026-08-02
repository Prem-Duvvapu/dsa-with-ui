package com.dsa.ui.algorithm.tree;

import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.*;

/**
 * Problem: Tree Postorder Traversal (Left -> Right -> Root)
 */
public class TreePostorderTraversal {

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
            "Start Tree Postorder Traversal (Left -> Right -> Root).",
            Map.of("rootVal", root != null ? String.valueOf(root.val) : "null"),
            "Tree", null, List.of("postorder(root)"), nodeStates, null
        ));

        traverse(root, result, recorder, nodeStates);

        recorder.record(new TraceEvent(
            "complete", 25,
            String.format("Postorder Traversal Complete! Result sequence: %s", result.toString()),
            Map.of("postorderResult", result.toString()),
            "Tree", null, List.of(), nodeStates, null
        ));

        return result;
    }

    private void traverse(Node node, List<Integer> result, TraceRecorder recorder, Map<Integer, String> nodeStates) {
        if (node == null) return;

        nodeStates.put(node.val, "calling");

        traverse(node.left, result, recorder, nodeStates);
        traverse(node.right, result, recorder, nodeStates);

        result.add(node.val);
        nodeStates.put(node.val, "visited");

        recorder.record(new TraceEvent(
            "visit_node", 20,
            String.format("Postorder Visit: Process Node %d AFTER left and right subtrees -> add %d to result. Total visited = %d.", node.val, node.val, result.size()),
            Map.of("visitedVal", String.valueOf(node.val)),
            "Tree", null, List.of("postorder(" + node.val + ")"), new HashMap<>(nodeStates), null
        ));
    }
}
