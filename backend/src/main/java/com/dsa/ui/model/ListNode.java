package com.dsa.ui.model;

import java.util.Objects;

public class ListNode {
    private int id;
    private String val;
    private Integer nextId;
    private Integer prevId;
    private String state; // "default", "active", "slow", "fast", "prev", "curr", "next", "visited"

    public ListNode() {}

    public ListNode(int id, String val, Integer nextId, Integer prevId, String state) {
        this.id = id;
        this.val = val;
        this.nextId = nextId;
        this.prevId = prevId;
        this.state = state;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getVal() { return val; }
    public void setVal(String val) { this.val = val; }

    public Integer getNextId() { return nextId; }
    public void setNextId(Integer nextId) { this.nextId = nextId; }

    public Integer getPrevId() { return prevId; }
    public void setPrevId(Integer prevId) { this.prevId = prevId; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    // Value semantics. TraceEncoder omits a field from a delta step only when it is equal
    // to the previous step's, so "equal" has to mean equal by content, not by identity.
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ListNode o)) return false;
        return id == o.id && Objects.equals(val, o.val) && Objects.equals(nextId, o.nextId) && Objects.equals(prevId, o.prevId) && Objects.equals(state, o.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, val, nextId, prevId, state);
    }
}
