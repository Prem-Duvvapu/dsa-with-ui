package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.TreeNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merging two BSTs into one sorted sequence, in O(n + m), by refusing to sort anything.
 *
 * <p>The naive route - collect both trees, concatenate, sort - throws away the fact that
 * each tree's inorder walk is ALREADY sorted, and pays O((n+m) log(n+m)) to rediscover it.
 * Here each tree is walked once in order, and the two sorted sequences are then merged with
 * the same two-index sweep a merge sort's combine step uses: compare the two front values,
 * take the smaller, advance only that side.
 *
 * <p>The result is the merged sorted list. Turning that list into a balanced BST is one
 * further standard step (take the middle element as the root, recurse on the halves), and
 * it is deliberately not folded in here: the merge is where the two input trees stop being
 * two trees, and that is what this trace is about.
 */
@Component
public class MergeTwoBstsTracer implements AlgorithmTracer {

    private static final double GAP = 340;

    @Override
    public String id() {
        return "merge-two-bsts";
    }

    @Override
    public DsType dsType() {
        return DsType.TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("first", FieldType.BINARY_TREE)
                        .label("First BST (level order)")
                        .help("A BST in level order, with null where a child is absent.")
                        .length(1, 15).values(-99, 99).bstOrdered()
                        .defaultValue(Arrays.asList(5, 3, 6, 2, 4))
                        .build(),
                InputField.of("second", FieldType.BINARY_TREE)
                        .label("Second BST (level order)")
                        .help("A BST in level order, with null where a child is absent.")
                        .length(1, 15).values(-99, 99).bstOrdered()
                        .defaultValue(Arrays.asList(2, 1, 3))
                        .build());
    }

    /** The two trees swapped, so it is the SECOND sequence that outlives the first and gets drained at the end. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "first", Arrays.asList(2, 1, 3),
                "second", Arrays.asList(5, 3, 6, 2, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> merge(TreeNode a, TreeNode b) {
                   List<Integer> first = new ArrayList<>();
                   List<Integer> second = new ArrayList<>();
                   inorder(a, first);
                   inorder(b, second);

                   List<Integer> merged = new ArrayList<>();
                   int i = 0, j = 0;
                   while (i < first.size() && j < second.size()) {
                       if (first.get(i) <= second.get(j)) {
                           // @a takeFromFirst
                           merged.add(first.get(i++));
                       } else {
                           // @a takeFromSecond
                           merged.add(second.get(j++));
                       }
                   }
                   while (i < first.size()) {
                       // @a drainFirst
                       merged.add(first.get(i++));
                   }
                   while (j < second.size()) {
                       // @a drainSecond
                       merged.add(second.get(j++));
                   }
                   // @a done
                   return merged;
               }

               private void inorder(TreeNode node, List<Integer> out) {
                   if (node == null) return;
                   inorder(node.left, out);
                   // @a collectInorder
                   out.add(node.val);      // a BST's inorder is already sorted
                   inorder(node.right, out);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout a = new BinaryTreeLayout(in.getBinaryTree("first"));
        BinaryTreeLayout b = new BinaryTreeLayout(in.getBinaryTree("second"));
        Map<Integer, String> aStates = new LinkedHashMap<>();
        Map<Integer, String> bStates = new LinkedHashMap<>();

        List<Integer> first = new ArrayList<>();
        List<Integer> second = new ArrayList<>();

        if (!a.isEmpty()) {
            collect(a, a.root(), first, "first", aStates, bStates, a, b, emit);
        }
        if (!b.isEmpty()) {
            collect(b, b.root(), second, "second", bStates, aStates, a, b, emit);
        }

        List<Integer> merged = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < first.size() && j < second.size()) {
            if (first.get(i) <= second.get(j)) {
                merged.add(first.get(i));
                emit.at("takeFromFirst")
                        .say("%d from the first tree is not larger than %d from the second, so "
                                        + "it goes next. Only the first index advances.",
                                first.get(i), second.get(j))
                        .var("merged", merged.toString())
                        .var("firstLeft", first.subList(i + 1, first.size()).toString())
                        .var("secondLeft", second.subList(j, second.size()).toString())
                        .tree(both(a, b, aStates, bStates)).step();
                i++;
            } else {
                merged.add(second.get(j));
                emit.at("takeFromSecond")
                        .say("%d from the second tree is smaller than %d from the first, so it "
                                        + "goes next instead.",
                                second.get(j), first.get(i))
                        .var("merged", merged.toString())
                        .var("firstLeft", first.subList(i, first.size()).toString())
                        .var("secondLeft", second.subList(j + 1, second.size()).toString())
                        .tree(both(a, b, aStates, bStates)).step();
                j++;
            }
        }

        while (i < first.size()) {
            merged.add(first.get(i));
            emit.at("drainFirst")
                    .say("The second sequence is used up, and everything left in the first is "
                            + "already sorted and already larger - append %d as it stands.",
                            first.get(i))
                    .var("merged", merged.toString())
                    .var("firstLeft", first.subList(i + 1, first.size()).toString())
                    .tree(both(a, b, aStates, bStates)).step();
            i++;
        }
        while (j < second.size()) {
            merged.add(second.get(j));
            emit.at("drainSecond")
                    .say("The first sequence is used up, so the rest of the second can be "
                            + "appended unchanged - %d next.", second.get(j))
                    .var("merged", merged.toString())
                    .var("secondLeft", second.subList(j + 1, second.size()).toString())
                    .tree(both(a, b, aStates, bStates)).step();
            j++;
        }

        emit.at("done")
                .say("Merged in one sweep: %s. No comparison was ever made twice, and nothing "
                        + "was sorted - both inputs arrived sorted by being BSTs.", merged)
                .var("merged", merged.toString()).var("size", merged.size())
                .tree(both(a, b, aStates, bStates)).step();
    }

    private void collect(BinaryTreeLayout tree, int index, List<Integer> out, String which,
                         Map<Integer, String> own, Map<Integer, String> other,
                         BinaryTreeLayout a, BinaryTreeLayout b, StepEmitter emit) {
        Integer left = tree.left(index);
        if (left != null) {
            collect(tree, left, out, which, own, other, a, b, emit);
        }
        out.add(tree.value(index));
        own.put(index, "visited");
        emit.at("collectInorder")
                .say("Inorder over the %s tree reaches %d. The sequence so far is %s - already "
                        + "sorted, with nothing sorted to make it so.",
                        which, tree.value(index), out)
                .var(which, out.toString())
                .tree(both(a, b, which.equals("first") ? own : other,
                        which.equals("first") ? other : own)).step();
        Integer right = tree.right(index);
        if (right != null) {
            collect(tree, right, out, which, own, other, a, b, emit);
        }
    }

    /** Both trees on one canvas: the first on the left, the second shifted right. */
    private List<TreeNode> both(BinaryTreeLayout a, BinaryTreeLayout b,
                                Map<Integer, String> aStates, Map<Integer, String> bStates) {
        List<TreeNode> out = new ArrayList<>();
        for (TreeNode node : a.render(aStates)) {
            out.add(new TreeNode(node.getId(), "A:" + node.getVal(), node.getX() / 2,
                    node.getY(), node.getLeftId(), node.getRightId(), node.getState()));
        }
        for (TreeNode node : b.render(bStates)) {
            Integer left = node.getLeftId() == null ? null : node.getLeftId() + 1000;
            Integer right = node.getRightId() == null ? null : node.getRightId() + 1000;
            out.add(new TreeNode(node.getId() + 1000, "B:" + node.getVal(),
                    node.getX() / 2 + GAP, node.getY(), left, right, node.getState()));
        }
        return out;
    }
}
