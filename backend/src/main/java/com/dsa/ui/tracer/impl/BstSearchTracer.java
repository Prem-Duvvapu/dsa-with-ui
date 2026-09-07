package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Binary search, except the halving is already built into the shape.
 *
 * <p>One comparison at each node discards an entire subtree - not because the algorithm
 * checked what was in there, but because the BST property guarantees it. That is the whole
 * difference between this and searching a general binary tree, where both children have to
 * be explored because neither can be ruled out.
 *
 * <p>The default deliberately looks for a value the tree does not hold, so the trace shows
 * the other half of the algorithm: the walk running out of tree, which is the ONLY way a
 * BST search can conclude that something is absent.
 */
@Component
public class BstSearchTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bst-search";
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
                InputField.of("target", FieldType.INT)
                        .label("Target")
                        .range(-99, 99)
                        .defaultValue(5)
                        .build());
    }

    /** A value that IS in the tree, and on the opposite spine: the walk ends on a match instead of on an empty branch. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("target", 14);
    }

    @Override
    public String annotatedCode() {
        return """
               public TreeNode search(TreeNode root, int target) {
                   TreeNode curr = root;
                   while (curr != null) {
                       // @a compare
                       if (curr.val == target) break;

                       if (target < curr.val) {
                           // @a goLeft
                           curr = curr.left;     // the whole right subtree is ruled out
                       } else {
                           // @a goRight
                           curr = curr.right;    // the whole left subtree is ruled out
                       }
                   }
                   // @a done
                   return curr;                  // null means it is not in the tree
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        int target = in.getInt("target");
        Map<Integer, String> states = new LinkedHashMap<>();

        Integer curr = tree.isEmpty() ? null : tree.root();
        int comparisons = 0;

        while (curr != null) {
            int value = tree.value(curr);
            comparisons++;
            states.put(curr, "visiting");

            if (value == target) {
                states.put(curr, "target");
                emit.at("compare")
                        .say("%d == %d. Found it, after %d comparison%s.",
                                value, target, comparisons, comparisons == 1 ? "" : "s")
                        .var("comparisons", comparisons).var("at", value)
                        .tree(tree.render(states)).step();
                break;
            }

            emit.at("compare")
                    .say("Compare %d against %d at this node.", target, value)
                    .var("comparisons", comparisons).var("at", value)
                    .tree(tree.render(states)).step();
            states.put(curr, "visited");

            if (target < value) {
                Integer next = tree.left(curr);
                emit.at("goLeft")
                        .say("%d < %d, so nothing in %d's RIGHT subtree can possibly be %d - it "
                                        + "is discarded unexamined. Go left.",
                                target, value, value, target)
                        .var("comparisons", comparisons).var("at", value)
                        .tree(tree.render(states)).step();
                curr = next;
            } else {
                Integer next = tree.right(curr);
                emit.at("goRight")
                        .say("%d > %d, so nothing in %d's LEFT subtree can possibly be %d - it "
                                        + "is discarded unexamined. Go right.",
                                target, value, value, target)
                        .var("comparisons", comparisons).var("at", value)
                        .tree(tree.render(states)).step();
                curr = next;
            }
        }

        if (curr == null) {
            emit.at("done")
                    .say("The walk ran off the bottom of the tree after %d comparison%s. In a "
                                    + "BST that is proof %d is absent: every subtree that was "
                                    + "skipped was ruled out by an ordering guarantee, not by "
                                    + "looking.",
                            comparisons, comparisons == 1 ? "" : "s", target)
                    .var("comparisons", comparisons).var("answer", "not found")
                    .tree(tree.render(states)).step();
        } else {
            emit.at("done")
                    .say("%d is in the tree, reached in %d comparison%s rather than the %d a "
                                    + "general binary tree would have needed in the worst case.",
                            target, comparisons, comparisons == 1 ? "" : "s", nodeCount(tree))
                    .var("comparisons", comparisons).var("answer", target)
                    .tree(tree.render(states)).step();
        }
    }

    private int nodeCount(BinaryTreeLayout tree) {
        return tree.isEmpty() ? 0 : count(tree, tree.root());
    }

    private int count(BinaryTreeLayout tree, int index) {
        int total = 1;
        Integer left = tree.left(index);
        if (left != null) {
            total += count(tree, left);
        }
        Integer right = tree.right(index);
        if (right != null) {
            total += count(tree, right);
        }
        return total;
    }
}
