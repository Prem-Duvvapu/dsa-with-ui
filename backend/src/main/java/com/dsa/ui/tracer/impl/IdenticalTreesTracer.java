package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.TreeNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Two independently supplied trees, walked in lockstep - which is what separates this from
 * the symmetry problem next door. Symmetry compares one tree's left against its own right
 * and has to cross over (outer with outer, inner with inner); this compares p's left with
 * q's LEFT and p's right with q's RIGHT, straight down, no crossing anywhere.
 *
 * <p>Three questions in order at every pair: are both branches empty (agree, stop), is
 * exactly one empty (the shapes differ, stop), do the values differ (stop). Only if all
 * three pass does the walk descend, and only if it never stops early are the trees
 * identical.
 *
 * <p>The defaults are two trees that agree for several levels and then differ in SHAPE;
 * the alternate input differs in VALUE instead. One run can only ever end on one of those,
 * so the two inputs together are what exercise both failure branches.
 */
@Component
public class IdenticalTreesTracer implements AlgorithmTracer {

    private static final double GAP = 340;

    @Override
    public String id() {
        return "identical-trees";
    }

    @Override
    public DsType dsType() {
        return DsType.TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("p", FieldType.BINARY_TREE)
                        .label("Tree p (level order)")
                        .help("Level order, with null where a child is absent.")
                        .length(1, 15).values(-99, 99)
                        .defaultValue(Arrays.asList(1, 2, 3, 4, 5))
                        .build(),
                InputField.of("q", FieldType.BINARY_TREE)
                        .label("Tree q (level order)")
                        .help("Level order, with null where a child is absent.")
                        .length(1, 15).values(-99, 99)
                        .defaultValue(Arrays.asList(1, 2, 3, 4, 5, 6))
                        .build());
    }

    /**
     * LeetCode 100's third example: two identically shaped trees holding the same three
     * values on opposite sides, so the walk ends on a VALUE mismatch rather than the SHAPE
     * mismatch the defaults end on - the two failure modes the algorithm distinguishes.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "p", Arrays.asList(1, 2, 1),
                "q", Arrays.asList(1, 1, 2));
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isSameTree(TreeNode p, TreeNode q) {
                   boolean same = same(p, q);
                   // @a done
                   return same;
               }

               private boolean same(TreeNode a, TreeNode b) {
                   if (a == null && b == null) {
                       // @a bothEmpty
                       return true;
                   }
                   if (a == null || b == null) {
                       // @a shapesDiffer
                       return false;
                   }
                   if (a.val != b.val) {
                       // @a valuesDiffer
                       return false;
                   }
                   // @a matchDescendLeft
                   if (!same(a.left, b.left)) return false;
                   // @a matchDescendRight
                   return same(a.right, b.right);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout p = new BinaryTreeLayout(in.getBinaryTree("p"));
        BinaryTreeLayout q = new BinaryTreeLayout(in.getBinaryTree("q"));
        Map<Integer, String> pStates = new LinkedHashMap<>();
        Map<Integer, String> qStates = new LinkedHashMap<>();

        Integer pRoot = p.isEmpty() ? null : p.root();
        Integer qRoot = q.isEmpty() ? null : q.root();
        boolean same = same(p, q, pRoot, qRoot, pStates, qStates, emit);

        emit.at("done")
                .say(same
                        ? "Every pair agreed and no branch ran out before its partner: the two trees ARE identical."
                        : "A pair disagreed, so the walk stopped there: the two trees are NOT identical.")
                .var("answer", same)
                .tree(both(p, q, pStates, qStates)).step();
    }

    private boolean same(BinaryTreeLayout p, BinaryTreeLayout q, Integer a, Integer b,
                         Map<Integer, String> pStates, Map<Integer, String> qStates,
                         StepEmitter emit) {
        if (a == null && b == null) {
            emit.at("bothEmpty")
                    .say("Both branches are empty at the same moment - they agree, so this pair "
                            + "is fine and the walk backs out.")
                    .var("verdict", "agree")
                    .tree(both(p, q, pStates, qStates)).step();
            return true;
        }
        if (a == null || b == null) {
            String present = a == null
                    ? "q still has " + q.value(b)
                    : "p still has " + p.value(a);
            if (a != null) {
                pStates.put(a, "target");
            } else {
                qStates.put(b, "target");
            }
            emit.at("shapesDiffer")
                    .say("One branch ran out and the other did not: %s. The shapes differ, "
                            + "whatever the remaining values are.", present)
                    .var("verdict", "shape mismatch")
                    .tree(both(p, q, pStates, qStates)).step();
            return false;
        }
        if (p.value(a) != q.value(b)) {
            pStates.put(a, "target");
            qStates.put(b, "target");
            emit.at("valuesDiffer")
                    .say("The shapes line up here but the values do not: p has %d where q has "
                            + "%d.", p.value(a), q.value(b))
                    .var("verdict", "value mismatch")
                    .tree(both(p, q, pStates, qStates)).step();
            return false;
        }

        emit.push("same(" + p.value(a) + ", " + q.value(b) + ")");
        pStates.put(a, "visiting");
        qStates.put(b, "visiting");
        emit.at("matchDescendLeft")
                .say("Both trees hold %d here. Now compare p's LEFT with q's LEFT - straight "
                        + "down on both sides, never crossed over.", p.value(a))
                .var("verdict", "match so far")
                .tree(both(p, q, pStates, qStates)).step();

        if (!same(p, q, p.left(a), q.left(b), pStates, qStates, emit)) {
            emit.pop();
            return false;
        }

        emit.at("matchDescendRight")
                .say("The left sides below %d agree. Compare p's RIGHT with q's RIGHT.",
                        p.value(a))
                .var("verdict", "match so far")
                .tree(both(p, q, pStates, qStates)).step();
        boolean rightSame = same(p, q, p.right(a), q.right(b), pStates, qStates, emit);

        if (rightSame) {
            pStates.put(a, "visited");
            qStates.put(b, "visited");
        }
        emit.pop();
        return rightSame;
    }

    /** Both trees on one canvas: p on the left, q shifted right by a fixed gap. */
    private List<TreeNode> both(BinaryTreeLayout p, BinaryTreeLayout q,
                                Map<Integer, String> pStates, Map<Integer, String> qStates) {
        List<TreeNode> out = new ArrayList<>();
        for (TreeNode node : p.render(pStates)) {
            out.add(new TreeNode(node.getId(), "p:" + node.getVal(), node.getX() / 2,
                    node.getY(), node.getLeftId(), node.getRightId(), node.getState()));
        }
        for (TreeNode node : q.render(qStates)) {
            Integer left = node.getLeftId() == null ? null : node.getLeftId() + 1000;
            Integer right = node.getRightId() == null ? null : node.getRightId() + 1000;
            out.add(new TreeNode(node.getId() + 1000, "q:" + node.getVal(),
                    node.getX() / 2 + GAP, node.getY(), left, right, node.getState()));
        }
        return out;
    }
}
