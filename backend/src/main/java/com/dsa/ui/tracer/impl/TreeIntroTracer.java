package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The vocabulary lesson run as an algorithm rather than told as a story: one walk that
 * names every node's role in the tree it was actually handed - which node is the root (the
 * only one with no parent), whose child each other node is, which nodes are leaves, and how
 * a node's height is one more than the taller of its two subtrees'.
 *
 * <p>The definitions are fixed; the answers are not, which is the whole reason this reads
 * its input instead of reciting a fixed four-node picture. Height is computed on the way
 * back up, after both children have reported theirs - the first place a learner meets the
 * shape every later tree algorithm reuses.
 */
@Component
public class TreeIntroTracer implements AlgorithmTracer {

    /** What one subtree reports back to its parent. */
    private record Stats(int height, int leaves) {}

    @Override
    public String id() {
        return "tree-intro";
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
                        .defaultValue(Arrays.asList(5, 3, 9, 2, 4, null, 11))
                        .build());
    }

    /** A chain rather than a bush: every node but the last has exactly one child, so the height equals the node count. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(7, 4, null, 2));
    }

    @Override
    public String annotatedCode() {
        return """
               public Stats summarise(Node root) {
                   Stats whole = describe(root, null, 0);
                   // @a done
                   return whole;              // height and leaf count for the whole tree
               }

               private Stats describe(Node node, Node parent, int depth) {
                   if (node == null) {
                       return new Stats(0, 0);
                   }
                   if (parent == null) {
                       // @a root
                       print(node.val + " is the root - the only node with no parent");
                   } else {
                       // @a childOfParent
                       print(node.val + " is a child of " + parent.val + ", depth " + depth);
                   }
                   if (node.left == null && node.right == null) {
                       // @a leaf
                       print(node.val + " is a leaf - nothing hangs below it");
                   }

                   Stats left = describe(node.left, node, depth + 1);
                   Stats right = describe(node.right, node, depth + 1);

                   // @a heightHere
                   int height = 1 + Math.max(left.height, right.height);
                   int leaves = left.leaves + right.leaves
                           + (node.left == null && node.right == null ? 1 : 0);
                   return new Stats(height, leaves);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();

        if (tree.isEmpty()) {
            emit.at("done")
                    .say("An empty tree has no root, no leaves, and height 0.")
                    .var("height", 0).var("leaves", 0)
                    .tree(tree.render(states)).step();
            return;
        }

        Stats whole = describe(tree, tree.root(), null, 0, states, emit);

        emit.at("done")
                .say("Whole tree: height %d (that is %d edge%s from the root down to the "
                                + "deepest leaf) and %d leaf node%s.",
                        whole.height(), whole.height() - 1, whole.height() == 2 ? "" : "s",
                        whole.leaves(), whole.leaves() == 1 ? "" : "s")
                .var("height", whole.height()).var("leaves", whole.leaves())
                .tree(tree.render(states)).step();
    }

    private Stats describe(BinaryTreeLayout tree, int index, Integer parent, int depth,
                           Map<Integer, String> states, StepEmitter emit) {
        emit.push("describe(" + tree.value(index) + ")");
        states.put(index, "visiting");

        if (parent == null) {
            emit.at("root")
                    .say("%d is the ROOT: the one node with nothing above it. Everything else "
                            + "in the tree is reached through it.", tree.value(index))
                    .var("node", tree.value(index)).var("depth", 0)
                    .tree(tree.render(states)).step();
        } else {
            emit.at("childOfParent")
                    .say("%d is a CHILD of %d - equivalently, %d is %d's PARENT. It sits at "
                                    + "depth %d, %d edge%s below the root.",
                            tree.value(index), tree.value(parent), tree.value(parent),
                            tree.value(index), depth, depth, depth == 1 ? "" : "s")
                    .var("node", tree.value(index)).var("parent", tree.value(parent))
                    .var("depth", depth)
                    .tree(tree.render(states)).step();
        }

        Integer left = tree.left(index);
        Integer right = tree.right(index);
        boolean isLeaf = left == null && right == null;

        if (isLeaf) {
            states.put(index, "target");
            emit.at("leaf")
                    .say("%d is a LEAF: neither a left nor a right child hangs off it, so its "
                            + "own height is 1.", tree.value(index))
                    .var("node", tree.value(index)).var("height", 1)
                    .tree(tree.render(states)).step();
        }

        Stats leftStats = left == null
                ? new Stats(0, 0)
                : describe(tree, left, index, depth + 1, states, emit);
        Stats rightStats = right == null
                ? new Stats(0, 0)
                : describe(tree, right, index, depth + 1, states, emit);

        int height = 1 + Math.max(leftStats.height(), rightStats.height());
        int leaves = leftStats.leaves() + rightStats.leaves() + (isLeaf ? 1 : 0);

        if (!isLeaf) {
            states.put(index, "visited");
        }
        emit.at("heightHere")
                .say("Both subtrees of %d have reported: height %d on the left, %d on the "
                                + "right. So %d's height is 1 + max(%d, %d) = %d.",
                        tree.value(index), leftStats.height(), rightStats.height(),
                        tree.value(index), leftStats.height(), rightStats.height(), height)
                .var("node", tree.value(index)).var("height", height).var("leavesBelow", leaves)
                .tree(tree.render(states)).step();

        emit.pop();
        return new Stats(height, leaves);
    }
}
