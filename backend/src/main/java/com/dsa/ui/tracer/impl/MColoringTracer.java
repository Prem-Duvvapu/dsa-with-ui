package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vertices are colored in order 0..n-1; a color is safe for a vertex only when no already-colored
 * neighbour already holds it. The first safe color found is tried immediately - if every later
 * vertex can then also be colored, the whole assignment stands. If not, that color is undone
 * and the next one is tried, so a dead end at vertex k reopens vertex k's own choice, not
 * just the vertex that failed.
 */
@Component
public class MColoringTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "m-coloring";
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
                        .help("Vertices to color so that no edge joins two same-colored vertices.")
                        .constraint("maxVertices", 8).constraint("maxEdges", 20)
                        // A triangle 0-1-2 that forces all three colors, plus a vertex 4
                        // that sees 1, 2 and 3. First-fit gives vertex 3 color 1, which
                        // leaves vertex 4 with nothing - so color 1 comes back off vertex 3
                        // and it retries with 2. The old default (K4 minus one edge) was
                        // colorable greedily in one pass: ten steps, not a single undo, in
                        // the problem whose whole subject is the undo.
                        .defaultValue(Map.of(
                                "vertices", 5,
                                "edges", java.util.List.of(
                                        java.util.List.of(0, 1), java.util.List.of(0, 2),
                                        java.util.List.of(1, 2), java.util.List.of(1, 4),
                                        java.util.List.of(2, 4), java.util.List.of(3, 4))))
                        .build(),
                InputField.of("m", FieldType.INT)
                        .label("Available colors (M)")
                        .range(1, 8)
                        .defaultValue(3)
                        .build());
    }

    /** The same triangle-containing graph with one fewer color than it needs — provably impossible. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "graph", Map.of(
                        "vertices", 5,
                        "edges", java.util.List.of(
                                java.util.List.of(0, 1), java.util.List.of(0, 2),
                                java.util.List.of(1, 2), java.util.List.of(1, 4),
                                java.util.List.of(2, 4), java.util.List.of(3, 4))),
                "m", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean graphColoring(boolean[][] adj, int m, int n) {
                   int[] color = new int[n];
                   // @a done
                   return solve(0, adj, color, m, n);
               }

               private boolean solve(int node, boolean[][] adj, int[] color, int m, int n) {
                   if (node == n) {
                       // @a allColored
                       return true;
                   }
                   for (int c = 1; c <= m; c++) {
                       if (!isSafe(node, adj, color, c, n)) {
                           // @a conflict
                           continue;
                       }
                       color[node] = c;
                       // @a assign
                       if (solve(node + 1, adj, color, m, n)) return true;
                       color[node] = 0;
                       // @a undo
                   }
                   return false;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int m = in.getInt("m");
        int n = graph.vertices();
        boolean[][] adj = new boolean[n][n];
        for (int[] e : graph.edges()) {
            adj[e[0]][e[1]] = true;
            adj[e[1]][e[0]] = true;
        }

        int[] color = new int[n];
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            states.put(i, "unvisited");
        }

        boolean ok = solve(0, adj, color, m, n, graph, states, emit);

        emit.at("done")
                .say(ok
                        ? String.format("Every vertex colored with at most %d colors. Assignment: %s.",
                                m, java.util.Arrays.toString(color))
                        : String.format("No assignment using only %d colors avoids every conflicting edge.", m))
                .var("possible", ok).var("colors", java.util.Arrays.toString(color))
                .graph(graph).nodes(states).step();
    }

    private boolean solve(int node, boolean[][] adj, int[] color, int m, int n, Inputs.GraphInput graph,
                           Map<Integer, String> states, StepEmitter emit) {
        emit.push("solve(node=" + node + ")");

        if (node == n) {
            emit.at("allColored")
                    .say("Every vertex 0..%d has a color. This assignment works.", n - 1)
                    .var("colors", java.util.Arrays.toString(color)).graph(graph).nodes(states).step();
            emit.pop();
            return true;
        }

        for (int c = 1; c <= m; c++) {
            boolean safe = true;
            for (int i = 0; i < n; i++) {
                if (adj[node][i] && color[i] == c) {
                    safe = false;
                    break;
                }
            }

            if (!safe) {
                emit.at("conflict")
                        .say("Color %d conflicts with a neighbour of vertex %d that already holds it - skip it.",
                                c, node)
                        .var("node", node).var("triedColor", c).graph(graph).nodes(states).step();
                continue;
            }

            color[node] = c;
            states.put(node, "visiting");
            emit.at("assign")
                    .say("Color %d is safe for vertex %d - assign it and %s.", c, node,
                            node + 1 == n
                                    ? "recurse once more, which finds no vertex left and checks the assignment"
                                    : "recurse into vertex " + (node + 1))
                    .var("node", node).var("assignedColor", c).graph(graph).nodes(states).step();

            if (solve(node + 1, adj, color, m, n, graph, states, emit)) {
                emit.pop();
                return true;
            }

            color[node] = 0;
            states.put(node, "unvisited");
            emit.at("undo")
                    .say("Coloring vertex %d with %d did not lead to a full solution - undo it and try the next color.",
                            node, c)
                    .var("node", node).graph(graph).nodes(states).step();
        }

        emit.pop();
        return false;
    }
}
