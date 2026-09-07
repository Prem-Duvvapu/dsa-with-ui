package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Rebuilding a BST from preorder alone - LeetCode 1008 - which is possible only because
 * BST-ness supplies the second traversal for free.
 *
 * <p>A general binary tree needs inorder alongside preorder to be pinned down. A BST does
 * not, because its inorder IS the sorted order of the same values, so nothing new would be
 * learned by being told it. The information inorder would have given - where each subtree
 * ends - is recovered instead from an upper bound carried down the recursion: everything in
 * a left subtree is smaller than its parent, so the moment the next preorder value exceeds
 * the current bound, that subtree is over and the value belongs to an ancestor.
 *
 * <p>The array is consumed strictly left to right by a single shared index, exactly once
 * per value, which is what makes this O(n) rather than the O(n^2) of re-scanning for a
 * split point.
 */
@Component
public class ConstructBstPreorderTracer implements AlgorithmTracer {

    private static final int NO_BOUND = Integer.MAX_VALUE;

    @Override
    public String id() {
        return "construct-bst-preorder";
    }

    @Override
    public DsType dsType() {
        return DsType.TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("preorder", FieldType.INT_ARRAY)
                        .label("Preorder of a BST")
                        .help("Root, then the whole left subtree, then the whole right subtree. "
                                + "Distinct values.")
                        .length(1, 12).values(1, 99).distinct()
                        .defaultValue(Arrays.asList(8, 5, 1, 7, 10, 12))
                        .build());
    }

    /** A strictly increasing preorder: every value exceeds its parent, so no left child is ever created and the tree degenerates into a chain. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("preorder", Arrays.asList(1, 2, 3, 4, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               private int i = 0;

               public TreeNode bstFromPreorder(int[] preorder) {
                   TreeNode root = build(preorder, Integer.MAX_VALUE);
                   // @a done
                   return root;
               }

               private TreeNode build(int[] pre, int bound) {
                   if (i == pre.length || pre[i] > bound) {
                       // @a outOfBound
                       return null;          // this subtree is finished
                   }
                   // @a takeValue
                   TreeNode node = new TreeNode(pre[i++]);

                   // @a buildLeft
                   node.left = build(pre, node.val);   // must stay below this node
                   // @a buildRight
                   node.right = build(pre, bound);     // inherits the caller's bound
                   return node;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] preorder = in.getIntArray("preorder");
        MutableBst builder = new MutableBst();
        Map<MutableBst.Node, String> states = new IdentityHashMap<>();
        MutableBst.Node[] root = new MutableBst.Node[1];
        int[] i = {0};

        root[0] = build(builder, preorder, i, NO_BOUND, states, root, emit);

        for (MutableBst.Node node : new java.util.ArrayList<>(states.keySet())) {
            states.put(node, "done");
        }
        emit.at("done")
                .say("All %d value%s consumed, each exactly once. Preorder alone was enough "
                        + "because the BST property already told us the inorder.",
                        preorder.length, preorder.length == 1 ? "" : "s")
                .var("nodes", preorder.length)
                .tree(builder.render(root[0], states)).step();
    }

    private MutableBst.Node build(MutableBst builder, int[] pre, int[] i, int bound,
                                  Map<MutableBst.Node, String> states,
                                  MutableBst.Node[] root, StepEmitter emit) {
        if (i[0] == pre.length) {
            emit.at("outOfBound")
                    .say("Preorder is exhausted, so there is nothing left to place here.")
                    .var("index", i[0]).var("bound", describe(bound))
                    .tree(builder.render(root[0], states)).step();
            return null;
        }
        if (pre[i[0]] > bound) {
            emit.at("outOfBound")
                    .say("The next value is %d, which exceeds the bound %d - so it cannot belong "
                                    + "in this subtree at all. It belongs to an ancestor; this "
                                    + "branch ends empty.",
                            pre[i[0]], bound)
                    .var("index", i[0]).var("bound", describe(bound))
                    .tree(builder.render(root[0], states)).step();
            return null;
        }

        int value = pre[i[0]++];
        MutableBst.Node node = builder.newNode(value);
        if (root[0] == null) {
            root[0] = node;
        }
        states.put(node, "target");
        emit.at("takeValue")
                .say("%d is within the bound %s, so it is this subtree's root. The index moves "
                        + "on and never comes back.", value, describe(bound))
                .var("index", i[0]).var("bound", describe(bound)).var("node", value)
                .tree(builder.render(root[0], states)).step();

        states.put(node, "visiting");
        emit.at("buildLeft")
                .say("Build %d's LEFT subtree with a tightened bound of %d: every value down "
                        + "there has to be smaller than %d.", value, value, value)
                .var("index", i[0]).var("bound", describe(value))
                .tree(builder.render(root[0], states)).step();
        node.left = build(builder, pre, i, value, states, root, emit);

        emit.at("buildRight")
                .say("Now %d's RIGHT subtree, which inherits the caller's bound %s unchanged - "
                                + "values there must exceed %d but are otherwise limited only by "
                                + "the ancestors above.",
                        value, describe(bound), value)
                .var("index", i[0]).var("bound", describe(bound))
                .tree(builder.render(root[0], states)).step();
        node.right = build(builder, pre, i, bound, states, root, emit);

        states.put(node, "visited");
        return node;
    }

    private String describe(int bound) {
        return bound == NO_BOUND ? "+inf" : String.valueOf(bound);
    }
}
