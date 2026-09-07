package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks the outline of the tree anti-clockwise: root, then down the left edge, then across
 * every leaf left to right, then back up the right edge.
 *
 * <p>Three sub-walks with three different rules, and the reason the problem is fiddly is
 * that they overlap. A leaf on the left edge belongs to the leaf pass, not the left pass;
 * the same for the right. So both edge walks skip leaves entirely, and only the leaf pass
 * emits them - which is what stops the corners being printed twice. The right edge is also
 * collected top-down and reversed at the end, because the outline needs it bottom-up.
 *
 * <p>The edge walk itself prefers the child on its own side but falls through to the other
 * one, so a missing left child does not end the left boundary prematurely.
 */
@Component
public class BoundaryTraversalTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "boundary-traversal";
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
                        .defaultValue(Arrays.asList(
                                1, 2, 3, 4, 5, 6, 7, null, null, 8, 9, null, null, 10, 11))
                        .build());
    }

    /** No left subtree at all, so the left-edge walk never runs a single iteration. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, null, 2, null, null, 3, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> boundary(TreeNode root) {
                   List<Integer> out = new ArrayList<>();
                   if (root == null) return out;
                   if (!isLeaf(root)) {
                       // @a addRoot
                       out.add(root.val);
                   }

                   TreeNode curr = root.left;
                   while (curr != null) {
                       if (!isLeaf(curr)) {
                           // @a leftEdge
                           out.add(curr.val);
                       } else {
                           // @a leftEdgeLeaf
                           ;                      // the leaf pass owns it
                       }
                       curr = curr.left != null ? curr.left : curr.right;
                   }

                   addLeaves(root, out);

                   List<Integer> rightSide = new ArrayList<>();
                   curr = root.right;
                   while (curr != null) {
                       if (!isLeaf(curr)) {
                           // @a rightEdge
                           rightSide.add(curr.val);
                       } else {
                           // @a rightEdgeLeaf
                           ;                      // the leaf pass owns it
                       }
                       curr = curr.right != null ? curr.right : curr.left;
                   }
                   // @a reverseRight
                   Collections.reverse(rightSide);
                   out.addAll(rightSide);

                   // @a done
                   return out;
               }

               private void addLeaves(TreeNode node, List<Integer> out) {
                   if (node == null) return;
                   if (isLeaf(node)) {
                       // @a leaf
                       out.add(node.val);
                       return;
                   }
                   addLeaves(node.left, out);
                   addLeaves(node.right, out);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();
        List<Integer> out = new ArrayList<>();

        if (tree.isEmpty()) {
            emit.at("done").say("The tree is empty, so its boundary is empty.")
                    .var("boundary", "[]").tree(tree.render(states)).step();
            return;
        }

        int root = tree.root();
        if (!isLeaf(tree, root)) {
            out.add(tree.value(root));
            states.put(root, "target");
            emit.at("addRoot")
                    .say("Start the outline at the root %d. It is not a leaf, so it belongs to "
                            + "the boundary once, right here.", tree.value(root))
                    .var("boundary", out).tree(tree.render(states)).step();
        }

        Integer curr = tree.left(root);
        while (curr != null) {
            if (!isLeaf(tree, curr)) {
                out.add(tree.value(curr));
                states.put(curr, "target");
                emit.at("leftEdge")
                        .say("Left edge: %d is not a leaf, so it joins the outline now, on the "
                                + "way down.", tree.value(curr))
                        .var("boundary", out).tree(tree.render(states)).step();
            } else {
                emit.at("leftEdgeLeaf")
                        .say("Left edge reaches %d, which IS a leaf - skip it here. Printing it "
                                        + "now and again in the leaf pass is the classic "
                                        + "duplicate this check prevents.",
                                tree.value(curr))
                        .var("boundary", out).tree(tree.render(states)).step();
            }
            Integer left = tree.left(curr);
            curr = left != null ? left : tree.right(curr);
        }

        addLeaves(tree, root, out, states, emit);

        List<Integer> rightSide = new ArrayList<>();
        curr = tree.right(root);
        while (curr != null) {
            if (!isLeaf(tree, curr)) {
                rightSide.add(tree.value(curr));
                states.put(curr, "queued");
                emit.at("rightEdge")
                        .say("Right edge: %d is not a leaf, so it is collected - but held back, "
                                        + "because the outline needs the right side bottom-up.",
                                tree.value(curr))
                        .var("boundary", out).var("rightSideTopDown", rightSide)
                        .tree(tree.render(states)).step();
            } else {
                emit.at("rightEdgeLeaf")
                        .say("Right edge reaches %d, which IS a leaf - the leaf pass already "
                                        + "printed it, so skip it here.",
                                tree.value(curr))
                        .var("boundary", out).var("rightSideTopDown", rightSide)
                        .tree(tree.render(states)).step();
            }
            Integer right = tree.right(curr);
            curr = right != null ? right : tree.left(curr);
        }

        Collections.reverse(rightSide);
        out.addAll(rightSide);
        emit.at("reverseRight")
                .say("Reverse the right side to %s and append it - that is what turns the walk "
                        + "anti-clockwise instead of doubling back down.", rightSide)
                .var("boundary", out).var("rightSideBottomUp", rightSide)
                .tree(tree.render(states)).step();

        emit.at("done")
                .say("Outline complete, anti-clockwise from the root: %s.", out)
                .var("boundary", out).tree(tree.render(states)).step();
    }

    private void addLeaves(BinaryTreeLayout tree, int index, List<Integer> out,
                           Map<Integer, String> states, StepEmitter emit) {
        if (isLeaf(tree, index)) {
            out.add(tree.value(index));
            states.put(index, "target");
            emit.at("leaf")
                    .say("Leaf pass, left to right: %d has no children, so it joins the "
                            + "outline.", tree.value(index))
                    .var("boundary", out).tree(tree.render(states)).step();
            return;
        }
        Integer left = tree.left(index);
        if (left != null) {
            addLeaves(tree, left, out, states, emit);
        }
        Integer right = tree.right(index);
        if (right != null) {
            addLeaves(tree, right, out, states, emit);
        }
    }

    private boolean isLeaf(BinaryTreeLayout tree, int index) {
        return tree.left(index) == null && tree.right(index) == null;
    }
}
