package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Distance in a tree is not depth. A node exactly k steps from the target can be below it,
 * above it, or off sideways through an ancestor - and a tree's pointers only run downward,
 * so two of those three directions are unreachable as the structure stands.
 *
 * <p>The fix is to stop treating it as a tree at all: record every node's parent once, and
 * from then on each node simply has up to three neighbours - left child, right child,
 * parent. That is an undirected graph, and the k-distance question on a graph is a plain
 * breadth-first search that stops after k rings. The visited set matters more than usual
 * here: without it the walk would immediately bounce back down the edge it came up.
 */
@Component
public class NodesDistanceKTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "nodes-distance-k";
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
                        .help("Level order, with null where a child is absent. Values must be "
                                + "distinct so the target is unambiguous.")
                        .length(1, 31).values(-99, 99)
                        .defaultValue(Arrays.asList(3, 5, 1, 6, 2, 0, 8, null, null, 7, 4))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target value")
                        .help("The node the distance is measured from.")
                        .range(-99, 99)
                        .defaultValue(5)
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Distance k")
                        .range(0, 10)
                        .defaultValue(2)
                        .build());
    }

    /** The root as the target and a single ring: only downward edges are ever used, so the parent map is never consulted. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("target", 3, "k", 1);
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> distanceK(TreeNode root, int targetValue, int k) {
                   Map<TreeNode, TreeNode> parent = new HashMap<>();
                   // @a mapParents
                   recordParents(root, null, parent);

                   // @a foundTarget
                   TreeNode target = find(root, targetValue);

                   Set<TreeNode> seen = new HashSet<>();
                   Queue<TreeNode> ring = new ArrayDeque<>();
                   ring.add(target);
                   seen.add(target);

                   for (int distance = 0; distance < k; distance++) {
                       // @a expandRing
                       int size = ring.size();
                       for (int i = 0; i < size; i++) {
                           TreeNode node = ring.poll();
                           for (TreeNode next : neighbours(node, parent)) {
                               if (seen.add(next)) {
                                   // @a stepOutward
                                   ring.add(next);
                               } else {
                                   // @a alreadySeen
                                   ;              // this is the edge we just came along
                               }
                           }
                       }
                   }
                   // @a done
                   return values(ring);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        int targetValue = in.getInt("target");
        int k = in.getInt("k");
        Map<Integer, String> states = new LinkedHashMap<>();

        Map<Integer, Integer> parent = new LinkedHashMap<>();
        if (!tree.isEmpty()) {
            recordParents(tree, tree.root(), null, parent);
        }
        emit.at("mapParents")
                .say("Every node's parent recorded. From here each node has up to three "
                        + "neighbours - left child, right child, parent - and the tree can be "
                        + "walked in any direction.")
                .var("nodesWithAParent", parent.size())
                .tree(tree.render(states)).step();

        Integer target = tree.isEmpty() ? null : find(tree, tree.root(), targetValue);
        if (target == null) {
            emit.at("done")
                    .say("No node holds the value %d, so there is nothing to measure from.",
                            targetValue)
                    .var("answer", "[]")
                    .tree(tree.render(states)).step();
            return;
        }

        states.put(target, "target");
        emit.at("foundTarget")
                .say("Found %d. It is ring 0 - distance 0 from itself.", targetValue)
                .var("target", targetValue).var("ring", 0)
                .tree(tree.render(states)).step();

        Set<Integer> seen = new LinkedHashSet<>();
        seen.add(target);
        Queue<Integer> ring = new ArrayDeque<>();
        ring.add(target);

        for (int distance = 0; distance < k && !ring.isEmpty(); distance++) {
            int size = ring.size();
            emit.at("expandRing")
                    .say("Ring %d holds %d node%s. Expanding it once gives ring %d.",
                            distance, size, size == 1 ? "" : "s", distance + 1)
                    .var("ring", distance).var("ringSize", size)
                    .var("frontier", values(tree, ring))
                    .tree(tree.render(states)).step();

            for (int i = 0; i < size; i++) {
                int node = ring.poll();
                if (!states.getOrDefault(node, "").equals("target")) {
                    states.put(node, "visited");
                }
                for (int next : neighbours(tree, node, parent)) {
                    if (seen.add(next)) {
                        ring.add(next);
                        states.put(next, "visiting");
                        emit.at("stepOutward")
                                .say("%d is one step from %d and has not been reached before, "
                                                + "so it lands in ring %d.",
                                        tree.value(next), tree.value(node), distance + 1)
                                .var("ring", distance + 1).var("reached", tree.value(next))
                                .var("frontier", values(tree, ring))
                                .tree(tree.render(states)).step();
                    } else {
                        emit.at("alreadySeen")
                                .say("%d is also next to %d, but it was reached at an earlier "
                                                + "distance - stepping back into it would "
                                                + "measure the edge we just came along.",
                                        tree.value(next), tree.value(node))
                                .var("ring", distance + 1).var("skipped", tree.value(next))
                                .var("frontier", values(tree, ring))
                                .tree(tree.render(states)).step();
                    }
                }
            }
        }

        List<Integer> answer = new ArrayList<>();
        for (int node : ring) {
            answer.add(tree.value(node));
            states.put(node, "done");
        }
        emit.at("done")
                .say("After %d ring%s, the frontier is exactly the nodes at distance %d from "
                                + "%d: %s.",
                        k, k == 1 ? "" : "s", k, targetValue, answer)
                .var("answer", answer.toString())
                .tree(tree.render(states)).step();
    }

    private void recordParents(BinaryTreeLayout tree, Integer index, Integer parentIndex,
                               Map<Integer, Integer> parent) {
        if (index == null) {
            return;
        }
        if (parentIndex != null) {
            parent.put(index, parentIndex);
        }
        recordParents(tree, tree.left(index), index, parent);
        recordParents(tree, tree.right(index), index, parent);
    }

    private Integer find(BinaryTreeLayout tree, Integer index, int value) {
        if (index == null) {
            return null;
        }
        if (tree.value(index) == value) {
            return index;
        }
        Integer left = find(tree, tree.left(index), value);
        return left != null ? left : find(tree, tree.right(index), value);
    }

    private List<Integer> neighbours(BinaryTreeLayout tree, int index,
                                     Map<Integer, Integer> parent) {
        List<Integer> out = new ArrayList<>(3);
        Integer left = tree.left(index);
        if (left != null) {
            out.add(left);
        }
        Integer right = tree.right(index);
        if (right != null) {
            out.add(right);
        }
        Integer up = parent.get(index);
        if (up != null) {
            out.add(up);
        }
        return out;
    }

    private String values(BinaryTreeLayout tree, Queue<Integer> ring) {
        List<Integer> out = new ArrayList<>(ring.size());
        for (int index : ring) {
            out.add(tree.value(index));
        }
        return out.toString();
    }
}
