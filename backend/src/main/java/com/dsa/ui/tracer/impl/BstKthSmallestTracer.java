package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The kth smallest value without sorting anything, because a BST's inorder walk is already
 * sorted - so the kth value it produces IS the answer.
 *
 * <p>What makes this more than "collect the inorder and index into it" is that the walk
 * stops the instant the counter reaches zero. The remainder of the tree is never visited,
 * and neither is any ancestor's right subtree: once an answer exists, every frame still on
 * the call stack unwinds without doing anything. That is O(h + k) rather than O(n), and the
 * steps where a frame notices the answer already exists are the ones that show it.
 */
@Component
public class BstKthSmallestTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bst-kth-smallest";
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
                        .defaultValue(Arrays.asList(8, 4, 12, 2, 6, 10, 14))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("k")
                        .help("1 means the smallest value in the tree.")
                        .range(1, 31)
                        .defaultValue(3)
                        .build());
    }

    /** The largest value in the tree, so the counter only reaches zero on the very last node and nothing is skipped at all. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("k", 7);
    }

    @Override
    public String annotatedCode() {
        return """
               private int remaining;
               private Integer answer = null;

               public int kthSmallest(TreeNode root, int k) {
                   remaining = k;
                   inorder(root);
                   // @a done
                   return answer;
               }

               private void inorder(TreeNode node) {
                   if (node == null || answer != null) return;
                   // @a descendLeft
                   inorder(node.left);

                   if (answer != null) {
                       // @a alreadyFound
                       return;              // the kth was somewhere in the left subtree
                   }
                   remaining--;
                   if (remaining == 0) {
                       // @a kthReached
                       answer = node.val;
                       return;
                   }
                   // @a countAndContinue
                   inorder(node.right);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        int k = in.getInt("k");
        Map<Integer, String> states = new LinkedHashMap<>();
        int[] remaining = {k};
        Integer[] answer = {null};

        if (!tree.isEmpty()) {
            inorder(tree, tree.root(), remaining, answer, states, emit);
        }

        if (answer[0] == null) {
            emit.at("done")
                    .say("The whole tree was walked and the counter never reached zero, so this "
                            + "BST holds fewer than %d values.", k)
                    .var("answer", "none").tree(tree.render(states)).step();
        } else {
            emit.at("done")
                    .say("The %d%s smallest value is %d.", k, ordinal(k), answer[0])
                    .var("answer", answer[0]).tree(tree.render(states)).step();
        }
    }

    private void inorder(BinaryTreeLayout tree, int index, int[] remaining, Integer[] answer,
                         Map<Integer, String> states, StepEmitter emit) {
        emit.push("inorder(" + tree.value(index) + ")");
        states.put(index, "visiting");

        Integer left = tree.left(index);
        if (left == null) {
            emit.at("descendLeft")
                    .say("%d has no left child, so it is the smallest value not yet counted in "
                            + "this subtree.", tree.value(index))
                    .var("remaining", remaining[0]).tree(tree.render(states)).step();
        } else {
            emit.at("descendLeft")
                    .say("Everything smaller than %d lies to its left, so nothing here can be "
                                    + "counted until %d's subtree is finished.",
                            tree.value(index), tree.value(left))
                    .var("remaining", remaining[0]).tree(tree.render(states)).step();
            inorder(tree, left, remaining, answer, states, emit);
        }

        if (answer[0] != null) {
            states.put(index, "unvisited");
            emit.at("alreadyFound")
                    .say("The answer was already found below %d, so this frame returns without "
                            + "counting and without touching its right subtree.", tree.value(index))
                    .var("remaining", remaining[0]).var("answer", answer[0])
                    .tree(tree.render(states)).step();
            emit.pop();
            return;
        }

        remaining[0]--;
        if (remaining[0] == 0) {
            answer[0] = tree.value(index);
            states.put(index, "target");
            emit.at("kthReached")
                    .say("Counting %d brings the counter to 0 - it is the kth smallest. Stop "
                            + "here; nothing to the right of it is ever visited.", tree.value(index))
                    .var("remaining", 0).var("answer", answer[0])
                    .tree(tree.render(states)).step();
            emit.pop();
            return;
        }

        states.put(index, "visited");
        Integer right = tree.right(index);
        emit.at("countAndContinue")
                .say("Count %d - %d value%s still to go. Continue into everything larger.",
                        tree.value(index), remaining[0], remaining[0] == 1 ? "" : "s")
                .var("remaining", remaining[0]).tree(tree.render(states)).step();
        if (right != null) {
            inorder(tree, right, remaining, answer, states, emit);
        }
        emit.pop();
    }

    private String ordinal(int k) {
        if (k % 100 >= 11 && k % 100 <= 13) {
            return "th";
        }
        return switch (k % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
