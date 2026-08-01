package com.dsa.ui.algorithm.backtracking;

import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Problem: M-Coloring Problem (Graph Coloring Backtracking)
 *
 * Color a graph with V vertices using at most M colors such that no two adjacent vertices have the same color.
 */
public class MColoring {

    public boolean solve(int[][] graph, int m, int v, TraceRecorder recorder) {
        int[] color = new int[v];
        List<String> callStack = new ArrayList<>();

        recorder.record(new TraceEvent(
            "start", 15,
            String.format("M-Coloring: Color %d graph vertices using at most M=%d colors.", v, m),
            Map.of("V", String.valueOf(v), "M", String.valueOf(m)),
            "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
        ));

        boolean solved = graphColoring(0, graph, m, v, color, recorder, callStack);

        recorder.record(new TraceEvent(
            "complete", 40,
            String.format("M-Coloring Complete! Status: %s. Color Assignments: %s", solved ? "SOLVED" : "IMPOSSIBLE", Arrays.toString(color)),
            Map.of("Status", solved ? "SOLVED" : "FAILED", "colors", Arrays.toString(color)),
            "Stack", null, List.of(), Map.of(), List.of()
        ));

        return solved;
    }

    private boolean graphColoring(int node, int[][] graph, int m, int v, int[] color, TraceRecorder recorder, List<String> callStack) {
        if (node == v) return true;

        callStack.add("solve(node=" + node + ")");

        for (int colChoice = 1; colChoice <= m; colChoice++) {
            if (isSafe(node, graph, color, colChoice, v)) {
                color[node] = colChoice;

                recorder.record(new TraceEvent(
                    "assign_color", 25,
                    String.format("Node %d: Try Color %d -> SAFE! Set color[%d] = %d. Recurse to node %d...", node, colChoice, node, colChoice, node + 1),
                    Map.of("node", String.valueOf(node), "color", String.valueOf(colChoice)),
                    "Stack", null, new ArrayList<>(callStack), Map.of(node, "visited"), List.of()
                ));

                if (graphColoring(node + 1, graph, m, v, color, recorder, callStack)) {
                    callStack.remove(callStack.size() - 1);
                    return true;
                }

                // Backtrack
                color[node] = 0;

                recorder.record(new TraceEvent(
                    "backtrack_color", 32,
                    String.format("BACKTRACK at Node %d: Color %d led to conflict down tree. Reset color[%d] = 0.", node, colChoice, node),
                    Map.of("node", String.valueOf(node), "resetColor", "0"),
                    "Stack", null, new ArrayList<>(callStack), Map.of(node, "unvisited"), List.of()
                ));
            } else {
                recorder.record(new TraceEvent(
                    "color_conflict", 36,
                    String.format("Node %d: Try Color %d -> CONFLICT! Adjacent neighbor has same color. Skip.", node, colChoice),
                    Map.of("node", String.valueOf(node), "conflictColor", String.valueOf(colChoice)),
                    "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
                ));
            }
        }

        callStack.remove(callStack.size() - 1);
        return false;
    }

    private boolean isSafe(int node, int[][] graph, int[] color, int colChoice, int v) {
        for (int i = 0; i < v; i++) {
            if (graph[node][i] == 1 && color[i] == colChoice) return false;
        }
        return true;
    }
}
