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
 * Postorder with one stack and no reversal buffer, which means the algorithm has to decide
 * for itself when a node is finished rather than letting a second stack decide mechanically.
 *
 * <p>The rule it uses: peek at the top node's right child. If there is one and it is not the
 * node we just came back from, descend into it. Otherwise the node's whole subtree is done,
 * so pop and output it - and then keep popping while each new top's right child is exactly
 * the node just emitted, because that means the parent's right side finished too. That
 * chain-unwind is the part the two-stack version never has to reason about.
 */
@Component
public class Postorder1StackTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "postorder-1-stack";
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
                        .defaultValue(Arrays.asList(12, 6, 18, 4, 9, 15, 21))
                        .build());
    }

    /** A tree whose root has no right child at all, so the unwind chain is driven entirely from the left subtree. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, 2, null, 3, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> postorder(TreeNode root) {
                   List<Integer> out = new ArrayList<>();
                   Deque<TreeNode> stack = new ArrayDeque<>();
                   TreeNode curr = root;

                   while (curr != null || !stack.isEmpty()) {
                       if (curr != null) {
                           // @a pushAndGoLeft
                           stack.push(curr);
                           curr = curr.left;
                       } else if (stack.peek().right != null) {
                           // @a descendRight
                           curr = stack.peek().right;
                       } else {
                           // @a subtreeFinished
                           TreeNode node = stack.pop();
                           out.add(node.val);
                           while (!stack.isEmpty() && stack.peek().right == node) {
                               // @a unwindParent
                               node = stack.pop();
                               out.add(node.val);
                           }
                       }
                   }
                   // @a done
                   return out;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();
        List<Integer> out = new ArrayList<>();

        if (tree.isEmpty()) {
            emit.at("done").say("The tree is empty, so the traversal is empty.")
                    .var("output", "[]").tree(tree.render(states)).step();
            return;
        }

        Deque<Integer> stack = new ArrayDeque<>();
        Integer curr = tree.root();

        while (curr != null || !stack.isEmpty()) {
            if (curr != null) {
                stack.push(curr);
                states.put(curr, "queued");
                Integer left = tree.left(curr);
                if (left == null) {
                    emit.at("pushAndGoLeft")
                            .say("Stack %d and step left - no left child, so curr goes null and "
                                            + "the next loop inspects the stack top instead.",
                                    tree.value(curr))
                            .var("output", out).var("stack", bottomUp(tree, stack))
                            .tree(tree.render(states)).step();
                } else {
                    emit.at("pushAndGoLeft")
                            .say("Stack %d and keep descending left into %d. Nothing is output "
                                            + "on the way down - postorder outputs last.",
                                    tree.value(curr), tree.value(left))
                            .var("output", out).var("stack", bottomUp(tree, stack))
                            .tree(tree.render(states)).step();
                }
                curr = left;
                continue;
            }

            int top = stack.peek();
            Integer right = tree.right(top);
            if (right != null) {
                emit.at("descendRight")
                        .say("%d is on top and still has an unexplored right child %d - leave "
                                        + "%d on the stack and descend into it.",
                                tree.value(top), tree.value(right), tree.value(top))
                        .var("output", out).var("stack", bottomUp(tree, stack))
                        .tree(tree.render(states)).step();
                curr = right;
                continue;
            }

            int node = stack.pop();
            out.add(tree.value(node));
            states.put(node, "visited");
            emit.at("subtreeFinished")
                    .say("%d has nothing left below it - both sides are done, so pop it and "
                                    + "output it now.",
                            tree.value(node))
                    .var("node", tree.value(node))
                    .var("output", out).var("stack", bottomUp(tree, stack))
                    .tree(tree.render(states)).step();

            while (!stack.isEmpty() && isRightChildOfTop(tree, stack, node)) {
                int parent = stack.pop();
                out.add(tree.value(parent));
                states.put(parent, "visited");
                emit.at("unwindParent")
                        .say("%d was %d's right child, so %d's right side has just finished too "
                                        + "- pop and output %d as well.",
                                tree.value(node), tree.value(parent), tree.value(parent),
                                tree.value(parent))
                        .var("node", tree.value(parent))
                        .var("output", out).var("stack", bottomUp(tree, stack))
                        .tree(tree.render(states)).step();
                node = parent;
            }
        }

        emit.at("done")
                .say("curr is null and the stack is empty - postorder result: %s.", out)
                .var("output", out)
                .tree(tree.render(states)).step();
    }

    private boolean isRightChildOfTop(BinaryTreeLayout tree, Deque<Integer> stack, int node) {
        Integer right = tree.right(stack.peek());
        return right != null && right == node;
    }

    /** Stack contents bottom-to-top, so the last entry printed is the top. */
    private List<Integer> bottomUp(BinaryTreeLayout tree, Deque<Integer> stack) {
        List<Integer> values = new ArrayList<>(stack.size());
        for (int index : stack) {
            values.add(0, tree.value(index));
        }
        return values;
    }
}
