package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Level order traversal that alternates reading direction per level. The queue still
 * enqueues children left-to-right every time - only the order a level is WRITTEN gets
 * reversed, after the fact, on every other level.
 */
@Component
public class ZigzagTraversalTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "zigzag-traversal";
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

    /** A perfect 3-level tree — zigzag reverses a full 4-node level, not just a 2-node one. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, 2, 3, 4, 5, 6, 7));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> zigzagLevelOrder(Node root) {
                   // @a init
                   Queue<Node> queue = new ArrayDeque<>();
                   List<List<Integer>> levels = new ArrayList<>();
                   boolean leftToRight = true;
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
                       if (!leftToRight) {
                           // @a reverse
                           Collections.reverse(level);
                       }
                       // @a close
                       levels.add(level);
                       leftToRight = !leftToRight;
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
        boolean leftToRight = true;

        emit.at("init")
                .say("Seed the queue with the root %d. Reading direction starts left-to-right.",
                        tree.value(tree.root()))
                .var("queue", queueValues(tree, queue)).var("direction", "left-to-right")
                .tree(tree.render(states)).step();

        while (!queue.isEmpty()) {
            int size = queue.size();
            List<Integer> level = new ArrayList<>();

            emit.at("level")
                    .say("Freeze the frontier: %d node%s form level %d, read %s this time.",
                            size, size == 1 ? "" : "s", levels.size(),
                            leftToRight ? "left-to-right" : "right-to-left")
                    .var("queueSize", size).var("levelIndex", levels.size())
                    .var("direction", leftToRight ? "left-to-right" : "right-to-left")
                    .tree(tree.render(states)).step();

            for (int i = 0; i < size; i++) {
                int node = queue.poll();
                level.add(tree.value(node));
                states.put(node, "visiting");

                emit.at("dequeue")
                        .say("Take %d off the front - the queue never changes order - and append it to level %d's raw order.",
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
                    emit.at("enqueue")
                            .say("%d's child%s %s always enqueue left-to-right, no matter which direction this level reads.",
                                    tree.value(node), left != null && right != null ? "ren" : "", kids)
                            .var("queue", queueValues(tree, queue)).tree(tree.render(states)).step();
                }

                states.put(node, "visited");
            }

            if (!leftToRight) {
                Collections.reverse(level);
                emit.at("reverse")
                        .say("This level reads right-to-left - reverse the raw dequeue order: %s.", level)
                        .var("level", level).tree(tree.render(states)).step();
            }

            levels.add(level);
            emit.at("close")
                    .say("Level %d is complete: %s.", levels.size() - 1, level)
                    .var("levels", levels).tree(tree.render(states)).step();

            leftToRight = !leftToRight;
        }

        emit.at("done").say("Queue drained - zigzag order: %s.", levels)
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
