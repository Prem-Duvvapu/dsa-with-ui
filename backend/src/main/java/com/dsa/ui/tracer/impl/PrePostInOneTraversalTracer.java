package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All three orders from a single recursive descent, by writing to a different list at each
 * of the three natural hook points inside one call.
 *
 * <p>This is the same result as {@code traversals-in-one-pass} reached the other way round,
 * and the catalogue lists both as separate ids. They are deliberately different techniques
 * rather than one implementation wearing two names: that one keeps an explicit stack of
 * (node, state) pairs and inspects a node three times; this one lets the call stack do the
 * remembering and never names a state at all. Seen side by side, the state number over
 * there is precisely which of the three hooks below a frame is parked at - which is the
 * point of having both.
 */
@Component
public class PrePostInOneTraversalTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "pre-post-in-one-traversal";
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
                        .defaultValue(Arrays.asList(6, 2, 8, 1, 4))
                        .build());
    }

    /** A right-only chain: every frame runs its three hooks back to back, so all three orders come out identical. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, null, 2, null, null, null, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public Orders allThree(TreeNode root) {
                   Orders orders = new Orders();
                   walk(root, orders);
                   // @a done
                   return orders;
               }

               private void walk(TreeNode node, Orders o) {
                   if (node == null) return;

                   // @a preHook
                   o.pre.add(node.val);     // on arrival, before either child
                   walk(node.left, o);

                   // @a inHook
                   o.in.add(node.val);      // between the two children
                   walk(node.right, o);

                   // @a postHook
                   o.post.add(node.val);    // on the way out, after both children
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

        walk(tree, tree.root(), states, pre, inorder, post, emit);

        emit.at("done")
                .say("One descent, three hooks: preorder %s, inorder %s, postorder %s.",
                        pre, inorder, post)
                .var("preorder", pre).var("inorder", inorder).var("postorder", post)
                .tree(tree.render(states)).step();
    }

    private void walk(BinaryTreeLayout tree, int index, Map<Integer, String> states,
                      List<Integer> pre, List<Integer> inorder, List<Integer> post,
                      StepEmitter emit) {
        emit.push("walk(" + tree.value(index) + ")");
        states.put(index, "visiting");

        pre.add(tree.value(index));
        emit.at("preHook")
                .say("Hook 1, the moment the call on %d begins: append %d to PREORDER, then "
                                + "descend left.",
                        tree.value(index), tree.value(index))
                .var("preorder", pre).var("inorder", inorder).var("postorder", post)
                .tree(tree.render(states)).step();

        Integer left = tree.left(index);
        if (left != null) {
            walk(tree, left, states, pre, inorder, post, emit);
        }

        inorder.add(tree.value(index));
        emit.at("inHook")
                .say("Hook 2, back inside %d's own frame with the left subtree finished and the "
                                + "right not started: append %d to INORDER.",
                        tree.value(index), tree.value(index))
                .var("preorder", pre).var("inorder", inorder).var("postorder", post)
                .tree(tree.render(states)).step();

        Integer right = tree.right(index);
        if (right != null) {
            walk(tree, right, states, pre, inorder, post, emit);
        }

        post.add(tree.value(index));
        states.put(index, "visited");
        emit.at("postHook")
                .say("Hook 3, the last statement of %d's frame with both subtrees finished: "
                                + "append %d to POSTORDER and return.",
                        tree.value(index), tree.value(index))
                .var("preorder", pre).var("inorder", inorder).var("postorder", post)
                .tree(tree.render(states)).step();

        emit.pop();
    }
}
