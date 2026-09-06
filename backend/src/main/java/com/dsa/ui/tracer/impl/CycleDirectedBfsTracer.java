package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Kahn's algorithm processes a vertex only once every prerequisite of it has already been
 * processed - a directed cycle is exactly the set of vertices that can NEVER satisfy that,
 * since each one's indegree only ever drops to zero if something outside the cycle points
 * into it. If Kahn's BFS finishes having processed fewer than V vertices, whatever is left
 * over is a cycle (or reachable only through one).
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
                        .defaultValue(Map.of(
                                "vertices", 3,
                                "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(2, 0))))
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
               public boolean isCyclic(int v, List<List<Integer>> adj) {
                   // @a indegree
                   int[] indegree = new int[v];
                   for (int node = 0; node < v; node++) {
                       for (int next : adj.get(node)) indegree[next]++;
                   }

                   // @a seed
                   Queue<Integer> queue = new LinkedList<>();
                   for (int i = 0; i < v; i++) {
                       if (indegree[i] == 0) queue.add(i);
                   }

                   int processed = 0;
                   while (!queue.isEmpty()) {
                       // @a poll
                       int node = queue.poll();
                       processed++;
                       for (int next : adj.get(node)) {
                           // @a decrement
                           indegree[next]--;
                           if (indegree[next] == 0) {
                               // @a enqueue
                               queue.add(next);
                           }
                       }
                   }
                   // @a verdict
                   return processed < v;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency(true);
        int v = graph.vertices();
        int[] indegree = new int[v];
        for (int node = 0; node < v; node++) {
            for (int next : adj.get(node)) indegree[next]++;
        }

        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) states.put(i, "unvisited");

        emit.at("indegree")
                .say("%d vertices, %d directed edges. Indegree of each: %s.",
                        v, graph.edges().length, Arrays.toString(indegree))
                .var("indegree", Arrays.toString(indegree))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < v; i++) {
            if (indegree[i] == 0) {
                queue.add(i);
                states.put(i, "queued");
            }
        }
        emit.at("seed")
                .say("Every vertex with indegree 0 has no unprocessed prerequisite - seed the queue with %s.",
                        queue)
                .var("queue", queue.toString())
                .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();

        int processed = 0;
        while (!queue.isEmpty()) {
            int node = queue.poll();
            processed++;
            states.put(node, "visited");
            emit.at("poll")
                    .say("Dequeue %d (processed %d of %d so far).", node, processed, v)
                    .var("node", node).var("processed", processed)
                    .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();

            for (int next : adj.get(node)) {
                indegree[next]--;
                emit.at("decrement")
                        .say("%d -> %d: %d's remaining indegree drops to %d.", node, next, next, indegree[next])
                        .var("node", node).var("neighbour", next).var("indegreeLeft", indegree[next])
                        .graph(layout.nodes(), layout.edges()).nodes(states)
                        .edges(List.of(node + "-" + next)).queue(queue).step();

                if (indegree[next] == 0) {
                    queue.add(next);
                    states.put(next, "queued");
                    emit.at("enqueue")
                            .say("%d has no prerequisite left - enqueue it.", next)
                            .var("enqueued", next)
                            .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();
                }
            }
        }

        boolean cyclic = processed < v;
        emit.at("verdict")
                .say(cyclic
                        ? "Only %d of %d vertices ever reached indegree 0 - the rest form a directed cycle."
                        : "All %d of %d vertices were processed - no directed cycle.",
                        processed, v)
                .var("processed", processed).var("answer", cyclic)
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
    }
}
