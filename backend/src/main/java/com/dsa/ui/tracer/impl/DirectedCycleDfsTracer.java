package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Detect a cycle in a directed graph by DFS with a recursion stack, traced on the owner's own
 * accepted code (from their Course Schedule submission): {@code visited} marks every vertex
 * ever explored, {@code recStack} only the vertices on the current DFS path. An edge into a
 * vertex still on the path closes a cycle. A vertex that is visited but off the path was
 * fully explored without finding one, so it is skipped. O(V + E).
 *
 * <p>Canvas: vertices on the current path are amber, finished ones grey, and the vertex an
 * edge closes the cycle on is outlined.
 */
@Component
public class DirectedCycleDfsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "directed-cycle-dfs";
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

    /** A DAG (diamond shape) instead of a cycle - the recursion unwinds cleanly for every node. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 4,
                "edges", List.of(List.of(0, 1), List.of(0, 2), List.of(1, 3), List.of(2, 3))));
    }

    @Override
    public String annotatedCode() {
        return """
               class Edge
               {
                   int src;
                   int dest;
                   public Edge(int s,int d)
                   {
                       src=s;
                       dest=d;
                   }
               }

               class Solution {
                   private boolean isCycleUtil(ArrayList<Edge>[] graph,int curr,boolean[] visited,boolean[] recStack)
                   {
                       // @a visit
                       visited[curr]=true;
                       recStack[curr]=true;

                       for (int i=0;i<graph[curr].size();i++)
                       {
                           Edge e=graph[curr].get(i);

                           if (recStack[e.dest])
                               // @a backEdge
                               return true;

                           // @a check
                           if (!visited[e.dest])
                               if (isCycleUtil(graph,e.dest,visited,recStack))
                                   // @a propagate
                                   return true;
                       }

                       // @a leave
                       recStack[curr]=false;
                       return false;
                   }

                   public boolean isCycle(ArrayList<Edge>[] graph)
                   {
                       // @a init
                       boolean[] visited=new boolean[graph.length];
                       boolean[] recStk=new boolean[graph.length];

                       for (int i=0;i<graph.length;i++)
                       {
                           if (!visited[i])
                               // @a start
                               if (isCycleUtil(graph,i,visited,recStk))
                                   // @a found
                                   return true;
                       }

                       // @a none
                       return false;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency(true);
        int v = graph.vertices();
        boolean[] visited = new boolean[v];
        boolean[] recStk = new boolean[v];
        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) {
            states.put(i, "unvisited");
        }

        emit.at("init").say("%d vertices, %d directed edges. visited[] marks every vertex ever explored; "
                        + "recStack[] marks only those on the current DFS path.", v, graph.edges().length)
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        for (int i = 0; i < v; i++) {
            if (visited[i]) {
                continue;
            }
            emit.at("start").say("Vertex %d is unvisited: start isCycleUtil(%d).", i, i)
                    .var("i", i).graph(layout.nodes(), layout.edges()).nodes(states).step();
            if (isCycleUtil(adj, i, visited, recStk, states, layout, emit)) {
                emit.at("found").say("isCycleUtil(%d) found an edge back into the current path: the graph "
                                + "has a directed cycle. Return true.", i)
                        .var("answer", true).graph(layout.nodes(), layout.edges()).nodes(states).step();
                return;
            }
        }

        emit.at("none").say("Every vertex was explored and no edge ever pointed back into the current path. "
                        + "There is no directed cycle. Return false.")
                .var("answer", false).graph(layout.nodes(), layout.edges()).nodes(states).step();
    }

    private static boolean isCycleUtil(List<List<Integer>> adj, int curr, boolean[] visited, boolean[] recStack,
                                       Map<Integer, String> states, GraphLayout.Layout layout, StepEmitter emit) {
        emit.push("isCycleUtil(" + curr + ")");
        visited[curr] = true;
        recStack[curr] = true;
        states.put(curr, "visiting");
        emit.at("visit").say("isCycleUtil(%d): mark %d visited and put it on the current path.", curr, curr)
                .var("curr", curr).var("path", path(recStack))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        for (int dest : adj.get(curr)) {
            if (recStack[dest]) {
                states.put(dest, "cycle");
                emit.at("backEdge").say("Edge %d -> %d points at %d, which is still on the current path %s: "
                                + "following it leads back round to %d. Cycle - return true.",
                                curr, dest, dest, path(recStack), curr)
                        .var("curr", curr).var("dest", dest).var("path", path(recStack))
                        .graph(layout.nodes(), layout.edges()).nodes(states)
                        .edges(List.of(curr + "-" + dest)).step();
                emit.pop();
                return true;
            }
            if (visited[dest]) {
                emit.at("check").say("Edge %d -> %d: %d is visited but off the current path - it was fully "
                                + "explored without finding a cycle. Skip it.", curr, dest, dest)
                        .var("curr", curr).var("dest", dest)
                        .graph(layout.nodes(), layout.edges()).nodes(states)
                        .edges(List.of(curr + "-" + dest)).step();
                continue;
            }
            emit.at("check").say("Edge %d -> %d: %d is unvisited. Recurse into isCycleUtil(%d).",
                            curr, dest, dest, dest)
                    .var("curr", curr).var("dest", dest)
                    .graph(layout.nodes(), layout.edges()).nodes(states)
                    .edges(List.of(curr + "-" + dest)).step();
            if (isCycleUtil(adj, dest, visited, recStack, states, layout, emit)) {
                emit.at("propagate").say("isCycleUtil(%d) found a cycle, so isCycleUtil(%d) returns true too.",
                                dest, curr)
                        .var("curr", curr).graph(layout.nodes(), layout.edges()).nodes(states).step();
                emit.pop();
                return true;
            }
        }

        recStack[curr] = false;
        states.put(curr, "visited");
        emit.at("leave").say("No edge out of %d closes a cycle. Take %d off the current path and return false.",
                        curr, curr)
                .var("curr", curr).var("path", path(recStack))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
        emit.pop();
        return false;
    }

    private static String path(boolean[] recStack) {
        List<Integer> on = new ArrayList<>();
        for (int i = 0; i < recStack.length; i++) {
            if (recStack[i]) on.add(i);
        }
        return on.toString();
    }
}
