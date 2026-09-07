package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.TreeNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rewires the tree in place into a right-leaning chain whose order is the tree's preorder -
 * and does it by walking in REVERSE preorder, which is the part worth watching.
 *
 * <p>A forward preorder walk cannot do this in place: the moment a node's right pointer is
 * aimed at its left subtree, the right subtree it was holding is unreachable and the rest of
 * the traversal has nowhere to go. Walking right, then left, then the node itself reaches
 * every node AFTER everything that will follow it in the finished chain, so a single
 * {@code prev} pointer is always already holding the correct successor. Each node then does
 * two assignments and is done forever.
 *
 * <p>Mid-flight the picture is genuinely a bit odd, and honestly so: a node that has been
 * relinked is reachable both through its old parent and through the growing chain, because
 * the old parent has not been rewired yet. The renderer draws each node once, at its first
 * encounter, rather than pretending the intermediate state is a clean tree.
 */
@Component
public class FlattenBtToLlTracer implements AlgorithmTracer {

    private static final double WIDTH = 640;
    private static final double TOP = 40;
    private static final double LEVEL_GAP = 74;

    @Override
    public String id() {
        return "flatten-bt-to-ll";
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
                        .defaultValue(Arrays.asList(1, 2, 5, 3, 4, null, 6))
                        .build());
    }

    /** A tree that is already a right-leaning chain: every relink writes back what was already there and the shape never moves. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, null, 2, null, null, null, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               private TreeNode prev = null;

               public void flatten(TreeNode node) {
                   if (node == null) {
                       // @a emptyBranch
                       return;
                   }
                   // @a goRightFirst
                   flatten(node.right);
                   // @a goLeftSecond
                   flatten(node.left);

                   // @a relink
                   node.right = prev;      // prev already holds this node's successor
                   node.left = null;
                   prev = node;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        MutableBst builder = new MutableBst();
        MutableBst.Node root = builder.build(in.getBinaryTree("tree"));
        Map<MutableBst.Node, String> states = new IdentityHashMap<>();

        if (root == null) {
            emit.at("emptyBranch").say("An empty tree is already a flat list.")
                    .var("chain", "[]").tree(List.of()).step();
            return;
        }

        MutableBst.Node[] prev = new MutableBst.Node[1];
        flatten(root, root, prev, states, emit);

        emit.at("relink")
                .say("Flattened in place. Following .right from the root now walks the tree's "
                        + "preorder: %s.", chain(prev[0]))
                .var("chain", chain(prev[0]))
                .tree(render(prev[0], states)).step();
    }

    private void flatten(MutableBst.Node origin, MutableBst.Node node, MutableBst.Node[] prev,
                         Map<MutableBst.Node, String> states, StepEmitter emit) {
        emit.push("flatten(" + node.val + ")");
        states.put(node, "visiting");

        if (node.right == null) {
            emit.at("emptyBranch")
                    .say("%d has no right child, so the reverse-preorder walk turns straight to "
                            + "its left side.", node.val)
                    .var("prev", prev[0] == null ? "null" : String.valueOf(prev[0].val))
                    .tree(render(origin, states)).step();
        } else {
            emit.at("goRightFirst")
                    .say("Descend into %d's RIGHT child %d first. Everything that will come "
                                    + "after %d in the finished chain must be rewired before "
                                    + "%d itself is.",
                            node.val, node.right.val, node.val, node.val)
                    .var("prev", prev[0] == null ? "null" : String.valueOf(prev[0].val))
                    .tree(render(origin, states)).step();
            flatten(origin, node.right, prev, states, emit);
        }

        if (node.left == null) {
            emit.at("emptyBranch")
                    .say("%d has no left child either, so it is ready to be relinked.", node.val)
                    .var("prev", prev[0] == null ? "null" : String.valueOf(prev[0].val))
                    .tree(render(origin, states)).step();
        } else {
            emit.at("goLeftSecond")
                    .say("Now descend into %d's LEFT child %d, which ends up immediately after "
                            + "%d in the chain.", node.val, node.left.val, node.val)
                    .var("prev", prev[0] == null ? "null" : String.valueOf(prev[0].val))
                    .tree(render(origin, states)).step();
            flatten(origin, node.left, prev, states, emit);
        }

        String successor = prev[0] == null ? "nothing at all - it is the chain's tail"
                : String.valueOf(prev[0].val);
        node.right = prev[0];
        node.left = null;
        prev[0] = node;
        states.put(node, "visited");

        emit.at("relink")
                .say("%d's subtree is already a chain and prev holds %s, which is exactly what "
                                + "must follow %d. Point %d.right at it and clear %d.left.",
                        node.val, successor, node.val, node.val, node.val)
                .var("prev", String.valueOf(node.val)).var("chain", chain(node))
                .tree(render(origin, states)).step();

        emit.pop();
    }

    private String chain(MutableBst.Node head) {
        List<Integer> out = new ArrayList<>();
        for (MutableBst.Node node = head; node != null; node = node.right) {
            out.add(node.val);
        }
        return out.toString();
    }

    /**
     * Draws whatever the pointers currently say, visiting each node once.
     *
     * <p>{@link MutableBst#render} assumes a real tree. Half-way through this algorithm the
     * structure briefly is not one - a relinked node is reachable both from its not-yet-
     * rewired parent and from the chain - so a node is placed at its first encounter and
     * skipped afterwards, rather than emitted twice under the same id.
     */
    private List<TreeNode> render(MutableBst.Node root, Map<MutableBst.Node, String> states) {
        List<MutableBst.Node> order = new ArrayList<>();
        Set<MutableBst.Node> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        inorder(root, order, seen);

        Map<MutableBst.Node, Integer> slot = new IdentityHashMap<>();
        for (int i = 0; i < order.size(); i++) {
            slot.put(order.get(i), i);
        }

        List<TreeNode> out = new ArrayList<>();
        Set<MutableBst.Node> drawn = Collections.newSetFromMap(new IdentityHashMap<>());
        place(root, 0, slot, Math.max(order.size(), 1), states, out, drawn);
        return out;
    }

    private void inorder(MutableBst.Node node, List<MutableBst.Node> order,
                         Set<MutableBst.Node> seen) {
        if (node == null || !seen.add(node)) {
            return;
        }
        inorder(node.left, order, seen);
        order.add(node);
        inorder(node.right, order, seen);
    }

    private void place(MutableBst.Node node, int depth, Map<MutableBst.Node, Integer> slot,
                       int slots, Map<MutableBst.Node, String> states, List<TreeNode> out,
                       Set<MutableBst.Node> drawn) {
        if (node == null || !drawn.add(node)) {
            return;
        }
        out.add(new TreeNode(
                node.id,
                String.valueOf(node.val),
                WIDTH * (slot.get(node) + 0.5) / slots,
                TOP + depth * LEVEL_GAP,
                node.left == null ? null : node.left.id,
                node.right == null ? null : node.right.id,
                states.getOrDefault(node, "unvisited")));
        place(node.left, depth + 1, slot, slots, states, out, drawn);
        place(node.right, depth + 1, slot, slots, states, out, drawn);
    }
}
