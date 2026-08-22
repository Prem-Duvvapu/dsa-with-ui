package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Breadth-first traversal over a caller-supplied graph. */
@Component
public class BfsTraversalTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bfs-traversal";
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Graph")
                        .help("Vertex count plus undirected edges.")
                        .constraint("maxVertices", 24)
                        .constraint("maxEdges", 60)
                        .defaultValue(Map.of(
                                "vertices", 6,
                                "edges", List.of(
                                        List.of(0, 1), List.of(0, 2), List.of(1, 3),
                                        List.of(2, 4), List.of(3, 5), List.of(4, 5))))
                        .build(),
                InputField.of("start", FieldType.INT)
                        .label("Start vertex")
                        .range(0, 23)
                        .defaultValue(0)
                        .build());
    }

    /** A path rather than a branching graph, so every level holds exactly one vertex. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "graph", Map.of(
                        "vertices", 4,
                        "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(2, 3))),
                "start", 0);
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> bfs(int v, List<List<Integer>> adj, int start) {
                   // @a init
                   boolean[] seen = new boolean[v];
                   Queue<Integer> queue = new LinkedList<>();
                   // @a seed
                   queue.add(start);
                   seen[start] = true;
                   while (!queue.isEmpty()) {
                       // @a poll
                       int node = queue.poll();
                       for (int next : adj.get(node)) {
                           // @a check
                           if (!seen[next]) {
                               // @a enqueue
                               seen[next] = true;
                               queue.add(next);
                           }
                       }
                   }
                   // @a done
                   return order;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int start = in.getInt("start");
        emit.using("Queue");

        if (start >= graph.vertices()) {
            throw new InputValidationException(Map.of("start",
                    "This graph only has vertices 0.." + (graph.vertices() - 1) + "."));
        }

        List<List<Integer>> adj = graph.adjacency();
        boolean[] seen = new boolean[graph.vertices()];
        Deque<Integer> queue = new ArrayDeque<>();
        Map<Integer, String> states = new LinkedHashMap<>();
        List<Integer> order = new ArrayList<>();

        for (int i = 0; i < graph.vertices(); i++) {
            states.put(i, "unvisited");
        }

        emit.at("init").say("%d vertices, %d edges. Nothing visited yet.",
                        graph.vertices(), graph.edges().length)
                .var("start", start).nodes(states).step();

        queue.add(start);
        seen[start] = true;
        states.put(start, "queued");

        emit.at("seed").say("Seed the queue with vertex %d and mark it seen.", start)
                .var("queue", queue).nodes(states).step();

        while (!queue.isEmpty()) {
            int node = queue.poll();
            states.put(node, "visiting");
            order.add(node);

            emit.at("poll").say("Dequeue %d. Everything at this distance is handled before going deeper.", node)
                    .var("node", node).var("queue", queue).var("order", order)
                    .nodes(states).step();

            for (int next : adj.get(node)) {
                if (seen[next]) {
                    emit.at("check").say("%d is already seen — skip it, or BFS would loop forever.", next)
                            .var("node", node).var("neighbour", next).var("queue", queue)
                            .nodes(states).edges(List.of(node + "-" + next)).step();
                    continue;
                }
                seen[next] = true;
                queue.add(next);
                states.put(next, "queued");

                emit.at("enqueue").say("%d is new. Mark it seen and enqueue it behind %s.",
                                next, queue.size() == 1 ? "nothing" : "the existing entries")
                        .var("node", node).var("neighbour", next).var("queue", queue)
                        .nodes(states).edges(List.of(node + "-" + next)).step();
            }

            states.put(node, "visited");
        }

        emit.at("done").say("Queue empty. BFS order: %s.", order)
                .var("order", order).nodes(states).step();
    }
}
