package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A correct BST's inorder traversal is strictly increasing, so every place it isn't is a
 * fingerprint of the swap. Two adjacent values swapped produces exactly one such inversion;
 * two far-apart values swapped produces two, and the two culprits are the earlier element
 * of the first inversion and the later element of the second - everything strictly between
 * them was never touched. Which of those two shapes happened decides which pair actually
 * gets swapped back.
 */
@Component
public class CorrectBstSwapTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "correct-bst-swap";
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
                        .help("A BST with exactly two node values swapped. Not checked - "
                                + "a tree that isn't a swapped BST still runs, it just "
                                + "won't mean anything.")
                        .length(1, 31).values(-99, 99)
                        .defaultValue(java.util.Arrays.asList(5, 8, 3, 1, 4, 7, 9))
                        .build());
    }

    /** An adjacent-in-inorder swap instead: only one inversion is ever found, a genuinely different code path. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", java.util.Arrays.asList(4, 3, 8, 1, 5, 7, 9));
    }

    @Override
    public String annotatedCode() {
        return """
               public void recoverTree(TreeNode root) {
                   TreeNode first = null, middle = null, prev = null, last = null;
                   TreeNode curr = root;
                   Deque<TreeNode> stack = new ArrayDeque<>();

                   while (curr != null || !stack.isEmpty()) {
                       while (curr != null) {
                           stack.push(curr);
                           curr = curr.left;
                       }
                       curr = stack.pop();
                       // @a visitInorder
                       if (prev != null && prev.val > curr.val) {
                           if (first == null) {
                               // @a firstInversion
                               first = prev;
                               middle = curr;
                           } else {
                               // @a secondInversion
                               last = curr;
                           }
                       }
                       prev = curr;
                       curr = curr.right;
                   }

                   if (last != null) {
                       // @a swapNonAdjacent
                       int temp = first.val; first.val = last.val; last.val = temp;
                   } else {
                       // @a swapAdjacent
                       int temp = first.val; first.val = middle.val; middle.val = temp;
                   }
                   // @a done
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();

        Integer first = null, middle = null, prev = null, last = null;
        Integer curr = tree.root();
        java.util.Deque<Integer> stack = new java.util.ArrayDeque<>();

        while (curr != null || !stack.isEmpty()) {
            while (curr != null) {
                stack.push(curr);
                curr = tree.left(curr);
            }
            curr = stack.pop();
            states.put(curr, "visiting");
            emit.at("visitInorder")
                    .say("Inorder visits %d.", tree.value(curr))
                    .var("visiting", tree.value(curr))
                    .tree(tree.render(states)).step();

            if (prev != null && tree.value(prev) > tree.value(curr)) {
                if (first == null) {
                    first = prev;
                    middle = curr;
                    emit.at("firstInversion")
                            .say("%d > %d - out of order. First inversion: first=%d, middle=%d.",
                                    tree.value(prev), tree.value(curr), tree.value(first), tree.value(middle))
                            .var("first", tree.value(first)).var("middle", tree.value(middle))
                            .tree(tree.render(states)).step();
                } else {
                    last = curr;
                    emit.at("secondInversion")
                            .say("%d > %d - out of order again. Second inversion: last=%d.",
                                    tree.value(prev), tree.value(curr), tree.value(last))
                            .var("last", tree.value(last))
                            .tree(tree.render(states)).step();
                }
            }
            prev = curr;
            curr = tree.right(curr);
        }

        if (last != null) {
            emit.at("swapNonAdjacent")
                    .say("Two separate inversions found - the swapped values are %d and %d, "
                            + "not adjacent in inorder order. Swap them back.",
                            tree.value(first), tree.value(last))
                    .var("swapped", tree.value(first) + " <-> " + tree.value(last))
                    .tree(tree.render(states)).step();
        } else {
            emit.at("swapAdjacent")
                    .say("Only one inversion found - the swapped values are %d and %d, "
                            + "adjacent in inorder order. Swap them back.",
                            tree.value(first), tree.value(middle))
                    .var("swapped", tree.value(first) + " <-> " + tree.value(middle))
                    .tree(tree.render(states)).step();
        }

        emit.at("done")
                .say("BST recovered.")
                .tree(tree.render(states)).step();
    }
}
