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
 * Preorder without recursion: an explicit {@link Deque} the algorithm pushes and pops
 * itself, so the "stack" is a visible object with contents rather than the JVM's hidden
 * call frames.
 *
 * <p>The one counter-intuitive line is that the RIGHT child is pushed before the left. A
 * stack hands back what went in last, so pushing right-then-left is what makes the left
 * child come out first - the same left-before-right order the recursive version gets for
 * free from statement order.
 */
@Component
public class IterativePreorderTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "iterative-preorder";
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
                        .defaultValue(Arrays.asList(8, 3, 10, 1, 6, null, 14))
                        .build());
    }

    /** A right-leaning chain: nothing is ever pushed as a left child, and the stack never holds more than one node. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, null, 2, null, null, null, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> preorder(TreeNode root) {
                   List<Integer> out = new ArrayList<>();
                   if (root == null) return out;

                   // @a seed
                   Deque<TreeNode> stack = new ArrayDeque<>();
                   stack.push(root);

                   while (!stack.isEmpty()) {
                       // @a pop
                       TreeNode node = stack.pop();
                       // @a visit
                       out.add(node.val);

                       if (node.right != null) {
                           // @a pushRight
                           stack.push(node.right);   // pushed first, so it comes out last
                       }
                       if (node.left != null) {
                           // @a pushLeft
                           stack.push(node.left);    // pushed last, so it comes out first
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
        stack.push(tree.root());
        states.put(tree.root(), "queued");

        emit.at("seed")
                .say("Seed the explicit stack with the root %d. No recursion from here on - "
                        + "this Deque is the only thing remembering where to go back to.",
                        tree.value(tree.root()))
                .var("output", out).var("stack", bottomUp(tree, stack))
                .tree(tree.render(states)).step();

        while (!stack.isEmpty()) {
            int node = stack.pop();
            states.put(node, "visiting");
            emit.at("pop")
                    .say("Pop %d off the top of the stack. The stack now holds %s.",
                            tree.value(node), describe(bottomUp(tree, stack)))
                    .var("node", tree.value(node)).var("stack", bottomUp(tree, stack))
                    .tree(tree.render(states)).step();

            out.add(tree.value(node));
            states.put(node, "visited");
            emit.at("visit")
                    .say("Visit %d immediately, before either child is looked at - that is what "
                            + "makes this preorder.", tree.value(node))
                    .var("output", out).var("stack", bottomUp(tree, stack))
                    .tree(tree.render(states)).step();

            Integer right = tree.right(node);
            if (right != null) {
                stack.push(right);
                states.put(right, "queued");
                emit.at("pushRight")
                        .say("Push %d's RIGHT child %d first, so it ends up underneath the left "
                                        + "one and comes back out second.",
                                tree.value(node), tree.value(right))
                        .var("pushed", tree.value(right)).var("stack", bottomUp(tree, stack))
                        .tree(tree.render(states)).step();
            }

            Integer left = tree.left(node);
            if (left != null) {
                stack.push(left);
                states.put(left, "queued");
                emit.at("pushLeft")
                        .say("Push %d's LEFT child %d last, which puts it on top - so the very "
                                        + "next pop descends left.",
                                tree.value(node), tree.value(left))
                        .var("pushed", tree.value(left)).var("stack", bottomUp(tree, stack))
                        .tree(tree.render(states)).step();
            }
        }

        emit.at("done")
                .say("Stack empty, traversal finished: %s.", out)
                .var("output", out).var("stack", "[]")
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

    private String describe(List<Integer> stack) {
        return stack.isEmpty() ? "nothing" : stack + " (top on the right)";
    }
}
