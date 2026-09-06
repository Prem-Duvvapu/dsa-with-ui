package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.TreeNode;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A real pointer-based BST for tracers whose algorithm changes the tree's shape
 * (insert, delete). {@link BinaryTreeLayout} assumes a fixed level-order address for every
 * node, which breaks the moment a node is added or a subtree is relinked one level up - a
 * one-child delete would need every descendant of the promoted child relabelled to a new
 * address. Real {@link Node} pointers make the textbook algorithm (and its recursion)
 * correct by construction; only rendering needs to be recomputed, which {@link #render}
 * does fresh from whatever shape the tree currently has.
 *
 * <p>Each node keeps a permanent id assigned at creation, not derived from position, so a
 * node that moves (an insert changes depth, a delete's promoted child moves up a level)
 * keeps the same id across steps instead of being recreated as "a different node" mid-trace.
 */
final class MutableBst {

    static final class Node {
        final int id;
        int val;
        Node left;
        Node right;

        Node(int id, int val) {
            this.id = id;
            this.val = val;
        }
    }

    private int nextId = 0;

    Node newNode(int val) {
        return new Node(nextId++, val);
    }

    /** Builds from the same level-order-with-nulls format {@code FieldType.BINARY_TREE} uses. */
    Node build(Integer[] values) {
        return build(values, 0);
    }

    private Node build(Integer[] values, int i) {
        if (i >= values.length || values[i] == null) {
            return null;
        }
        Node n = newNode(values[i]);
        n.left = build(values, 2 * i + 1);
        n.right = build(values, 2 * i + 2);
        return n;
    }

    private static final double WIDTH = 640;
    private static final double TOP = 40;
    private static final double LEVEL_GAP = 74;

    /**
     * Renders the tree's current shape: inorder position decides x (so left stays left of
     * right, whatever the shape), depth decides y.
     */
    List<TreeNode> render(Node root, Map<Node, String> states) {
        List<Node> inorder = new ArrayList<>();
        collectInorder(root, inorder);
        Map<Node, Integer> slot = new IdentityHashMap<>();
        for (int i = 0; i < inorder.size(); i++) {
            slot.put(inorder.get(i), i);
        }
        int slots = Math.max(inorder.size(), 1);
        List<TreeNode> out = new ArrayList<>(inorder.size());
        collect(root, 0, slot, slots, states, out);
        return out;
    }

    private void collectInorder(Node node, List<Node> out) {
        if (node == null) {
            return;
        }
        collectInorder(node.left, out);
        out.add(node);
        collectInorder(node.right, out);
    }

    private void collect(Node node, int depth, Map<Node, Integer> slot, int slots,
                          Map<Node, String> states, List<TreeNode> out) {
        if (node == null) {
            return;
        }
        double x = WIDTH * (slot.get(node) + 0.5) / slots;
        double y = TOP + depth * LEVEL_GAP;
        Integer leftId = node.left == null ? null : node.left.id;
        Integer rightId = node.right == null ? null : node.right.id;
        out.add(new TreeNode(node.id, String.valueOf(node.val), x, y, leftId, rightId,
                states.getOrDefault(node, "unvisited")));
        collect(node.left, depth + 1, slot, slots, states, out);
        collect(node.right, depth + 1, slot, slots, states, out);
    }
}
