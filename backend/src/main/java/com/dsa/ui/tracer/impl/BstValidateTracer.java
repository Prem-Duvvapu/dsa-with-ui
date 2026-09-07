package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validating a BST by carrying a permitted RANGE down the tree, rather than by comparing
 * each node to its own two children.
 *
 * <p>The local check is the classic wrong answer, and LeetCode 98's own second example is
 * built to defeat it: in {@code [5,1,4,null,null,3,6]} every parent is genuinely larger
 * than its left child and smaller than its right, yet 3 sits in 5's RIGHT subtree while
 * being smaller than 5. A node is not constrained by its parent; it is constrained by every
 * ancestor at once.
 *
 * <p>Descending left tightens the upper bound to the current value and descending right
 * tightens the lower bound, so by the time the walk reaches a node the interval it must
 * fall inside already encodes every turn taken to get there. An open bound stays null
 * rather than being faked with a sentinel, because a real tree may legitimately hold the
 * smallest or largest representable value.
 */
@Component
public class BstValidateTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bst-validate";
    }

    @Override
    public DsType dsType() {
        return DsType.TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("tree", FieldType.BINARY_TREE)
                        // The label does not promise a BST: deciding whether it is one is the
                        // job. bstOrdered() here is only the contract test's growth hint - see
                        // InputField.Builder.bstOrdered and RCA-021.
                        .label("Tree (level order)")
                        .help("Any binary tree in level order, with null where a child is "
                                + "absent. Whether it is a BST is the question.")
                        .length(1, 31).values(-99, 99).bstOrdered()
                        .defaultValue(Arrays.asList(5, 1, 4, null, null, 3, 6))
                        .build());
    }

    /** LeetCode 98's first example, a genuine BST, so no node ever falls outside its inherited range. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(2, 1, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isValidBST(TreeNode root) {
                   boolean valid = check(root, null, null);
                   // @a done
                   return valid;
               }

               private boolean check(TreeNode node, Integer low, Integer high) {
                   if (node == null) {
                       // @a emptyIsValid
                       return true;
                   }
                   if ((low != null && node.val <= low) || (high != null && node.val >= high)) {
                       // @a outOfRange
                       return false;
                   }
                   // @a narrowLeft
                   if (!check(node.left, low, node.val)) return false;
                   // @a narrowRight
                   return check(node.right, node.val, high);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();

        boolean valid = tree.isEmpty() || check(tree, tree.root(), null, null, states, emit);

        if (valid) {
            emit.at("done")
                    .say("Every node fell inside the range its ancestors handed it, so the tree "
                            + "IS a valid BST.")
                    .var("answer", true)
                    .tree(tree.render(states)).step();
        } else {
            emit.at("done")
                    .say("A node broke the range its ancestors imposed, so the tree is NOT a "
                            + "valid BST - and note that a parent-versus-children check would "
                            + "not have noticed.")
                    .var("answer", false)
                    .tree(tree.render(states)).step();
        }
    }

    private boolean check(BinaryTreeLayout tree, Integer index, Integer low, Integer high,
                          Map<Integer, String> states, StepEmitter emit) {
        if (index == null) {
            emit.at("emptyIsValid")
                    .say("An empty branch imposes nothing and breaks nothing, so it is valid "
                            + "for the range %s.", range(low, high))
                    .var("range", range(low, high)).var("verdict", "valid")
                    .tree(tree.render(states)).step();
            return true;
        }

        int value = tree.value(index);
        emit.push("check(" + value + ", " + range(low, high) + ")");

        if ((low != null && value <= low) || (high != null && value >= high)) {
            states.put(index, "target");
            emit.at("outOfRange")
                    .say("%d must lie strictly inside %s to be where it is - the turns taken "
                                    + "from the root demand it - and it does not. This is the "
                                    + "violation.",
                            value, range(low, high))
                    .var("range", range(low, high)).var("verdict", "violation at " + value)
                    .tree(tree.render(states)).step();
            emit.pop();
            return false;
        }

        states.put(index, "visiting");
        emit.at("narrowLeft")
                .say("%d is inside %s, so it is legal here. Going LEFT tightens the upper bound "
                                + "to %d: everything down there must be smaller than %d AND "
                                + "still satisfy every earlier bound.",
                        value, range(low, high), value, value)
                .var("range", range(low, value)).var("verdict", "ok so far")
                .tree(tree.render(states)).step();

        if (!check(tree, tree.left(index), low, value, states, emit)) {
            emit.pop();
            return false;
        }

        emit.at("narrowRight")
                .say("%d's left side is clean. Going RIGHT tightens the lower bound to %d "
                                + "instead, leaving %s.",
                        value, value, range(value, high))
                .var("range", range(value, high)).var("verdict", "ok so far")
                .tree(tree.render(states)).step();

        boolean rightOk = check(tree, tree.right(index), value, high, states, emit);
        if (rightOk) {
            states.put(index, "visited");
        }
        emit.pop();
        return rightOk;
    }

    private String range(Integer low, Integer high) {
        String lower = low == null ? "(-inf" : "(" + low;
        String upper = high == null ? "+inf)" : high + ")";
        return lower + ", " + upper;
    }
}
