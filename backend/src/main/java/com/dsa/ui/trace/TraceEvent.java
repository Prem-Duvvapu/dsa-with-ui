package com.dsa.ui.trace;

import com.dsa.ui.model.TreeNode;

import java.util.List;
import java.util.Map;

public class TraceEvent {
    private final String operation;
    private final int codeLine;
    private final String description;
    private final Map<String, String> variables;
    private final String dsType;
    private final Object snapshot;
    private final List<String> callStack;
    private final Map<Integer, String> nodeStates;
    private final List<String> activeEdges;
    private final List<TreeNode> treeNodes;

    public TraceEvent(String operation, int codeLine, String description, Map<String, String> variables, String dsType, Object snapshot) {
        this(operation, codeLine, description, variables, dsType, snapshot, List.of(), Map.of(), List.of(), List.of());
    }

    public TraceEvent(String operation, int codeLine, String description, Map<String, String> variables, String dsType, Object snapshot, List<String> callStack, Map<Integer, String> nodeStates, List<String> activeEdges) {
        this(operation, codeLine, description, variables, dsType, snapshot, callStack, nodeStates, activeEdges, List.of());
    }

    public TraceEvent(String operation, int codeLine, String description, Map<String, String> variables, String dsType, Object snapshot, List<String> callStack, Map<Integer, String> nodeStates, List<String> activeEdges, List<TreeNode> treeNodes) {
        this.operation = operation;
        this.codeLine = codeLine;
        this.description = description;
        this.variables = variables != null ? variables : Map.of();
        this.dsType = dsType;
        this.snapshot = snapshot;
        this.callStack = callStack != null ? callStack : List.of();
        this.nodeStates = nodeStates != null ? nodeStates : Map.of();
        this.activeEdges = activeEdges != null ? activeEdges : List.of();
        this.treeNodes = treeNodes != null ? treeNodes : List.of();
    }

    public String getOperation() { return operation; }
    public int getCodeLine() { return codeLine; }
    public String getDescription() { return description; }
    public Map<String, String> getVariables() { return variables; }
    public String getDsType() { return dsType; }
    public Object getSnapshot() { return snapshot; }
    public List<String> getCallStack() { return callStack; }
    public Map<Integer, String> getNodeStates() { return nodeStates; }
    public List<String> getActiveEdges() { return activeEdges; }
    public List<TreeNode> getTreeNodes() { return treeNodes; }
}
