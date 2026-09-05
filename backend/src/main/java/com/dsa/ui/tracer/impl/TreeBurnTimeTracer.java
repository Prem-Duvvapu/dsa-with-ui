package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Fire spreads to every adjacent node each minute, and "adjacent" in an undirected sense -
 * a node's neighbors are its parent as much as its children. A binary tree's own left/right
 * pointers only go downward, so the first pass builds a parent map before any spreading
 * starts; from there it is an ordinary multi-source BFS, one frontier per minute, and the
 * answer is simply how many frontiers it took to reach every node.
 */
@Component
public class TreeBurnTimeTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "tree-burn-time";
    }

    @Override
    public DsType dsType() {
        return DsType.TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("tree", FieldType.BINARY_TREE)
                        .label("Tree (level order)")
                        .help("Level order, with null where a child is absent.")
                        .length(1, 31).values(-99, 99)
                        .defaultValue(java.util.Arrays.asList(1, 5, 3, 4, 9, 6, 2))
                        .build(),
                InputField.of("start", FieldType.INT)
                        .label("Starting value")
                        .range(-99, 99)
                        .defaultValue(9)
                        .build());
    }

    /** A sparser tree with an absent branch, starting mid-tree rather than at a leaf. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", java.util.Arrays.asList(1, 5, 3, null, 4, 10, 6), "start", 4);
    }

    @Override
    public String annotatedCode() {
        return """
               public int amountOfTime(TreeNode root, int start) {
                   Map<TreeNode, TreeNode> parent = new HashMap<>();
                   // @a buildParentMap
                   buildParentMap(root, null, parent);

                   TreeNode startNode = find(root, start);
                   // @a foundStart
                   Set<TreeNode> visited = new HashSet<>();
                   Queue<TreeNode> queue = new LinkedList<>();
                   queue.add(startNode);
                   visited.add(startNode);
                   int minutes = 0;

                   while (!queue.isEmpty()) {
                       boolean spread = false;
                       for (int i = queue.size(); i > 0; i--) {
                           TreeNode node = queue.poll();
                           for (TreeNode neighbor : neighbors(node, parent)) {
                               if (!visited.contains(neighbor)) {
                                   // @a spreadToNeighbor
                                   visited.add(neighbor);
                                   queue.add(neighbor);
                                   spread = true;
                               }
                           }
                       }
                       if (spread) {
                           // @a minuteComplete
                           minutes++;
                       }
                   }
                   // @a done
                   return minutes;
               }

               private void buildParentMap(TreeNode node, TreeNode parentNode, Map<TreeNode, TreeNode> parent) {
                   if (node == null) return;
                   if (parentNode != null) parent.put(node, parentNode);
                   buildParentMap(node.left, node, parent);
                   buildParentMap(node.right, node, parent);
               }

               private TreeNode find(TreeNode node, int value) {
                   if (node == null || node.val == value) return node;
                   TreeNode left = find(node.left, value);
                   return left != null ? left : find(node.right, value);
               }

               private List<TreeNode> neighbors(TreeNode node, Map<TreeNode, TreeNode> parent) {
                   List<TreeNode> out = new ArrayList<>();
                   if (parent.containsKey(node)) out.add(parent.get(node));
                   if (node.left != null) out.add(node.left);
                   if (node.right != null) out.add(node.right);
                   return out;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        int start = in.getInt("start");
        Map<Integer, String> states = new LinkedHashMap<>();

        Map<Integer, Integer> parent = new LinkedHashMap<>();
        buildParentMap(tree, tree.root(), null, parent);
        emit.at("buildParentMap")
                .say("Parent map built for every node - each node's neighbors are its "
                        + "parent plus its children.")
                .tree(tree.render(states)).step();

        int startIdx = findByValue(tree, tree.root(), start);
        states.put(startIdx, "target");
        emit.at("foundStart")
                .say("Found the starting node: %d. The fire begins here at minute 0.", start)
                .var("start", start)
                .tree(tree.render(states)).step();

        Map<Integer, Boolean> visited = new LinkedHashMap<>();
        visited.put(startIdx, true);
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(startIdx);
        int minutes = 0;

        while (!queue.isEmpty()) {
            boolean spread = false;
            int frontierSize = queue.size();
            for (int i = 0; i < frontierSize; i++) {
                int node = queue.poll();
                for (int neighbor : neighborsOf(tree, node, parent)) {
                    if (!visited.containsKey(neighbor)) {
                        visited.put(neighbor, true);
                        queue.add(neighbor);
                        states.put(neighbor, "burned");
                        spread = true;
                        emit.at("spreadToNeighbor")
                                .say("Fire spreads from %d to %d.", tree.value(node), tree.value(neighbor))
                                .var("from", tree.value(node)).var("to", tree.value(neighbor))
                                .tree(tree.render(states)).step();
                    }
                }
            }
            if (spread) {
                minutes++;
                emit.at("minuteComplete")
                        .say("Minute %d complete.", minutes)
                        .var("minutes", minutes)
                        .tree(tree.render(states)).step();
            }
        }

        emit.at("done")
                .say("Every node is on fire. Total time: %d minute%s.", minutes, minutes == 1 ? "" : "s")
                .var("answer", minutes)
                .tree(tree.render(states)).step();
    }

    private void buildParentMap(BinaryTreeLayout tree, Integer index, Integer parentIdx,
                                 Map<Integer, Integer> parent) {
        if (index == null) {
            return;
        }
        if (parentIdx != null) {
            parent.put(index, parentIdx);
        }
        buildParentMap(tree, tree.left(index), index, parent);
        buildParentMap(tree, tree.right(index), index, parent);
    }

    private Integer findByValue(BinaryTreeLayout tree, Integer index, int value) {
        if (index == null) {
            return null;
        }
        if (tree.value(index) == value) {
            return index;
        }
        Integer left = findByValue(tree, tree.left(index), value);
        if (left != null) {
            return left;
        }
        return findByValue(tree, tree.right(index), value);
    }

    private java.util.List<Integer> neighborsOf(BinaryTreeLayout tree, int index, Map<Integer, Integer> parent) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        if (parent.containsKey(index)) {
            out.add(parent.get(index));
        }
        if (tree.left(index) != null) {
            out.add(tree.left(index));
        }
        if (tree.right(index) != null) {
            out.add(tree.right(index));
        }
        return out;
    }
}
