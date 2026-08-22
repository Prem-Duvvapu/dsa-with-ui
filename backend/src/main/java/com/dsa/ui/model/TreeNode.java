package com.dsa.ui.model;

import java.util.Objects;

public class TreeNode {
    private int id;
    private String val;
    private double x;
    private double y;
    private Integer leftId;
    private Integer rightId;
    private String state; // "unvisited", "visiting", "visited", "active", "target", "burned"

    public TreeNode() {}

    public TreeNode(int id, String val, double x, double y, Integer leftId, Integer rightId, String state) {
        this.id = id;
        this.val = val;
        this.x = x;
        this.y = y;
        this.leftId = leftId;
        this.rightId = rightId;
        this.state = state;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getVal() { return val; }
    public void setVal(String val) { this.val = val; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public Integer getLeftId() { return leftId; }
    public void setLeftId(Integer leftId) { this.leftId = leftId; }

    public Integer getRightId() { return rightId; }
    public void setRightId(Integer rightId) { this.rightId = rightId; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    // Value semantics. TraceEncoder omits a field from a delta step only when it is equal
    // to the previous step's, so "equal" has to mean equal by content, not by identity.
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TreeNode o)) return false;
        return id == o.id && Double.compare(x, o.x) == 0 && Double.compare(y, o.y) == 0 && Objects.equals(val, o.val) && Objects.equals(leftId, o.leftId) && Objects.equals(rightId, o.rightId) && Objects.equals(state, o.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, val, x, y, leftId, rightId, state);
    }
}
