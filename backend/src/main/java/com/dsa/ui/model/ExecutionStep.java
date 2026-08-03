package com.dsa.ui.model;

import java.util.List;
import java.util.Map;

public class ExecutionStep {
    private int stepNumber;
    private int activeLine;
    private String description;
    private List<String> queueOrStackState; // Elements currently in queue or stack
    private Map<Integer, String> nodeStates; // Node ID -> state ("unvisited", "queued", "visiting", "visited", "cycle")
    private List<String> activeEdges; // Edge identifiers e.g. "0-1"
    private Map<String, String> variables; // Debug variable values e.g. {"curr": "0", "vis[0]": "true"}
    private String dsType; // "Queue" or "Stack" or "Matrix" or "Array" or "LinkedList" or "Trie"
    private int[][] gridState; // Optional 2D grid matrix state for matrix algorithms
    private List<ArrayElement> arrayState; // Optional array state for sorting & array algorithms
    private List<ListNode> listState; // Optional linked list state
    private List<TrieNodeModel> trieState; // Optional trie state
    private List<TreeNode> treeNodes; // Optional dynamic recursion tree nodes state

    public ExecutionStep() {}

    public ExecutionStep(int stepNumber, int activeLine, String description,
                         List<String> queueOrStackState, Map<Integer, String> nodeStates,
                         List<String> activeEdges, Map<String, String> variables,
                         String dsType, int[][] gridState) {
        this(stepNumber, activeLine, description, queueOrStackState, nodeStates, activeEdges, variables, dsType, gridState, null, null, null, null);
    }

    public ExecutionStep(int stepNumber, int activeLine, String description,
                         List<String> queueOrStackState, Map<Integer, String> nodeStates,
                         List<String> activeEdges, Map<String, String> variables,
                         String dsType, int[][] gridState, List<ArrayElement> arrayState,
                         List<ListNode> listState, List<TrieNodeModel> trieState) {
        this(stepNumber, activeLine, description, queueOrStackState, nodeStates, activeEdges, variables, dsType, gridState, arrayState, listState, trieState, null);
    }

    public ExecutionStep(int stepNumber, int activeLine, String description,
                         List<String> queueOrStackState, Map<Integer, String> nodeStates,
                         List<String> activeEdges, Map<String, String> variables,
                         String dsType, int[][] gridState, List<ArrayElement> arrayState,
                         List<ListNode> listState, List<TrieNodeModel> trieState,
                         List<TreeNode> treeNodes) {
        this.stepNumber = stepNumber;
        this.activeLine = activeLine;
        this.description = description;
        this.queueOrStackState = queueOrStackState;
        this.nodeStates = nodeStates;
        this.activeEdges = activeEdges;
        this.variables = variables;
        this.dsType = dsType;
        this.gridState = gridState;
        this.arrayState = arrayState;
        this.listState = listState;
        this.trieState = trieState;
        this.treeNodes = treeNodes;
    }

    public int getStepNumber() { return stepNumber; }
    public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }

    public int getActiveLine() { return activeLine; }
    public void setActiveLine(int activeLine) { this.activeLine = activeLine; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getQueueOrStackState() { return queueOrStackState; }
    public void setQueueOrStackState(List<String> queueOrStackState) { this.queueOrStackState = queueOrStackState; }

    public Map<Integer, String> getNodeStates() { return nodeStates; }
    public void setNodeStates(Map<Integer, String> nodeStates) { this.nodeStates = nodeStates; }

    public List<String> getActiveEdges() { return activeEdges; }
    public void setActiveEdges(List<String> activeEdges) { this.activeEdges = activeEdges; }

    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }

    public String getDsType() { return dsType; }
    public void setDsType(String dsType) { this.dsType = dsType; }

    public int[][] getGridState() { return gridState; }
    public void setGridState(int[][] gridState) { this.gridState = gridState; }

    public List<ArrayElement> getArrayState() { return arrayState; }
    public void setArrayState(List<ArrayElement> arrayState) { this.arrayState = arrayState; }

    public List<ListNode> getListState() { return listState; }
    public void setListState(List<ListNode> listState) { this.listState = listState; }

    public List<TrieNodeModel> getTrieState() { return trieState; }
    public void setTrieState(List<TrieNodeModel> trieState) { this.trieState = trieState; }

    public List<TreeNode> getTreeNodes() { return treeNodes; }
    public void setTreeNodes(List<TreeNode> treeNodes) { this.treeNodes = treeNodes; }
}
