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
 * What you would see looking up at the tree from underneath: for each vertical column, the
 * one node with nothing hanging below it.
 *
 * <p>Same horizontal-distance bookkeeping and the same breadth-first walk as the top view,
 * and exactly one line differs: the column is overwritten on every arrival instead of only
 * the first. Because BFS arrives strictly in depth order, the LAST writer to a column is
 * necessarily the deepest node in it, so no depth comparison is ever needed - the winner
 * is decided purely by who wrote last.
 */
@Component
public class BottomViewTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bottom-view-bt";
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
                        .defaultValue(Arrays.asList(20, 8, 22, 5, 3, null, 25))
                        .build());
    }

    /** A perfect three-node tree: every column is written exactly once, so nothing is ever overwritten. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(4, 2, 6));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> bottomView(TreeNode root) {
                   Map<Integer, Integer> column = new TreeMap<>();
                   Queue<Object[]> queue = new ArrayDeque<>();
                   // @a seed
                   queue.add(new Object[]{root, 0});

                   while (!queue.isEmpty()) {
                       // @a dequeue
                       Object[] entry = queue.poll();
                       TreeNode node = (TreeNode) entry[0];
                       int hd = (int) entry[1];

                       if (column.containsKey(hd)) {
                           // @a overwriteColumn
                           column.put(hd, node.val);   // deeper, so it wins
                       } else {
                           // @a firstInColumn
                           column.put(hd, node.val);
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
        Map<Integer, Integer> ownerIndex = new LinkedHashMap<>();

        if (tree.isEmpty()) {
            emit.at("done").say("The tree is empty, so there is nothing to see from below.")
                    .var("bottomView", "[]").tree(tree.render(states)).step();
            return;
        }

        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{tree.root(), 0});
        states.put(tree.root(), "queued");

        emit.at("seed")
                .say("Seed the queue with the root %d at column 0. Every column starts out "
                        + "owned by whoever writes to it, and keeps changing hands as the walk "
                        + "goes deeper.", tree.value(tree.root()))
                .var("columns", column.toString())
                .tree(tree.render(states)).step();

        while (!queue.isEmpty()) {
            int[] entry = queue.poll();
            int index = entry[0];
            int hd = entry[1];

            emit.at("dequeue")
                    .say("Dequeue %d, sitting in column %d.", tree.value(index), hd)
                    .var("node", tree.value(index)).var("column", hd)
                    .var("columns", column.toString())
                    .tree(tree.render(states)).step();

            if (column.containsKey(hd)) {
                int previous = column.get(hd);
                states.put(ownerIndex.get(hd), "visited");
                column.put(hd, tree.value(index));
                ownerIndex.put(hd, index);
                states.put(index, "target");
                emit.at("overwriteColumn")
                        .say("Column %d was showing %d, but %d was dequeued later and so lies "
                                        + "deeper - it takes the column over.",
                                hd, previous, tree.value(index))
                        .var("node", tree.value(index)).var("column", hd)
                        .var("columns", column.toString())
                        .tree(tree.render(states)).step();
            } else {
                column.put(hd, tree.value(index));
                ownerIndex.put(hd, index);
                states.put(index, "target");
                emit.at("firstInColumn")
                        .say("Column %d is empty, so %d takes it - provisionally. Anything that "
                                        + "later lands in column %d will be deeper and will "
                                        + "replace it.",
                                hd, tree.value(index), hd)
                        .var("node", tree.value(index)).var("column", hd)
                        .var("columns", column.toString())
                        .tree(tree.render(states)).step();
            }

            Integer left = tree.left(index);
            if (left != null) {
                queue.add(new int[]{left, hd - 1});
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
                .say("Nobody is left to overwrite anything. Reading the columns left to right "
                        + "gives the bottom view: %s.", view)
                .var("bottomView", view).var("columns", column.toString())
                .tree(tree.render(states)).step();
    }
}
