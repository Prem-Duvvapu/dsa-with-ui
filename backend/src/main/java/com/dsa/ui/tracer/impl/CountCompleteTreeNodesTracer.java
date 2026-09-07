package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Counting the nodes of a COMPLETE tree without visiting them.
 *
 * <p>Walking every node is correct and O(n), and it throws away the only thing that makes
 * this problem interesting. In a complete tree, if the leftmost spine and the rightmost
 * spine of a subtree are the same length, that subtree is perfect - and a perfect subtree of
 * height h holds exactly 2^h - 1 nodes, an answer arrived at without descending into it at
 * all. Only when the two spines disagree does the count split into two recursive calls, and
 * completeness guarantees at most one of those two disagrees at each level.
 *
 * <p>So the work is O(log^2 n): O(log n) levels, each measuring two spines of O(log n). The
 * steps to watch are the perfect-subtree ones, where a whole block of nodes is counted and
 * skipped in a single line.
 */
@Component
public class CountCompleteTreeNodesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "count-complete-tree-nodes";
    }

    @Override
    public DsType dsType() {
        return DsType.TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("tree", FieldType.BINARY_TREE)
                        .label("Complete tree (level order)")
                        .help("A complete binary tree in level order: every level full except "
                                + "possibly the last, which fills from the left.")
                        .length(1, 31).values(-99, 99)
                        .defaultValue(Arrays.asList(1, 2, 3, 4, 5, 6))
                        .build());
    }

    /** A perfect tree, where the two spines match at the root and the entire answer is one shift - no recursion at all. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, 2, 3, 4, 5, 6, 7));
    }

    @Override
    public String annotatedCode() {
        return """
               public int countCompleteTree(TreeNode root) {
                   int total = countNodes(root);
                   // @a done
                   return total;
               }

               private int countNodes(TreeNode node) {
                   if (node == null) {
                       // @a emptyIsZero
                       return 0;
                   }
                   int left = leftSpine(node);
                   int right = rightSpine(node);

                   if (left == right) {
                       // @a perfectSubtree
                       return (1 << left) - 1;    // counted without visiting one of them
                   }
                   // @a splitAndRecurse
                   return 1 + countNodes(node.left) + countNodes(node.right);
               }

               private int leftSpine(TreeNode node) {
                   int h = 0;
                   while (node != null) {
                       // @a walkLeftSpine
                       h++;
                       node = node.left;
                   }
                   return h;
               }

               private int rightSpine(TreeNode node) {
                   int h = 0;
                   while (node != null) {
                       // @a walkRightSpine
                       h++;
                       node = node.right;
                   }
                   return h;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();

        int total = tree.isEmpty() ? 0 : count(tree, tree.root(), states, emit);

        emit.at("done")
                .say("Total nodes: %d - reached without ever visiting most of them.", total)
                .var("answer", total)
                .tree(tree.render(states)).step();
    }

    private int count(BinaryTreeLayout tree, int index, Map<Integer, String> states,
                      StepEmitter emit) {
        emit.push("countNodes(" + tree.value(index) + ")");

        int left = spine(tree, index, true, states, emit);
        int right = spine(tree, index, false, states, emit);

        if (left == right) {
            int nodes = (1 << left) - 1;
            states.put(index, "target");
            emit.at("perfectSubtree")
                    .say("Both spines under %d are %d long, so that whole subtree is PERFECT: "
                                    + "2^%d - 1 = %d node%s, counted without descending into a "
                                    + "single one of them.",
                            tree.value(index), left, left, nodes, nodes == 1 ? "" : "s")
                    .var("leftSpine", left).var("rightSpine", right).var("counted", nodes)
                    .tree(tree.render(states)).step();
            emit.pop();
            return nodes;
        }

        states.put(index, "visiting");
        emit.at("splitAndRecurse")
                .say("The spines under %d disagree - %d on the left against %d on the right - "
                                + "so its last level is only partly filled. Count %d itself and "
                                + "ask both children separately.",
                        tree.value(index), left, right, tree.value(index))
                .var("leftSpine", left).var("rightSpine", right)
                .tree(tree.render(states)).step();

        int total = 1;
        Integer leftChild = tree.left(index);
        if (leftChild == null) {
            emit.at("emptyIsZero")
                    .say("%d has no left child - that side contributes 0.", tree.value(index))
                    .var("counted", 0).tree(tree.render(states)).step();
        } else {
            total += count(tree, leftChild, states, emit);
        }

        Integer rightChild = tree.right(index);
        if (rightChild == null) {
            emit.at("emptyIsZero")
                    .say("%d has no right child - that side contributes 0.", tree.value(index))
                    .var("counted", 0).tree(tree.render(states)).step();
        } else {
            total += count(tree, rightChild, states, emit);
        }

        states.put(index, "visited");
        emit.pop();
        return total;
    }

    private int spine(BinaryTreeLayout tree, int index, boolean goLeft,
                      Map<Integer, String> states, StepEmitter emit) {
        String anchor = goLeft ? "walkLeftSpine" : "walkRightSpine";
        String side = goLeft ? "left" : "right";
        int height = 0;
        Integer node = index;
        while (node != null) {
            height++;
            emit.at(anchor)
                    .say("Walking the %s spine from %d: %d is step %d.",
                            side, tree.value(index), tree.value(node), height)
                    .var("spine", side).var("height", height)
                    .tree(tree.render(states)).step();
            node = goLeft ? tree.left(node) : tree.right(node);
        }
        return height;
    }
}
