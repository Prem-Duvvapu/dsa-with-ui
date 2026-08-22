package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preorder traversal, traced for real.
 *
 * <p>The Binary Trees category holds 54 problems and previously served all of them from
 * one three-step hardcoded narration, so preorder, inorder, postorder and level order
 * were literally the same animation.
 */
@Component
public class TreePreorderTracer implements AlgorithmTracer {

    static final List<Integer> DEFAULT_TREE = Arrays.asList(1, 2, 3, 4, 5, null, 6);

    @Override
    public String id() {
        return "tree-preorder";
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("tree", FieldType.BINARY_TREE)
                        .label("Tree (level order)")
                        .help("Level order, with null where a child is absent.")
                        .length(1, 31).values(-99, 99)
                        .defaultValue(DEFAULT_TREE)
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
               public void preorder(Node node, List<Integer> out) {
                   // @a base
                   if (node == null) return;
                   // @a visit
                   out.add(node.val);
                   // @a left
                   preorder(node.left, out);
                   // @a right
                   preorder(node.right, out);
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

        emit.at("base").say("Traversal complete. Preorder visits root, then left, then right: %s.", output)
                .var("output", output).tree(tree.render(states)).step();
    }

    private void walk(BinaryTreeLayout tree, Integer index, Map<Integer, String> states,
                      List<Integer> output, StepEmitter emit) {
        if (index == null) {
            return;
        }
        emit.push("preorder(" + tree.value(index) + ")");
        states.put(index, "visiting");

        emit.at("visit").say("Visit %d before either subtree — that is what makes this preorder.",
                        tree.value(index))
                .var("node", tree.value(index)).var("output", output)
                .tree(tree.render(states)).step();

        output.add(tree.value(index));
        states.put(index, "visited");

        Integer left = tree.left(index);
        if (left != null) {
            emit.at("left").say("Descend into %d's left child, %d.", tree.value(index), tree.value(left))
                    .var("output", output).tree(tree.render(states)).step();
            walk(tree, left, states, output, emit);
        }

        Integer right = tree.right(index);
        if (right != null) {
            emit.at("right").say("Left subtree of %d is done. Descend right into %d.",
                            tree.value(index), tree.value(right))
                    .var("output", output).tree(tree.render(states)).step();
            walk(tree, right, states, output, emit);
        }

        emit.pop();
    }
}
