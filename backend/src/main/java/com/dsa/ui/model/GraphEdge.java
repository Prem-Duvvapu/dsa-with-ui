package com.dsa.ui.model;

import java.util.Objects;

public class GraphEdge {
    private int from;
    private int to;
    private Integer weight;
    private boolean directed;
    private boolean highlighted;

    public GraphEdge() {}

    public GraphEdge(int from, int to, boolean directed) {
        this(from, to, null, directed, false);
    }

    public GraphEdge(int from, int to, Integer weight, boolean directed, boolean highlighted) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.directed = directed;
        this.highlighted = highlighted;
    }

    public int getFrom() { return from; }
    public void setFrom(int from) { this.from = from; }

    public int getTo() { return to; }
    public void setTo(int to) { this.to = to; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public boolean isDirected() { return directed; }
    public void setDirected(boolean directed) { this.directed = directed; }

    public boolean isHighlighted() { return highlighted; }
    public void setHighlighted(boolean highlighted) { this.highlighted = highlighted; }

    /** Content equality lets the delta encoder carry reconstructed, unchanged edges. */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GraphEdge edge)) return false;
        return from == edge.from
                && to == edge.to
                && directed == edge.directed
                && highlighted == edge.highlighted
                && Objects.equals(weight, edge.weight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, weight, directed, highlighted);
    }
}
