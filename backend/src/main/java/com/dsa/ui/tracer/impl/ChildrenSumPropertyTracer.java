package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.TreeNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The one problem in this batch that CHANGES the tree rather than measuring it: every
 * non-leaf node must end up holding exactly the sum of its children, and values may only be
 * increased.
 *
 * <p>That "only increased" rule is what forces two passes over each node. On the way down,
 * whichever of the node and its children's sum is larger is copied to the other side - if
 * the children already out-total the parent the parent is raised, otherwise each child is
 * raised to the parent's value, which can only ever add. On the way back up the parent is
 * set to the real sum of whatever its children finally became, which is what makes the fix
 * stick after the subtrees below have been rewritten by their own descents.
 */
@Component
public class ChildrenSumPropertyTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "children-sum-property";
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
                        .length(1, 31).values(0, 99)
                        .defaultValue(Arrays.asList(20, 7, 2, 3, 5, 1, 30))
                        .build());
    }

    /** A node with a single child, where the missing side contributes 0 to the sum rather than being invented. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(10, 4, null, 2, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public TreeNode childrenSum(TreeNode root) {
                   changeTree(root);
                   // @a done
                   return root;
               }

               private void changeTree(TreeNode node) {
                   if (node == null) return;
                   if (node.left == null && node.right == null) {
                       // @a leafIsAlreadyValid
                       return;                       // a leaf has no children to sum
                   }

                   int childSum = value(node.left) + value(node.right);
                   if (childSum >= node.val) {
                       // @a raiseParent
                       node.val = childSum;
                   } else {
                       // @a raiseChildren
                       if (node.left != null) node.left.val = node.val;
                       if (node.right != null) node.right.val = node.val;
                   }

                   changeTree(node.left);
                   changeTree(node.right);

                   // @a settleOnTheWayBack
                   node.val = value(node.left) + value(node.right);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Integer[] values = in.getBinaryTree("tree");
        BinaryTreeLayout shape = new BinaryTreeLayout(values);
        Map<Integer, String> states = new LinkedHashMap<>();

        if (shape.isEmpty()) {
            emit.at("done")
                    .say("An empty tree trivially satisfies the children-sum property.")
                    .var("tree", "[]").tree(shape.render(states)).step();
            return;
        }

        change(shape, values, shape.root(), states, emit);

        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                states.put(i, "done");
            }
        }
        emit.at("done")
                .say("Every non-leaf node now holds the sum of its children, and no value was "
                        + "ever lowered along the way.")
                .var("tree", levelOrder(values))
                .tree(render(values, states)).step();
    }

    private void change(BinaryTreeLayout shape, Integer[] values, int index,
                        Map<Integer, String> states, StepEmitter emit) {
        Integer left = shape.left(index);
        Integer right = shape.right(index);

        emit.push("changeTree(" + values[index] + ")");
        states.put(index, "visiting");

        if (left == null && right == null) {
            states.put(index, "visited");
            emit.at("leafIsAlreadyValid")
                    .say("%d is a leaf. It has no children to sum, so it is left exactly as it "
                            + "is - leaves are the only values the algorithm never rewrites.",
                            values[index])
                    .var("node", values[index]).var("tree", levelOrder(values))
                    .tree(render(values, states)).step();
            emit.pop();
            return;
        }

        int childSum = valueOf(values, left) + valueOf(values, right);
        if (childSum >= values[index]) {
            int before = values[index];
            values[index] = childSum;
            emit.at("raiseParent")
                    .say("%d's children already total %d, which is not less than %d. Raise the "
                                    + "parent to %d - raising the children instead would mean "
                                    + "lowering something later.",
                            before, childSum, before, childSum)
                    .var("childSum", childSum).var("node", values[index])
                    .var("tree", levelOrder(values))
                    .tree(render(values, states)).step();
        } else {
            int parent = values[index];
            if (left != null) {
                values[left] = parent;
            }
            if (right != null) {
                values[right] = parent;
            }
            emit.at("raiseChildren")
                    .say("%d's children only total %d, short of %d. Copy %d down into every "
                                    + "child - overshooting is safe because the way back up "
                                    + "will pull each parent back to the real sum.",
                            parent, childSum, parent, parent)
                    .var("childSum", childSum).var("node", parent)
                    .var("tree", levelOrder(values))
                    .tree(render(values, states)).step();
        }

        if (left != null) {
            change(shape, values, left, states, emit);
        }
        if (right != null) {
            change(shape, values, right, states, emit);
        }

        int settled = valueOf(values, left) + valueOf(values, right);
        values[index] = settled;
        states.put(index, "visited");
        emit.at("settleOnTheWayBack")
                .say("Both subtrees below this node are final now. Set it to what its "
                                + "children actually became: %s = %d.",
                        describe(values, left, right), settled)
                .var("node", settled).var("tree", levelOrder(values))
                .tree(render(values, states)).step();

        emit.pop();
    }

    private String describe(Integer[] values, Integer left, Integer right) {
        if (left != null && right != null) {
            return values[left] + " + " + values[right];
        }
        if (left != null) {
            return values[left] + " + 0";
        }
        return "0 + " + values[right];
    }

    private int valueOf(Integer[] values, Integer index) {
        return index == null ? 0 : values[index];
    }

    private String levelOrder(Integer[] values) {
        return Arrays.toString(values);
    }

    private List<TreeNode> render(Integer[] values, Map<Integer, String> states) {
        return new BinaryTreeLayout(values).render(states);
    }
}
