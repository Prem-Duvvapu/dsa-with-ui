package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Symmetry stated level by level: a tree is a mirror of itself exactly when every level,
 * written out WITH a placeholder for each missing child, reads the same forwards and
 * backwards.
 *
 * <p>The placeholders are the whole difficulty. Reading only the nodes that exist, level 2
 * of {@code [1,2,2,null,3,null,3]} is "3, 3" - a palindrome, and the wrong answer. Written
 * positionally it is "-, 3, -, 3", which is not, and 3's two positions are what a mirror
 * would have to swap. So every level is captured across the full width it would occupy in a
 * complete tree, gaps included, before any verdict is reached.
 *
 * <p>The capture runs to the bottom before the checking starts, deliberately: the whole
 * shape is visible on screen before any level is judged, and the level that breaks the
 * symmetry is then pointed at inside a picture that is already complete.
 */
@Component
public class SymmetricTreeTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "symmetric-tree";
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
                        .defaultValue(Arrays.asList(1, 2, 2, 3, 4, 4, 3))
                        .build());
    }

    /** LeetCode 101's second example: the values that exist read as a palindrome, but their POSITIONS do not. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, 2, 2, null, 3, null, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isSymmetric(TreeNode root) {
                   List<List<Integer>> rows = new ArrayList<>();
                   List<TreeNode> level = new ArrayList<>(List.of(root));

                   while (containsANode(level)) {
                       List<Integer> values = new ArrayList<>();
                       List<TreeNode> next = new ArrayList<>();
                       for (TreeNode node : level) {
                           // @a readSlot
                           values.add(node == null ? null : node.val);
                           next.add(node == null ? null : node.left);
                           next.add(node == null ? null : node.right);
                       }
                       // @a levelCaptured
                       rows.add(values);
                       level = next;
                   }

                   for (List<Integer> row : rows) {
                       if (!isPalindrome(row)) {
                           // @a levelBreaksSymmetry
                           return false;
                       }
                       // @a levelMirrors
                   }
                   // @a done
                   return true;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();

        if (tree.isEmpty()) {
            emit.at("done").say("An empty tree is trivially its own mirror image.")
                    .var("answer", true).tree(tree.render(states)).step();
            return;
        }

        List<List<String>> rows = new ArrayList<>();
        List<Integer> level = new ArrayList<>();
        level.add(tree.root());

        while (level.stream().anyMatch(java.util.Objects::nonNull)) {
            List<String> values = new ArrayList<>();
            List<Integer> next = new ArrayList<>();
            for (Integer node : level) {
                if (node == null) {
                    values.add("-");
                    next.add(null);
                    next.add(null);
                    emit.at("readSlot")
                            .say("This position holds no node. It still occupies a slot in the "
                                    + "level and is written as a gap - dropping it is what makes "
                                    + "the naive version wrong.")
                            .var("level", rows.size()).var("row", values.toString())
                            .tree(tree.render(states)).step();
                } else {
                    values.add(String.valueOf(tree.value(node)));
                    next.add(tree.left(node));
                    next.add(tree.right(node));
                    states.put(node, "visiting");
                    emit.at("readSlot")
                            .say("Read %d into level %d's row.", tree.value(node), rows.size())
                            .var("level", rows.size()).var("row", values.toString())
                            .tree(tree.render(states)).step();
                }
            }
            rows.add(values);
            emit.at("levelCaptured")
                    .say("Level %d captured across its full width: %s.",
                            rows.size() - 1, values)
                    .var("level", rows.size() - 1).var("row", values.toString())
                    .tree(tree.render(states)).step();
            level = next;
        }

        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (!isPalindrome(row)) {
                emit.at("levelBreaksSymmetry")
                        .say("Level %d reads %s forwards and %s backwards - a mirror would have "
                                        + "to leave it unchanged, and it does not. The tree is "
                                        + "not symmetric.",
                                i, row, reversed(row))
                        .var("level", i).var("row", row.toString()).var("answer", false)
                        .tree(tree.render(states)).step();
                emit.at("done")
                        .say("Stopped at the first level that fails: the tree is NOT symmetric.")
                        .var("answer", false)
                        .tree(tree.render(states)).step();
                return;
            }
            emit.at("levelMirrors")
                    .say("Level %d reads %s in both directions, so this level is unchanged by a "
                            + "mirror.", i, row)
                    .var("level", i).var("row", row.toString()).var("answer", true)
                    .tree(tree.render(states)).step();
        }

        emit.at("done")
                .say("Every level is a palindrome across its full width, so the tree IS "
                        + "symmetric.")
                .var("answer", true)
                .tree(tree.render(states)).step();
    }

    private boolean isPalindrome(List<String> row) {
        for (int i = 0, j = row.size() - 1; i < j; i++, j--) {
            if (!row.get(i).equals(row.get(j))) {
                return false;
            }
        }
        return true;
    }

    private List<String> reversed(List<String> row) {
        List<String> out = new ArrayList<>(row);
        java.util.Collections.reverse(out);
        return out;
    }
}
