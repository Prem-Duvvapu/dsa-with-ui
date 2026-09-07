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
 * The same threading trick as Morris inorder, moved one step earlier.
 *
 * <p>Both versions build the identical thread - the left subtree's rightmost node's empty
 * right pointer aimed back at the current node - and both remove it the moment they arrive
 * back along it. The ONLY difference is when the current node is output: inorder waits until
 * the thread is followed back, which is after the whole left subtree has been walked;
 * preorder outputs the moment the thread is created, which is before descending left. Move
 * that one line and root-left-right becomes left-root-right.
 *
 * <p>Consequently the return trip here does nothing but unpick the thread: it emits no
 * value, because the node it returns to was already output on the way down.
 */
@Component
public class MorrisPreorderTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "morris-preorder";
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
                        .defaultValue(Arrays.asList(10, 5, 15, 3, 7, null, 18))
                        .build());
    }

    /** A root with no right child: every thread lives inside the one left subtree, and the walk ends by falling out of it. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(6, 4, null, 2, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> morrisPreorder(TreeNode root) {
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
                               // @a visitThenThread
                               result.add(curr.val);   // BEFORE descending: preorder
                               pred.right = curr;
                               curr = curr.left;
                           } else {
                               // @a threadRemovedNoVisit
                               pred.right = null;      // already output on the way down
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

        if (tree.isEmpty()) {
            emit.at("done").say("The tree is empty, so the traversal is empty.")
                    .var("answer", "[]").tree(tree.render(states)).step();
            return;
        }

        Integer curr = tree.root();
        while (curr != null) {
            Integer left = tree.left(curr);
            if (left == null) {
                result.add(tree.value(curr));
                states.put(curr, "visited");
                emit.at("noLeftVisitNow")
                        .say("%d has no left subtree to postpone for, so output it and step to "
                                        + "its right - which may be a real child or a thread "
                                        + "back up.",
                                tree.value(curr))
                        .var("visited", tree.value(curr)).var("result", result.toString())
                        .tree(tree.render(states)).step();
                curr = effectiveRight(tree, threadRight, curr);
                continue;
            }

            Integer pred = left;
            Integer predRight = effectiveRight(tree, threadRight, pred);
            while (predRight != null && !predRight.equals(curr)) {
                pred = predRight;
                predRight = effectiveRight(tree, threadRight, pred);
            }
            emit.at("findPredecessor")
                    .say("Walk to the rightmost node of %d's left subtree: %d. That is where "
                                    + "the traversal will surface again.",
                            tree.value(curr), tree.value(pred))
                    .var("predecessor", tree.value(pred)).var("result", result.toString())
                    .tree(tree.render(states)).step();

            if (predRight == null) {
                result.add(tree.value(curr));
                states.put(curr, "visited");
                threadRight.put(pred, curr);
                emit.at("visitThenThread")
                        .say("First arrival at %d: output it NOW, before descending. Then thread "
                                        + "%d's empty right pointer back to %d and go left.",
                                tree.value(curr), tree.value(pred), tree.value(curr))
                        .var("visited", tree.value(curr)).var("result", result.toString())
                        .var("threadFrom", tree.value(pred)).var("threadTo", tree.value(curr))
                        .tree(tree.render(states)).step();
                curr = left;
            } else {
                threadRight.remove(pred);
                emit.at("threadRemovedNoVisit")
                        .say("Arrived back at %d along the thread from %d. %d was already output "
                                        + "on the way down, so remove the thread and move right "
                                        + "without emitting anything.",
                                tree.value(curr), tree.value(pred), tree.value(curr))
                        .var("threadRemovedFrom", tree.value(pred))
                        .var("result", result.toString())
                        .tree(tree.render(states)).step();
                curr = effectiveRight(tree, threadRight, curr);
            }
        }

        emit.at("done")
                .say("Preorder complete: %s. Every thread was unpicked as it was used, so the "
                        + "tree is byte-for-byte the tree we started with.", result)
                .var("answer", result.toString())
                .tree(tree.render(states)).step();
    }

    private Integer effectiveRight(BinaryTreeLayout tree, Map<Integer, Integer> threadRight, int index) {
        return threadRight.containsKey(index) ? threadRight.get(index) : tree.right(index);
    }
}
