package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why inorder is the traversal you cannot do without, demonstrated on the tree in front of
 * you rather than asserted.
 *
 * <p>Preorder and postorder between them pin down every node's subtree boundaries EXCEPT at
 * a node with exactly one child: preorder says "root, then the child", postorder says "the
 * child, then root", and both sentences read identically whether that child hangs left or
 * right. Each such node therefore doubles the number of distinct trees sharing the same
 * (preorder, postorder) pair, so the count is 2 raised to the number of one-child nodes.
 *
 * <p>Inorder is exactly what settles it: it places a left child BEFORE its parent and a
 * right child AFTER it, so a single inorder position collapses each of those factors of two
 * to one. This walks the tree, classifies every node, and reports both counts.
 */
@Component
public class UniqueBtRequirementsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "unique-bt-requirements";
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
                        .defaultValue(Arrays.asList(1, 2, 3, 4, null, null, 7))
                        .build());
    }

    /** A perfect tree: no node has exactly one child, so preorder and postorder alone already pin it down. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, 2, 3, 4, 5, 6, 7));
    }

    @Override
    public String annotatedCode() {
        return """
               public long treesSharingPreAndPost(TreeNode root) {
                   int ambiguous = classify(root);
                   // @a countThem
                   long trees = 1L << ambiguous;
                   // @a done
                   return trees;      // 1 means preorder + postorder already suffice
               }

               private int classify(TreeNode node) {
                   if (node == null) return 0;
                   int ambiguous = classify(node.left) + classify(node.right);

                   if (node.left == null && node.right == null) {
                       // @a leafIsPinned
                       return ambiguous;               // nothing below to place
                   }
                   if (node.left != null && node.right != null) {
                       // @a twoChildrenArePinned
                       return ambiguous;               // preorder splits them, postorder joins them
                   }
                   // @a oneChildIsAmbiguous
                   return ambiguous + 1;               // left or right? pre+post cannot say
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();

        if (tree.isEmpty()) {
            emit.at("done")
                    .say("An empty tree needs no traversal at all to identify it.")
                    .var("answer", 1).tree(tree.render(states)).step();
            return;
        }

        int ambiguous = classify(tree, tree.root(), states, emit);
        long trees = 1L << ambiguous;

        emit.at("countThem")
                .say("%d node%s with exactly one child, each of which could hang on either "
                                + "side: 2^%d = %d different trees all produce this same "
                                + "preorder and postorder.",
                        ambiguous, ambiguous == 1 ? "" : "s", ambiguous, trees)
                .var("oneChildNodes", ambiguous).var("treesSharingPreAndPost", trees)
                .tree(tree.render(states)).step();

        if (ambiguous == 0) {
            emit.at("done")
                    .say("Exactly one tree matches, so for THIS shape preorder and postorder "
                            + "happen to be enough. That is a property of the shape, not a rule "
                            + "- inorder plus either one is what works for every shape.")
                    .var("answer", trees).tree(tree.render(states)).step();
        } else {
            emit.at("done")
                    .say("Inorder settles every one of those %d choices at once: a left child "
                                    + "appears BEFORE its parent in inorder and a right child "
                                    + "AFTER it. Inorder plus preorder, or inorder plus "
                                    + "postorder, identifies the tree uniquely; preorder plus "
                                    + "postorder does not.",
                            ambiguous)
                    .var("answer", trees).tree(tree.render(states)).step();
        }
    }

    private int classify(BinaryTreeLayout tree, int index, Map<Integer, String> states,
                         StepEmitter emit) {
        Integer left = tree.left(index);
        Integer right = tree.right(index);

        int ambiguous = 0;
        if (left != null) {
            ambiguous += classify(tree, left, states, emit);
        }
        if (right != null) {
            ambiguous += classify(tree, right, states, emit);
        }

        if (left == null && right == null) {
            states.put(index, "visited");
            emit.at("leafIsPinned")
                    .say("%d is a leaf. There is nothing below it to place, so it adds no "
                            + "ambiguity.", tree.value(index))
                    .var("node", tree.value(index)).var("oneChildNodes", ambiguous)
                    .tree(tree.render(states)).step();
            return ambiguous;
        }
        if (left != null && right != null) {
            states.put(index, "visited");
            emit.at("twoChildrenArePinned")
                    .say("%d has both children. Preorder lists %d's subtree before %d's and "
                                    + "postorder ends them in the same order, so the boundary "
                                    + "between the two is visible without inorder.",
                            tree.value(index), tree.value(left), tree.value(right))
                    .var("node", tree.value(index)).var("oneChildNodes", ambiguous)
                    .tree(tree.render(states)).step();
            return ambiguous;
        }

        Integer only = left != null ? left : right;
        states.put(index, "target");
        emit.at("oneChildIsAmbiguous")
                .say("%d has exactly one child, %d. Preorder reads \"%d then %d\" and postorder "
                                + "reads \"%d then %d\" whether %d is a LEFT or a RIGHT child - "
                                + "the two traversals cannot tell those trees apart.",
                        tree.value(index), tree.value(only), tree.value(index), tree.value(only),
                        tree.value(only), tree.value(index), tree.value(only))
                .var("node", tree.value(index)).var("oneChildNodes", ambiguous + 1)
                .tree(tree.render(states)).step();
        return ambiguous + 1;
    }
}
