package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

/**
 * Groups every node by a column coordinate that starts at 0 for the root and shifts by
 * -1/+1 down each left/right edge, tracking row depth alongside it. Two nodes can land in
 * the very same (row, column) slot - the tie is broken by value, not by whichever one a
 * traversal happened to visit first, which is the one rule that makes this problem more
 * than a plain BFS with bookkeeping.
 */
@Component
public class VerticalOrderTraversalTracer implements AlgorithmTracer {

    private record Entry(int row, int value, int index) {}

    @Override
    public String id() {
        return "vertical-order-traversal";
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
                        .defaultValue(java.util.Arrays.asList(1, 2, 3, 4, 10, 9, 11))
                        .build());
    }

    /** No same-row-and-column collision this time: every column ends up with a single, unambiguous order. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", java.util.Arrays.asList(3, 9, 20, null, null, 15, 7));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> verticalTraversal(TreeNode root) {
                   Map<Integer, List<int[]>> columns = new TreeMap<>();
                   Queue<Object[]> queue = new LinkedList<>(); // {node, row, col}
                   queue.add(new Object[]{root, 0, 0});

                   while (!queue.isEmpty()) {
                       Object[] entry = queue.poll();
                       TreeNode node = (TreeNode) entry[0];
                       int row = (int) entry[1], col = (int) entry[2];
                       // @a visit
                       // @a assignColumn
                       columns.computeIfAbsent(col, k -> new ArrayList<>())
                               .add(new int[]{row, node.val});
                       if (node.left != null) queue.add(new Object[]{node.left, row + 1, col - 1});
                       if (node.right != null) queue.add(new Object[]{node.right, row + 1, col + 1});
                   }

                   List<List<Integer>> result = new ArrayList<>();
                   for (List<int[]> column : columns.values()) {
                       // @a tieBreakByValue
                       column.sort((a, b) -> a[0] != b[0] ? a[0] - b[0] : a[1] - b[1]);
                       List<Integer> values = new ArrayList<>();
                       for (int[] e : column) values.add(e[1]);
                       result.add(values);
                   }
                   // @a done
                   return result;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();

        Map<Integer, List<Entry>> columns = new TreeMap<>();
        Queue<int[]> queue = new ArrayDeque<>(); // {index, row, col}
        queue.add(new int[]{tree.root(), 0, 0});

        while (!queue.isEmpty()) {
            int[] entry = queue.poll();
            int index = entry[0], row = entry[1], col = entry[2];
            states.put(index, "visiting");
            emit.at("visit")
                    .say("Visit %d at row %d, column %d.", tree.value(index), row, col)
                    .var("value", tree.value(index)).var("row", row).var("col", col)
                    .tree(tree.render(states)).step();

            columns.computeIfAbsent(col, k -> new ArrayList<>())
                    .add(new Entry(row, tree.value(index), index));
            emit.at("assignColumn")
                    .say("%d joins column %d.", tree.value(index), col)
                    .var("column", col)
                    .tree(tree.render(states)).step();
            states.put(index, "visited");

            Integer left = tree.left(index);
            Integer right = tree.right(index);
            if (left != null) {
                queue.add(new int[]{left, row + 1, col - 1});
            }
            if (right != null) {
                queue.add(new int[]{right, row + 1, col + 1});
            }
        }

        List<List<Integer>> result = new ArrayList<>();
        for (Map.Entry<Integer, List<Entry>> col : columns.entrySet()) {
            List<Entry> entries = col.getValue();
            Map<Integer, Long> rowCounts = new LinkedHashMap<>();
            for (Entry e : entries) {
                rowCounts.merge(e.row(), 1L, Long::sum);
            }
            boolean tie = rowCounts.values().stream().anyMatch(c -> c > 1);

            entries.sort((a, b) -> a.row() != b.row() ? a.row() - b.row() : a.value() - b.value());
            List<Integer> values = new ArrayList<>();
            for (Entry e : entries) {
                values.add(e.value());
            }
            result.add(values);

            if (tie) {
                emit.at("tieBreakByValue")
                        .say("Column %d has two nodes sharing the same row - sort those by "
                                + "value, not visit order: %s.", col.getKey(), values)
                        .var("column", col.getKey()).var("resolved", values.toString())
                        .tree(tree.render(states)).step();
            }
        }

        emit.at("done")
                .say("Every column collected, ordered left to right: %s.", result)
                .var("answer", result.toString())
                .tree(tree.render(states)).step();
    }
}
