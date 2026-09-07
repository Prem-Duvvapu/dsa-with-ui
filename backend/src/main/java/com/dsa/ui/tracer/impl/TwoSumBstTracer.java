package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The two-pointer trick from a sorted array, run on a BST that is never flattened into one.
 *
 * <p>Two iterators walk the same tree in opposite directions - one producing values in
 * ascending order, the other in descending - and each keeps only the stack of ancestors it
 * still has to come back to, which is O(h), not O(n). If the pair sums too low the ascending
 * side advances, because only a larger small value can help; too high and the descending
 * side retreats. They stop when they meet, which is exactly when every pair has been ruled
 * out.
 *
 * <p>Collecting the whole inorder into a list first and then two-pointering it would give
 * the same answers with the same comparisons and O(n) memory. What this version shows
 * instead is that "the next value" and "the previous value" are each one stack operation
 * away in a BST, so the sorted array never has to exist.
 */
@Component
public class TwoSumBstTracer implements AlgorithmTracer {

    /** One inorder cursor over the tree; {@code descending} reverses left and right. */
    private static final class Cursor {
        private final BinaryTreeLayout tree;
        private final boolean descending;
        private final Deque<Integer> stack = new ArrayDeque<>();

        Cursor(BinaryTreeLayout tree, boolean descending) {
            this.tree = tree;
            this.descending = descending;
            if (!tree.isEmpty()) {
                push(tree.root());
            }
        }

        private void push(Integer index) {
            while (index != null) {
                stack.push(index);
                index = descending ? tree.right(index) : tree.left(index);
            }
        }

        boolean hasNext() {
            return !stack.isEmpty();
        }

        int next() {
            int index = stack.pop();
            push(descending ? tree.left(index) : tree.right(index));
            return index;
        }

        List<Integer> pending() {
            List<Integer> out = new ArrayList<>(stack.size());
            for (int index : stack) {
                out.add(0, tree.value(index));
            }
            return out;
        }
    }

    @Override
    public String id() {
        return "two-sum-bst";
    }

    @Override
    public DsType dsType() {
        return DsType.TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("tree", FieldType.BINARY_TREE)
                        .label("BST (level order)")
                        .help("A BST in level order, with null where a child is absent.")
                        .length(1, 31).values(-99, 99).bstOrdered()
                        .defaultValue(Arrays.asList(5, 3, 6, 2, 4, null, 7))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Target sum k")
                        .help("Chosen so the DESCENDING cursor does the work; see the class doc.")
                        .range(-198, 198)
                        .defaultValue(5)
                        .build());
    }

    /** LeetCode 653's second example: no pair exists, so the two cursors run until they meet. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("k", 28);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean findTarget(TreeNode root, int k) {
                   // @a seedCursors
                   Cursor low = new Cursor(root, false);   // ascending
                   Cursor high = new Cursor(root, true);   // descending
                   int a = low.next(), b = high.next();
                   boolean found = false;

                   while (a < b && !found) {
                       int sum = a + b;
                       if (sum == k) {
                           // @a pairFound
                           found = true;
                       } else if (sum < k) {
                           // @a advanceLow
                           a = low.next();      // only a larger small value can help
                       } else {
                           // @a retreatHigh
                           b = high.next();     // only a smaller large value can help
                       }
                   }
                   // @a done
                   return found;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        int k = in.getInt("k");
        Map<Integer, String> states = new LinkedHashMap<>();

        Cursor low = new Cursor(tree, false);
        Cursor high = new Cursor(tree, true);

        if (!low.hasNext() || !high.hasNext()) {
            emit.at("done").say("The tree is empty, so no pair can sum to %d.", k)
                    .var("answer", false).tree(tree.render(states)).step();
            return;
        }

        int aIndex = low.next();
        int bIndex = high.next();
        states.put(aIndex, "visiting");
        states.put(bIndex, "queued");

        emit.at("seedCursors")
                .say("Two cursors over the same tree: the ascending one starts at the smallest "
                                + "value %d, the descending one at the largest, %d. Each holds "
                                + "only the ancestors it still has to return to.",
                        tree.value(aIndex), tree.value(bIndex))
                .var("low", tree.value(aIndex)).var("high", tree.value(bIndex))
                .var("lowPending", low.pending().toString())
                .var("highPending", high.pending().toString())
                .tree(tree.render(states)).step();

        boolean found = false;
        while (tree.value(aIndex) < tree.value(bIndex) && !found) {
            int a = tree.value(aIndex);
            int b = tree.value(bIndex);
            int sum = a + b;

            if (sum == k) {
                found = true;
                states.put(aIndex, "target");
                states.put(bIndex, "target");
                emit.at("pairFound")
                        .say("%d + %d = %d. That is the target, so the answer is yes.", a, b, k)
                        .var("low", a).var("high", b).var("sum", sum)
                        .tree(tree.render(states)).step();
            } else if (sum < k) {
                states.put(aIndex, "visited");
                if (!low.hasNext()) {
                    break;
                }
                aIndex = low.next();
                states.put(aIndex, "visiting");
                emit.at("advanceLow")
                        .say("%d + %d = %d, short of %d. Nothing paired with %d can reach %d "
                                        + "either, since %d is already the largest value left - "
                                        + "so advance the ascending cursor to %d.",
                                a, b, sum, k, a, k, b, tree.value(aIndex))
                        .var("low", tree.value(aIndex)).var("high", b).var("sum", sum)
                        .var("lowPending", low.pending().toString())
                        .tree(tree.render(states)).step();
            } else {
                states.put(bIndex, "visited");
                if (!high.hasNext()) {
                    break;
                }
                bIndex = high.next();
                states.put(bIndex, "queued");
                emit.at("retreatHigh")
                        .say("%d + %d = %d, over %d. Nothing paired with %d can come down to %d, "
                                        + "since %d is already the smallest value left - so "
                                        + "retreat the descending cursor to %d.",
                                a, b, sum, k, b, k, a, tree.value(bIndex))
                        .var("low", a).var("high", tree.value(bIndex)).var("sum", sum)
                        .var("highPending", high.pending().toString())
                        .tree(tree.render(states)).step();
            }
        }

        if (found) {
            emit.at("done")
                    .say("A pair summing to %d exists, found without ever materialising the "
                            + "tree's sorted order as a list.", k)
                    .var("answer", true).tree(tree.render(states)).step();
        } else {
            emit.at("done")
                    .say("The two cursors met at %d and %d without ever hitting %d. Every pair "
                                    + "has been ruled out, so no two values sum to %d.",
                            tree.value(aIndex), tree.value(bIndex), k, k)
                    .var("answer", false).tree(tree.render(states)).step();
        }
    }
}
