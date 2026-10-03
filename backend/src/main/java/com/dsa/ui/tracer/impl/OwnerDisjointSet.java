package com.dsa.ui.tracer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

/**
 * The owner's own {@code DisjointSet}, shared by every DSU problem they solved: path compression
 * in {@code getUltimateParent} and union by SIZE in {@code unionBySize}, which returns early when
 * both ends already share a root. This is the version from their Accounts Merge, Making a Large
 * Island and Swim in Rising Water submissions.
 *
 * <p>{@link #code(Set)} is the class as it is displayed. A tracer passes the branch anchors its
 * own inputs actually reach - the contract test rejects a highlight nothing ever lights - and the
 * other lines are shown without one. {@link #union} reports which branch ran so the tracer can
 * narrate it at that line, or at its own call site when that line carries no anchor.
 */
final class OwnerDisjointSet {

    static final String SAME_SET = "sameSet";
    static final String ATTACH_TO_U = "attachToU";
    static final String ATTACH_TO_V = "attachToV";

    final int[] parent;
    final int[] size;

    OwnerDisjointSet(int n) {
        parent = new int[n];
        size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }

    static String code(Set<String> anchored) {
        return """
               class DisjointSet {
                   int[] parent;
                   int[] size;

                   public DisjointSet(int n) {
                       parent = new int[n];
                       size = new int[n];

                       for (int i=0;i<n;i++) {
                           parent[i] = i;
                           size[i] = 1;
                       }
                   }

                   public int getUltimateParent(int node) {
                       if (parent[node] == node)
                           return node;

                       return parent[node] = getUltimateParent(parent[node]);
                   }

                   public void unionBySize(int u, int v) {
                       int ultParU = getUltimateParent(u);
                       int ultParV = getUltimateParent(v);

                       if (ultParU == ultParV)
               %s            return;

                       if (size[ultParU] >= size[ultParV]) {
               %s            size[ultParU] += size[ultParV];
                           parent[ultParV] = ultParU;
                       } else {
               %s            size[ultParV] += size[ultParU];
                           parent[ultParU] = ultParV;
                       }
                   }
               }""".formatted(
                anchor(anchored, SAME_SET, "            "),
                anchor(anchored, ATTACH_TO_U, "            "),
                anchor(anchored, ATTACH_TO_V, "            "));
    }

    private static String anchor(Set<String> anchored, String name, String indent) {
        return anchored.contains(name) ? indent + "// @a " + name + "\n" : "";
    }

    /** getUltimateParent with path compression; every parent it shortens is added to {@code compressed}. */
    int find(int node, List<String> compressed) {
        if (parent[node] == node) {
            return node;
        }
        int root = find(parent[node], compressed);
        if (parent[node] != root) {
            compressed.add("parent[" + node + "] = " + root);
            parent[node] = root;
        }
        return root;
    }

    /** What one unionBySize did. */
    record Union(String branch, int rootU, int rootV, List<String> compressed) {
        /** The root that survives. */
        int root() {
            return ATTACH_TO_V.equals(branch) ? rootV : rootU;
        }

        String narrate(int u, int v) {
            String finds = compressed.isEmpty() ? ""
                    : " Path compression on the way: " + String.join(", ", compressed) + ".";
            return switch (branch) {
                case SAME_SET -> "unionBySize(" + u + ", " + v + "): both have root " + rootU
                        + ", so they are already in one set - return." + finds;
                case ATTACH_TO_U -> "unionBySize(" + u + ", " + v + "): roots " + rootU + " and " + rootV
                        + ". Set " + rootU + " is at least as big, so " + rootV + " hangs under it." + finds;
                default -> "unionBySize(" + u + ", " + v + "): roots " + rootU + " and " + rootV
                        + ". Set " + rootV + " is bigger, so " + rootU + " hangs under it." + finds;
            };
        }
    }

    Union union(int u, int v) {
        List<String> compressed = new ArrayList<>();
        int ultParU = find(u, compressed);
        int ultParV = find(v, compressed);
        if (ultParU == ultParV) {
            return new Union(SAME_SET, ultParU, ultParV, compressed);
        }
        if (size[ultParU] >= size[ultParV]) {
            size[ultParU] += size[ultParV];
            parent[ultParV] = ultParU;
            return new Union(ATTACH_TO_U, ultParU, ultParV, compressed);
        }
        size[ultParV] += size[ultParU];
        parent[ultParU] = ultParV;
        return new Union(ATTACH_TO_V, ultParU, ultParV, compressed);
    }

    String parents() {
        return Arrays.toString(parent);
    }

    String sizes() {
        return Arrays.toString(size);
    }

    /**
     * The sets, each sorted, ordered by smallest member: "{0}, {1, 2, 3}". Only elements that
     * {@code include} accepts appear; {@code label} names them. Reads roots WITHOUT compressing,
     * so describing the state never changes it.
     */
    String sets(IntPredicate include, IntFunction<String> label) {
        Map<Integer, List<Integer>> byRoot = new TreeMap<>();
        for (int i = 0; i < parent.length; i++) {
            if (!include.test(i)) continue;
            int r = i;
            while (parent[r] != r) r = parent[r];
            byRoot.computeIfAbsent(r, k -> new ArrayList<>()).add(i);
        }
        List<List<Integer>> groups = new ArrayList<>(byRoot.values());
        groups.sort((a, b) -> Integer.compare(a.get(0), b.get(0)));
        List<String> out = new ArrayList<>();
        for (List<Integer> g : groups) {
            List<String> names = new ArrayList<>();
            for (int i : g) names.add(label.apply(i));
            out.add("{" + String.join(", ", names) + "}");
        }
        return String.join(", ", out);
    }

    String sets() {
        return sets(i -> true, String::valueOf);
    }
}
