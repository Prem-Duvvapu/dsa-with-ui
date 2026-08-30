package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Level order traversal. Where the other traversals recurse, this one walks the tree
 * with a queue and slices it into levels — each level closes only when it has consumed
 * exactly the nodes that were queued when the level began.
 */
@Component
public class TreeLevelOrderTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "tree-level-order";
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
                        .defaultValue(TreePreorderTracer.DEFAULT_TREE)
                        .build());
    }

    /** A right-skewed chain — three levels of one node each, against the bushy default. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, 2, null, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> levelOrder(Node root) {
                   // @a init
                   Queue<Node> queue = new ArrayDeque<>();
                   List<List<Integer>> levels = new ArrayList<>();
                   if (root != null) queue.add(root);
                   while (!queue.isEmpty()) {
                       // @a level
                       int size = queue.size();
                       List<Integer> level = new ArrayList<>();
                       for (int i = 0; i < size; i++) {
                           // @a dequeue
                           Node node = queue.poll();
                           level.add(node.val);
                           // @a enqueue
                           if (node.left != null) queue.add(node.left);
                           if (node.right != null) queue.add(node.right);
                       }
                       // @a close
                       levels.add(level);
                   }
                   // @a done
                   return levels;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));


        if (tree.isEmpty()) {
            emit.at("done").say("The tree is empty, so there are no levels.")
                    .var("levels", "[]").tree(tree.render(Map.of())).step();
            return;
        }

        Map<Integer, String> states = new LinkedHashMap<>();
        List<List<Integer>> levels = new ArrayList<>();
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(tree.root());
        states.put(tree.root(), "queued");

        emit.at("init").say("Seed the queue with the root %d. Everything else enters through a parent.",
                        tree.value(tree.root()))
                .var("queue", queueValues(tree, queue)).tree(tree.render(states)).step();

        while (!queue.isEmpty()) {
            int size = queue.size();
            List<Integer> level = new ArrayList<>();

            emit.at("level").say("Freeze the frontier: %d node%s entered the queue last round, so level %d has exactly that many.",
                            size, size == 1 ? "" : "s", levels.size())
                    .var("queueSize", size).var("levelIndex", levels.size())
                    .tree(tree.render(states)).step();

            for (int i = 0; i < size; i++) {
                int node = queue.poll();
                level.add(tree.value(node));
                states.put(node, "visiting");

                emit.at("dequeue").say("Take %d off the front — first in, first out — and write it into level %d.",
                                tree.value(node), levels.size())
                        .var("node", tree.value(node)).var("levelSoFar", level)
                        .var("queue", queueValues(tree, queue)).tree(tree.render(states)).step();

                Integer left = tree.left(node);
                Integer right = tree.right(node);
                if (left != null || right != null) {
                    if (left != null) {
                        queue.add(left);
                        states.put(left, "queued");
                    }
                    if (right != null) {
                        queue.add(right);
                        states.put(right, "queued");
                    }
                    String kids = (left != null && right != null)
                            ? tree.value(left) + " and " + tree.value(right)
                            : String.valueOf(tree.value(left != null ? left : right));
                    emit.at("enqueue").say("%d's child%s %s go to the BACK of the queue — they wait for every node on this level.",
                                    tree.value(node), left != null && right != null ? "ren" : "", kids)
                            .var("queue", queueValues(tree, queue)).tree(tree.render(states)).step();
                }

                states.put(node, "visited");
            }

            levels.add(level);
            emit.at("close").say("Level %d is complete: %s. Whatever is queued now belongs to the next one.",
                            levels.size() - 1, level)
                    .var("levels", levels).tree(tree.render(states)).step();
        }

        emit.at("done").say("Queue drained — every level is built: %s.", levels)
                .var("levels", levels).tree(tree.render(states)).step();
    }

    private List<Integer> queueValues(BinaryTreeLayout tree, Queue<Integer> queue) {
        List<Integer> out = new ArrayList<>(queue.size());
        for (int index : queue) {
            out.add(tree.value(index));
        }
        return out;
    }
}
