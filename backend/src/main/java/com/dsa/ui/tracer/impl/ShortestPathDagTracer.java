package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Shortest Path in a Directed Acyclic Graph (GfG), in the owner's style: their {@code Edge}
 * class and {@code buildGraph}, a DFS topological sort onto a stack, then one relaxation pass in
 * topological order. Unreachable vertices become -1, as in their submission.
 *
 * <p>The owner's submission ran Dijkstra here. That is correct (GfG's weights are non-negative)
 * but O(E log V); a DAG lets every vertex be settled in topological order in O(V + E) with no
 * priority queue, which is the answer an interviewer expects for this problem. The displayed
 * code says so in a comment. Weights are kept non-negative, as in the GfG problem, so the -1
 * "unreachable" marker can never be confused with a real distance.
 */
@Component
public class ShortestPathDagTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "shortest-path-dag";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Weighted DAG")
                        .help("Vertex count plus directed, weighted edges [from, to, weight], weights 0 to 100. "
                                + "Assumed acyclic.")
                        .directed()
                        .weighted()
                        .weights(0, 100)
                        .constraint("maxVertices", 20)
                        .constraint("maxEdges", 48)
                        .defaultValue(Map.of(
                                "vertices", 5,
                                "edges", List.of(
                                        List.of(0, 1, 2), List.of(0, 2, 4), List.of(1, 2, 1),
                                        List.of(1, 3, 7), List.of(2, 3, 3), List.of(3, 4, 2))))
                        .build(),
                InputField.of("start", FieldType.INT)
                        .label("Source vertex")
                        .range(0, 19)
                        .defaultValue(0)
                        .build());
    }

    /** A DAG with a vertex the source can never reach. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "graph", Map.of(
                        "vertices", 4,
                        "edges", List.of(List.of(0, 1, 5), List.of(1, 2, 3))),
                "start", 0);
    }

    @Override
    public String annotatedCode() {
        return """
               class Edge
               {
                   int src;
                   int dest;
                   int wt;

                   Edge(int s,int d,int w)
                   {
                       src=s;
                       dest=d;
                       wt=w;
                   }
               }

               // Changed from your submission: it ran Dijkstra, O(E log V). In a DAG, taking the
               // vertices in topological order and relaxing each one's edges once is enough -
               // O(V + E), no priority queue, and still right if weights were negative.
               class Solution {
                   public void buildGraph(ArrayList<Edge>[] graph,int v,int[][] edges)
                   {
                       for (int i=0;i<v;i++)
                           graph[i]=new ArrayList<>();

                       for (int[] e: edges)
                       {
                           graph[e[0]].add(new Edge(e[0],e[1],e[2]));
                       }
                   }

                   public void topoSort(ArrayList<Edge>[] graph,int curr,boolean[] visited,Stack<Integer> stack)
                   {
                       // @a visit
                       visited[curr]=true;

                       for (int i=0;i<graph[curr].size();i++)
                       {
                           Edge e=graph[curr].get(i);
                           if (!visited[e.dest])
                               topoSort(graph,e.dest,visited,stack);
                       }

                       // @a finish
                       stack.push(curr);
                   }

                   public int[] shortestPath(int N,int M,int[][] edges,int src) {
                       // @a init
                       ArrayList<Edge>[] graph=new ArrayList[N];
                       buildGraph(graph,N,edges);
                       boolean[] visited=new boolean[N];
                       Stack<Integer> stack=new Stack<>();

                       for (int i=0;i<N;i++)
                           if (!visited[i])
                               topoSort(graph,i,visited,stack);

                       // @a dist
                       int[] distance=new int[N];
                       Arrays.fill(distance,Integer.MAX_VALUE);
                       distance[src]=0;

                       while (!stack.isEmpty())
                       {
                           // @a pop
                           int curr=stack.pop();

                           if (distance[curr]!=Integer.MAX_VALUE)
                           {
                               for (int i=0;i<graph[curr].size();i++)
                               {
                                   Edge e=graph[curr].get(i);
                                   if (distance[e.src]+e.wt<distance[e.dest])
                                       // @a relax
                                       distance[e.dest]=distance[e.src]+e.wt;
                               }
                           }
                       }

                       for (int i=0;i<N;i++)
                           if (distance[i]==Integer.MAX_VALUE)
                               // @a unreachable
                               distance[i]=-1;

                       // @a done
                       return distance;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int N = graph.vertices();
        int src = in.getInt("start");
        if (src >= N) {
            throw new InputValidationException(Map.of("start",
                    "This graph only has vertices 0.." + (N - 1) + "."));
        }
        List<List<int[]>> out = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            out.add(new ArrayList<>());
        }
        for (int[] e : graph.edges()) {
            out.get(e[0]).add(new int[]{e[1], e[2]});
        }
        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < N; i++) {
            states.put(i, "unvisited");
        }
        boolean[] visited = new boolean[N];
        Deque<Integer> stack = new ArrayDeque<>();

        emit.at("init").say("%d vertices, %d weighted edges. First a DFS lists the vertices in topological "
                        + "order: a vertex is pushed only after everything it points to.",
                        N, graph.edges().length)
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
        for (int i = 0; i < N; i++) {
            if (!visited[i]) {
                topoSort(out, i, visited, stack, states, layout, emit);
            }
        }

        int[] distance = new int[N];
        Arrays.fill(distance, Integer.MAX_VALUE);
        distance[src] = 0;
        for (int i = 0; i < N; i++) {
            states.put(i, "unvisited");
        }
        states.put(src, "queued");
        emit.at("dist").say("The stack holds a topological order, top first: %s. Every distance starts at "
                        + "infinity except the source: distance[%d] = 0.", stack, src)
                .var("distance", show(distance)).graph(layout.nodes(), layout.edges()).nodes(states)
                .stack(stack).step();

        while (!stack.isEmpty()) {
            int curr = stack.pop();
            if (distance[curr] == Integer.MAX_VALUE) {
                emit.at("pop").say("Pop %d. No path from %d has reached it, so it has no distance to pass on.",
                                curr, src)
                        .var("curr", curr).var("distance", show(distance))
                        .graph(layout.nodes(), layout.edges()).nodes(states).stack(stack).step();
                continue;
            }
            states.put(curr, "visiting");
            emit.at("pop").say("Pop %d. Every edge into it came from a vertex earlier in the order, so "
                            + "distance[%d] = %d is final. Relax its outgoing edges.", curr, curr, distance[curr])
                    .var("curr", curr).var("distance", show(distance))
                    .graph(layout.nodes(), layout.edges()).nodes(states).stack(stack).step();
            for (int[] e : out.get(curr)) {
                if (distance[curr] + e[1] < distance[e[0]]) {
                    int before = distance[e[0]];
                    distance[e[0]] = distance[curr] + e[1];
                    emit.at("relax").say("%d -> %d costs %d + %d = %d, better than %s: distance[%d] = %d.",
                                    curr, e[0], distance[curr], e[1], distance[e[0]],
                                    before == Integer.MAX_VALUE ? "infinity" : String.valueOf(before), e[0], distance[e[0]])
                            .var("curr", curr).var("distance", show(distance))
                            .graph(layout.nodes(), layout.edges()).nodes(states)
                            .edges(List.of(curr + "-" + e[0])).stack(stack).step();
                }
            }
            states.put(curr, "done");
        }

        for (int i = 0; i < N; i++) {
            if (distance[i] == Integer.MAX_VALUE) {
                distance[i] = -1;
                states.put(i, "cycle");
                emit.at("unreachable").say("No path from %d reaches vertex %d: distance[%d] = -1.", src, i, i)
                        .var("distance", show(distance)).graph(layout.nodes(), layout.edges()).nodes(states).step();
            }
        }
        emit.at("done").say("Return distance = %s.", Arrays.toString(distance))
                .var("distance", Arrays.toString(distance))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
    }

    private static void topoSort(List<List<int[]>> out, int curr, boolean[] visited, Deque<Integer> stack,
                                 Map<Integer, String> states, GraphLayout.Layout layout, StepEmitter emit) {
        emit.push("topoSort(" + curr + ")");
        visited[curr] = true;
        states.put(curr, "visiting");
        emit.at("visit").say("topoSort(%d): mark %d visited, then explore every vertex it points to first.",
                        curr, curr)
                .var("curr", curr).graph(layout.nodes(), layout.edges()).nodes(states).stack(stack).step();
        for (int[] e : out.get(curr)) {
            if (!visited[e[0]]) {
                topoSort(out, e[0], visited, stack, states, layout, emit);
            }
        }
        stack.push(curr);
        states.put(curr, "visited");
        emit.at("finish").say("Every successor of vertex %d is already on the stack, so push %d on top.", curr, curr)
                .var("curr", curr).graph(layout.nodes(), layout.edges()).nodes(states).stack(stack).step();
        emit.pop();
    }

    private static String show(int[] distance) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < distance.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(distance[i] == Integer.MAX_VALUE ? "inf" : String.valueOf(distance[i]));
        }
        return sb.append(']').toString();
    }
}
