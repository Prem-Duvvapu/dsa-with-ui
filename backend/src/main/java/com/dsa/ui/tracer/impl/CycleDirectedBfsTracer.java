package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Detect a cycle in a directed graph with Kahn's algorithm, written the way the owner wrote
 * Course Schedule (LeetCode 207): count indegrees, repeatedly take a vertex with none, and lower
 * the count of its successors. Vertices on a cycle always keep an incoming edge, so they are
 * never taken; any indegree left above 0 at the end means a cycle. O(V + E).
 */
@Component
public class CycleDirectedBfsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "cycle-directed-bfs";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Directed graph")
                        .help("Vertex count plus directed edges [from, to].")
                        .directed()
                        .constraint("maxVertices", 20)
                        .constraint("maxEdges", 48)
                        // A tail feeding a cycle, NOT a pure cycle. In a pure 3-cycle every
                        // vertex has indegree 1, so the queue seeds empty and Kahn's reports
                        // "cycle" in three steps without ever polling, decrementing or
                        // enqueueing - the learner is told the answer and shown none of the
                        // algorithm. Here 0 and 1 are processed first and the queue THEN
                        // empties with work outstanding, which is what makes
                        // "processed < V means a cycle" mean something.
                        .defaultValue(Map.of(
                                "vertices", 4,
                                "edges", List.of(List.of(0, 1), List.of(1, 2),
                                        List.of(2, 3), List.of(3, 2))))
                        .build());
    }

    /** A DAG (diamond shape) - Kahn's BFS processes every vertex, so no cycle is reported. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 4,
                "edges", List.of(List.of(0, 1), List.of(0, 2), List.of(1, 3), List.of(2, 3))));
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public boolean isCyclic(int V, int[][] edges) {
                       // @a init
                       List<List<Integer>> adjList = new ArrayList<>();
                       int[] indegree = new int[V];
                       Queue<Integer> q = new LinkedList<>();

                       for (int i=0;i<V;i++)
                           adjList.add(new ArrayList<>());

                       for (int[] edge: edges) {
                           int u = edge[0];
                           int v = edge[1];

                           adjList.get(u).add(v);
                           indegree[v]++;
                       }

                       // @a seed
                       for (int i=0;i<V;i++)
                           if (indegree[i] == 0)
                               q.add(i);

                       while (!q.isEmpty()) {
                           // @a poll
                           int curr = q.poll();
                           for (int ngbr: adjList.get(curr)) {
                               // @a decrement
                               indegree[ngbr]--;

                               if (indegree[ngbr] == 0)
                                   // @a enqueue
                                   q.add(ngbr);
                           }
                       }

                       for (int i=0;i<V;i++)
                           if (indegree[i] > 0)
                               // @a cycle
                               return true;

                       // @a acyclic
                       return false;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int V = graph.vertices();
        List<List<Integer>> adjList = new ArrayList<>();
        int[] indegree = new int[V];
        Deque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < V; i++) {
            adjList.add(new ArrayList<>());
        }
        for (int[] edge : graph.edges()) {
            adjList.get(edge[0]).add(edge[1]);
            indegree[edge[1]]++;
        }

        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < V; i++) {
            states.put(i, "unvisited");
        }
        emit.at("init").say("%d vertices and %d directed edge%s. indegree[i] counts the edges coming into i: %s.",
                        V, graph.edges().length, Narration.s(graph.edges().length), Arrays.toString(indegree))
                .var("indegree", Arrays.toString(indegree))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        for (int i = 0; i < V; i++) {
            if (indegree[i] == 0) {
                q.add(i);
                states.put(i, "queued");
            }
        }
        emit.at("seed").say(q.isEmpty()
                        ? "No vertex has indegree 0, so nothing can be taken first."
                        : "Vertices with no incoming edges can be taken now: queue " + q + ".")
                .var("indegree", Arrays.toString(indegree)).var("q", q.toString())
                .graph(layout.nodes(), layout.edges()).nodes(states).queue(q).step();

        while (!q.isEmpty()) {
            int curr = q.poll();
            states.put(curr, "done");
            emit.at("poll").say("Take vertex %d, then remove its outgoing edges.", curr)
                    .var("curr", curr).var("indegree", Arrays.toString(indegree))
                    .graph(layout.nodes(), layout.edges()).nodes(states).queue(q).step();

            for (int ngbr : adjList.get(curr)) {
                indegree[ngbr]--;
                emit.at("decrement").say("%d -> %d removed: %d has %d incoming edge%s left.",
                                curr, ngbr, ngbr, indegree[ngbr], Narration.s(indegree[ngbr]))
                        .var("curr", curr).var("ngbr", ngbr).var("indegree", Arrays.toString(indegree))
                        .graph(layout.nodes(), layout.edges()).nodes(states)
                        .edges(List.of(curr + "-" + ngbr)).queue(q).step();
                if (indegree[ngbr] == 0) {
                    q.add(ngbr);
                    states.put(ngbr, "queued");
                    emit.at("enqueue").say("Vertex %d has no incoming edges left - queue it.", ngbr)
                            .var("ngbr", ngbr).var("q", q.toString())
                            .graph(layout.nodes(), layout.edges()).nodes(states).queue(q).step();
                }
            }
        }

        for (int i = 0; i < V; i++) {
            if (indegree[i] > 0) {
                for (int j = 0; j < V; j++) {
                    if (indegree[j] > 0) states.put(j, "cycle");
                }
                emit.at("cycle").say("The queue is empty but vertex %d still has %d incoming edge%s. The outlined "
                                + "vertices feed each other in a cycle, so none could ever be taken. Return true.",
                                i, indegree[i], Narration.s(indegree[i]))
                        .var("indegree", Arrays.toString(indegree)).var("answer", true)
                        .graph(layout.nodes(), layout.edges()).nodes(states).step();
                return;
            }
        }
        emit.at("acyclic").say("Every vertex was taken, so the edges can all be ordered front to back: no "
                        + "cycle. Return false.")
                .var("indegree", Arrays.toString(indegree)).var("answer", false)
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
    }
}
