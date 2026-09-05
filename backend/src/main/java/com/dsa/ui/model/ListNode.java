package com.dsa.ui.model;

import java.util.Objects;

public class ListNode {
    private int id;
    private String val;
    private Integer nextId;
    private Integer prevId;
    // Both additive and nullable: absent (null) for every tracer that predates them, so
    // existing traces serialize identically in shape except for these two always-null
    // extra fields. See RCA-020 for why the byte estimate had to move alongside them.
    private Integer childId;   // multilevel structures (e.g. flattening-ll); null when absent
    private Integer randomId;  // random-pointer structures (e.g. clone-ll-random-pointer); null when absent
    private String state; // "default", "active", "slow", "fast", "prev", "curr", "next", "visited"

    public ListNode() {}

    /** The original five-field shape every pre-existing linked-list tracer still uses. */
    public ListNode(int id, String val, Integer nextId, Integer prevId, String state) {
        this(id, val, nextId, prevId, null, null, state);
    }

    public ListNode(int id, String val, Integer nextId, Integer prevId, Integer childId, Integer randomId, String state) {
        this.id = id;
        this.val = val;
        this.nextId = nextId;
        this.prevId = prevId;
        this.childId = childId;
        this.randomId = randomId;
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

    public Integer getChildId() { return childId; }
    public void setChildId(Integer childId) { this.childId = childId; }

    public Integer getRandomId() { return randomId; }
    public void setRandomId(Integer randomId) { this.randomId = randomId; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    // Value semantics. TraceEncoder omits a field from a delta step only when it is equal
    // to the previous step's, so "equal" has to mean equal by content, not by identity.
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ListNode o)) return false;
        return id == o.id && Objects.equals(val, o.val) && Objects.equals(nextId, o.nextId)
                && Objects.equals(prevId, o.prevId) && Objects.equals(childId, o.childId)
                && Objects.equals(randomId, o.randomId) && Objects.equals(state, o.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, val, nextId, prevId, childId, randomId, state);
    }
}
