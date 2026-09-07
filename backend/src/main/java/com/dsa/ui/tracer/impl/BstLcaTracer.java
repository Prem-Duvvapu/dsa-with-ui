package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The same question as {@code tree-lca}, answered without exploring anything.
 *
 * <p>The general-tree version has to descend into BOTH children of every node and wait for
 * the two answers to come back, because nothing about a plain binary tree says where a
 * value lives. In a BST the ordering says it exactly: if both targets are smaller than the
 * current node they are both somewhere in its left subtree, if both are larger they are
 * both on the right, and the FIRST node where those two facts disagree is the point their
 * two root-downward paths separate - which is the definition of the lowest common ancestor.
 *
 * <p>So this is a single walk down one path, never a search: no recursion, no backtracking,
 * no subtree explored and discarded. A node that equals one of the targets also counts as
 * a split, because a node is allowed to be its own ancestor.
 */
@Component
public class BstLcaTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bst-lca";
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
                        .defaultValue(Arrays.asList(6, 2, 8, 0, 4, 7, 9, null, null, 3, 5))
                        .build(),
                InputField.of("p", FieldType.INT)
                        .label("First target value")
                        .range(-99, 99)
                        .defaultValue(0)
                        .build(),
                InputField.of("q", FieldType.INT)
                        .label("Second target value")
                        .range(-99, 99)
                        .defaultValue(3)
                        .build());
    }

    /** Two values in the RIGHT half of the tree, so the first move goes the other way and the split happens one level down. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("p", 7, "q", 9);
    }

    @Override
    public String annotatedCode() {
        return """
               public TreeNode lowestCommonAncestor(TreeNode root, int p, int q) {
                   TreeNode curr = root;
                   TreeNode answer = null;

                   while (curr != null && answer == null) {
                       if (p < curr.val && q < curr.val) {
                           // @a bothSmallerGoLeft
                           curr = curr.left;      // both are in the left subtree
                       } else if (p > curr.val && q > curr.val) {
                           // @a bothLargerGoRight
                           curr = curr.right;     // both are in the right subtree
                       } else {
                           // @a pathsSplit
                           answer = curr;         // one each side, or curr IS one of them
                       }
                   }
                   // @a done
                   return answer;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        int p = in.getInt("p");
        int q = in.getInt("q");
        Map<Integer, String> states = new LinkedHashMap<>();

        Integer curr = tree.isEmpty() ? null : tree.root();
        Integer answer = null;
        int steps = 0;

        while (curr != null && answer == null) {
            int value = tree.value(curr);
            steps++;

            if (p < value && q < value) {
                states.put(curr, "visited");
                emit.at("bothSmallerGoLeft")
                        .say("%d and %d are both smaller than %d, so both must lie in its LEFT "
                                        + "subtree - the right side cannot contain either and "
                                        + "is never looked at.",
                                p, q, value)
                        .var("at", value).var("steps", steps)
                        .tree(tree.render(states)).step();
                curr = tree.left(curr);
            } else if (p > value && q > value) {
                states.put(curr, "visited");
                emit.at("bothLargerGoRight")
                        .say("%d and %d are both larger than %d, so both must lie in its RIGHT "
                                        + "subtree. Same reasoning, mirrored.",
                                p, q, value)
                        .var("at", value).var("steps", steps)
                        .tree(tree.render(states)).step();
                curr = tree.right(curr);
            } else {
                answer = curr;
                states.put(curr, "target");
                emit.at("pathsSplit")
                        .say("At %d the two targets stop agreeing: %d and %d are no longer on "
                                        + "the same side. This is exactly where their two paths "
                                        + "from the root separate, so %d is the LCA.",
                                value, p, q, value)
                        .var("at", value).var("steps", steps)
                        .tree(tree.render(states)).step();
            }
        }

        if (answer == null) {
            emit.at("done")
                    .say("The walk ran out of tree, so %d and %d are not both present.", p, q)
                    .var("answer", "none").var("steps", steps)
                    .tree(tree.render(states)).step();
        } else {
            emit.at("done")
                    .say("Lowest common ancestor of %d and %d is %d, found in %d comparison%s "
                                    + "down a single path - no subtree was ever explored and "
                                    + "discarded.",
                            p, q, tree.value(answer), steps, steps == 1 ? "" : "s")
                    .var("answer", tree.value(answer)).var("steps", steps)
                    .tree(tree.render(states)).step();
        }
    }
}
