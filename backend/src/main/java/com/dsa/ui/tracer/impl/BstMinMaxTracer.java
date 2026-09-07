package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The two easiest questions a BST answers, and the only two whose walk never has to decide
 * anything.
 *
 * <p>Search compares at every node; floor keeps a best-so-far. This does neither. The
 * smallest value is wherever the left pointers run out and the largest is wherever the
 * right pointers run out - no comparison against a target, no candidate to remember, just
 * one direction held all the way down. It is also the clearest statement of what the BST
 * property means: the leftmost node cannot have anything smaller below it, because it has
 * nothing below it on the left at all.
 *
 * <p>A leaf on the way down is not the answer just because it is a leaf; only the node
 * whose relevant pointer is null is.
 */
@Component
public class BstMinMaxTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bst-min-max";
    }

    @Override
    public DsType dsType() {
        return DsType.TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("tree", FieldType.BINARY_TREE)
                        .label("BST (level order)")
                        .help("A BST in level order, with null where a child is absent.")
                        .length(1, 31).values(-99, 99).bstOrdered()
                        .defaultValue(Arrays.asList(30, 20, 40, 10, 25, 35, 50))
                        .build());
    }

    /** A right-leaning BST: the minimum is the root itself, so the left walk takes zero steps and the right walk takes every one. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(5, null, 9, null, null, 7, 12));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] minAndMax(TreeNode root) {
                   TreeNode curr = root;
                   while (curr.left != null) {
                       // @a alwaysLeft
                       curr = curr.left;
                   }
                   // @a minimumFound
                   int min = curr.val;

                   curr = root;
                   while (curr.right != null) {
                       // @a alwaysRight
                       curr = curr.right;
                   }
                   // @a maximumFound
                   int max = curr.val;

                   // @a done
                   return new int[]{min, max};
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();

        if (tree.isEmpty()) {
            emit.at("done").say("An empty BST has no minimum and no maximum.")
                    .var("min", "none").var("max", "none")
                    .tree(tree.render(states)).step();
            return;
        }

        int curr = tree.root();
        int leftSteps = 0;
        while (tree.left(curr) != null) {
            int next = tree.left(curr);
            leftSteps++;
            states.put(curr, "visited");
            emit.at("alwaysLeft")
                    .say("%d has a left child, so something smaller exists. Step to %d - no "
                                    + "comparison needed, the direction is fixed.",
                            tree.value(curr), tree.value(next))
                    .var("at", tree.value(next)).var("stepsLeft", leftSteps)
                    .tree(tree.render(states)).step();
            curr = next;
        }
        int min = tree.value(curr);
        states.put(curr, "target");
        emit.at("minimumFound")
                .say("%d has no left child at all, so nothing in the tree is smaller. Minimum "
                        + "= %d, after %d step%s.",
                        min, min, leftSteps, leftSteps == 1 ? "" : "s")
                .var("min", min).var("stepsLeft", leftSteps)
                .tree(tree.render(states)).step();

        curr = tree.root();
        int rightSteps = 0;
        while (tree.right(curr) != null) {
            int next = tree.right(curr);
            rightSteps++;
            if (!"target".equals(states.get(curr))) {
                states.put(curr, "visited");
            }
            if (rightSteps == 1) {
                emit.at("alwaysRight")
                        .say("Back at the root, heading the other way: %d has a right child, so "
                                        + "something larger exists. Step to %d.",
                                tree.value(curr), tree.value(next))
                        .var("at", tree.value(next)).var("stepsRight", rightSteps)
                        .tree(tree.render(states)).step();
            } else {
                emit.at("alwaysRight")
                        .say("%d still has a right child, so it is not the largest either. "
                                        + "Step to %d.",
                                tree.value(curr), tree.value(next))
                        .var("at", tree.value(next)).var("stepsRight", rightSteps)
                        .tree(tree.render(states)).step();
            }
            curr = next;
        }
        int max = tree.value(curr);
        states.put(curr, "target");
        emit.at("maximumFound")
                .say("%d has no right child, so nothing in the tree is larger. Maximum = %d, "
                        + "after %d step%s.",
                        max, max, rightSteps, rightSteps == 1 ? "" : "s")
                .var("max", max).var("stepsRight", rightSteps)
                .tree(tree.render(states)).step();

        emit.at("done")
                .say("Minimum %d, maximum %d - found in %d and %d pointer hops, with no value "
                                + "ever compared against another.",
                        min, max, leftSteps, rightSteps)
                .var("min", min).var("max", max)
                .tree(tree.render(states)).step();
    }
}
