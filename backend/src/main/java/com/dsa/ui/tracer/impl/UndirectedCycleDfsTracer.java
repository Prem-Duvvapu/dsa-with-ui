package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Cycle detection in an undirected graph by recursive DFS, threading each call's parent
 * down so that reaching an already-visited vertex that is NOT the caller's parent proves a
 * second path exists.
 *
 * <p>Paired with {@code undirected-cycle-bfs}: this tracer's default is a tree (no cycle)
 * and its alternate has one, the opposite pairing from the BFS tracer, so across the two
 * tracers both the cycle and no-cycle branches are exercised by a genuinely different graph
 * shape rather than a relabelled copy of the same one.
 */
@Component
public class UndirectedCycleDfsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "undirected-cycle-dfs";
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
                                "vertices", 5,
                                "edges", List.of(
                                        List.of(0, 1), List.of(1, 2), List.of(1, 3), List.of(3, 4))))
                        .build());
    }

    /** One extra edge closing 0-1-3-2-0 into a cycle. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 4,
                "edges", List.of(List.of(0, 1), List.of(0, 2), List.of(1, 3), List.of(2, 3))));
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
                           if (dfs(i, -1, adj, vis)) {
                               return true;
                           }
                       }
                   }
                   // @a noCycle
                   return false;
               }

               private boolean dfs(int node, int parent, List<List<Integer>> adj, boolean[] vis) {
                   // @a visit
                   vis[node] = true;
                   for (int next : adj.get(node)) {
                       if (!vis[next]) {
                           // @a recurse
                           if (dfs(next, node, adj, vis)) {
                               return true;
                           }
                       } else if (next != parent) {
                           // @a cycleDetected
                           return true;
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

        emit.at("init").say("%d vertices, %d edges. DFS each unvisited component, remembering each call's parent.",
                        v, graph.edges().length)
                .graph(graph).nodes(states).step();

        boolean cycle = false;
        for (int i = 0; i < v && !cycle; i++) {
            if (vis[i]) {
                continue;
            }
            emit.at("component").say("%d starts a new component. dfs(%d, parent=-1).", i, i)
                    .var("src", i).graph(graph).nodes(states).step();

            cycle = dfs(i, -1, adj, vis, states, graph, emit);
        }

        if (!cycle) {
            emit.at("noCycle").say("Every component explored with no back-edge to a non-parent. No cycle.")
                    .graph(graph).nodes(states).step();
        }
    }

    private boolean dfs(int node, int parent, List<List<Integer>> adj, boolean[] vis,
                         Map<Integer, String> states, Inputs.GraphInput graph, StepEmitter emit) {
        emit.push("dfs(" + node + "," + parent + ")");
        vis[node] = true;
        states.put(node, "visiting");
        emit.at("visit").say("Enter %d with parent %d.", node, parent)
                .var("node", node).var("parent", parent).graph(graph).nodes(states).step();

        for (int next : adj.get(node)) {
            if (!vis[next]) {
                emit.at("recurse").say("%d is unvisited - descend into it with parent=%d.", next, node)
                        .var("node", node).var("neighbour", next)
                        .graph(graph).nodes(states).edges(List.of(node + "-" + next)).step();
                if (dfs(next, node, adj, vis, states, graph, emit)) {
                    emit.pop();
                    return true;
                }
            } else if (next != parent) {
                states.put(next, "cycle");
                emit.at("cycleDetected").say(
                                "%d is visited and is NOT %d's parent (%d) - a back-edge exists, so a cycle exists.",
                                next, node, parent)
                        .var("node", node).var("neighbour", next)
                        .graph(graph).nodes(states).edges(List.of(node + "-" + next)).step();
                emit.pop();
                return true;
            }
        }

        states.put(node, "visited");
        emit.pop();
        return false;
    }
}
