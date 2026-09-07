package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Both side views out of one reverse-preorder descent, keyed on DEPTH rather than on the
 * horizontal distance the top and bottom views use.
 *
 * <p>Visiting root, then right, then left means that within any one depth the nodes are met
 * strictly right to left. So the FIRST node recorded at a depth is that level's rightmost -
 * the right view - and the LAST node to overwrite that depth is its leftmost - the left
 * view. Two answers, one walk, no second traversal and no mirrored copy of the code.
 */
@Component
public class RightLeftViewTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "right-left-view-bt";
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
                        .defaultValue(Arrays.asList(1, 2, 3, null, 5, null, 4))
                        .build());
    }

    /** A left-only chain: each level holds exactly one node, so the right view and the left view come out identical. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, 2, null, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public Views sideViews(TreeNode root) {
                   Map<Integer, Integer> rightView = new TreeMap<>();
                   Map<Integer, Integer> leftView = new TreeMap<>();
                   dfs(root, 0, rightView, leftView);
                   // @a done
                   return new Views(rightView.values(), leftView.values());
               }

               private void dfs(TreeNode node, int depth,
                                Map<Integer, Integer> rightView,
                                Map<Integer, Integer> leftView) {
                   if (node == null) return;

                   if (!rightView.containsKey(depth)) {
                       // @a firstAtDepth
                       rightView.put(depth, node.val);   // rightmost: met first
                   }
                   // @a overwriteLeft
                   leftView.put(depth, node.val);        // leftmost: the last writer wins

                   if (node.right != null) {
                       // @a goRightFirst
                       dfs(node.right, depth + 1, rightView, leftView);
                   }
                   if (node.left != null) {
                       // @a goLeftSecond
                       dfs(node.left, depth + 1, rightView, leftView);
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();
        Map<Integer, Integer> rightView = new TreeMap<>();
        Map<Integer, Integer> leftView = new TreeMap<>();

        if (tree.isEmpty()) {
            emit.at("done").say("The tree is empty, so both side views are empty.")
                    .var("rightView", "[]").var("leftView", "[]")
                    .tree(tree.render(states)).step();
            return;
        }

        dfs(tree, tree.root(), 0, rightView, leftView, states, emit);

        List<Integer> right = new ArrayList<>(rightView.values());
        List<Integer> left = new ArrayList<>(leftView.values());
        emit.at("done")
                .say("Right view (first seen at each depth) is %s; left view (last seen at each "
                        + "depth) is %s.", right, left)
                .var("rightView", right).var("leftView", left)
                .tree(tree.render(states)).step();
    }

    private void dfs(BinaryTreeLayout tree, int index, int depth,
                     Map<Integer, Integer> rightView, Map<Integer, Integer> leftView,
                     Map<Integer, String> states, StepEmitter emit) {
        emit.push("dfs(" + tree.value(index) + ", depth " + depth + ")");
        states.put(index, "visiting");

        if (!rightView.containsKey(depth)) {
            rightView.put(depth, tree.value(index));
            emit.at("firstAtDepth")
                    .say("Depth %d has not been reached before, and this walk meets a level "
                                    + "right to left - so %d is that level's rightmost node.",
                            depth, tree.value(index))
                    .var("depth", depth)
                    .var("rightView", new ArrayList<>(rightView.values()))
                    .var("leftView", new ArrayList<>(leftView.values()))
                    .tree(tree.render(states)).step();
        }

        Integer previousLeft = leftView.put(depth, tree.value(index));
        if (previousLeft == null) {
            emit.at("overwriteLeft")
                    .say("%d also becomes depth %d's left-view candidate for now - it is the "
                                    + "only node seen at that depth so far.",
                            tree.value(index), depth)
                    .var("depth", depth)
                    .var("rightView", new ArrayList<>(rightView.values()))
                    .var("leftView", new ArrayList<>(leftView.values()))
                    .tree(tree.render(states)).step();
        } else {
            emit.at("overwriteLeft")
                    .say("%d replaces %d as depth %d's left-view candidate: anything met later "
                                    + "at a depth is further left than what was there.",
                            tree.value(index), previousLeft, depth)
                    .var("depth", depth)
                    .var("rightView", new ArrayList<>(rightView.values()))
                    .var("leftView", new ArrayList<>(leftView.values()))
                    .tree(tree.render(states)).step();
        }

        states.put(index, "visited");

        Integer right = tree.right(index);
        if (right != null) {
            emit.at("goRightFirst")
                    .say("Descend into %d's RIGHT child %d first - that ordering is the whole "
                                    + "trick, and it is what makes \"first\" mean \"rightmost\".",
                            tree.value(index), tree.value(right))
                    .var("depth", depth + 1)
                    .var("rightView", new ArrayList<>(rightView.values()))
                    .var("leftView", new ArrayList<>(leftView.values()))
                    .tree(tree.render(states)).step();
            dfs(tree, right, depth + 1, rightView, leftView, states, emit);
        }

        Integer left = tree.left(index);
        if (left != null) {
            emit.at("goLeftSecond")
                    .say("Now descend into %d's LEFT child %d, after the whole right subtree.",
                            tree.value(index), tree.value(left))
                    .var("depth", depth + 1)
                    .var("rightView", new ArrayList<>(rightView.values()))
                    .var("leftView", new ArrayList<>(leftView.values()))
                    .tree(tree.render(states)).step();
            dfs(tree, left, depth + 1, rightView, leftView, states, emit);
        }

        emit.pop();
    }
}
