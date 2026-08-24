package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Postorder traversal — children finish before the root is written, which preorder and
 * inorder never show. The narration keeps pointing at WHY the node waits: its whole
 * right subtree still owes work.
 */
@Component
public class TreePostorderTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "tree-postorder";
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

    /** A fuller tree — two grandchildren on each side, so the deferred visits stack up. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(5, 3, 8, 1, null, 7, 9));
    }

    @Override
    public String annotatedCode() {
        return """
               public void postorder(Node node, List<Integer> out) {
                   // @a base
                   if (node == null) return;
                   // @a left
                   postorder(node.left, out);
                   // @a right
                   postorder(node.right, out);
                   // @a visit
                   out.add(node.val);
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

        emit.at("base").say("Traversal complete. Postorder writes every child before its parent: %s.", output)
                .var("output", output).tree(tree.render(states)).step();
    }

    private void walk(BinaryTreeLayout tree, Integer index, Map<Integer, String> states,
                      List<Integer> output, StepEmitter emit) {
        if (index == null) {
            return;
        }
        emit.push("postorder(" + tree.value(index) + ")");
        states.put(index, "visiting");

        Integer left = tree.left(index);
        if (left != null) {
            emit.at("left").say("%d cannot be written yet — first everything under its left child %d.",
                            tree.value(index), tree.value(left))
                    .var("output", output).tree(tree.render(states)).step();
            walk(tree, left, states, output, emit);
        }

        Integer right = tree.right(index);
        if (right != null) {
            emit.at("right").say("Left side of %d is done. The right subtree %d still comes first.",
                            tree.value(index), tree.value(right))
                    .var("output", output).tree(tree.render(states)).step();
            walk(tree, right, states, output, emit);
        }

        emit.at("visit").say("Both subtrees of %d are exhausted — only NOW is it written.", tree.value(index))
                .var("node", tree.value(index)).var("output", output)
                .tree(tree.render(states)).step();

        output.add(tree.value(index));
        states.put(index, "visited");

        emit.pop();
    }
}
