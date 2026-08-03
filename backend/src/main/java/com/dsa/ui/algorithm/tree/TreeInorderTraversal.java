package com.dsa.ui.algorithm.tree;

import com.dsa.ui.model.TreeNode;
import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.*;

/**
 * Problem: Binary Tree Inorder Traversal (Left -> Root -> Right)
 */
public class TreeInorderTraversal {

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
        List<String> callStack = new ArrayList<>();
        Map<Integer, String> nodeStates = new HashMap<>();

        recorder.record(new TraceEvent(
            "start", 12,
            "Binary Tree Inorder Traversal: Start recursive traversal (Left -> Visit Root -> Right).",
            Map.of("root", root != null ? String.valueOf(root.val) : "null"),
            "Tree", null, new ArrayList<>(callStack), new HashMap<>(nodeStates), List.of()
        ));

        inorder(root, result, callStack, nodeStates, recorder);

        recorder.record(new TraceEvent(
            "complete", 35,
            String.format("Inorder Traversal Complete! Result sequence: %s", result.toString()),
            Map.of("InorderResult", result.toString()),
            "Tree", null, List.of(), new HashMap<>(nodeStates), List.of()
        ));

        return result;
    }

    private void inorder(Node node, List<Integer> result, List<String> callStack,
                         Map<Integer, String> nodeStates, TraceRecorder recorder) {
        if (node == null) return;

        callStack.add("inorder(" + node.val + ")");
        nodeStates.put(node.val, "visiting");

        recorder.record(new TraceEvent(
            "recurse_left", 20,
            String.format("Node %d: Traverse LEFT subtree of node %d...", node.val, node.val),
            Map.of("node", String.valueOf(node.val)),
            "Tree", null, new ArrayList<>(callStack), new HashMap<>(nodeStates), List.of()
        ));

        inorder(node.left, result, callStack, nodeStates, recorder);

        // Visit Root
        result.add(node.val);
        nodeStates.put(node.val, "visited");

        recorder.record(new TraceEvent(
            "visit_node", 25,
            String.format("VISIT Node %d. Add to Inorder sequence: %s", node.val, result.toString()),
            Map.of("visited", String.valueOf(node.val), "result", result.toString()),
            "Tree", null, new ArrayList<>(callStack), new HashMap<>(nodeStates), List.of()
        ));

        recorder.record(new TraceEvent(
            "recurse_right", 30,
            String.format("Node %d: Traverse RIGHT subtree of node %d...", node.val, node.val),
            Map.of("node", String.valueOf(node.val)),
            "Tree", null, new ArrayList<>(callStack), new HashMap<>(nodeStates), List.of()
        ));

        inorder(node.right, result, callStack, nodeStates, recorder);

        callStack.remove(callStack.size() - 1);
    }
}
