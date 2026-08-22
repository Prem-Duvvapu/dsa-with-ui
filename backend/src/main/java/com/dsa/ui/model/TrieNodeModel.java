package com.dsa.ui.model;

import java.util.Objects;

import java.util.List;
import java.util.Map;

public class TrieNodeModel {
    private int id;
    private String character;
    private boolean endOfWord;
    private double x;
    private double y;
    private Map<String, Integer> children; // char -> child node ID
    private String state; // "default", "active", "match", "inserted", "end"

    public TrieNodeModel() {}

    public TrieNodeModel(int id, String character, boolean endOfWord, double x, double y, Map<String, Integer> children, String state) {
        this.id = id;
        this.character = character;
        this.endOfWord = endOfWord;
        this.x = x;
        this.y = y;
        this.children = children;
        this.state = state;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCharacter() { return character; }
    public void setCharacter(String character) { this.character = character; }

    public boolean isEndOfWord() { return endOfWord; }
    public void setEndOfWord(boolean endOfWord) { this.endOfWord = endOfWord; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public Map<String, Integer> getChildren() { return children; }
    public void setChildren(Map<String, Integer> children) { this.children = children; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    // Value semantics. TraceEncoder omits a field from a delta step only when it is equal
    // to the previous step's, so "equal" has to mean equal by content, not by identity.
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TrieNodeModel o)) return false;
        return id == o.id && endOfWord == o.endOfWord && Double.compare(x, o.x) == 0 && Double.compare(y, o.y) == 0 && Objects.equals(character, o.character) && Objects.equals(children, o.children) && Objects.equals(state, o.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, character, endOfWord, x, y, children, state);
    }
}
