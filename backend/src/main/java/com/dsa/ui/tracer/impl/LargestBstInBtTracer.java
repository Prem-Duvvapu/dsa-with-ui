package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Finding the biggest valid BST hiding inside a tree that is not one.
 *
 * <p>Checking every subtree independently is O(n^2), because validating a subtree re-walks
 * everything below it. One postorder pass avoids that by making each node report three
 * things upward at once - whether its subtree is a BST, how big it is, and the smallest and
 * largest values it contains. A node is then a BST exactly when both children reported one
 * AND its own value sits strictly between the left subtree's maximum and the right subtree's
 * minimum, which is a constant-time check against numbers that already came back.
 *
 * <p>This is the one problem in the BST group whose input is deliberately NOT expected to be
 * a BST - so it does not opt into the contract test's BST-aware growth. A malformed tree is
 * exactly the interesting input here.
 */
@Component
public class LargestBstInBtTracer implements AlgorithmTracer {

    private record Info(int size, int min, int max, boolean isBst) {}

    @Override
    public String id() {
        return "largest-bst-in-bt";
    }

    @Override
    public DsType dsType() {
        return DsType.TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("tree", FieldType.BINARY_TREE)
                        .label("Binary tree (level order)")
                        .help("Any binary tree in level order, with null where a child is "
                                + "absent. It need not be a BST - that is the point.")
                        .length(1, 31).values(-99, 99)
                        .defaultValue(Arrays.asList(10, 5, 15, 1, 8, null, 7))
                        .build());
    }

    /** A tree that IS a BST from top to bottom, so the largest embedded BST is the whole thing and nothing is ever rejected. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(8, 4, 12, 2, 6, 10, 14));
    }

    @Override
    public String annotatedCode() {
        return """
               private int best = 0;

               public int largestBst(TreeNode root) {
                   visit(root);
                   // @a done
                   return best;
               }

               private Info visit(TreeNode node) {
                   if (node == null) {
                       // @a emptySubtree
                       return new Info(0, MAX_VALUE, MIN_VALUE, true);
                   }
                   Info left = visit(node.left);
                   Info right = visit(node.right);

                   if (left.isBst && right.isBst
                           && left.max < node.val && node.val < right.min) {
                       int size = left.size + right.size + 1;
                       if (size > best) {
                           // @a newLargest
                           best = size;
                       }
                       // @a subtreeIsBst
                       return new Info(size, min(node.val, left.min), max(node.val, right.max), true);
                   }
                   // @a notABst
                   return new Info(max(left.size, right.size), MIN_VALUE, MAX_VALUE, false);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();
        int[] best = {0};

        if (!tree.isEmpty()) {
            visit(tree, tree.root(), states, best, emit);
        }

        emit.at("done")
                .say("The largest subtree that is a valid BST holds %d node%s - found in one "
                        + "postorder pass, with every subtree checked in constant time against "
                        + "what its children reported.",
                        best[0], best[0] == 1 ? "" : "s")
                .var("answer", best[0])
                .tree(tree.render(states)).step();
    }

    private Info visit(BinaryTreeLayout tree, int index, Map<Integer, String> states,
                       int[] best, StepEmitter emit) {
        emit.push("visit(" + tree.value(index) + ")");

        Integer leftIndex = tree.left(index);
        Info left;
        if (leftIndex == null) {
            left = new Info(0, Integer.MAX_VALUE, Integer.MIN_VALUE, true);
            emit.at("emptySubtree")
                    .say("%d has no left child. An empty subtree is a BST of size 0, and it "
                                    + "reports impossible bounds so it never blocks its parent.",
                            tree.value(index))
                    .var("size", 0).var("isBst", true)
                    .tree(tree.render(states)).step();
        } else {
            left = visit(tree, leftIndex, states, best, emit);
        }

        Integer rightIndex = tree.right(index);
        Info right;
        if (rightIndex == null) {
            right = new Info(0, Integer.MAX_VALUE, Integer.MIN_VALUE, true);
            emit.at("emptySubtree")
                    .say("%d has no right child either, so that side also reports a BST of "
                            + "size 0.", tree.value(index))
                    .var("size", 0).var("isBst", true)
                    .tree(tree.render(states)).step();
        } else {
            right = visit(tree, rightIndex, states, best, emit);
        }

        int value = tree.value(index);
        boolean isBst = left.isBst() && right.isBst() && left.max() < value && value < right.min();

        if (isBst) {
            int size = left.size() + right.size() + 1;
            if (size > best[0]) {
                best[0] = size;
                emit.at("newLargest")
                        .say("That subtree holds %d node%s - larger than anything valid found "
                                        + "so far, so it becomes the running answer.",
                                size, size == 1 ? "" : "s")
                        .var("size", size).var("best", best[0])
                        .tree(tree.render(states)).step();
            }
            states.put(index, "target");
            emit.at("subtreeIsBst")
                    .say("Both sides of %d reported a BST, and %s < %d < %s. So the subtree at "
                                    + "%d is itself a BST, spanning %s.",
                            value, bound(left.max(), "nothing below"), value,
                            bound(right.min(), "nothing above"), value,
                            span(Math.min(value, left.min()), Math.max(value, right.max())))
                    .var("size", size).var("best", best[0]).var("isBst", true)
                    .tree(tree.render(states)).step();
            emit.pop();
            return new Info(size, Math.min(value, left.min()), Math.max(value, right.max()), true);
        }

        states.put(index, "visited");
        emit.at("notABst")
                .say("%d fails: %s. Its subtree is not a BST, so it reports impossible bounds "
                                + "upward - every ancestor will fail on it too - while still "
                                + "passing on the best size found underneath.",
                        value, reason(left, right, value))
                .var("size", Math.max(left.size(), right.size())).var("best", best[0])
                .var("isBst", false)
                .tree(tree.render(states)).step();
        emit.pop();
        return new Info(Math.max(left.size(), right.size()),
                Integer.MIN_VALUE, Integer.MAX_VALUE, false);
    }

    private String reason(Info left, Info right, int value) {
        if (!left.isBst()) {
            return "its left subtree is already not a BST";
        }
        if (!right.isBst()) {
            return "its right subtree is already not a BST";
        }
        if (left.max() >= value) {
            return "its left subtree contains " + left.max() + ", which is not below " + value;
        }
        return "its right subtree contains " + right.min() + ", which is not above " + value;
    }

    private String bound(int value, String whenAbsent) {
        if (value == Integer.MAX_VALUE || value == Integer.MIN_VALUE) {
            return whenAbsent;
        }
        return String.valueOf(value);
    }

    private String span(int min, int max) {
        return "[" + min + ", " + max + "]";
    }
}
