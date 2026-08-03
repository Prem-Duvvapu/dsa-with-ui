package com.dsa.ui.model;

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
}
