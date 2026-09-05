package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every call explores both children unconditionally, then looks at what came back. Finding
 * a target short-circuits its own subtree - if p is an ancestor of q, p never needs to look
 * below itself for q to be a correct answer. A node only becomes the LCA when both of its
 * subtrees separately report a target: that is the exact point their paths diverge.
 */
@Component
public class TreeLcaTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "tree-lca";
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
                        .defaultValue(Arrays.asList(3, 5, 1, 6, 2, 0, 8, null, null, 7, 4))
                        .build(),
                InputField.of("p", FieldType.INT)
                        .label("First target value")
                        .range(-99, 99)
                        .defaultValue(7)
                        .build(),
                InputField.of("q", FieldType.INT)
                        .label("Second target value")
                        .range(-99, 99)
                        .defaultValue(4)
                        .build());
    }

    /** Same tree, but q now lives inside p's own subtree - the answer is p itself, not an ancestor. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("p", 5, "q", 4);
    }

    @Override
    public String annotatedCode() {
        return """
               public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
                   TreeNode result = lca(root, p, q);
                   // @a done
                   return result;
               }

               private TreeNode lca(TreeNode root, TreeNode p, TreeNode q) {
                   if (root == null || root.val == p.val || root.val == q.val) {
                       // @a foundOrNull
                       return root;
                   }
                   TreeNode left = lca(root.left, p, q);
                   TreeNode right = lca(root.right, p, q);
                   if (left != null && right != null) {
                       // @a bothSides
                       return root;
                   }
                   // @a oneSide
                   return left != null ? left : right;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        int p = in.getInt("p");
        int q = in.getInt("q");
        Map<Integer, String> states = new LinkedHashMap<>();

        Integer resultIdx = tree.isEmpty() ? null : lca(tree, tree.root(), p, q, states, emit);

        emit.at("done")
                .say(resultIdx == null
                        ? String.format("Searched the whole tree - no ancestor found for %d and %d.", p, q)
                        : String.format("Lowest common ancestor of %d and %d is %d.", p, q, tree.value(resultIdx)))
                .var("answer", resultIdx == null ? "null" : tree.value(resultIdx))
                .tree(tree.render(states)).step();
    }

    private Integer lca(BinaryTreeLayout tree, Integer index, int p, int q,
                         Map<Integer, String> states, StepEmitter emit) {
        if (index == null) {
            emit.at("foundOrNull")
                    .say("Empty branch - nothing here to be an ancestor of anything.")
                    .var("returned", "null").tree(tree.render(states)).step();
            return null;
        }

        emit.push("lca(" + tree.value(index) + ")");
        states.put(index, "visiting");

        if (tree.value(index) == p || tree.value(index) == q) {
            emit.at("foundOrNull")
                    .say("%d matches one of the targets - stop descending here and report it upward.",
                            tree.value(index))
                    .var("returned", tree.value(index)).tree(tree.render(states)).step();
            emit.pop();
            return index;
        }

        Integer leftResult = lca(tree, tree.left(index), p, q, states, emit);
        Integer rightResult = lca(tree, tree.right(index), p, q, states, emit);

        Integer result;
        if (leftResult != null && rightResult != null) {
            result = index;
            emit.at("bothSides")
                    .say("Both subtrees of %d reported a target - %d is exactly where their paths split, the LCA.",
                            tree.value(index), tree.value(index))
                    .var("returned", tree.value(index)).tree(tree.render(states)).step();
        } else {
            result = leftResult != null ? leftResult : rightResult;
            if (result != null) {
                emit.at("oneSide")
                        .say("Only one side of %d found anything - pass %d straight up unchanged.",
                                tree.value(index), tree.value(result))
                        .var("returned", tree.value(result)).tree(tree.render(states)).step();
            } else {
                emit.at("oneSide")
                        .say("Neither side of %d found a target - pass null up.", tree.value(index))
                        .var("returned", "null").tree(tree.render(states)).step();
            }
        }

        states.put(index, "visited");
        emit.pop();
        return result;
    }
}
