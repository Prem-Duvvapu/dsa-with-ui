package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Inorder traversal — visibly different from preorder, which was not previously true. */
@Component
public class TreeInorderTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "tree-inorder";
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("tree", FieldType.BINARY_TREE)
                        .label("Tree (level order)")
                        .help("Level order, with null where a child is absent.")
                        .length(1, 31).values(-99, 99)
                        .defaultValue(TreePreorderTracer.DEFAULT_TREE)
                        .build());
    }

    /** A different shape and a different visit order. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(10, 20, 30, 40));
    }

    @Override
    public String annotatedCode() {
        return """
               public void inorder(Node node, List<Integer> out) {
                   // @a base
                   if (node == null) return;
                   // @a left
                   inorder(node.left, out);
                   // @a visit
                   out.add(node.val);
                   // @a right
                   inorder(node.right, out);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        emit.using("Tree");

        if (tree.isEmpty()) {
            emit.at("base").say("The tree is empty, so the traversal is empty.")
                    .var("output", "[]").tree(tree.render(Map.of())).step();
            return;
        }

        Map<Integer, String> states = new LinkedHashMap<>();
        List<Integer> output = new ArrayList<>();
        walk(tree, tree.root(), states, output, emit);

        emit.at("base").say("Traversal complete. Inorder visits left, then root, then right: %s.", output)
                .var("output", output).tree(tree.render(states)).step();
    }

    private void walk(BinaryTreeLayout tree, Integer index, Map<Integer, String> states,
                      List<Integer> output, StepEmitter emit) {
        if (index == null) {
            return;
        }
        emit.push("inorder(" + tree.value(index) + ")");
        states.put(index, "visiting");

        Integer left = tree.left(index);
        if (left != null) {
            emit.at("left").say("Before recording %d, its entire left subtree must be visited. Descend to %d.",
                            tree.value(index), tree.value(left))
                    .var("output", output).tree(tree.render(states)).step();
            walk(tree, left, states, output, emit);
        }

        emit.at("visit").say("Left subtree of %d is exhausted, so now record %d.",
                        tree.value(index), tree.value(index))
                .var("node", tree.value(index)).var("output", output)
                .tree(tree.render(states)).step();

        output.add(tree.value(index));
        states.put(index, "visited");

        Integer right = tree.right(index);
        if (right != null) {
            emit.at("right").say("Now descend into %d's right child, %d.",
                            tree.value(index), tree.value(right))
                    .var("output", output).tree(tree.render(states)).step();
            walk(tree, right, states, output, emit);
        }

        emit.pop();
    }
}
