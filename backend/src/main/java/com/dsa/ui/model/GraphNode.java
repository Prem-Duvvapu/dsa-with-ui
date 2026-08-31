package com.dsa.ui.model;

import java.util.Objects;

public class GraphNode {
    private int id;
    private String label;
    private double x;
    private double y;
    private String state; // "unvisited", "queued", "visiting", "visited", "cycle"

    public GraphNode() {}

    public GraphNode(int id, String label, double x, double y, String state) {
        this.id = id;
        this.label = label;
        this.x = x;
        this.y = y;
        this.state = state;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    /**
     * Graph topology is delta-encoded by value. Tracers are free to reconstruct the same
     * layout for a later step, so object identity must not make an unchanged node look new.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GraphNode node)) return false;
        return id == node.id
                && Double.compare(x, node.x) == 0
                && Double.compare(y, node.y) == 0
                && Objects.equals(label, node.label)
                && Objects.equals(state, node.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, x, y, state);
    }
}
