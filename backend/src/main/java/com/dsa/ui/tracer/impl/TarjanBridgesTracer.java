package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Bridges in a graph (Tarjan), traced on the owner's own accepted Critical Connections submission
 * (LeetCode 1192). {@code firstVisitedTime} stamps each vertex as the DFS reaches it, and
 * {@code lowestTime} is the earliest stamp reachable from its subtree without using the edge back
 * to its parent. The edge curr-ngbr is a bridge exactly when {@code lowestTime[ngbr] >
 * firstVisitedTime[curr]}: nothing below ngbr can get back above curr another way. O(V + E).
 *
 * <p>One change: the submission ran one DFS from vertex 0 because LeetCode guarantees a connected
 * network. Here the graph may have several parts, so a DFS starts from every unvisited vertex; a
 * comment in the displayed code says so.
 */
@Component
public class TarjanBridgesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "tarjan-bridges";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Undirected graph")
                        .help("Vertex count plus undirected edges [u, v].")
                        .constraint("maxVertices", 12)
                        .constraint("maxEdges", 24)
                        .defaultValue(Map.of(
                                "vertices", 5,
                                "edges", List.of(
                                        List.of(1, 0), List.of(0, 2), List.of(2, 1),
                                        List.of(0, 3), List.of(3, 4))))
                        .build());
    }

    /** A single cycle: every edge has an alternative route around, so nothing is a bridge. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 4,
                "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(2, 3), List.of(3, 0))));
    }

    @Override
    public String annotatedCode() {
        return """
               // Changed from your submission: it ran one DFS from vertex 0, since LeetCode guarantees a
               // connected network. Here the graph may have several parts, so a DFS starts from every
               // vertex not yet visited.
               class Solution {
                   public List<List<Integer>> criticalConnections(int n, List<List<Integer>> connections) {
                       // @a init
                       List<List<Integer>> res = new ArrayList<>();
                       List<List<Integer>> adjList = new ArrayList<>();
                       int[] firstVisitedTime = new int[n];
                       int[] lowestTime = new int[n];
                       boolean[] visited = new boolean[n];
                       int[] time = {1};

                       for (int i=0;i<n;i++)
                           adjList.add(new ArrayList<>());

                       for (List<Integer> edge: connections) {
                           int u = edge.get(0);
                           int v = edge.get(1);

                           adjList.get(u).add(v);
                           adjList.get(v).add(u);
                       }

                       for (int i=0;i<n;i++)
                           if (!visited[i])
                               // @a start
                               dfs(i,-1,time,visited,firstVisitedTime,lowestTime,res,adjList);
                       // @a done
                       return res;
                   }

                   public void dfs(int curr,int parent,int[] time,boolean[] visited,int[] firstVisitedTime,int[] lowestTime,List<List<Integer>> res,List<List<Integer>> adjList) {
                       // @a visit
                       visited[curr] = true;
                       firstVisitedTime[curr] = time[0];
                       lowestTime[curr] = time[0];
                       time[0]++;

                       for (int ngbr: adjList.get(curr)) {
                           if (ngbr == parent)
                               continue;

                           if (!visited[ngbr])
                               dfs(ngbr,curr,time,visited,firstVisitedTime,lowestTime,res,adjList);

                           // @a low
                           lowestTime[curr] = Math.min(lowestTime[curr], lowestTime[ngbr]);

                           if (lowestTime[ngbr] > firstVisitedTime[curr])
                               // @a bridge
                               res.add(new ArrayList<>(Arrays.asList(curr,ngbr)));
                       }
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int n = graph.vertices();
        List<List<Integer>> adjList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjList.add(new ArrayList<>());
        }
        for (int[] e : graph.edges()) {
            adjList.get(e[0]).add(e[1]);
            adjList.get(e[1]).add(e[0]);
        }
        Walk w = new Walk(n, graph, emit);

        emit.at("init").say("%d vertices, %d edges. A DFS stamps each vertex with the time it is first "
                        + "reached, and works out the earliest stamp its subtree can get back to.",
                        n, graph.edges().length)
                .graph(graph).nodes(w.states).step();

        for (int i = 0; i < n; i++) {
            if (!w.visited[i]) {
                emit.at("start").say("Vertex %d is unvisited: start a DFS from it.", i)
                        .var("i", i).graph(graph).nodes(w.states).step();
                w.dfs(i, -1, adjList);
            }
        }

        emit.at("done").say(w.res.isEmpty()
                        ? "Every edge lies on a cycle, so removing any one leaves the graph connected. No bridges."
                        : "The DFS is complete. The bridges are " + w.show() + ".")
                .var("res", w.show()).var("firstVisitedTime", Arrays.toString(w.first))
                .var("lowestTime", Arrays.toString(w.low))
                .graph(graph).nodes(w.states).edges(w.bridgeKeys()).step();
    }

    /** The DFS state, so the recursion can be written the way the code is. */
    private static final class Walk {
        final boolean[] visited;
        final int[] first;
        final int[] low;
        final List<int[]> res = new ArrayList<>();
        final Map<Integer, String> states = new LinkedHashMap<>();
        final Inputs.GraphInput graph;
        final StepEmitter emit;
        int time = 1;

        Walk(int n, Inputs.GraphInput graph, StepEmitter emit) {
            visited = new boolean[n];
            first = new int[n];
            low = new int[n];
            this.graph = graph;
            this.emit = emit;
            for (int i = 0; i < n; i++) states.put(i, "unvisited");
        }

        void dfs(int curr, int parent, List<List<Integer>> adjList) {
            emit.push("dfs(" + curr + ")");
            visited[curr] = true;
            first[curr] = time;
            low[curr] = time;
            time++;
            states.put(curr, "visiting");
            emit.at("visit").say("dfs(%d): first reached at time %d, so firstVisitedTime[%d] = lowestTime[%d] = %d.",
                            curr, first[curr], curr, curr, first[curr])
                    .var("curr", curr).var("firstVisitedTime", Arrays.toString(first))
                    .var("lowestTime", Arrays.toString(low)).graph(graph).nodes(states).edges(bridgeKeys()).step();

            for (int ngbr : adjList.get(curr)) {
                if (ngbr == parent) continue;
                boolean child = !visited[ngbr];
                if (child) {
                    dfs(ngbr, curr, adjList);
                }
                int before = low[curr];
                low[curr] = Math.min(low[curr], low[ngbr]);
                String why = child
                        ? String.format("Back from child %d, whose subtree reaches time %d", ngbr, low[ngbr])
                        : String.format("%d was already reached another way, so edge %d-%d closes a cycle; take "
                                + "%d's lowestTime, %d", ngbr, curr, ngbr, ngbr, low[ngbr]);
                emit.at("low").say("%s: lowestTime[%d] = min(%d, %d) = %d.", why, curr, before, low[ngbr], low[curr])
                        .var("curr", curr).var("ngbr", ngbr).var("lowestTime", Arrays.toString(low))
                        .graph(graph).nodes(states).edges(withEdge(curr, ngbr)).step();
                if (low[ngbr] > first[curr]) {
                    res.add(new int[]{curr, ngbr});
                    emit.at("bridge").say("lowestTime[%d] = %d is later than firstVisitedTime[%d] = %d: nothing under %d "
                                    + "can get back to %d or above except through this edge. %d-%d is a bridge.",
                                    ngbr, low[ngbr], curr, first[curr], ngbr, curr, curr, ngbr)
                            .var("res", show()).graph(graph).nodes(states).edges(bridgeKeys()).step();
                }
            }
            states.put(curr, "visited");
            emit.pop();
        }

        String show() {
            if (res.isEmpty()) return "none";
            List<String> out = new ArrayList<>();
            for (int[] b : res) out.add(b[0] + "-" + b[1]);
            return String.join(", ", out);
        }

        /** The canvas names an undirected edge in its input orientation. */
        String key(int a, int b) {
            for (int[] e : graph.edges()) {
                if (e[0] == a && e[1] == b) return a + "-" + b;
                if (e[0] == b && e[1] == a) return b + "-" + a;
            }
            return a + "-" + b;
        }

        List<String> bridgeKeys() {
            List<String> out = new ArrayList<>();
            for (int[] b : res) out.add(key(b[0], b[1]));
            return out;
        }

        List<String> withEdge(int a, int b) {
            List<String> out = bridgeKeys();
            out.add(key(a, b));
            return out;
        }
    }
}
