package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rebuilds a binary tree from its preorder and inorder traversals - LeetCode 105.
 *
 * <p>Preorder's first element is the root, always: that is what preorder means. Inorder then
 * says how big each side is, because everything before the root's position in inorder is the
 * left subtree and everything after it is the right. Those two facts alone reconstruct the
 * tree, and each recursive call gets the same two facts about a smaller pair of ranges.
 *
 * <p>The one thing worth building an index map for is finding the root inside inorder; done
 * by scanning, the whole algorithm slips from O(n) to O(n^2) on a skewed tree.
 *
 * <p>The input is two arrays rather than a tree, because the tree is the OUTPUT here. It is
 * drawn as it is assembled, one node per step.
 */
@Component
public class ConstructBtPreInTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "construct-bt-pre-in";
    }

    @Override
    public DsType dsType() {
        return DsType.TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("preorder", FieldType.INT_ARRAY)
                        .label("Preorder")
                        .help("Root, then the whole left subtree, then the whole right subtree.")
                        .length(1, 12).values(1, 99).distinct()
                        .defaultValue(Arrays.asList(3, 9, 20, 15, 7))
                        .build(),
                InputField.of("inorder", FieldType.INT_ARRAY)
                        .label("Inorder")
                        .help("Left subtree, then the root, then the right subtree. Must contain "
                                + "exactly the same values as preorder.")
                        .length(1, 12).values(1, 99).distinct()
                        .defaultValue(Arrays.asList(9, 3, 15, 20, 7))
                        .build());
    }

    /** A perfect seven-node tree, where every split has a non-empty subtree on both sides. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "preorder", Arrays.asList(1, 2, 4, 5, 3, 6, 7),
                "inorder", Arrays.asList(4, 2, 5, 1, 6, 3, 7));
    }

    @Override
    public String annotatedCode() {
        return """
               public TreeNode buildTree(int[] preorder, int[] inorder) {
                   Map<Integer, Integer> where = new HashMap<>();
                   for (int i = 0; i < inorder.length; i++) where.put(inorder[i], i);
                   TreeNode root = build(preorder, 0, preorder.length - 1,
                                         inorder, 0, inorder.length - 1, where);
                   // @a done
                   return root;
               }

               private TreeNode build(int[] pre, int preLo, int preHi,
                                      int[] in, int inLo, int inHi,
                                      Map<Integer, Integer> where) {
                   if (preLo > preHi) {
                       // @a emptyRange
                       return null;
                   }
                   // @a rootIsFirstInPreorder
                   TreeNode node = new TreeNode(pre[preLo]);

                   int at = where.get(pre[preLo]);
                   int leftSize = at - inLo;
                   // @a inorderGivesTheSizes
                   node.left = build(pre, preLo + 1, preLo + leftSize,
                                     in, inLo, at - 1, where);
                   node.right = build(pre, preLo + leftSize + 1, preHi,
                                      in, at + 1, inHi, where);
                   return node;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] pre = in.getIntArray("preorder");
        int[] inorder = in.getIntArray("inorder");

        if (pre.length != inorder.length
                || !new java.util.HashSet<>(boxed(pre)).equals(new java.util.HashSet<>(boxed(inorder)))) {
            emit.at("done")
                    .say("These two arrays cannot come from the same tree: preorder holds %s "
                                    + "and inorder holds %s. Reconstruction is only defined for "
                                    + "two orderings of the SAME node set.",
                            Arrays.toString(pre), Arrays.toString(inorder))
                    .var("preorder", Arrays.toString(pre)).var("inorder", Arrays.toString(inorder))
                    .tree(java.util.List.of()).step();
            return;
        }

        Map<Integer, Integer> where = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) {
            where.put(inorder[i], i);
        }

        MutableBst builder = new MutableBst();
        Map<MutableBst.Node, String> states = new IdentityHashMap<>();
        MutableBst.Node[] root = new MutableBst.Node[1];

        root[0] = build(builder, pre, 0, pre.length - 1, inorder, 0, inorder.length - 1,
                where, states, root, emit);

        for (MutableBst.Node node : new java.util.ArrayList<>(states.keySet())) {
            states.put(node, "done");
        }
        emit.at("done")
                .say("Every value in preorder has been placed. The tree is reconstructed.")
                .var("nodes", pre.length)
                .tree(builder.render(root[0], states)).step();
    }

    private MutableBst.Node build(MutableBst builder, int[] pre, int preLo, int preHi,
                                  int[] inorder, int inLo, int inHi,
                                  Map<Integer, Integer> where,
                                  Map<MutableBst.Node, String> states,
                                  MutableBst.Node[] root, StepEmitter emit) {
        if (preLo > preHi) {
            emit.at("emptyRange")
                    .say("The preorder range is empty, so this branch has no node at all.")
                    .var("preRange", "empty")
                    .tree(builder.render(root[0], states)).step();
            return null;
        }

        int value = pre[preLo];
        MutableBst.Node node = builder.newNode(value);
        if (root[0] == null) {
            root[0] = node;
        }
        states.put(node, "target");
        emit.at("rootIsFirstInPreorder")
                .say("Preorder %s starts with %d, so %d is the root of this subtree - that is "
                                + "all preorder ever tells you, and it is enough.",
                        slice(pre, preLo, preHi), value, value)
                .var("preRange", slice(pre, preLo, preHi))
                .var("inRange", slice(inorder, inLo, inHi))
                .var("root", value)
                .tree(builder.render(root[0], states)).step();

        int at = where.get(value);
        int leftSize = at - inLo;
        int rightSize = inHi - at;
        states.put(node, "visiting");
        emit.at("inorderGivesTheSizes")
                .say("%d sits at inorder position %d: %d value%s on its left, %d on its right. "
                                + "That splits preorder's remainder into exactly those two "
                                + "blocks.",
                        value, at, leftSize, leftSize == 1 ? "" : "s", rightSize)
                .var("leftSize", leftSize).var("rightSize", rightSize)
                .var("leftInorder", slice(inorder, inLo, at - 1))
                .var("rightInorder", slice(inorder, at + 1, inHi))
                .tree(builder.render(root[0], states)).step();

        node.left = build(builder, pre, preLo + 1, preLo + leftSize,
                inorder, inLo, at - 1, where, states, root, emit);
        node.right = build(builder, pre, preLo + leftSize + 1, preHi,
                inorder, at + 1, inHi, where, states, root, emit);

        states.put(node, "visited");
        return node;
    }

    private String slice(int[] values, int lo, int hi) {
        if (lo > hi) {
            return "[]";
        }
        return Arrays.toString(Arrays.copyOfRange(values, lo, hi + 1));
    }

    private java.util.List<Integer> boxed(int[] values) {
        java.util.List<Integer> out = new java.util.ArrayList<>(values.length);
        for (int value : values) {
            out.add(value);
        }
        return out;
    }
}
