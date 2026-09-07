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
 * All three orders from one walk, using an explicit stack of (node, state) pairs.
 *
 * <p>A node is pushed once and inspected three times. State 1 is the first look, so its
 * value goes to preorder and the left child is pushed. State 2 is the look after the left
 * subtree finished, so its value goes to inorder and the right child is pushed. State 3 is
 * the look after both children finished, so its value goes to postorder and the node is
 * finally popped. Three orders, one push per node, one pop per node.
 *
 * <p>Its recursive twin is {@code pre-post-in-one-traversal}: the state number here is
 * exactly which of the three hook points a recursive call would be sitting at.
 */
@Component
public class TraversalsInOnePassTracer implements AlgorithmTracer {

    private static final class Frame {
        final int index;
        int state;

        Frame(int index) {
            this.index = index;
            this.state = 1;
        }
    }

    @Override
    public String id() {
        return "traversals-in-one-pass";
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
                        .defaultValue(Arrays.asList(1, 2, 3, 4, 5, 6, 7))
                        .build());
    }

    /** A left-leaning tree with a missing right subtree, so several nodes go 1 -> 2 -> 3 without ever pushing a right child. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(8, 4, null, 2, 6));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> allThree(TreeNode root) {
                   List<Integer> pre = new ArrayList<>();
                   List<Integer> in = new ArrayList<>();
                   List<Integer> post = new ArrayList<>();
                   Deque<Frame> stack = new ArrayDeque<>();
                   // @a seed
                   if (root != null) stack.push(new Frame(root, 1));

                   while (!stack.isEmpty()) {
                       Frame top = stack.peek();
                       if (top.state == 1) {
                           // @a state1
                           pre.add(top.node.val);
                           top.state = 2;
                           if (top.node.left != null) stack.push(new Frame(top.node.left, 1));
                       } else if (top.state == 2) {
                           // @a state2
                           in.add(top.node.val);
                           top.state = 3;
                           if (top.node.right != null) stack.push(new Frame(top.node.right, 1));
                       } else {
                           // @a state3
                           post.add(top.node.val);
                           stack.pop();
                       }
                   }
                   // @a done
                   return List.of(pre, in, post);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();
        List<Integer> pre = new ArrayList<>();
        List<Integer> inorder = new ArrayList<>();
        List<Integer> post = new ArrayList<>();

        if (tree.isEmpty()) {
            emit.at("done").say("The tree is empty, so all three orders are empty.")
                    .var("preorder", "[]").var("inorder", "[]").var("postorder", "[]")
                    .tree(tree.render(states)).step();
            return;
        }

        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(tree.root()));
        states.put(tree.root(), "queued");

        emit.at("seed")
                .say("Push the root %d with state 1. Every node enters at state 1 and leaves "
                        + "at state 3, contributing to one order at each stop.",
                        tree.value(tree.root()))
                .var("preorder", pre).var("inorder", inorder).var("postorder", post)
                .var("stack", frames(tree, stack))
                .tree(tree.render(states)).step();

        while (!stack.isEmpty()) {
            Frame top = stack.peek();
            if (top.state == 1) {
                pre.add(tree.value(top.index));
                top.state = 2;
                states.put(top.index, "visiting");
                Integer left = tree.left(top.index);
                if (left != null) {
                    stack.push(new Frame(left));
                    states.put(left, "queued");
                    emit.at("state1")
                            .say("%d is at state 1 - first look. Its value joins PREORDER, its "
                                            + "state becomes 2, and its left child %d is pushed "
                                            + "at state 1.",
                                    tree.value(top.index), tree.value(left))
                            .var("preorder", pre).var("inorder", inorder).var("postorder", post)
                            .var("stack", frames(tree, stack))
                            .tree(tree.render(states)).step();
                } else {
                    emit.at("state1")
                            .say("%d is at state 1 - first look. Its value joins PREORDER and "
                                            + "its state becomes 2. There is no left child to "
                                            + "push, so the next look is at %d again.",
                                    tree.value(top.index), tree.value(top.index))
                            .var("preorder", pre).var("inorder", inorder).var("postorder", post)
                            .var("stack", frames(tree, stack))
                            .tree(tree.render(states)).step();
                }
            } else if (top.state == 2) {
                inorder.add(tree.value(top.index));
                top.state = 3;
                Integer right = tree.right(top.index);
                if (right != null) {
                    stack.push(new Frame(right));
                    states.put(right, "queued");
                    emit.at("state2")
                            .say("%d is back on top at state 2, so its left subtree is finished. "
                                            + "Its value joins INORDER, its state becomes 3, and "
                                            + "its right child %d is pushed.",
                                    tree.value(top.index), tree.value(right))
                            .var("preorder", pre).var("inorder", inorder).var("postorder", post)
                            .var("stack", frames(tree, stack))
                            .tree(tree.render(states)).step();
                } else {
                    emit.at("state2")
                            .say("%d is back on top at state 2, so its left subtree is finished. "
                                            + "Its value joins INORDER and its state becomes 3. "
                                            + "No right child to push.",
                                    tree.value(top.index))
                            .var("preorder", pre).var("inorder", inorder).var("postorder", post)
                            .var("stack", frames(tree, stack))
                            .tree(tree.render(states)).step();
                }
            } else {
                post.add(tree.value(top.index));
                stack.pop();
                states.put(top.index, "visited");
                emit.at("state3")
                        .say("%d reaches state 3 - both subtrees are done. Its value joins "
                                        + "POSTORDER and it is finally popped, never to be "
                                        + "looked at again.",
                                tree.value(top.index))
                        .var("preorder", pre).var("inorder", inorder).var("postorder", post)
                        .var("stack", frames(tree, stack))
                        .tree(tree.render(states)).step();
            }
        }

        emit.at("done")
                .say("One pass, one push and one pop per node: preorder %s, inorder %s, "
                        + "postorder %s.", pre, inorder, post)
                .var("preorder", pre).var("inorder", inorder).var("postorder", post)
                .var("stack", "[]")
                .tree(tree.render(states)).step();
    }

    /** Stack contents bottom-to-top as {@code value:state}, so the last entry printed is the top. */
    private List<String> frames(BinaryTreeLayout tree, Deque<Frame> stack) {
        List<String> out = new ArrayList<>(stack.size());
        for (Frame frame : stack) {
            out.add(0, tree.value(frame.index) + ":" + frame.state);
        }
        return out;
    }
}
