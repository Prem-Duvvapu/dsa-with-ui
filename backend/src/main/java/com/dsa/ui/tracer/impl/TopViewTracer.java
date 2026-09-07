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
import java.util.TreeMap;

/**
 * What you would see looking straight down at the tree: for each vertical column, the one
 * node nothing is stacked on top of.
 *
 * <p>The column is a horizontal distance - 0 at the root, one less down every left edge and
 * one more down every right edge - and the winner of a column is the SHALLOWEST node in it.
 * That is why the traversal has to be breadth-first: BFS visits strictly in depth order, so
 * the first node to reach a column is necessarily the highest one, and every later arrival
 * can simply be ignored. A DFS would arrive at columns in an order that says nothing about
 * depth, and would need an explicit depth comparison to be correct.
 */
@Component
public class TopViewTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "top-view-bt";
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
                        .defaultValue(Arrays.asList(1, 2, 3, 4, 5, 6, 7))
                        .build());
    }

    /** A left-leaning tree: only three columns exist, and the node hidden behind the root sits two levels down. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(7, 3, null, 1, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> topView(TreeNode root) {
                   Map<Integer, Integer> column = new TreeMap<>();
                   Queue<Object[]> queue = new ArrayDeque<>();
                   // @a seed
                   queue.add(new Object[]{root, 0});

                   while (!queue.isEmpty()) {
                       // @a dequeue
                       Object[] entry = queue.poll();
                       TreeNode node = (TreeNode) entry[0];
                       int hd = (int) entry[1];

                       if (!column.containsKey(hd)) {
                           // @a claimColumn
                           column.put(hd, node.val);
                       } else {
                           // @a columnTaken
                           ;                       // a shallower node already owns it
                       }

                       if (node.left != null) {
                           // @a enqueueLeft
                           queue.add(new Object[]{node.left, hd - 1});
                       }
                       if (node.right != null) {
                           // @a enqueueRight
                           queue.add(new Object[]{node.right, hd + 1});
                       }
                   }
                   // @a done
                   return new ArrayList<>(column.values());
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();
        Map<Integer, Integer> column = new TreeMap<>();

        if (tree.isEmpty()) {
            emit.at("done").say("The tree is empty, so there is nothing to see from above.")
                    .var("topView", "[]").tree(tree.render(states)).step();
            return;
        }

        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{tree.root(), 0});
        states.put(tree.root(), "queued");

        emit.at("seed")
                .say("Seed the queue with the root %d at column 0. BFS order is depth order, "
                        + "which is what makes \"first to arrive\" mean \"highest up\".",
                        tree.value(tree.root()))
                .var("columns", column.toString())
                .tree(tree.render(states)).step();

        while (!queue.isEmpty()) {
            int[] entry = queue.poll();
            int index = entry[0];
            int hd = entry[1];
            states.put(index, "visiting");

            emit.at("dequeue")
                    .say("Dequeue %d, sitting in column %d.", tree.value(index), hd)
                    .var("node", tree.value(index)).var("column", hd)
                    .var("columns", column.toString())
                    .tree(tree.render(states)).step();

            if (!column.containsKey(hd)) {
                column.put(hd, tree.value(index));
                states.put(index, "target");
                emit.at("claimColumn")
                        .say("Column %d is empty, and BFS guarantees nothing above %d can still "
                                        + "be waiting - %d is visible from the top.",
                                hd, tree.value(index), tree.value(index))
                        .var("node", tree.value(index)).var("column", hd)
                        .var("columns", column.toString())
                        .tree(tree.render(states)).step();
            } else {
                states.put(index, "visited");
                emit.at("columnTaken")
                        .say("Column %d already belongs to %d, which was dequeued earlier and so "
                                        + "sits higher. %d is hidden underneath it.",
                                hd, column.get(hd), tree.value(index))
                        .var("node", tree.value(index)).var("column", hd)
                        .var("columns", column.toString())
                        .tree(tree.render(states)).step();
            }

            Integer left = tree.left(index);
            if (left != null) {
                queue.add(new int[]{left, hd - 1});
                states.put(left, "queued");
                emit.at("enqueueLeft")
                        .say("%d's left child %d goes one column further left, to %d.",
                                tree.value(index), tree.value(left), hd - 1)
                        .var("queued", tree.value(left)).var("column", hd - 1)
                        .var("columns", column.toString())
                        .tree(tree.render(states)).step();
            }
            Integer right = tree.right(index);
            if (right != null) {
                queue.add(new int[]{right, hd + 1});
                states.put(right, "queued");
                emit.at("enqueueRight")
                        .say("%d's right child %d goes one column further right, to %d.",
                                tree.value(index), tree.value(right), hd + 1)
                        .var("queued", tree.value(right)).var("column", hd + 1)
                        .var("columns", column.toString())
                        .tree(tree.render(states)).step();
            }
        }

        List<Integer> view = new ArrayList<>(column.values());
        emit.at("done")
                .say("Reading the claimed columns left to right gives the top view: %s.", view)
                .var("topView", view).var("columns", column.toString())
                .tree(tree.render(states)).step();
    }
}
