package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The plainest possible statement of height, and the subroutine three other problems in
 * this category are built on: an empty branch is 0 tall, and everything else is one level
 * taller than its taller subtree.
 *
 * <p>The whole answer is assembled on the way back UP. Nothing is known on the way down -
 * the descent only exists to reach the empty branches, which are the only places a real
 * number is ever produced from nothing.
 */
@Component
public class TreeHeightTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "tree-height";
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
                        .defaultValue(Arrays.asList(3, 9, 20, null, null, 15, 7))
                        .build());
    }

    /** A single left-leaning chain, where every node has exactly one child and the height equals the node count. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(8, 6, null, 4, null, null, null, 2));
    }

    @Override
    public String annotatedCode() {
        return """
               public int maxDepth(TreeNode root) {
                   int h = height(root);
                   // @a done
                   return h;
               }

               private int height(TreeNode node) {
                   if (node == null) {
                       // @a emptyIsZero
                       return 0;
                   }
                   // @a descendLeft
                   int left = height(node.left);
                   // @a descendRight
                   int right = height(node.right);
                   // @a oneAboveTaller
                   return 1 + Math.max(left, right);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();

        int height = tree.isEmpty() ? 0 : height(tree, tree.root(), states, emit);

        emit.at("done")
                .say("The tree's height is %d.", height)
                .var("answer", height)
                .tree(tree.render(states)).step();
    }

    private int height(BinaryTreeLayout tree, int index, Map<Integer, String> states,
                       StepEmitter emit) {
        emit.push("height(" + tree.value(index) + ")");
        states.put(index, "visiting");

        Integer left = tree.left(index);
        if (left == null) {
            emit.at("emptyIsZero")
                    .say("%d's left branch is empty. An empty branch has height 0 - this is the "
                            + "only place a number is created rather than combined.",
                            tree.value(index))
                    .var("returned", 0).tree(tree.render(states)).step();
        } else {
            emit.at("descendLeft")
                    .say("Descend into %d's left child %d. Nothing can be decided about %d until "
                                    + "this call comes back.",
                            tree.value(index), tree.value(left), tree.value(index))
                    .var("node", tree.value(index)).tree(tree.render(states)).step();
        }
        int leftHeight = left == null ? 0 : height(tree, left, states, emit);

        Integer right = tree.right(index);
        if (right == null) {
            emit.at("emptyIsZero")
                    .say("%d's right branch is empty, so it contributes height 0.",
                            tree.value(index))
                    .var("returned", 0).tree(tree.render(states)).step();
        } else {
            emit.at("descendRight")
                    .say("%d's left side reported height %d. Now descend into its right child %d.",
                            tree.value(index), leftHeight, tree.value(right))
                    .var("leftHeight", leftHeight).tree(tree.render(states)).step();
        }
        int rightHeight = right == null ? 0 : height(tree, right, states, emit);

        int result = 1 + Math.max(leftHeight, rightHeight);
        states.put(index, "visited");
        emit.at("oneAboveTaller")
                .say("Both sides of %d are in: %d on the left, %d on the right. %d is one level "
                                + "taller than its taller side, so it returns %d.",
                        tree.value(index), leftHeight, rightHeight, tree.value(index), result)
                .var("leftHeight", leftHeight).var("rightHeight", rightHeight)
                .var("returned", result)
                .tree(tree.render(states)).step();

        emit.pop();
        return result;
    }
}
