package com.dsa.ui.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

public class ExecutionStep {
    private int stepNumber;
    private int activeLine;
    private String description;
    private List<String> queueOrStackState; // Elements currently in queue or stack
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> callStack; // Recursive call frames, independent of the algorithm's data structure
    private Map<Integer, String> nodeStates; // Node ID -> state ("unvisited", "queued", "visiting", "visited", "cycle")
    private List<String> activeEdges; // Edge identifiers e.g. "0-1"
    private Map<String, String> variables; // Debug variable values e.g. {"curr": "0", "vis[0]": "true"}
    private DsType dsType;
    private int[][] gridState; // Optional 2D grid matrix state for matrix algorithms
    private List<ArrayElement> arrayState; // Optional array state for sorting & array algorithms
    private List<ListNode> listState; // Optional linked list state
    private List<TrieNodeModel> trieState; // Optional trie state
    private List<TreeNode> treeNodes; // Optional dynamic recursion tree nodes state
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<GraphNode> graphNodes; // Optional graph topology derived from caller input
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<GraphEdge> graphEdges; // Optional graph topology derived from caller input
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DpTable dpTable; // Optional labelled table with per-cell Bench states

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
        this(stepNumber, activeLine, description, queueOrStackState, nodeStates, activeEdges,
                variables, DsType.fromWireValue(dsType), gridState, arrayState, listState,
                trieState, treeNodes);
    }

    public ExecutionStep(int stepNumber, int activeLine, String description,
                         List<String> queueOrStackState, Map<Integer, String> nodeStates,
                         List<String> activeEdges, Map<String, String> variables,
                         DsType dsType, int[][] gridState, List<ArrayElement> arrayState,
                         List<ListNode> listState, List<TrieNodeModel> trieState,
                         List<TreeNode> treeNodes) {
        this(stepNumber, activeLine, description, queueOrStackState, nodeStates, activeEdges,
                variables, dsType, gridState, arrayState, listState, trieState, treeNodes,
                null, null);
    }

    public ExecutionStep(int stepNumber, int activeLine, String description,
                         List<String> queueOrStackState, Map<Integer, String> nodeStates,
                         List<String> activeEdges, Map<String, String> variables,
                         DsType dsType, int[][] gridState, List<ArrayElement> arrayState,
                         List<ListNode> listState, List<TrieNodeModel> trieState,
                         List<TreeNode> treeNodes, List<GraphNode> graphNodes,
                         List<GraphEdge> graphEdges) {
        this(stepNumber, activeLine, description, queueOrStackState, nodeStates, activeEdges,
                variables, dsType, gridState, arrayState, listState, trieState, treeNodes,
                graphNodes, graphEdges, null);
    }

    public ExecutionStep(int stepNumber, int activeLine, String description,
                         List<String> queueOrStackState, Map<Integer, String> nodeStates,
                         List<String> activeEdges, Map<String, String> variables,
                         DsType dsType, int[][] gridState, List<ArrayElement> arrayState,
                         List<ListNode> listState, List<TrieNodeModel> trieState,
                         List<TreeNode> treeNodes, List<GraphNode> graphNodes,
                         List<GraphEdge> graphEdges, DpTable dpTable) {
        this(stepNumber, activeLine, description, queueOrStackState, nodeStates, activeEdges,
                variables, dsType, gridState, arrayState, listState, trieState, treeNodes,
                graphNodes, graphEdges, dpTable, null);
    }

    public ExecutionStep(int stepNumber, int activeLine, String description,
                         List<String> queueOrStackState, Map<Integer, String> nodeStates,
                         List<String> activeEdges, Map<String, String> variables,
                         DsType dsType, int[][] gridState, List<ArrayElement> arrayState,
                         List<ListNode> listState, List<TrieNodeModel> trieState,
                         List<TreeNode> treeNodes, List<GraphNode> graphNodes,
                         List<GraphEdge> graphEdges, DpTable dpTable,
                         List<String> callStack) {
        this.stepNumber = stepNumber;
        this.activeLine = activeLine;
        this.description = description;
        this.queueOrStackState = queueOrStackState;
        this.callStack = callStack;
        this.nodeStates = nodeStates;
        this.activeEdges = activeEdges;
        this.variables = variables;
        this.dsType = dsType;
        this.gridState = gridState;
        this.arrayState = arrayState;
        this.listState = listState;
        this.trieState = trieState;
        this.treeNodes = treeNodes;
        this.graphNodes = graphNodes;
        this.graphEdges = graphEdges;
        this.dpTable = dpTable;
    }

    public int getStepNumber() { return stepNumber; }
    public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }

    public int getActiveLine() { return activeLine; }
    public void setActiveLine(int activeLine) { this.activeLine = activeLine; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getQueueOrStackState() { return queueOrStackState; }
    public void setQueueOrStackState(List<String> queueOrStackState) { this.queueOrStackState = queueOrStackState; }

    public List<String> getCallStack() { return callStack; }
    public void setCallStack(List<String> callStack) { this.callStack = callStack; }

    public Map<Integer, String> getNodeStates() { return nodeStates; }
    public void setNodeStates(Map<Integer, String> nodeStates) { this.nodeStates = nodeStates; }

    public List<String> getActiveEdges() { return activeEdges; }
    public void setActiveEdges(List<String> activeEdges) { this.activeEdges = activeEdges; }

    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }

    public DsType getDsType() { return dsType; }
    public void setDsType(DsType dsType) { this.dsType = dsType; }

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

    public List<GraphNode> getGraphNodes() { return graphNodes; }
    public void setGraphNodes(List<GraphNode> graphNodes) { this.graphNodes = graphNodes; }

    public List<GraphEdge> getGraphEdges() { return graphEdges; }
    public void setGraphEdges(List<GraphEdge> graphEdges) { this.graphEdges = graphEdges; }

    public DpTable getDpTable() { return dpTable; }
    public void setDpTable(DpTable dpTable) { this.dpTable = dpTable; }
}
