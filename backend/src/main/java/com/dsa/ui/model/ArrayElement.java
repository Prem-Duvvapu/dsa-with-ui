package com.dsa.ui.model;

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
}
