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
 * Inorder without recursion, and structurally unlike the iterative preorder: there is no
 * "push both children" step at all. A {@code curr} pointer runs as far left as it can,
 * stacking every node it passes as a promise to come back; when it runs out of tree the
 * stack gives back the deepest unfinished node, which is visited and then handed its right
 * subtree to repeat the whole thing on.
 *
 * <p>A node therefore sits on the stack across two separate visits to it - once on the way
 * down, once on the way back - which is exactly the state a recursive call frame was
 * holding invisibly.
 */
@Component
public class IterativeInorderTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "iterative-inorder";
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
                        .defaultValue(Arrays.asList(4, 2, 7, 1, 3, 6, 9))
                        .build());
    }

    /** A pure left chain: the stack fills to full depth before a single node is visited. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(5, 4, null, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> inorder(TreeNode root) {
                   List<Integer> out = new ArrayList<>();
                   Deque<TreeNode> stack = new ArrayDeque<>();
                   TreeNode curr = root;

                   while (curr != null || !stack.isEmpty()) {
                       if (curr != null) {
                           // @a pushAndGoLeft
                           stack.push(curr);          // remember to come back to it
                           curr = curr.left;
                       } else {
                           // @a popVisit
                           curr = stack.pop();
                           out.add(curr.val);
                           // @a goRight
                           curr = curr.right;         // its left side is finished
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
                            .say("Stack %d and step left - there is no left child, so curr "
                                            + "becomes null and the descent stops here.",
                                    tree.value(curr))
                            .var("stacked", tree.value(curr))
                            .var("output", out).var("stack", bottomUp(tree, stack))
                            .tree(tree.render(states)).step();
                } else {
                    emit.at("pushAndGoLeft")
                            .say("Stack %d as unfinished business and keep going left into %d. "
                                            + "Nothing is visited on the way down.",
                                    tree.value(curr), tree.value(left))
                            .var("stacked", tree.value(curr))
                            .var("output", out).var("stack", bottomUp(tree, stack))
                            .tree(tree.render(states)).step();
                }
                curr = left;
            } else {
                curr = stack.pop();
                out.add(tree.value(curr));
                states.put(curr, "visited");
                emit.at("popVisit")
                        .say("Ran out of left. Pop %d - the deepest node whose left side is "
                                        + "finished - and visit it now.",
                                tree.value(curr))
                        .var("node", tree.value(curr))
                        .var("output", out).var("stack", bottomUp(tree, stack))
                        .tree(tree.render(states)).step();

                Integer right = tree.right(curr);
                if (right == null) {
                    emit.at("goRight")
                            .say("%d has no right subtree, so curr goes null again and the next "
                                            + "loop pops from the stack instead.",
                                    tree.value(curr))
                            .var("output", out).var("stack", bottomUp(tree, stack))
                            .tree(tree.render(states)).step();
                } else {
                    emit.at("goRight")
                            .say("Hand curr %d's right subtree, rooted at %d, and run the same "
                                            + "left-descent on it.",
                                    tree.value(curr), tree.value(right))
                            .var("output", out).var("stack", bottomUp(tree, stack))
                            .tree(tree.render(states)).step();
                }
                curr = right;
            }
        }

        emit.at("done")
                .say("curr is null and the stack is empty at the same time - inorder result: %s.", out)
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
}
