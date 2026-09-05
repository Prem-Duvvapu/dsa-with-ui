package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Post-order DFS that returns one thing to its parent - the best downward path starting at
 * this node - while separately tracking a global best that a parent never sees: the best
 * path THROUGH this node, using both children at once. A path can only bend once, at its
 * highest node, so "bends here" and "continues upward" have to be tracked independently.
 */
@Component
public class TreeMaxPathSumTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "tree-max-path-sum";
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
                        .help("Level order, with null where a child is absent. Values may be negative.")
                        .length(1, 31).values(-99, 99)
                        .defaultValue(Arrays.asList(1, 2, 3))
                        .build());
    }

    /** Negative root: the best path skips it entirely and lives in the right subtree alone. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(-10, 9, 20, null, null, 15, 7));
    }

    @Override
    public String annotatedCode() {
        return """
               public int maxPathSum(TreeNode root) {
                   int[] best = {Integer.MIN_VALUE};
                   maxGain(root, best);
                   // @a done
                   return best[0];
               }

               private int maxGain(TreeNode node, int[] best) {
                   if (node == null) {
                       // @a base
                       return 0;
                   }
                   int fromLeft = Math.max(0, maxGain(node.left, best));
                   int fromRight = Math.max(0, maxGain(node.right, best));
                   // @a throughHere
                   int through = node.val + fromLeft + fromRight;
                   if (through > best[0]) {
                       // @a newBest
                       best[0] = through;
                   }
                   // @a returnUp
                   return node.val + Math.max(fromLeft, fromRight);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));

        if (tree.isEmpty()) {
            emit.at("base").say("The tree is empty, so there is no path at all.")
                    .var("best", "undefined").tree(tree.render(Map.of())).step();
            return;
        }

        Map<Integer, String> states = new LinkedHashMap<>();
        int[] best = {Integer.MIN_VALUE};
        maxGain(tree, tree.root(), states, best, emit);

        emit.at("done")
                .say("Every node considered as the highest point of some path. Maximum path sum: %d.", best[0])
                .var("answer", best[0]).tree(tree.render(states)).step();
    }

    private int maxGain(BinaryTreeLayout tree, Integer index, Map<Integer, String> states,
                       int[] best, StepEmitter emit) {
        if (index == null) {
            emit.at("base").say("Null child - contributes 0 and nothing to explore.")
                    .var("returned", 0).tree(tree.render(states)).step();
            return 0;
        }

        emit.push("maxGain(" + tree.value(index) + ")");
        states.put(index, "visiting");

        Integer leftIdx = tree.left(index);
        int fromLeft = Math.max(0, maxGain(tree, leftIdx, states, best, emit));
        Integer rightIdx = tree.right(index);
        int fromRight = Math.max(0, maxGain(tree, rightIdx, states, best, emit));

        int through = tree.value(index) + fromLeft + fromRight;
        emit.at("throughHere")
                .say("At %d: best downward from left (clamped to 0) is %d, from right is %d. "
                        + "Bending the path here would total %d + %d + %d = %d.",
                        tree.value(index), fromLeft, fromRight, tree.value(index), fromLeft,
                        fromRight, through)
                .var("node", tree.value(index)).var("fromLeft", fromLeft)
                .var("fromRight", fromRight).var("through", through)
                .tree(tree.render(states)).step();

        if (through > best[0]) {
            best[0] = through;
            emit.at("newBest")
                    .say("%d beats the best path seen so far - new maximum %d.", through, best[0])
                    .var("best", best[0])
                    .tree(tree.render(states)).step();
        }

        int upward = tree.value(index) + Math.max(fromLeft, fromRight);
        states.put(index, "visited");
        emit.at("returnUp")
                .say("%d cannot offer both children to its own parent - only one path may "
                        + "continue upward. Hand back %d + max(%d, %d) = %d.",
                        tree.value(index), tree.value(index), fromLeft, fromRight, upward)
                .var("returned", upward)
                .tree(tree.render(states)).step();
        emit.pop();
        return upward;
    }
}
