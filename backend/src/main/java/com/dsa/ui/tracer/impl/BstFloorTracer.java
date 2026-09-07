package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Floor alone: the largest value in the tree that is not greater than the target.
 *
 * <p>The already-traced {@code bst-floor-ceil} carries two candidates down the same walk;
 * this one carries a single running answer, and that changes what each step means. Going
 * LEFT here is pure elimination - the node was too big, nothing is learned, no answer is
 * recorded. Going RIGHT is the opposite: the node is a legal floor, so it is written down
 * as the best so far and the walk continues only to try to beat it. The answer at the end
 * is whatever survived, which may have been recorded several nodes ago.
 *
 * <p>That asymmetry is why a floor search cannot stop when it runs out of tree the way a
 * plain search can: falling off the bottom is a normal ending, not a failure.
 */
@Component
public class BstFloorTracer implements AlgorithmTracer {

    private static final int NONE = Integer.MIN_VALUE;

    @Override
    public String id() {
        return "bst-floor";
    }

    @Override
    public DsType dsType() {
        return DsType.TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("tree", FieldType.BINARY_TREE)
                        .label("BST (level order)")
                        .help("A BST in level order, with null where a child is absent.")
                        .length(1, 31).values(-99, 99).bstOrdered()
                        .defaultValue(Arrays.asList(15, 10, 20, 8, 12, 17, 25))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target")
                        .help("The floor is the largest value <= this.")
                        .range(-99, 99)
                        .defaultValue(13)
                        .build());
    }

    /** A target the tree actually holds, so the walk ends on an exact hit instead of running out of tree with a candidate in hand. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("target", 12);
    }

    @Override
    public String annotatedCode() {
        return """
               public int floor(TreeNode root, int target) {
                   int best = NONE;
                   TreeNode curr = root;

                   while (curr != null) {
                       if (curr.val == target) {
                           // @a exactHit
                           return curr.val;         // nothing can beat the target itself
                       }
                       if (curr.val > target) {
                           // @a tooBigGoLeft
                           curr = curr.left;        // not a candidate; record nothing
                       } else {
                           // @a candidateGoRight
                           best = curr.val;         // legal floor - keep it and try to beat it
                           curr = curr.right;
                       }
                   }
                   // @a done
                   return best;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        int target = in.getInt("target");
        Map<Integer, String> states = new LinkedHashMap<>();

        int best = NONE;
        Integer bestAt = null;
        boolean exact = false;
        Integer curr = tree.isEmpty() ? null : tree.root();

        while (curr != null) {
            int value = tree.value(curr);

            if (value == target) {
                best = value;
                bestAt = curr;
                exact = true;
                states.put(curr, "target");
                emit.at("exactHit")
                        .say("%d is in the tree. No value <= %d can be larger than %d itself, "
                                + "so the search stops here.", value, target, target)
                        .var("best", best).tree(tree.render(states)).step();
                break;
            }

            if (value > target) {
                states.put(curr, "visited");
                emit.at("tooBigGoLeft")
                        .say("%d > %d, so it is not a legal floor and neither is anything in "
                                        + "its right subtree. Go left; the best so far stays %s.",
                                value, target, best == NONE ? "nothing" : String.valueOf(best))
                        .var("best", best == NONE ? "none yet" : String.valueOf(best))
                        .tree(tree.render(states)).step();
                curr = tree.left(curr);
            } else {
                if (bestAt != null) {
                    states.put(bestAt, "visited");
                }
                best = value;
                bestAt = curr;
                states.put(curr, "target");
                emit.at("candidateGoRight")
                        .say("%d <= %d, so it IS a legal floor - record it as the best so far. "
                                        + "Anything better must be larger, so it can only be "
                                        + "to the right.",
                                value, target)
                        .var("best", best)
                        .tree(tree.render(states)).step();
                curr = tree.right(curr);
            }
        }

        if (best == NONE) {
            emit.at("done")
                    .say("The walk ran out of tree without ever standing on a value <= %d, so "
                            + "this BST has no floor for it.", target)
                    .var("answer", "none")
                    .tree(tree.render(states)).step();
        } else if (exact) {
            emit.at("done")
                    .say("Floor of %d is %d - the target itself, so no running candidate was "
                            + "ever needed.", target, best)
                    .var("answer", best)
                    .tree(tree.render(states)).step();
        } else {
            emit.at("done")
                    .say("Floor of %d is %d - the last candidate recorded, which nothing "
                            + "further right managed to beat.", target, best)
                    .var("answer", best)
                    .tree(tree.render(states)).step();
        }
    }
}
