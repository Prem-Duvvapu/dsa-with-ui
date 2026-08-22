package com.dsa.ui.model;

import java.util.Objects;

public class ArrayElement {
    private int index;
    private int value;
    private String state; // "default", "comparing", "swapping", "sorted", "active", "pivot"

    public ArrayElement() {}

    public ArrayElement(int index, int value, String state) {
        this.index = index;
        this.value = value;
        this.state = state;
    }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    // Value semantics. TraceEncoder omits a field from a delta step only when it is equal
    // to the previous step's, so "equal" has to mean equal by content, not by identity.
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ArrayElement o)) return false;
        return index == o.index && value == o.value && Objects.equals(state, o.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, value, state);
    }
}
