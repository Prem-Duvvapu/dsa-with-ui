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
 * Postorder by running a MODIFIED preorder and reversing the answer, using a second stack
 * as the reversal device.
 *
 * <p>Stack one produces root, right, left - preorder with the children swapped. Reverse
 * that and you have left, right, root, which is postorder. Nothing here decides when a
 * node is "finished"; the second stack does that mechanically, which is why this version
 * is so much easier to get right than the one-stack form and why it costs O(n) extra space
 * to do it.
 */
@Component
public class Postorder2StacksTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "postorder-2-stacks";
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
                        .defaultValue(Arrays.asList(2, 7, 5, 9, 6, null, 11))
                        .build());
    }

    /** A left-only chain, where the root-right-left order collapses to plain root-left and the reversal is all the work. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(3, 2, null, 1));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> postorder(TreeNode root) {
                   List<Integer> out = new ArrayList<>();
                   if (root == null) return out;

                   Deque<TreeNode> main = new ArrayDeque<>();
                   Deque<TreeNode> collected = new ArrayDeque<>();
                   // @a seed
                   main.push(root);

                   while (!main.isEmpty()) {
                       // @a moveAcross
                       TreeNode node = main.pop();
                       collected.push(node);

                       if (node.left != null) {
                           // @a pushLeft
                           main.push(node.left);     // popped second
                       }
                       if (node.right != null) {
                           // @a pushRight
                           main.push(node.right);    // popped first
                       }
                   }

                   while (!collected.isEmpty()) {
                       // @a drain
                       out.add(collected.pop().val);
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

        Deque<Integer> main = new ArrayDeque<>();
        Deque<Integer> collected = new ArrayDeque<>();
        main.push(tree.root());
        states.put(tree.root(), "queued");

        emit.at("seed")
                .say("Seed stack one with the root %d. Stack two stays empty for now - it only "
                        + "ever receives what stack one hands over.", tree.value(tree.root()))
                .var("stack1", bottomUp(tree, main)).var("stack2", bottomUp(tree, collected))
                .tree(tree.render(states)).step();

        while (!main.isEmpty()) {
            int node = main.pop();
            collected.push(node);
            states.put(node, "visiting");

            emit.at("moveAcross")
                    .say("Pop %d from stack one and push it straight onto stack two. Nothing is "
                                    + "output yet - stack two is being loaded in the exact "
                                    + "reverse of the order the answer needs.",
                            tree.value(node))
                    .var("moved", tree.value(node))
                    .var("stack1", bottomUp(tree, main)).var("stack2", bottomUp(tree, collected))
                    .tree(tree.render(states)).step();

            Integer left = tree.left(node);
            if (left != null) {
                main.push(left);
                states.put(left, "queued");
                emit.at("pushLeft")
                        .say("Push %d's LEFT child %d onto stack one first, so it is popped "
                                        + "second and lands deeper in stack two.",
                                tree.value(node), tree.value(left))
                        .var("pushed", tree.value(left))
                        .var("stack1", bottomUp(tree, main)).var("stack2", bottomUp(tree, collected))
                        .tree(tree.render(states)).step();
            }

            Integer right = tree.right(node);
            if (right != null) {
                main.push(right);
                states.put(right, "queued");
                emit.at("pushRight")
                        .say("Push %d's RIGHT child %d last, so it is popped first - stack one "
                                        + "walks root, right, left.",
                                tree.value(node), tree.value(right))
                        .var("pushed", tree.value(right))
                        .var("stack1", bottomUp(tree, main)).var("stack2", bottomUp(tree, collected))
                        .tree(tree.render(states)).step();
            }
        }

        boolean firstDrain = true;
        while (!collected.isEmpty()) {
            int node = collected.pop();
            out.add(tree.value(node));
            states.put(node, "visited");
            if (firstDrain) {
                emit.at("drain")
                        .say("Stack one is exhausted. Popping stack two now reads its "
                                        + "root-right-left loading order backwards, which is "
                                        + "left-right-root - postorder. First out: %d.",
                                tree.value(node))
                        .var("output", out).var("stack2", bottomUp(tree, collected))
                        .tree(tree.render(states)).step();
                firstDrain = false;
            } else {
                emit.at("drain")
                        .say("Pop %d off stack two - postorder so far is %s.",
                                tree.value(node), out)
                        .var("output", out).var("stack2", bottomUp(tree, collected))
                        .tree(tree.render(states)).step();
            }
        }

        emit.at("done")
                .say("Both stacks empty - postorder result: %s.", out)
                .var("output", out)
                .tree(tree.render(states)).step();
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
