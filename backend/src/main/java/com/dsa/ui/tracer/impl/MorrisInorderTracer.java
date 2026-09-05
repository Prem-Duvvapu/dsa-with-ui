package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Trades the call stack (or an explicit stack) for a temporary pointer instead: before
 * descending into a left subtree, thread its rightmost node's empty right pointer back to
 * the current node - a breadcrumb saying "come back here next." Following that thread later
 * is how the traversal returns upward without ever having pushed anything. The thread is
 * removed the moment it is used, so the tree is bit-for-bit the same tree once the
 * traversal finishes as it was before it started - the "extra memory" this saves is real,
 * but only because nothing about the shape is left behind.
 */
@Component
public class MorrisInorderTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "morris-inorder";
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
                        .defaultValue(java.util.Arrays.asList(4, 2, 6, 1, 3, 5, 7))
                        .build());
    }

    /** A lopsided tree with one missing child, so the predecessor walk and the no-left-child case both look different from the default. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", java.util.Arrays.asList(5, 3, 8, 1, 4, 7, 9, null, 2));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> morrisInorder(TreeNode root) {
                   List<Integer> result = new ArrayList<>();
                   TreeNode curr = root;

                   while (curr != null) {
                       if (curr.left == null) {
                           // @a noLeftVisitNow
                           result.add(curr.val);
                           curr = curr.right;
                       } else {
                           TreeNode pred = curr.left;
                           while (pred.right != null && pred.right != curr) {
                               pred = pred.right;
                           }
                           // @a findPredecessor
                           if (pred.right == null) {
                               // @a threadCreated
                               pred.right = curr;
                               curr = curr.left;
                           } else {
                               // @a threadFollowedRemoved
                               pred.right = null;
                               result.add(curr.val);
                               curr = curr.right;
                           }
                       }
                   }
                   // @a done
                   return result;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();
        Map<Integer, Integer> threadRight = new LinkedHashMap<>();
        List<Integer> result = new ArrayList<>();

        Integer curr = tree.root();
        while (curr != null) {
            Integer left = tree.left(curr);
            if (left == null) {
                result.add(tree.value(curr));
                states.put(curr, "visited");
                emit.at("noLeftVisitNow")
                        .say("%d has no left child - visit it now and move right.", tree.value(curr))
                        .var("visited", tree.value(curr)).var("result", result.toString())
                        .tree(tree.render(states)).step();
                curr = effectiveRight(tree, threadRight, curr);
            } else {
                Integer pred = left;
                Integer predRight = effectiveRight(tree, threadRight, pred);
                while (predRight != null && !predRight.equals(curr)) {
                    pred = predRight;
                    predRight = effectiveRight(tree, threadRight, pred);
                }
                emit.at("findPredecessor")
                        .say("The inorder predecessor of %d (rightmost node in its left "
                                + "subtree) is %d.", tree.value(curr), tree.value(pred))
                        .var("predecessor", tree.value(pred))
                        .tree(tree.render(states)).step();

                if (predRight == null) {
                    threadRight.put(pred, curr);
                    emit.at("threadCreated")
                            .say("%d's right pointer is empty - thread it to %d, then descend left.",
                                    tree.value(pred), tree.value(curr))
                            .var("threadFrom", tree.value(pred)).var("threadTo", tree.value(curr))
                            .tree(tree.render(states)).step();
                    curr = left;
                } else {
                    threadRight.remove(pred);
                    result.add(tree.value(curr));
                    states.put(curr, "visited");
                    emit.at("threadFollowedRemoved")
                            .say("The thread from %d back to %d already exists - the left "
                                    + "subtree is fully explored. Remove the thread, visit %d, "
                                    + "and move right.", tree.value(pred), tree.value(curr), tree.value(curr))
                            .var("visited", tree.value(curr)).var("result", result.toString())
                            .tree(tree.render(states)).step();
                    curr = effectiveRight(tree, threadRight, curr);
                }
            }
        }

        emit.at("done")
                .say("Traversal complete: %s. The tree is unchanged - every thread was removed as it was used.", result)
                .var("answer", result.toString())
                .tree(tree.render(states)).step();
    }

    private Integer effectiveRight(BinaryTreeLayout tree, Map<Integer, Integer> threadRight, int index) {
        return threadRight.containsKey(index) ? threadRight.get(index) : tree.right(index);
    }
}
