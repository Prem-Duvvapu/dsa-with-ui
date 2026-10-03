package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Is Graph Bipartite? (LeetCode 785), traced on the owner's own accepted submission: 2-colour
 * each uncoloured component by DFS, giving every neighbour the other colour, and stop at the
 * first edge whose two ends got the same colour. O(V + E).
 *
 * <p>Canvas: colour 0 is drawn grey, colour 1 green, and the vertex where a clash is found
 * amber.
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
               class Solution {
                   public boolean isBipartite(int[][] graph) {
                       // @a init
                       int n = graph.length;
                       int[] color = new int[n];

                       Arrays.fill(color, -1);

                       for (int i=0;i<n;i++) {
                           if (color[i] == -1) {
                               // @a component
                               if (!dfs(i,0,color,graph))
                                   // @a notBipartite
                                   return false;
                           }
                       }

                       // @a bipartite
                       return true;
                   }

                   private boolean dfs(int curr,int currColor,int[] color,int[][] graph) {
                       // @a colour
                       color[curr] = currColor;

                       for (int ngbr: graph[curr]) {
                           if (color[ngbr] == -1) {
                               // @a recurse
                               if (!dfs(ngbr,1 - currColor,color,graph))
                                   // @a propagate
                                   return false;
                           } else if (color[ngbr] == currColor) {
                               // @a conflict
                               return false;
                           }
                       }

                       // @a return
                       return true;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency();
        int n = graph.vertices();
        int[] color = new int[n];
        Arrays.fill(color, -1);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            states.put(i, "unvisited");
        }

        emit.at("init").say("%d vertices, %d edges, none coloured yet (-1). Every edge must join a "
                        + "colour-0 vertex to a colour-1 vertex.", n, graph.edges().length)
                .var("color", Arrays.toString(color)).graph(graph).nodes(states).step();

        for (int i = 0; i < n; i++) {
            if (color[i] != -1) {
                continue;
            }
            emit.at("component").say("Vertex %d is uncoloured, so it starts a new component: dfs(%d, 0).", i, i)
                    .var("i", i).var("color", Arrays.toString(color)).graph(graph).nodes(states).step();
            if (!dfs(i, 0, adj, color, states, graph, emit)) {
                emit.at("notBipartite").say("dfs(%d, 0) found a clash, so the graph cannot be split into two "
                                + "sides. Return false.", i)
                        .var("color", Arrays.toString(color)).var("answer", false)
                        .graph(graph).nodes(states).step();
                return;
            }
        }

        emit.at("bipartite").say("Every vertex is coloured and no edge joins two vertices of the same "
                        + "colour: the grey and green vertices are the two sides. Return true.")
                .var("color", Arrays.toString(color)).var("answer", true)
                .graph(graph).nodes(states).step();
    }

    private static boolean dfs(int curr, int currColor, List<List<Integer>> adj, int[] color,
                               Map<Integer, String> states, Inputs.GraphInput graph, StepEmitter emit) {
        emit.push("dfs(" + curr + "," + currColor + ")");
        color[curr] = currColor;
        states.put(curr, currColor == 0 ? "visited" : "done");
        emit.at("colour").say("dfs(%d, %d): give vertex %d colour %d, then check its neighbours.",
                        curr, currColor, curr, currColor)
                .var("curr", curr).var("currColor", currColor).var("color", Arrays.toString(color))
                .graph(graph).nodes(states).step();

        for (int ngbr : adj.get(curr)) {
            if (color[ngbr] == -1) {
                emit.at("recurse").say("Neighbour %d is uncoloured: it must take the other colour, %d. "
                                + "Recurse into dfs(%d, %d).", ngbr, 1 - currColor, ngbr, 1 - currColor)
                        .var("curr", curr).var("ngbr", ngbr)
                        .graph(graph).nodes(states).edges(List.of(curr + "-" + ngbr)).step();
                if (!dfs(ngbr, 1 - currColor, adj, color, states, graph, emit)) {
                    emit.at("propagate").say("dfs(%d, %d) reported a clash further down, so dfs(%d, %d) "
                                    + "returns false too.", ngbr, 1 - currColor, curr, currColor)
                            .var("curr", curr).graph(graph).nodes(states).step();
                    emit.pop();
                    return false;
                }
            } else if (color[ngbr] == currColor) {
                states.put(ngbr, "current");
                emit.at("conflict").say("Neighbour %d already has colour %d - the same as %d. An edge "
                                + "joins two vertices of one colour. Return false.", ngbr, currColor, curr)
                        .var("curr", curr).var("ngbr", ngbr)
                        .graph(graph).nodes(states).edges(List.of(curr + "-" + ngbr)).step();
                emit.pop();
                return false;
            }
        }

        emit.at("return").say("Every neighbour of %d has the opposite colour. dfs(%d, %d) returns true.",
                        curr, curr, currColor)
                .var("curr", curr).graph(graph).nodes(states).step();
        emit.pop();
        return true;
    }
}
