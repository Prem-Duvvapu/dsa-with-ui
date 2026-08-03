package com.dsa.ui.algorithm.graph;

import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.*;

/**
 * Problem: DFS Traversal of Graph
 *
 * Depth First Search (DFS) explores graph as deep as possible along each branch before backtracking.
 */
public class DfsTraversal {

    public List<Integer> solve(int v, Map<Integer, List<Integer>> adj, TraceRecorder recorder) {
        List<Integer> dfs = new ArrayList<>();
        boolean[] visited = new boolean[v];
        Map<Integer, String> nodeStates = new HashMap<>();
        List<String> activeEdges = new ArrayList<>();
        List<String> callStack = new ArrayList<>();

        for (int i = 0; i < v; i++) nodeStates.put(i, "unvisited");

        recorder.record(new TraceEvent(
            "start", 12,
            String.format("DFS Traversal: Start recursive search from vertex 0 on V=%d graph.", v),
            Map.of("StartNode", "0"),
            "Stack", null, new ArrayList<>(callStack), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
        ));

        dfsRecursive(0, adj, visited, dfs, nodeStates, activeEdges, callStack, recorder);

        recorder.record(new TraceEvent(
            "complete", 40,
            String.format("DFS Traversal Complete! Complete traversal order: %s", dfs.toString()),
            Map.of("DFS Result", dfs.toString()),
            "Stack", null, List.of(), new HashMap<>(nodeStates), List.of()
        ));

        return dfs;
    }

    private void dfsRecursive(int node, Map<Integer, List<Integer>> adj, boolean[] visited, List<Integer> dfs,
                              Map<Integer, String> nodeStates, List<String> activeEdges,
                              List<String> callStack, TraceRecorder recorder) {
        visited[node] = true;
        dfs.add(node);
        nodeStates.put(node, "visiting");
        callStack.add("dfs(" + node + ")");

        recorder.record(new TraceEvent(
            "visit_node", 22,
            String.format("DFS Visit vertex %d. Mark visited[%d] = true. Current DFS path: %s", node, node, dfs.toString()),
            Map.of("node", String.valueOf(node), "dfsPath", dfs.toString()),
            "Stack", null, new ArrayList<>(callStack), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
        ));

        List<Integer> neighbors = adj.getOrDefault(node, List.of());
        for (int neighbor : neighbors) {
            String edgeKey = Math.min(node, neighbor) + "-" + Math.max(node, neighbor);
            activeEdges.clear();
            activeEdges.add(edgeKey);

            if (!visited[neighbor]) {
                recorder.record(new TraceEvent(
                    "recurse_neighbor", 28,
                    String.format("Vertex %d -> Unvisited neighbor %d found. Recurse deeper into dfs(%d)...", node, neighbor, neighbor),
                    Map.of("from", String.valueOf(node), "to", String.valueOf(neighbor)),
                    "Stack", null, new ArrayList<>(callStack), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
                ));

                dfsRecursive(neighbor, adj, visited, dfs, nodeStates, activeEdges, callStack, recorder);
            } else {
                recorder.record(new TraceEvent(
                    "already_visited", 32,
                    String.format("Vertex %d -> Neighbor %d is already visited. Skip.", node, neighbor),
                    Map.of("from", String.valueOf(node), "alreadyVisited", String.valueOf(neighbor)),
                    "Stack", null, new ArrayList<>(callStack), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
                ));
            }
        }

        nodeStates.put(node, "visited");
        activeEdges.clear();
        callStack.remove(callStack.size() - 1);

        recorder.record(new TraceEvent(
            "backtrack_node", 38,
            String.format("BACKTRACK from vertex %d. All neighbors processed.", node),
            Map.of("backtrackedFrom", String.valueOf(node)),
            "Stack", null, new ArrayList<>(callStack), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
        ));
    }
}
