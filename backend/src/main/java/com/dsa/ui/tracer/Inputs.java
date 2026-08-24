package com.dsa.ui.tracer;

import java.util.List;
import java.util.Map;

/**
 * A validated set of inputs, handed to a tracer.
 *
 * <p>Instances only ever come from {@link InputValidator}, so accessors assume the values
 * are present and well-typed. A tracer that asks for a field it did not declare gets an
 * {@link IllegalArgumentException} rather than a silent null.
 */
public final class Inputs {

    private final Map<String, Object> values;

    Inputs(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    public Map<String, Object> asMap() {
        return values;
    }

    private Object require(String name) {
        Object v = values.get(name);
        if (v == null) {
            throw new IllegalArgumentException(
                    "Tracer asked for undeclared input '" + name + "'; declared: " + values.keySet());
        }
        return v;
    }

    public int getInt(String name) {
        return ((Number) require(name)).intValue();
    }

    public int[] getIntArray(String name) {
        @SuppressWarnings("unchecked")
        List<Number> raw = (List<Number>) require(name);
        int[] out = new int[raw.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = raw.get(i).intValue();
        }
        return out;
    }

    public String getString(String name) {
        return (String) require(name);
    }

    public int[][] getGrid(String name) {
        @SuppressWarnings("unchecked")
        List<List<Number>> raw = (List<List<Number>>) require(name);
        int[][] out = new int[raw.size()][];
        for (int r = 0; r < out.length; r++) {
            List<Number> row = raw.get(r);
            out[r] = new int[row.size()];
            for (int c = 0; c < row.size(); c++) {
                out[r][c] = row.get(c).intValue();
            }
        }
        return out;
    }

    /** Node values in list order; {@link #getIntArray} by another name, for readability. */
    public int[] getLinkedList(String name) {
        return getIntArray(name);
    }

    /**
     * Level-order tree values where a null entry means "no node here".
     * Returned boxed so absent children stay distinguishable from a zero value.
     */
    public Integer[] getBinaryTree(String name) {
        @SuppressWarnings("unchecked")
        List<Number> raw = (List<Number>) require(name);
        Integer[] out = new Integer[raw.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = raw.get(i) == null ? null : raw.get(i).intValue();
        }
        return out;
    }

    public GraphInput getGraph(String name) {
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = (Map<String, Object>) require(name);
        @SuppressWarnings("unchecked")
        List<List<Number>> edges = (List<List<Number>>) raw.get("edges");
        int vertices = ((Number) raw.get("vertices")).intValue();

        int[][] out = new int[edges.size()][];
        for (int i = 0; i < edges.size(); i++) {
            List<Number> e = edges.get(i);
            out[i] = new int[e.size()];
            for (int j = 0; j < e.size(); j++) {
                out[i][j] = e.get(j).intValue();
            }
        }
        return new GraphInput(vertices, out);
    }

    /** A graph as vertex count plus edge triples {@code [from, to]} or {@code [from, to, weight]}. */
    public record GraphInput(int vertices, int[][] edges) {

        /** Undirected adjacency list, weights discarded. */
        public List<List<Integer>> adjacency() {
            return adjacency(false);
        }

        public List<List<Integer>> adjacency(boolean directed) {
            List<List<Integer>> adj = new java.util.ArrayList<>();
            for (int i = 0; i < vertices; i++) {
                adj.add(new java.util.ArrayList<>());
            }
            for (int[] e : edges) {
                adj.get(e[0]).add(e[1]);
                if (!directed) {
                    adj.get(e[1]).add(e[0]);
                }
            }
            return adj;
        }

        /** One weighted edge in a weighted adjacency list. */
        public record Neighbor(int to, int weight) {}

        /**
         * Weighted adjacency list. Requires the spec to declare {@code .weighted()} — every
         * edge then carries a third element, {@code [from, to, weight]}; this throws on a
         * two-element edge rather than silently defaulting to weight 1, since that would
         * make a shortest-path tracer quietly run an unweighted graph.
         */
        public List<List<Neighbor>> weightedAdjacency() {
            return weightedAdjacency(false);
        }

        public List<List<Neighbor>> weightedAdjacency(boolean directed) {
            List<List<Neighbor>> adj = new java.util.ArrayList<>();
            for (int i = 0; i < vertices; i++) {
                adj.add(new java.util.ArrayList<>());
            }
            for (int[] e : edges) {
                if (e.length != 3) {
                    throw new IllegalStateException(
                            "weightedAdjacency() needs edges of [from, to, weight]; got length " + e.length
                                    + ". Did the InputField forget .weighted()?");
                }
                adj.get(e[0]).add(new Neighbor(e[1], e[2]));
                if (!directed) {
                    adj.get(e[1]).add(new Neighbor(e[0], e[2]));
                }
            }
            return adj;
        }
    }
}
