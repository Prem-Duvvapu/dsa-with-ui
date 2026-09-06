package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A BST insert never needs to look past a null child: the value's own comparisons against
 * every node on the way down already say which side it belongs on, so the first empty slot
 * it reaches is the only place preserving the ordering property allows it to go.
 */
@Component
public class BstInsertTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bst-insert";
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
                InputField.of("val", FieldType.INT)
                        .label("Value to insert")
                        .range(-99, 99)
                        .defaultValue(6)
                        .build());
    }

    /** A different tree and a value that lands via the mirror-image path (left, right, right). */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "tree", Arrays.asList(10, 5, 15, 3, 7),
                "val", 8);
    }

    @Override
    public String annotatedCode() {
        return """
               public TreeNode insertIntoBST(TreeNode root, int val) {
                   TreeNode curr = root;
                   while (true) {
                       if (val < curr.val) {
                           // @a compareLeft
                           if (curr.left == null) {
                               // @a attachLeft
                               curr.left = new TreeNode(val);
                               break;
                           }
                           curr = curr.left;
                       } else {
                           // @a compareRight
                           if (curr.right == null) {
                               // @a attachRight
                               curr.right = new TreeNode(val);
                               break;
                           }
                           curr = curr.right;
                       }
                   }
                   // @a done
                   return root;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Integer[] treeArr = in.getBinaryTree("tree");
        int val = in.getInt("val");
        MutableBst bst = new MutableBst();
        MutableBst.Node root = bst.build(treeArr);
        Map<MutableBst.Node, String> states = new LinkedHashMap<>();

        MutableBst.Node curr = root;
        while (true) {
            states.put(curr, "visiting");
            if (val < curr.val) {
                if (curr.left == null) {
                    MutableBst.Node created = bst.newNode(val);
                    curr.left = created;
                    states.put(created, "target");
                    emit.at("attachLeft")
                            .say("%d < %d and %d has no left child - attach %d there.",
                                    val, curr.val, curr.val, val)
                            .var("inserted", val).var("parent", curr.val)
                            .tree(bst.render(root, states)).step();
                    break;
                }
                emit.at("compareLeft")
                        .say("%d < %d - %d belongs in %d's left subtree.", val, curr.val, val, curr.val)
                        .var("current", curr.val).var("value", val)
                        .tree(bst.render(root, states)).step();
                states.put(curr, "visited");
                curr = curr.left;
            } else {
                if (curr.right == null) {
                    MutableBst.Node created = bst.newNode(val);
                    curr.right = created;
                    states.put(created, "target");
                    emit.at("attachRight")
                            .say("%d >= %d and %d has no right child - attach %d there.",
                                    val, curr.val, curr.val, val)
                            .var("inserted", val).var("parent", curr.val)
                            .tree(bst.render(root, states)).step();
                    break;
                }
                emit.at("compareRight")
                        .say("%d >= %d - %d belongs in %d's right subtree.", val, curr.val, val, curr.val)
                        .var("current", curr.val).var("value", val)
                        .tree(bst.render(root, states)).step();
                states.put(curr, "visited");
                curr = curr.right;
            }
        }

        emit.at("done")
                .say("%d inserted - the BST ordering property holds everywhere.", val)
                .tree(bst.render(root, states)).step();
    }
}
