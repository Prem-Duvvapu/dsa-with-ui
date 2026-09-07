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
 * Width counts the GAPS, not the nodes: a level with two nodes six positions apart is seven
 * wide. So counting how many nodes a level holds is the wrong answer, and the level has to
 * be measured by position instead.
 *
 * <p>Each node is carried through the queue with the position it would occupy in a complete
 * tree - a left child is at {@code 2i}, a right child at {@code 2i + 1} - and a level's
 * width is its last position minus its first, plus one. Those positions double every level,
 * so they are re-based to 0 at the start of each level; without that a deep tree overflows,
 * which is the trap this problem is really about.
 */
@Component
public class MaxWidthTracer implements AlgorithmTracer {

    private record Entry(int index, long position) {}

    @Override
    public String id() {
        return "max-width-bt";
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
                        .defaultValue(Arrays.asList(1, 3, 2, 5, 3, null, 9, 6))
                        .build());
    }

    /** A left-only chain: every level holds exactly one node, so the widest level is 1 and no level ever improves on the first. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, 2, null, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public int widthOfBinaryTree(TreeNode root) {
                   int best = 0;
                   Queue<Entry> queue = new ArrayDeque<>();
                   // @a seed
                   queue.add(new Entry(root, 0));

                   while (!queue.isEmpty()) {
                       int size = queue.size();
                       long first = queue.peek().position;
                       long last = first;

                       for (int i = 0; i < size; i++) {
                           Entry e = queue.poll();
                           // @a rebase
                           long position = e.position - first;
                           last = e.position;
                           if (e.node.left != null) {
                               // @a placeLeft
                               queue.add(new Entry(e.node.left, 2 * position));
                           }
                           if (e.node.right != null) {
                               // @a placeRight
                               queue.add(new Entry(e.node.right, 2 * position + 1));
                           }
                       }

                       long width = last - first + 1;
                       if (width > best) {
                           // @a widest
                           best = (int) width;
                       } else {
                           // @a notWider
                           ;
                       }
                   }
                   // @a done
                   return best;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();
        int best = 0;

        if (tree.isEmpty()) {
            emit.at("done").say("An empty tree has width 0.")
                    .var("answer", 0).tree(tree.render(states)).step();
            return;
        }

        Queue<Entry> queue = new ArrayDeque<>();
        queue.add(new Entry(tree.root(), 0));
        states.put(tree.root(), "queued");

        emit.at("seed")
                .say("The root %d sits at position 0. Every node carries the position it would "
                        + "occupy if the tree were complete, which is what lets a gap be "
                        + "counted.", tree.value(tree.root()))
                .var("best", best).tree(tree.render(states)).step();

        int levelIndex = 0;
        while (!queue.isEmpty()) {
            int size = queue.size();
            long first = queue.peek().position();
            long last = first;
            List<String> row = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                Entry e = queue.poll();
                long position = e.position() - first;
                last = e.position();
                states.put(e.index(), "visiting");
                row.add(tree.value(e.index()) + "@" + position);

                emit.at("rebase")
                        .say("%d is at position %d on level %d; re-based against this level's "
                                        + "first position it becomes %d, which keeps the numbers "
                                        + "small enough not to overflow.",
                                tree.value(e.index()), e.position(), levelIndex, position)
                        .var("level", levelIndex).var("row", row.toString()).var("best", best)
                        .tree(tree.render(states)).step();

                Integer left = tree.left(e.index());
                if (left != null) {
                    queue.add(new Entry(left, 2 * position));
                    states.put(left, "queued");
                    emit.at("placeLeft")
                            .say("%d's left child %d takes position %d on the next level.",
                                    tree.value(e.index()), tree.value(left), 2 * position)
                            .var("level", levelIndex).var("row", row.toString()).var("best", best)
                            .tree(tree.render(states)).step();
                }
                Integer right = tree.right(e.index());
                if (right != null) {
                    queue.add(new Entry(right, 2 * position + 1));
                    states.put(right, "queued");
                    emit.at("placeRight")
                            .say("%d's right child %d takes position %d on the next level.",
                                    tree.value(e.index()), tree.value(right), 2 * position + 1)
                            .var("level", levelIndex).var("row", row.toString()).var("best", best)
                            .tree(tree.render(states)).step();
                }
                states.put(e.index(), "visited");
            }

            long width = last - first + 1;
            if (width > best) {
                best = (int) width;
                emit.at("widest")
                        .say("Level %d spans positions %d to %d, so it is %d wide - counting "
                                        + "the %d node%s only would have said %d. New widest.",
                                levelIndex, first, last, width, size, size == 1 ? "" : "s", size)
                        .var("level", levelIndex).var("width", width).var("best", best)
                        .tree(tree.render(states)).step();
            } else {
                emit.at("notWider")
                        .say("Level %d spans positions %d to %d, only %d wide - the best is "
                                        + "still %d.",
                                levelIndex, first, last, width, best)
                        .var("level", levelIndex).var("width", width).var("best", best)
                        .tree(tree.render(states)).step();
            }
            levelIndex++;
        }

        emit.at("done")
                .say("Every level measured. The maximum width is %d.", best)
                .var("answer", best).tree(tree.render(states)).step();
    }
}
