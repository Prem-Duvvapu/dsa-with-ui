package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the BST property actually buys you, shown rather than stated: walk the tree in
 * order and the values come out sorted.
 *
 * <p>"Left is smaller, right is larger, everywhere" and "the inorder traversal is strictly
 * increasing" are the same sentence. That equivalence is the foundation every other BST
 * algorithm in this category stands on - search, floor, kth-smallest and successor are all
 * just ways of navigating that sorted sequence without materialising it.
 *
 * <p>This walks the whole tree rather than stopping at the first value that is out of
 * order, so a supplied tree that is not a BST shows exactly WHERE and how often the
 * ordering breaks instead of only that it does.
 */
@Component
public class BstIntroTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bst-intro";
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
                        .help("Level order, with null where a child is absent.")
                        .length(1, 31).values(-99, 99).bstOrdered()
                        .defaultValue(Arrays.asList(8, 4, 12, 2, 6, 10, 14))
                        .build());
    }

    /** The same shape with 9 hidden in 8's LEFT subtree: every parent-child pair still looks fine locally, and inorder still catches it. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(8, 4, 12, 2, 9, 10, 14));
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean inorderIsSorted(TreeNode root) {
                   List<Integer> seen = new ArrayList<>();
                   boolean sorted = walk(root, seen);
                   // @a done
                   return sorted;
               }

               private boolean walk(TreeNode node, List<Integer> seen) {
                   if (node == null) return true;
                   boolean sorted = walk(node.left, seen);

                   if (seen.isEmpty()) {
                       // @a firstValue
                       ;                               // nothing to compare against yet
                   } else if (node.val > seen.get(seen.size() - 1)) {
                       // @a largerThanLast
                       ;                               // the property holding, one step at a time
                   } else {
                       // @a notLarger
                       sorted = false;
                   }
                   seen.add(node.val);

                   return walk(node.right, seen) && sorted;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();
        List<Integer> seen = new ArrayList<>();

        boolean sorted = tree.isEmpty() || walk(tree, tree.root(), seen, states, emit);

        if (sorted) {
            emit.at("done")
                    .say("Inorder came out strictly increasing: %s. That IS the BST property - "
                            + "every other BST algorithm is a way of navigating this sorted "
                            + "sequence without ever building it.", seen)
                    .var("inorder", seen.toString()).var("isBst", true)
                    .tree(tree.render(states)).step();
        } else {
            emit.at("done")
                    .say("Inorder came out as %s, which is not increasing - so this tree is not "
                            + "a BST, and searching it by comparison would walk into the wrong "
                            + "subtree.", seen)
                    .var("inorder", seen.toString()).var("isBst", false)
                    .tree(tree.render(states)).step();
        }
    }

    private boolean walk(BinaryTreeLayout tree, int index, List<Integer> seen,
                         Map<Integer, String> states, StepEmitter emit) {
        boolean sorted = true;
        Integer left = tree.left(index);
        if (left != null) {
            sorted = walk(tree, left, seen, states, emit);
        }

        int value = tree.value(index);
        if (seen.isEmpty()) {
            states.put(index, "visited");
            emit.at("firstValue")
                    .say("%d is the leftmost node, so it is the first value inorder produces "
                            + "and the smallest the tree can hold.", value)
                    .var("value", value).var("inorder", "[" + value + "]")
                    .tree(tree.render(states)).step();
        } else if (value > seen.get(seen.size() - 1)) {
            states.put(index, "visited");
            emit.at("largerThanLast")
                    .say("%d comes next, and %d > %d - the previous value. Everything already "
                                    + "emitted lies in %d's left subtree or above it, which is "
                                    + "why inorder cannot go backwards in a BST.",
                            value, value, seen.get(seen.size() - 1), value)
                    .var("value", value).var("previous", seen.get(seen.size() - 1))
                    .tree(tree.render(states)).step();
        } else {
            sorted = false;
            states.put(index, "target");
            emit.at("notLarger")
                    .say("%d comes next but the previous value was %d, so inorder just went "
                                    + "BACKWARDS. Somewhere above, %d is sitting on the wrong "
                                    + "side of an ancestor.",
                            value, seen.get(seen.size() - 1), value)
                    .var("value", value).var("previous", seen.get(seen.size() - 1))
                    .tree(tree.render(states)).step();
        }
        seen.add(value);

        Integer right = tree.right(index);
        if (right != null) {
            sorted = walk(tree, right, seen, states, emit) && sorted;
        }
        return sorted;
    }
}
