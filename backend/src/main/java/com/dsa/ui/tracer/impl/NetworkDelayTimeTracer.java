package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Network Delay Time (LeetCode 743), traced on the owner's own accepted submission: Dijkstra with
 * a priority queue of {@code Node(dest, wt)}, a {@code (int)1e5} "not reached" sentinel, then the
 * largest arrival time (or -1 if any node was never reached). Optimal: O(E log V).
 *
 * <p>The one change is numbering: the app's nodes are 0 to n - 1 where LeetCode's are 1 to n, so
 * the arrays have n slots and the loops start at 0. The displayed code says so in a comment.
 * The priority queue is a real {@link PriorityQueue} with the same comparator, so ties pop in
 * the same order the submission's would.
 */
@Component
public class NetworkDelayTimeTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "network-delay-time";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Signal network")
                        .help("Node count plus directed links [from, to, travel time]. Nodes are 0-based.")
                        .directed()
                        .weighted()
                        .weights(0, 1000)
                        .constraint("maxVertices", 14)
                        .constraint("maxEdges", 36)
                        .defaultValue(Map.of(
                                "vertices", 5,
                                "edges", List.of(
                                        List.of(0, 1, 2), List.of(0, 2, 5), List.of(1, 2, 1),
                                        List.of(1, 3, 4), List.of(2, 3, 1), List.of(3, 4, 3),
                                        List.of(4, 3, 1))))
                        .build(),
                InputField.of("source", FieldType.INT)
                        .label("Broadcasting node")
                        .range(0, 13)
                        .defaultValue(0)
                        .build());
    }

    /** A network whose last node has no inbound link at all, so the answer is -1. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "graph", Map.of(
                        "vertices", 4,
                        "edges", List.of(List.of(0, 1, 1), List.of(1, 2, 2))),
                "source", 0);
    }

    @Override
    public String annotatedCode() {
        return """
               class Node {
                   int dest;
                   int wt;

                    Node(int dest,int wt) {
                       this.dest=dest;
                       this.wt=wt;
                    }
               }

               // Changed from your submission: nodes are numbered 0 to n - 1 here (LeetCode numbers
               // them 1 to n), so the arrays have n slots and the loops start at 0.
               class Solution {
                   public int networkDelayTime(int[][] times, int n, int k) {
                       // @a init
                       int initialSource=k;
                       List<List<Node>> adjList=new ArrayList<>();
                       int[] dist=new int[n];
                       PriorityQueue<Node> q=new PriorityQueue<>((x,y)->(x.wt-y.wt));
                       q.add(new Node(initialSource,0));

                       Arrays.fill(dist,(int)1e5);
                       dist[initialSource]=0;

                       for (int i=0;i<n;i++)
                           adjList.add(new ArrayList<>());

                       for (int[] e: times) {
                           int u=e[0];
                           int v=e[1];
                           int w=e[2];

                           adjList.get(u).add(new Node(v,w));
                       }

                       while (!q.isEmpty()) {
                           // @a poll
                           Node curr=q.poll();
                           int currSrc=curr.dest;
                           int currWt=curr.wt;

                           for (Node neighbour: adjList.get(currSrc)) {
                               if (dist[currSrc]+neighbour.wt<dist[neighbour.dest]) {
                                   // @a relax
                                   dist[neighbour.dest]=dist[currSrc]+neighbour.wt;
                                   q.add(new Node(neighbour.dest,dist[neighbour.dest]));
                               }
                           }
                       }

                       for (int i=0;i<n;i++)
                           if (dist[i]==(int)1e5)
                               // @a unreachable
                               return -1;

                       // @a done
                       int maxTime=-1;
                       for (int i=0;i<n;i++)
                           maxTime=Math.max(maxTime,dist[i]);

                       return maxTime;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int k = in.getInt("source");
        int n = graph.vertices();
        if (k >= n) {
            throw new InputValidationException(Map.of("source",
                    "This network only has nodes 0.." + (n - 1) + "."));
        }
        final int unreached = (int) 1e5;
        List<List<int[]>> adjList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjList.add(new ArrayList<>());
        }
        for (int[] e : graph.edges()) {
            adjList.get(e[0]).add(new int[]{e[1], e[2]});
        }
        int[] dist = new int[n];
        Arrays.fill(dist, unreached);
        dist[k] = 0;
        PriorityQueue<int[]> q = new PriorityQueue<>((x, y) -> (x[1] - y[1]));
        q.add(new int[]{k, 0});

        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            states.put(i, "unvisited");
        }
        states.put(k, "queued");
        emit.at("init").say("%d nodes. The signal starts at node %d at time 0; every other node starts at "
                        + "1e5, meaning \"not reached\". The priority queue always hands back the earliest "
                        + "known arrival.", n, k)
                .var("dist", show(dist, unreached)).graph(layout.nodes(), layout.edges()).nodes(states)
                .queue(pq(q)).step();

        while (!q.isEmpty()) {
            int[] curr = q.poll();
            int currSrc = curr[0];
            int currWt = curr[1];
            states.put(currSrc, "visiting");
            emit.at("poll").say(currWt > dist[currSrc]
                            ? "Poll node " + currSrc + " at time " + currWt + ". It was already reached sooner (at "
                                    + dist[currSrc] + "), so none of its links can improve anything."
                            : "Poll node " + currSrc + " at time " + currWt + ": the earliest arrival still waiting. "
                                    + "Try each link out of it.")
                    .var("currSrc", currSrc).var("currWt", currWt).var("dist", show(dist, unreached))
                    .graph(layout.nodes(), layout.edges()).nodes(states).queue(pq(q)).step();

            for (int[] neighbour : adjList.get(currSrc)) {
                if (dist[currSrc] + neighbour[1] < dist[neighbour[0]]) {
                    int before = dist[neighbour[0]];
                    dist[neighbour[0]] = dist[currSrc] + neighbour[1];
                    q.add(new int[]{neighbour[0], dist[neighbour[0]]});
                    states.put(neighbour[0], "queued");
                    emit.at("relax").say("%d -> %d arrives at %d + %d = %d, earlier than %s. dist[%d] = %d.",
                                    currSrc, neighbour[0], dist[currSrc], neighbour[1], dist[neighbour[0]],
                                    before == unreached ? "never" : String.valueOf(before), neighbour[0], dist[neighbour[0]])
                            .var("currSrc", currSrc).var("dist", show(dist, unreached))
                            .graph(layout.nodes(), layout.edges()).nodes(states)
                            .edges(List.of(currSrc + "-" + neighbour[0])).queue(pq(q)).step();
                }
            }
            states.put(currSrc, "done");
        }

        for (int i = 0; i < n; i++) {
            if (dist[i] == unreached) {
                states.put(i, "cycle");
                emit.at("unreachable").say("Node %d still holds 1e5: no chain of links reaches it, so the "
                                + "signal never gets there. Return -1.", i)
                        .var("dist", show(dist, unreached)).var("answer", -1)
                        .graph(layout.nodes(), layout.edges()).nodes(states).step();
                return;
            }
        }
        int maxTime = -1;
        for (int i = 0; i < n; i++) {
            maxTime = Math.max(maxTime, dist[i]);
        }
        emit.at("done").say("Every node is reached. The last arrival is the largest time in dist: %d.", maxTime)
                .var("dist", show(dist, unreached)).var("answer", maxTime)
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
    }

    /** The queue, earliest first, as "node@time". */
    private static List<String> pq(PriorityQueue<int[]> q) {
        List<int[]> items = new ArrayList<>(q);
        items.sort((x, y) -> x[1] - y[1]);
        List<String> out = new ArrayList<>();
        for (int[] it : items) {
            out.add(it[0] + "@" + it[1]);
        }
        return out;
    }

    private static String show(int[] dist, int unreached) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < dist.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(dist[i] == unreached ? "1e5" : String.valueOf(dist[i]));
        }
        return sb.append(']').toString();
    }
}
