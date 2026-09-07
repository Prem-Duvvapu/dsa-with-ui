package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Rebuilds a binary tree from its postorder and inorder traversals - LeetCode 106 - and it
 * is the mirror image of LeetCode 105 in two places at once, both of which have to flip
 * together.
 *
 * <p>Postorder puts the root LAST, not first, so the root is read from the END of the range
 * and consumed backwards. And because the values immediately before the root in postorder
 * are its RIGHT subtree, the right side must be built before the left - taking the left
 * first would slice the wrong block out of postorder even though inorder's split point is
 * identical. Getting one of the two flips right and not the other is the classic way this
 * problem goes wrong, so the trace names both on every node.
 *
 * <p>The defaults are LeetCode 106's published example, which happens to describe the very
 * same tree as LeetCode 105's - so the two tracers can be watched side by side reaching the
 * same answer from opposite ends of their arrays.
 */
@Component
public class ConstructBtPostInTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "construct-bt-post-in";
    }

    @Override
    public DsType dsType() {
        return DsType.TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("postorder", FieldType.INT_ARRAY)
                        .label("Postorder")
                        .help("Whole left subtree, then the whole right subtree, then the root.")
                        .length(1, 12).values(1, 99).distinct()
                        .defaultValue(Arrays.asList(9, 15, 7, 20, 3))
                        .build(),
                InputField.of("inorder", FieldType.INT_ARRAY)
                        .label("Inorder")
                        .help("Left subtree, then the root, then the right subtree. Must contain "
                                + "exactly the same values as postorder.")
                        .length(1, 12).values(1, 99).distinct()
                        .defaultValue(Arrays.asList(9, 3, 15, 20, 7))
                        .build());
    }

    /** A perfect seven-node tree, so every split has a real subtree on both sides of the root. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "postorder", Arrays.asList(4, 5, 2, 6, 7, 3, 1),
                "inorder", Arrays.asList(4, 2, 5, 1, 6, 3, 7));
    }

    @Override
    public String annotatedCode() {
        return """
               public TreeNode buildTree(int[] inorder, int[] postorder) {
                   Map<Integer, Integer> where = new HashMap<>();
                   for (int i = 0; i < inorder.length; i++) where.put(inorder[i], i);
                   TreeNode root = build(postorder, 0, postorder.length - 1,
                                         inorder, 0, inorder.length - 1, where);
                   // @a done
                   return root;
               }

               private TreeNode build(int[] post, int postLo, int postHi,
                                      int[] in, int inLo, int inHi,
                                      Map<Integer, Integer> where) {
                   if (postLo > postHi) {
                       // @a emptyRange
                       return null;
                   }
                   // @a rootIsLastInPostorder
                   TreeNode node = new TreeNode(post[postHi]);

                   int at = where.get(post[postHi]);
                   int rightSize = inHi - at;
                   // @a inorderGivesTheSizes
                   node.right = build(post, postHi - rightSize, postHi - 1,
                                      in, at + 1, inHi, where);
                   // @a leftIsWhatRemains
                   node.left = build(post, postLo, postHi - rightSize - 1,
                                     in, inLo, at - 1, where);
                   return node;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] post = in.getIntArray("postorder");
        int[] inorder = in.getIntArray("inorder");

        if (post.length != inorder.length
                || !new java.util.HashSet<>(boxed(post)).equals(new java.util.HashSet<>(boxed(inorder)))) {
            emit.at("done")
                    .say("These two arrays cannot come from the same tree: postorder holds %s "
                                    + "and inorder holds %s. Reconstruction is only defined for "
                                    + "two orderings of the SAME node set.",
                            Arrays.toString(post), Arrays.toString(inorder))
                    .var("postorder", Arrays.toString(post))
                    .var("inorder", Arrays.toString(inorder))
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

        root[0] = build(builder, post, 0, post.length - 1, inorder, 0, inorder.length - 1,
                where, states, root, emit);

        for (MutableBst.Node node : new java.util.ArrayList<>(states.keySet())) {
            states.put(node, "done");
        }
        emit.at("done")
                .say("Postorder has been consumed from its last element to its first, and the "
                        + "tree is reconstructed.")
                .var("nodes", post.length)
                .tree(builder.render(root[0], states)).step();
    }

    private MutableBst.Node build(MutableBst builder, int[] post, int postLo, int postHi,
                                  int[] inorder, int inLo, int inHi,
                                  Map<Integer, Integer> where,
                                  Map<MutableBst.Node, String> states,
                                  MutableBst.Node[] root, StepEmitter emit) {
        if (postLo > postHi) {
            emit.at("emptyRange")
                    .say("The postorder range is empty, so this branch has no node at all.")
                    .var("postRange", "empty")
                    .tree(builder.render(root[0], states)).step();
            return null;
        }

        int value = post[postHi];
        MutableBst.Node node = builder.newNode(value);
        if (root[0] == null) {
            root[0] = node;
        }
        states.put(node, "target");
        emit.at("rootIsLastInPostorder")
                .say("Postorder %s ENDS with %d, so %d is this subtree's root. Preorder would "
                                + "have put it first; here the range is consumed from the right.",
                        slice(post, postLo, postHi), value, value)
                .var("postRange", slice(post, postLo, postHi))
                .var("inRange", slice(inorder, inLo, inHi))
                .var("root", value)
                .tree(builder.render(root[0], states)).step();

        int at = where.get(value);
        int rightSize = at == -1 ? 0 : inHi - at;
        int leftSize = at - inLo;
        states.put(node, "visiting");
        emit.at("inorderGivesTheSizes")
                .say("%d sits at inorder position %d: %d value%s on its right. In postorder "
                                + "those occupy the %d slot%s immediately BEFORE the root, so "
                                + "the right subtree has to be built first.",
                        value, at, rightSize, rightSize == 1 ? "" : "s",
                        rightSize, rightSize == 1 ? "" : "s")
                .var("leftSize", leftSize).var("rightSize", rightSize)
                .var("rightPostorder", slice(post, postHi - rightSize, postHi - 1))
                .var("rightInorder", slice(inorder, at + 1, inHi))
                .tree(builder.render(root[0], states)).step();

        node.right = build(builder, post, postHi - rightSize, postHi - 1,
                inorder, at + 1, inHi, where, states, root, emit);

        emit.at("leftIsWhatRemains")
                .say("%d's right subtree is placed. Everything still left in postorder, %s, is "
                                + "its left subtree - only now is it safe to slice.",
                        value, slice(post, postLo, postHi - rightSize - 1))
                .var("leftSize", leftSize)
                .var("leftPostorder", slice(post, postLo, postHi - rightSize - 1))
                .var("leftInorder", slice(inorder, inLo, at - 1))
                .tree(builder.render(root[0], states)).step();

        node.left = build(builder, post, postLo, postHi - rightSize - 1,
                inorder, inLo, at - 1, where, states, root, emit);

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
