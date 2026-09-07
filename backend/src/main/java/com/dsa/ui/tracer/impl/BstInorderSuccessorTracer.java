package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The next value up from a given node, which splits into two genuinely different searches
 * depending on one thing: whether the node has a right subtree.
 *
 * <p>If it does, the successor is inside it - and it is the LEFTMOST node in there, not the
 * right child itself, because the right child may have smaller descendants that still
 * exceed the target. That search runs downward.
 *
 * <p>If it does not, nothing below the node can be the answer at all, so the successor has
 * to be an ancestor - specifically the deepest ancestor the walk turned LEFT at on its way
 * down, since turning left is precisely the moment the path passes a value larger than the
 * target. That search runs from the root again, remembering candidates. A node with no
 * right subtree and no left turn above it is the maximum, and has no successor.
 */
@Component
public class BstInorderSuccessorTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bst-inorder-successor";
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
                        .defaultValue(Arrays.asList(
                                8, 4, 12, 2, 6, 10, 14, 1, 3, 5, 7, 9, 11, 13, 15))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target value")
                        .help("The node whose inorder successor is wanted.")
                        .range(-99, 99)
                        .defaultValue(8)
                        .build());
    }

    /** A leaf: with no right subtree the answer cannot be below it, so the entirely different ancestor search runs instead. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("target", 7);
    }

    @Override
    public String annotatedCode() {
        return """
               public TreeNode inorderSuccessor(TreeNode root, int target) {
                   TreeNode node = root;
                   while (node != null) {
                       // @a locate
                       if (node.val == target) break;
                       node = target < node.val ? node.left : node.right;
                   }
                   if (node == null) return null;

                   if (node.right != null) {
                       TreeNode curr = node.right;
                       while (curr.left != null) {
                           // @a leftmostOfRight
                           curr = curr.left;
                       }
                       // @a successorBelow
                       return curr;
                   }

                   TreeNode successor = null;
                   TreeNode curr = root;
                   while (curr != null && curr.val != target) {
                       if (target < curr.val) {
                           // @a rememberAncestor
                           successor = curr;      // a left turn passes a larger value
                           curr = curr.left;
                       } else {
                           // @a passUnderneath
                           curr = curr.right;     // a right turn passes a smaller one
                       }
                   }
                   // @a successorAbove
                   return successor;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        int target = in.getInt("target");
        Map<Integer, String> states = new LinkedHashMap<>();

        Integer node = tree.isEmpty() ? null : tree.root();
        while (node != null) {
            if (tree.value(node) == target) {
                emit.at("locate")
                        .say("Located %d.", target)
                        .var("at", tree.value(node))
                        .tree(tree.render(states)).step();
                break;
            }
            states.put(node, "visited");
            Integer next = target < tree.value(node) ? tree.left(node) : tree.right(node);
            emit.at("locate")
                    .say("Locating %d: it is %s than %d, so keep descending.",
                            target, target < tree.value(node) ? "smaller" : "larger",
                            tree.value(node))
                    .var("at", tree.value(node))
                    .tree(tree.render(states)).step();
            node = next;
        }

        if (node == null) {
            emit.at("successorAbove")
                    .say("No node holds the value %d, so it has no successor in this tree.",
                            target)
                    .var("answer", "none").tree(tree.render(states)).step();
            return;
        }
        states.put(node, "target");

        Integer right = tree.right(node);
        if (right != null) {
            Integer curr = right;
            while (tree.left(curr) != null) {
                Integer next = tree.left(curr);
                states.put(curr, "visited");
                emit.at("leftmostOfRight")
                        .say("%d is inside %d's right subtree and so is larger than %d - but %d "
                                        + "has a left child, %d, which is larger than %d too and "
                                        + "smaller than %d. Keep going left.",
                                tree.value(curr), target, target, tree.value(curr),
                                tree.value(next), target, tree.value(curr))
                        .var("at", tree.value(curr)).tree(tree.render(states)).step();
                curr = next;
            }
            states.put(curr, "target");
            emit.at("successorBelow")
                    .say("%d has a right subtree, so the successor is inside it: the leftmost "
                                    + "node there, %d. Nothing between %d and %d exists in the "
                                    + "tree.",
                            target, tree.value(curr), target, tree.value(curr))
                    .var("answer", tree.value(curr)).tree(tree.render(states)).step();
            return;
        }

        Integer successor = null;
        Integer curr = tree.root();
        while (curr != null && tree.value(curr) != target) {
            if (target < tree.value(curr)) {
                successor = curr;
                states.put(curr, "queued");
                emit.at("rememberAncestor")
                        .say("%d has no right subtree, so the answer is above it. Walking down "
                                        + "again: turning LEFT at %d means %d was passed on the "
                                        + "way to something smaller, so it is a candidate.",
                                target, tree.value(curr), tree.value(curr))
                        .var("candidate", tree.value(curr)).tree(tree.render(states)).step();
                curr = tree.left(curr);
            } else {
                emit.at("passUnderneath")
                        .say("Turning RIGHT at %d means %d is smaller than the target, so it "
                                        + "cannot be the successor. The candidate stays %s.",
                                tree.value(curr), tree.value(curr),
                                successor == null ? "unset" : String.valueOf(tree.value(successor)))
                        .var("candidate", successor == null ? "none" : String.valueOf(tree.value(successor)))
                        .tree(tree.render(states)).step();
                curr = tree.right(curr);
            }
        }

        if (successor == null) {
            emit.at("successorAbove")
                    .say("%d has no right subtree and the walk never turned left above it, so "
                            + "%d is the largest value in the tree and has no successor.",
                            target, target)
                    .var("answer", "none").tree(tree.render(states)).step();
        } else {
            states.put(successor, "target");
            emit.at("successorAbove")
                    .say("The deepest left turn was at %d, so that is %d's inorder successor.",
                            tree.value(successor), target)
                    .var("answer", tree.value(successor)).tree(tree.render(states)).step();
        }
    }
}
