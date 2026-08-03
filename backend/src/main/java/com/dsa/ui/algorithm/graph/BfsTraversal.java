package com.dsa.ui.algorithm.graph;

import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.*;

/**
 * Problem: BFS Traversal of Graph
 *
 * Breadth First Search (BFS) explores graph level-by-level using a Queue (FIFO).
 */
public class BfsTraversal {

    public List<Integer> solve(int v, Map<Integer, List<Integer>> adj, TraceRecorder recorder) {
        List<Integer> bfs = new ArrayList<>();
        boolean[] visited = new boolean[v];
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, String> nodeStates = new HashMap<>();
        List<String> activeEdges = new ArrayList<>();

        for (int i = 0; i < v; i++) nodeStates.put(i, "unvisited");

        recorder.record(new TraceEvent(
            "start", 12,
            String.format("BFS Traversal: Initialize Queue and Visited array for V=%d graph. Enqueue start node 0.", v),
            Map.of("Queue", "[0]", "Start", "0"),
            "Queue", null, List.of("0"), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
        ));

        queue.add(0);
        visited[0] = true;
        nodeStates.put(0, "queued");

        recorder.record(new TraceEvent(
            "enqueue_start", 16,
            "Enqueue start vertex 0. Mark visited[0] = true.",
            Map.of("queued", "0"),
            "Queue", null, getQueueState(queue), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
        ));

        while (!queue.isEmpty()) {
            int node = queue.poll();
            bfs.add(node);
            nodeStates.put(node, "visiting");

            recorder.record(new TraceEvent(
                "dequeue", 22,
                String.format("Dequeue front vertex %d. Add to BFS result sequence: %s.", node, bfs.toString()),
                Map.of("dequeued", String.valueOf(node), "bfsOrder", bfs.toString()),
                "Queue", null, getQueueState(queue), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
            ));

            List<Integer> neighbors = adj.getOrDefault(node, List.of());
            for (int neighbor : neighbors) {
                String edgeKey = Math.min(node, neighbor) + "-" + Math.max(node, neighbor);
                activeEdges.clear();
                activeEdges.add(edgeKey);

                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    queue.add(neighbor);
                    nodeStates.put(neighbor, "queued");

                    recorder.record(new TraceEvent(
                        "enqueue_neighbor", 28,
                        String.format("Vertex %d -> Unvisited neighbor %d found. Mark visited[%d] = true and Enqueue %d.", node, neighbor, neighbor, neighbor),
                        Map.of("from", String.valueOf(node), "to", String.valueOf(neighbor)),
                        "Queue", null, getQueueState(queue), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
                    ));
                } else {
                    recorder.record(new TraceEvent(
                        "already_visited", 32,
                        String.format("Vertex %d -> Neighbor %d is already visited. Skip.", node, neighbor),
                        Map.of("from", String.valueOf(node), "alreadyVisited", String.valueOf(neighbor)),
                        "Queue", null, getQueueState(queue), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
                    ));
                }
            }

            nodeStates.put(node, "visited");
        }

        activeEdges.clear();
        recorder.record(new TraceEvent(
            "complete", 40,
            String.format("BFS Traversal Complete! Complete traversal order: %s", bfs.toString()),
            Map.of("BFS Result", bfs.toString()),
            "Queue", null, List.of(), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
        ));

        return bfs;
    }

    private List<String> getQueueState(Queue<Integer> q) {
        List<String> list = new ArrayList<>();
        for (Integer item : q) list.add(String.valueOf(item));
        return list;
    }
}
