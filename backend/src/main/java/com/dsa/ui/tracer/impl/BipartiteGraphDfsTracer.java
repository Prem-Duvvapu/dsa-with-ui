package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * A graph is bipartite iff its vertices can be 2-coloured so that no edge joins two vertices
 * of the same colour. DFS assigns the opposite colour to every unvisited neighbour and, on an
 * already-coloured neighbour, checks it actually got the opposite colour - a same-colour
 * neighbour is a direct proof of an odd cycle, which is exactly what breaks bipartiteness.
 */
@Component
public class BipartiteGraphDfsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bipartite-graph-dfs";
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
                                        List.of(0, 1), List.of(1, 2), List.of(2, 3), List.of(3, 0))))
                        .build());
    }

    /** A triangle - an odd cycle, so no valid 2-colouring exists. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 3,
                "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(2, 0))));
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isBipartite(int v, List<List<Integer>> adj) {
                   // @a init
                   int[] color = new int[v];
                   Arrays.fill(color, -1);
                   for (int i = 0; i < v; i++) {
                       if (color[i] == -1) {
                           // @a component
                           if (!dfs(i, 0, adj, color)) {
                               return false;
                           }
                       }
                   }
                   // @a bipartite
                   return true;
               }

               private boolean dfs(int node, int col, List<List<Integer>> adj, int[] color) {
                   // @a colorNode
                   color[node] = col;
                   for (int next : adj.get(node)) {
                       if (color[next] == -1) {
                           // @a recurse
                           if (!dfs(next, 1 - col, adj, color)) {
                               return false;
                           }
                       } else if (color[next] == col) {
                           // @a conflict
                           return false;
                       }
                   }
                   return true;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency();
        int v = graph.vertices();
        int[] color = new int[v];
        Arrays.fill(color, -1);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) {
            states.put(i, "unvisited");
        }

        emit.at("init").say("%d vertices, %d edges. 2-colour each unvisited component by DFS.",
                        v, graph.edges().length)
                .graph(graph).nodes(states).step();

        boolean bipartite = true;
        for (int i = 0; i < v && bipartite; i++) {
            if (color[i] != -1) {
                continue;
            }
            emit.at("component").say("%d starts a new component. Colour it 0.", i)
                    .var("src", i).graph(graph).nodes(states).step();

            bipartite = dfs(i, 0, adj, color, states, graph, emit);
        }

        emit.at(bipartite ? "bipartite" : "conflict")
                .say(bipartite
                        ? "Every component was 2-coloured with no adjacent same-colour pair. Bipartite."
                        : "A conflict ended the search early. Not bipartite.")
                .graph(graph).nodes(states).step();
    }

    private boolean dfs(int node, int col, List<List<Integer>> adj, int[] color,
                         Map<Integer, String> states, Inputs.GraphInput graph, StepEmitter emit) {
        emit.push("dfs(" + node + "," + col + ")");
        color[node] = col;
        states.put(node, col == 0 ? "queued" : "visiting");
        emit.at("colorNode").say("Colour %d = %d.", node, col)
                .var("node", node).var("color", col)
                .graph(graph).nodes(states).step();

        for (int next : adj.get(node)) {
            if (color[next] == -1) {
                emit.at("recurse").say("%d is uncoloured - descend into it with colour %d.", next, 1 - col)
                        .var("node", node).var("neighbour", next)
                        .graph(graph).nodes(states).edges(List.of(node + "-" + next)).step();
                if (!dfs(next, 1 - col, adj, color, states, graph, emit)) {
                    emit.pop();
                    return false;
                }
            } else if (color[next] == col) {
                states.put(next, "cycle");
                emit.at("conflict").say(
                                "%d already has colour %d, the SAME as %d - two adjacent vertices share a colour. Not bipartite.",
                                next, color[next], node)
                        .var("node", node).var("neighbour", next)
                        .graph(graph).nodes(states).edges(List.of(node + "-" + next)).step();
                emit.pop();
                return false;
            }
        }

        emit.pop();
        return true;
    }
}
