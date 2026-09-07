package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Height again as the subroutine, but the answer is a running maximum kept OUTSIDE the
 * recursion - and the two numbers are deliberately different.
 *
 * <p>At every node, the longest path that bends through that node is leftHeight +
 * rightHeight; that candidate is offered to a running best. What the node RETURNS to its
 * parent is something else entirely: 1 + max(leftHeight, rightHeight), because a path that
 * continues upward can only use one of the two sides. Confusing those two expressions is
 * the classic wrong answer here, so the trace shows both on every node.
 *
 * <p>The diameter need not pass through the root, which is exactly why a running maximum is
 * needed rather than a single expression evaluated at the top.
 */
@Component
public class TreeDiameterTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "tree-diameter";
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
                        .defaultValue(Arrays.asList(1, 2, 3, 4, 5))
                        .build());
    }

    /** A deeper, lopsided tree whose best path runs through the root but starts two levels further down. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, 2, 3, 4, 5, null, null, 6, 7));
    }

    @Override
    public String annotatedCode() {
        return """
               private int best = 0;

               public int diameterOfBinaryTree(TreeNode root) {
                   height(root);
                   // @a done
                   return best;
               }

               private int height(TreeNode node) {
                   if (node == null) return 0;
                   int left = height(node.left);
                   int right = height(node.right);

                   if (left + right > best) {
                       // @a newBest
                       best = left + right;      // the path that BENDS here
                   } else {
                       // @a keepBest
                       ;                         // a longer bend was found elsewhere
                   }
                   // @a heightUpward
                   return 1 + Math.max(left, right);   // the path that CONTINUES upward
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();
        int[] best = {0};
        int[] bestAt = {-1};

        if (!tree.isEmpty()) {
            height(tree, tree.root(), states, best, bestAt, emit);
            if (bestAt[0] >= 0) {
                states.put(bestAt[0], "target");
            }
        }

        emit.at("done")
                .say("Every node offered its bend to the running best. The diameter is %d "
                        + "edge%s.", best[0], best[0] == 1 ? "" : "s")
                .var("answer", best[0])
                .tree(tree.render(states)).step();
    }

    private int height(BinaryTreeLayout tree, int index, Map<Integer, String> states,
                       int[] best, int[] bestAt, StepEmitter emit) {
        emit.push("height(" + tree.value(index) + ")");
        states.put(index, "visiting");

        Integer left = tree.left(index);
        Integer right = tree.right(index);
        int leftHeight = left == null ? 0 : height(tree, left, states, best, bestAt, emit);
        int rightHeight = right == null ? 0 : height(tree, right, states, best, bestAt, emit);

        int bend = leftHeight + rightHeight;
        if (bend > best[0]) {
            best[0] = bend;
            bestAt[0] = index;
            emit.at("newBest")
                    .say("A path bending through %d spans %d + %d = %d edges - longer than "
                                    + "anything seen so far. New best.",
                            tree.value(index), leftHeight, rightHeight, bend)
                    .var("bendHere", bend).var("best", best[0])
                    .tree(tree.render(states)).step();
        } else {
            emit.at("keepBest")
                    .say("A path bending through %d spans only %d + %d = %d edges, no better "
                                    + "than the best of %d already found elsewhere.",
                            tree.value(index), leftHeight, rightHeight, bend, best[0])
                    .var("bendHere", bend).var("best", best[0])
                    .tree(tree.render(states)).step();
        }

        int height = 1 + Math.max(leftHeight, rightHeight);
        states.put(index, "visited");
        emit.at("heightUpward")
                .say("What %d hands its parent is different: a path continuing upward can use "
                                + "only one side, so it returns 1 + max(%d, %d) = %d, not %d.",
                        tree.value(index), leftHeight, rightHeight, height, bend)
                .var("bendHere", bend).var("best", best[0]).var("returned", height)
                .tree(tree.render(states)).step();

        emit.pop();
        return height;
    }
}
