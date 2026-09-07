package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Height is only the subroutine here; the answer is the ABANDONMENT.
 *
 * <p>Computing every height and then checking every node separately is O(n^2), because each
 * check re-walks the subtree below it. The fix is to make one number carry both jobs: the
 * recursion returns a real height when the subtree is balanced and the sentinel -1 when it
 * is not. A -1 arriving from either side is an instruction to stop - the node does not even
 * look at its other child, and its own parent will do the same. So the interesting steps in
 * this trace are the ones where a subtree is never visited at all.
 */
@Component
public class TreeBalancedTracer implements AlgorithmTracer {

    private static final int UNBALANCED = -1;

    @Override
    public String id() {
        return "tree-balanced";
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
                        // 5's left side is two deep and its right side empty, so the
                        // imbalance is found at 5, abandoned upward by 2, then by 1 - and
                        // node 3 is never examined at all.
                        .defaultValue(Arrays.asList(
                                1, 2, 3, 4, 5, null, null, null, null, 6, null,
                                null, null, null, null, null, null, null, null, 7))
                        .build());
    }

    /** A perfectly balanced tree, so nothing is ever abandoned and every node is examined. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, 2, 3, 4, 5, 6, 7));
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isBalanced(TreeNode root) {
                   boolean balanced = check(root) != -1;
                   // @a done
                   return balanced;
               }

               private int check(TreeNode node) {
                   if (node == null) {
                       // @a emptyIsZero
                       return 0;
                   }
                   int left = check(node.left);
                   if (left == -1) {
                       // @a abandonAfterLeft
                       return -1;              // the right child is never visited
                   }
                   int right = check(node.right);
                   if (right == -1) {
                       // @a abandonAfterRight
                       return -1;
                   }
                   if (Math.abs(left - right) > 1) {
                       // @a imbalanceFound
                       return -1;
                   }
                   // @a balancedHere
                   return 1 + Math.max(left, right);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();

        int result = tree.isEmpty() ? 0 : check(tree, tree.root(), states, emit);
        boolean balanced = result != UNBALANCED;

        emit.at("done")
                .say(balanced
                        ? "No node ever reported -1, so every subtree's two sides are within one level of each other: the tree IS balanced."
                        : "A -1 reached the root, so the tree is NOT balanced.")
                .var("answer", balanced)
                .tree(tree.render(states)).step();
    }

    private int check(BinaryTreeLayout tree, int index, Map<Integer, String> states,
                      StepEmitter emit) {
        emit.push("check(" + tree.value(index) + ")");
        states.put(index, "visiting");

        Integer left = tree.left(index);
        int leftHeight = 0;
        if (left == null) {
            emit.at("emptyIsZero")
                    .say("%d has no left child, so that side is height 0.", tree.value(index))
                    .var("height", 0).tree(tree.render(states)).step();
        } else {
            leftHeight = check(tree, left, states, emit);
        }

        if (leftHeight == UNBALANCED) {
            Integer skipped = tree.right(index);
            if (skipped == null) {
                emit.at("abandonAfterLeft")
                        .say("%d's left side came back -1. %d passes -1 straight up without "
                                        + "measuring anything of its own.",
                                tree.value(index), tree.value(index))
                        .var("returned", -1).tree(tree.render(states)).step();
            } else {
                emit.at("abandonAfterLeft")
                        .say("%d's left side came back -1. The right subtree rooted at %d is "
                                        + "now never visited at all - that skipped work is what "
                                        + "makes this O(n) instead of O(n^2).",
                                tree.value(index), tree.value(skipped))
                        .var("returned", -1).tree(tree.render(states)).step();
            }
            emit.pop();
            return UNBALANCED;
        }

        Integer right = tree.right(index);
        int rightHeight = 0;
        if (right == null) {
            emit.at("emptyIsZero")
                    .say("%d has no right child, so that side is height 0.", tree.value(index))
                    .var("height", 0).tree(tree.render(states)).step();
        } else {
            rightHeight = check(tree, right, states, emit);
        }

        if (rightHeight == UNBALANCED) {
            emit.at("abandonAfterRight")
                    .say("%d's right side came back -1, so %d reports -1 too. Its own heights "
                                    + "no longer matter.",
                            tree.value(index), tree.value(index))
                    .var("returned", -1).tree(tree.render(states)).step();
            emit.pop();
            return UNBALANCED;
        }

        if (Math.abs(leftHeight - rightHeight) > 1) {
            states.put(index, "target");
            emit.at("imbalanceFound")
                    .say("%d has height %d on the left and %d on the right - a gap of %d, more "
                                    + "than one level. This is the violation; report -1.",
                            tree.value(index), leftHeight, rightHeight,
                            Math.abs(leftHeight - rightHeight))
                    .var("leftHeight", leftHeight).var("rightHeight", rightHeight)
                    .var("returned", -1)
                    .tree(tree.render(states)).step();
            emit.pop();
            return UNBALANCED;
        }

        int height = 1 + Math.max(leftHeight, rightHeight);
        states.put(index, "visited");
        emit.at("balancedHere")
                .say("%d is fine: %d against %d is a gap of %d. Report a real height, %d, so "
                                + "the parent can keep going.",
                        tree.value(index), leftHeight, rightHeight,
                        Math.abs(leftHeight - rightHeight), height)
                .var("leftHeight", leftHeight).var("rightHeight", rightHeight)
                .var("returned", height)
                .tree(tree.render(states)).step();

        emit.pop();
        return height;
    }
}
