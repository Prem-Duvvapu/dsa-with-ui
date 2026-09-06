package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Cycle detection in an undirected graph by BFS, carrying each queued vertex's parent so
 * that finding it again through a *different* edge (not the one it was discovered from)
 * proves a second path exists — i.e. a cycle.
 *
 * <p>Paired with {@code undirected-cycle-dfs}: the default here HAS a cycle and its
 * alternate does not, while that tracer's default/alternate run the other way round, so the
 * cycle and no-cycle code paths are each exercised by a genuinely different graph shape
 * across the pair rather than a relabelled copy of the same one.
 */
@Component
public class UndirectedCycleBfsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "undirected-cycle-bfs";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
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
                                "vertices", 4,
                                "edges", List.of(
                                        List.of(0, 1), List.of(0, 2), List.of(1, 3), List.of(2, 3))))
                        .build());
    }

    /** A tree (one fewer edge than a spanning cycle would need) - no cycle exists. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 5,
                "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(1, 3), List.of(3, 4))));
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isCycle(int v, List<List<Integer>> adj) {
                   // @a init
                   boolean[] vis = new boolean[v];
                   for (int i = 0; i < v; i++) {
                       if (!vis[i]) {
                           // @a component
                           if (checkForCycle(i, adj, vis)) {
                               return true;
                           }
                       }
                   }
                   // @a noCycle
                   return false;
               }

               private boolean checkForCycle(int src, List<List<Integer>> adj, boolean[] vis) {
                   // @a seed
                   vis[src] = true;
                   Queue<int[]> queue = new LinkedList<>();
                   queue.add(new int[]{src, -1});

                   while (!queue.isEmpty()) {
                       // @a poll
                       int[] pair = queue.poll();
                       int node = pair[0], parent = pair[1];

                       for (int next : adj.get(node)) {
                           if (!vis[next]) {
                               // @a enqueue
                               vis[next] = true;
                               queue.add(new int[]{next, node});
                           } else if (next != parent) {
                               // @a cycleDetected
                               return true;
                           }
                       }
                   }
                   return false;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency();
        int v = graph.vertices();
        boolean[] vis = new boolean[v];
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) {
            states.put(i, "unvisited");
        }

        emit.at("init").say("%d vertices, %d edges. BFS each unvisited component, remembering each node's parent.",
                        v, graph.edges().length)
                .graph(graph).nodes(states).step();

        boolean cycle = false;
        for (int i = 0; i < v && !cycle; i++) {
            if (vis[i]) {
                continue;
            }
            emit.at("component").say("%d starts a new component. Seed the queue with (node=%d, parent=-1).", i, i)
                    .var("src", i).graph(graph).nodes(states).step();

            cycle = checkForCycle(i, adj, vis, states, graph, emit);
        }

        if (!cycle) {
            emit.at("noCycle").say("Every component explored; no vertex was reached twice through different edges. No cycle.")
                    .graph(graph).nodes(states).step();
        }
    }

    private boolean checkForCycle(int src, List<List<Integer>> adj, boolean[] vis,
                                   Map<Integer, String> states, Inputs.GraphInput graph, StepEmitter emit) {
        vis[src] = true;
        states.put(src, "queued");
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{src, -1});

        emit.at("seed").say("Mark %d seen; its parent is -1 (the root of this BFS).", src)
                .var("node", src).graph(graph).nodes(states).queue(pairs(queue)).step();

        while (!queue.isEmpty()) {
            int[] pair = queue.poll();
            int node = pair[0], parent = pair[1];
            states.put(node, "visiting");

            emit.at("poll").say("Dequeue %d (parent %d).", node, parent)
                    .var("node", node).var("parent", parent)
                    .graph(graph).nodes(states).queue(pairs(queue)).step();

            for (int next : adj.get(node)) {
                if (!vis[next]) {
                    vis[next] = true;
                    states.put(next, "queued");
                    queue.add(new int[]{next, node});
                    emit.at("enqueue").say("%d is new. Mark it seen and enqueue it with parent %d.", next, node)
                            .var("node", node).var("neighbour", next)
                            .graph(graph).nodes(states).edges(List.of(node + "-" + next))
                            .queue(pairs(queue)).step();
                } else if (next != parent) {
                    states.put(next, "cycle");
                    emit.at("cycleDetected").say(
                                    "%d is already visited and is NOT %d's parent (%d) - a second path reaches it, so a cycle exists.",
                                    next, node, parent)
                            .var("node", node).var("neighbour", next)
                            .graph(graph).nodes(states).edges(List.of(node + "-" + next))
                            .queue(pairs(queue)).step();
                    return true;
                }
            }

            states.put(node, "visited");
        }
        return false;
    }

    private static List<String> pairs(Deque<int[]> queue) {
        List<String> out = new ArrayList<>();
        for (int[] p : queue) {
            out.add("(" + p[0] + "," + p[1] + ")");
        }
        return out;
    }
}
