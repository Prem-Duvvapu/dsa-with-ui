package com.dsa.ui.tracer.wire;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.model.GraphEdge;
import com.dsa.ui.model.GraphNode;
import com.dsa.ui.model.ListNode;
import com.dsa.ui.model.TreeNode;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * One step on the wire, carrying only what changed since the step before it.
 *
 * <p>Every {@code ExecutionStep} holds a full snapshot of the data structure, so a response
 * grows as steps x n — kadane-algo costs 2,268 bytes/step on a 40-element array, which puts
 * a 5000-step trace near 11 MB. Most of that is repetition: measured across the eight
 * tracers, the payload field that actually differs from one step to the next is a single
 * one per problem, and the other six are the same null or the same empty list all the way
 * through while still being serialised every time.
 *
 * <p><b>Reading a delta stream.</b> Two rules, and the flag says which applies:
 * <ul>
 *   <li>{@code keyframe: true} — discard what you were holding. A field present here is its
 *       value; a field ABSENT here is genuinely empty.</li>
 *   <li>otherwise — carry the previous step forward. A field present here replaces what you
 *       held; a field absent here is unchanged.</li>
 * </ul>
 *
 * <p>The distinction is load-bearing rather than tidy. bfs-traversal empties
 * {@code activeEdges} on 6 of its 21 steps and the tree traversals empty their call stack on
 * the last one; if "absent" always meant "unchanged", the canvas would keep highlighting
 * edges the algorithm had already left and never unwind the stack. A field that changes TO
 * empty is therefore sent explicitly as {@code []}, and a field that changes to null forces
 * a keyframe.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DeltaStep {

    private final int stepNumber;
    private final int activeLine;
    private final String description;
    private final Boolean keyframe;

    private final List<String> queueOrStackState;
    private final Map<Integer, String> nodeStates;
    private final List<String> activeEdges;
    private final Map<String, String> variables;
    private final DsType dsType;
    private final int[][] gridState;
    private final List<ArrayElement> arrayState;
    private final List<ListNode> listState;
    private final List<TreeNode> treeNodes;
    private final List<GraphNode> graphNodes;
    private final List<GraphEdge> graphEdges;

    DeltaStep(int stepNumber, int activeLine, String description, Boolean keyframe,
              List<String> queueOrStackState, Map<Integer, String> nodeStates,
              List<String> activeEdges, Map<String, String> variables, DsType dsType,
              int[][] gridState, List<ArrayElement> arrayState, List<ListNode> listState,
              List<TreeNode> treeNodes, List<GraphNode> graphNodes,
              List<GraphEdge> graphEdges) {
        this.stepNumber = stepNumber;
        this.activeLine = activeLine;
        this.description = description;
        this.keyframe = keyframe;
        this.queueOrStackState = queueOrStackState;
        this.nodeStates = nodeStates;
        this.activeEdges = activeEdges;
        this.variables = variables;
        this.dsType = dsType;
        this.gridState = gridState;
        this.arrayState = arrayState;
        this.listState = listState;
        this.treeNodes = treeNodes;
        this.graphNodes = graphNodes;
        this.graphEdges = graphEdges;
    }

    public int getStepNumber() { return stepNumber; }
    public int getActiveLine() { return activeLine; }
    public String getDescription() { return description; }

    /** True on a step that stands alone. Absent on an ordinary delta step. */
    public Boolean getKeyframe() { return keyframe; }

    public List<String> getQueueOrStackState() { return queueOrStackState; }
    public Map<Integer, String> getNodeStates() { return nodeStates; }
    public List<String> getActiveEdges() { return activeEdges; }
    public Map<String, String> getVariables() { return variables; }
    public DsType getDsType() { return dsType; }
    public int[][] getGridState() { return gridState; }
    public List<ArrayElement> getArrayState() { return arrayState; }
    public List<ListNode> getListState() { return listState; }
    public List<TreeNode> getTreeNodes() { return treeNodes; }
    public List<GraphNode> getGraphNodes() { return graphNodes; }
    public List<GraphEdge> getGraphEdges() { return graphEdges; }
}
