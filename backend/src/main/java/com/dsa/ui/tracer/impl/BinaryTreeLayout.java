package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.TreeNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a level-order value array into positioned {@link TreeNode}s the canvas can draw.
 *
 * <p>Shared by every tree tracer. Coordinates are computed from the tree's actual depth
 * rather than hardcoded, which is what made the previous tree visuals work for exactly
 * one shape.
 */
final class BinaryTreeLayout {

    private static final double WIDTH = 640;
    private static final double TOP = 40;
    private static final double LEVEL_GAP = 74;

    private final Integer[] values;
    private final Map<Integer, Integer> leftChild = new LinkedHashMap<>();
    private final Map<Integer, Integer> rightChild = new LinkedHashMap<>();
    private final Map<Integer, Double> x = new LinkedHashMap<>();
    private final Map<Integer, Double> y = new LinkedHashMap<>();

    /** Indices into the level-order array that hold a real node, in index order. */
    private final List<Integer> present = new ArrayList<>();

    BinaryTreeLayout(Integer[] values) {
        this.values = values;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                continue;
            }
            // A node whose parent is absent cannot be part of the tree.
            if (i > 0 && values[(i - 1) / 2] == null) {
                continue;
            }
            present.add(i);
            int depth = (int) (Math.log(i + 1) / Math.log(2));
            int indexInLevel = i - ((1 << depth) - 1);
            int nodesAtDepth = 1 << depth;
            x.put(i, WIDTH * (indexInLevel + 0.5) / nodesAtDepth);
            y.put(i, TOP + depth * LEVEL_GAP);
        }
        for (int i : present) {
            int l = 2 * i + 1;
            int r = 2 * i + 2;
            if (l < values.length && values[l] != null) leftChild.put(i, l);
            if (r < values.length && values[r] != null) rightChild.put(i, r);
        }
    }

    boolean isEmpty() {
        return present.isEmpty();
    }

    int root() {
        return 0;
    }

    Integer left(int index) {
        return leftChild.get(index);
    }

    Integer right(int index) {
        return rightChild.get(index);
    }

    int value(int index) {
        return values[index];
    }

    /** Renders every node, colouring by the supplied per-index states. */
    List<TreeNode> render(Map<Integer, String> states) {
        List<TreeNode> out = new ArrayList<>(present.size());
        for (int i : present) {
            out.add(new TreeNode(
                    i,
                    String.valueOf(values[i]),
                    x.get(i),
                    y.get(i),
                    leftChild.get(i),
                    rightChild.get(i),
                    states.getOrDefault(i, "unvisited")));
        }
        return out;
    }
}
