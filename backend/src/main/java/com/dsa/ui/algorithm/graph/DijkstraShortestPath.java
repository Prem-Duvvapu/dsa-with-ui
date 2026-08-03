package com.dsa.ui.algorithm.graph;

import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.*;

/**
 * Problem: Dijkstra's Shortest Path Algorithm (Priority Queue)
 *
 * Find shortest distance from src node 0 to all vertices in non-negatively weighted graph.
 */
public class DijkstraShortestPath {

    public static class Edge {
        public int to;
        public int weight;
        public Edge(int to, int weight) {
            this.to = to;
            this.weight = weight;
        }
    }

    public int[] solve(int v, Map<Integer, List<Edge>> adj, int src, TraceRecorder recorder) {
        int[] dist = new int[v];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1])); // [node, distance]
        pq.add(new int[]{src, 0});

        Map<Integer, String> nodeStates = new HashMap<>();
        List<String> activeEdges = new ArrayList<>();
        for (int i = 0; i < v; i++) nodeStates.put(i, "unvisited");
        nodeStates.put(src, "queued");

        recorder.record(new TraceEvent(
            "start", 15,
            String.format("Dijkstra's Algorithm: Source node = %d. Initialize dist array = %s. Enqueue (src=0, dist=0).", src, Arrays.toString(dist)),
            Map.of("src", String.valueOf(src), "distances", Arrays.toString(dist)),
            "PriorityQueue", null, getPqState(pq), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
        ));

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int u = curr[0];
            int d = curr[1];

            if (d > dist[u]) continue;
            nodeStates.put(u, "visiting");

            recorder.record(new TraceEvent(
                "extract_min", 22,
                String.format("Priority Queue Extract Min: Pop node %d with shortest known distance = %d. Relax outgoing edges...", u, d),
                Map.of("node", String.valueOf(u), "minDist", String.valueOf(d)),
                "PriorityQueue", null, getPqState(pq), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
            ));

            List<Edge> neighbors = adj.getOrDefault(u, List.of());
            for (Edge edge : neighbors) {
                int nxt = edge.to;
                int wt = edge.weight;
                activeEdges.clear();
                activeEdges.add(Math.min(u, nxt) + "-" + Math.max(u, nxt));

                if (dist[u] + wt < dist[nxt]) {
                    int oldDist = dist[nxt];
                    dist[nxt] = dist[u] + wt;
                    pq.add(new int[]{nxt, dist[nxt]});
                    nodeStates.put(nxt, "queued");

                    recorder.record(new TraceEvent(
                        "relax_edge", 30,
                        String.format("RELAX EDGE (%d -> %d, wt=%d): New dist[%d] = dist[%d](%d) + wt(%d) = %d < old(%s). Enqueue!",
                            u, nxt, wt, nxt, u, dist[u], wt, dist[nxt], oldDist == Integer.MAX_VALUE ? "INF" : oldDist),
                        Map.of("edge", u + "->" + nxt, "newDist", String.valueOf(dist[nxt])),
                        "PriorityQueue", null, getPqState(pq), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
                    ));
                } else {
                    recorder.record(new TraceEvent(
                        "no_relaxation", 35,
                        String.format("Edge (%d -> %d, wt=%d): dist[%d](%d) + wt(%d) = %d >= dist[%d](%d). No update.",
                            u, nxt, wt, u, dist[u], wt, dist[u] + wt, nxt, dist[nxt]),
                        Map.of("edge", u + "->" + nxt, "currentDist", String.valueOf(dist[nxt])),
                        "PriorityQueue", null, getPqState(pq), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
                    ));
                }
            }

            nodeStates.put(u, "visited");
        }

        activeEdges.clear();
        recorder.record(new TraceEvent(
            "complete", 45,
            String.format("Dijkstra Complete! Final Shortest Distances from Node %d: %s", src, Arrays.toString(dist)),
            Map.of("ShortestDistances", Arrays.toString(dist)),
            "PriorityQueue", null, List.of(), new HashMap<>(nodeStates), new ArrayList<>(activeEdges)
        ));

        return dist;
    }

    private List<String> getPqState(PriorityQueue<int[]> pq) {
        List<String> list = new ArrayList<>();
        for (int[] item : pq) list.add("(node:" + item[0] + ", dist:" + item[1] + ")");
        return list;
    }
}
