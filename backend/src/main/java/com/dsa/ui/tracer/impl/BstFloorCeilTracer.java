package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Floor (largest value &lt;= target) and ceil (smallest value &gt;= target) fall out of the
 * same single walk down the BST: every node smaller than the target is a floor candidate
 * only until a bigger one is found further right, and every node bigger is a ceil candidate
 * only until a smaller one is found further left - the last candidate kept when the walk
 * runs out of tree is the answer.
 */
@Component
public class BstFloorCeilTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bst-floor-ceil";
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
                        .help("A BST, level order, with null where a child is absent.")
                        .length(1, 31).values(-99, 99)
                        .defaultValue(Arrays.asList(5, 3, 8, 1, 4, 7, 9))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target")
                        .range(-99, 99)
                        .defaultValue(6)
                        .build());
    }

    /** A target that exists in the tree, so the walk ends on an exact match instead of running out. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("target", 4);
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] floorCeil(TreeNode root, int target) {
                   int floor = -1, ceil = -1;
                   TreeNode curr = root;

                   while (curr != null) {
                       if (curr.val == target) {
                           // @a exactMatch
                           floor = curr.val;
                           ceil = curr.val;
                           break;
                       } else if (curr.val < target) {
                           // @a updateFloor
                           floor = curr.val;
                           curr = curr.right;
                       } else {
                           // @a updateCeil
                           ceil = curr.val;
                           curr = curr.left;
                       }
                   }
                   // @a done
                   return new int[]{floor, ceil};
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        int target = in.getInt("target");
        Map<Integer, String> states = new LinkedHashMap<>();

        int floor = -1;
        int ceil = -1;
        Integer curr = tree.isEmpty() ? null : tree.root();

        while (curr != null) {
            states.put(curr, "visiting");
            int val = tree.value(curr);
            if (val == target) {
                floor = val;
                ceil = val;
                states.put(curr, "target");
                emit.at("exactMatch")
                        .say("%d is in the tree - it is its own floor and ceil.", val)
                        .var("floor", floor).var("ceil", ceil)
                        .tree(tree.render(states)).step();
                break;
            } else if (val < target) {
                floor = val;
                emit.at("updateFloor")
                        .say("%d < %d - it's a candidate floor, but a closer one might be further right.",
                                val, target)
                        .var("floor", floor).var("ceil", ceil == -1 ? "none yet" : ceil)
                        .tree(tree.render(states)).step();
                states.put(curr, "visited");
                curr = tree.right(curr);
            } else {
                ceil = val;
                emit.at("updateCeil")
                        .say("%d > %d - it's a candidate ceil, but a closer one might be further left.",
                                val, target)
                        .var("floor", floor == -1 ? "none yet" : floor).var("ceil", ceil)
                        .tree(tree.render(states)).step();
                states.put(curr, "visited");
                curr = tree.left(curr);
            }
        }

        emit.at("done")
                .say("Floor of %d is %s, ceil is %s.", target,
                        floor == -1 ? "none" : String.valueOf(floor),
                        ceil == -1 ? "none" : String.valueOf(ceil))
                .var("floor", floor).var("ceil", ceil)
                .tree(tree.render(states)).step();
    }
}
