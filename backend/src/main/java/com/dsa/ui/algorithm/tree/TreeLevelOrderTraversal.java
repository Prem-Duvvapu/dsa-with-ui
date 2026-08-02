package com.dsa.ui.algorithm.tree;

import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.*;

/**
 * Problem: Tree Level Order Traversal (BFS level-by-level)
 */
public class TreeLevelOrderTraversal {

    public static class Node {
        public int val;
        public Node left;
        public Node right;

        public Node(int val) {
            this.val = val;
        }
    }

    public List<List<Integer>> solve(Node root, TraceRecorder recorder) {
        List<List<Integer>> result = new ArrayList<>();
        Map<Integer, String> nodeStates = new HashMap<>();

        if (root == null) return result;

        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        nodeStates.put(root.val, "calling");

        recorder.record(new TraceEvent(
            "start", 12,
            String.format("Start Level Order Traversal (BFS). Push root node %d to queue.", root.val),
            Map.of("queue", List.of(root.val).toString()),
            "Tree", null, List.of("levelOrder()"), new HashMap<>(nodeStates), null
        ));

        int level = 0;
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<Integer> currentLevel = new ArrayList<>();

            recorder.record(new TraceEvent(
                "level_start", 18,
                String.format("Begin Level %d: Queue size = %d nodes to process.", level, levelSize),
                Map.of("level", String.valueOf(level), "levelSize", String.valueOf(levelSize)),
                "Tree", null, List.of("levelOrder()"), new HashMap<>(nodeStates), null
            ));

            for (int i = 0; i < levelSize; i++) {
                Node curr = queue.poll();
                currentLevel.add(curr.val);
                nodeStates.put(curr.val, "visited");

                if (curr.left != null) {
                    queue.add(curr.left);
                    nodeStates.put(curr.left.val, "calling");
                }
                if (curr.right != null) {
                    queue.add(curr.right);
                    nodeStates.put(curr.right.val, "calling");
                }

                recorder.record(new TraceEvent(
                    "process_node", 26,
                    String.format("Level %d: Dequeue Node %d -> Add to level %d result list %s.", level, curr.val, level, currentLevel.toString()),
                    Map.of("processedVal", String.valueOf(curr.val), "currentLevel", currentLevel.toString()),
                    "Tree", null, List.of("levelOrder()"), new HashMap<>(nodeStates), null
                ));
            }

            result.add(currentLevel);
            level++;
        }

        recorder.record(new TraceEvent(
            "complete", 35,
            String.format("Level Order Traversal Complete! Levels: %s", result.toString()),
            Map.of("resultLevels", result.toString()),
            "Tree", null, List.of(), new HashMap<>(nodeStates), null
        ));

        return result;
    }
}
