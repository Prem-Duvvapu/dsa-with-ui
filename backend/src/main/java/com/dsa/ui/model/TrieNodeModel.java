package com.dsa.ui.model;

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
}
