package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Number of Ways to Arrive at Destination (LeetCode 1976), traced on the owner's own accepted
 * submission: Dijkstra from 0 that also counts shortest routes. A strictly faster arrival at a
 * node replaces its count with the count of the node it came from; an equally fast one adds to
 * it. Optimal: O(E log V).
 *
 * <p>The submission does not skip outdated queue entries, and it does not need to: a node polled
 * at a time later than its best can never improve or tie a neighbour's best (road times are at
 * least 1), so those polls change nothing. The trace shows them doing nothing.
 */
@Component
public class NumberOfWaysDestinationTracer implements AlgorithmTracer {

    private static final int MOD = 1_000_000_007;

    @Override
    public String id() {
        return "number-of-ways-destination";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Road network")
                        .help("Intersection count plus bidirectional roads [from, to, time]. Routes run "
                                + "from 0 to the last intersection. The default is LeetCode 1976's first "
                                + "example, whose answer is 4.")
                        .weighted()
                        .weights(1, 1000)
                        .constraint("maxVertices", 12)
                        .constraint("maxEdges", 30)
                        .defaultValue(Map.of(
                                "vertices", 7,
                                "edges", List.of(
                                        List.of(0, 6, 7), List.of(0, 1, 2), List.of(1, 2, 3),
                                        List.of(1, 3, 3), List.of(6, 3, 3), List.of(3, 5, 1),
                                        List.of(6, 5, 1), List.of(2, 5, 1), List.of(0, 4, 5),
                                        List.of(4, 6, 2))))
                        .build());
    }

    /**
     * LeetCode 1976's second example: two intersections joined by a single road, so there
     * is exactly one shortest route and no tie ever arises.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 2,
                "edges", List.of(List.of(1, 0, 10))));
    }

    @Override
    public String annotatedCode() {
        return """
               class Pair {
                   long time;
                   int node;

                   Pair(long time,int node) {
                       this.time = time;
                       this.node = node;
                   }
               }

               class Solution {
                   public static final int MOD = 1_000_000_007;

                   public int countPaths(int n, int[][] roads) {
                       // @a init
                       List<List<Pair>> adjList = new ArrayList<>();
                       long[] timeArr = new long[n];
                       int[] ways = new int[n];
                       PriorityQueue<Pair> pq = new PriorityQueue<>((x,y) -> Long.compare(x.time, y.time));
                       int src = 0;
                       int dest = n-1;

                       Arrays.fill(timeArr, Long.MAX_VALUE);

                       for (int i=0;i<n;i++)
                           adjList.add(new ArrayList<>());

                       for (int[] e: roads) {
                           int u = e[0];
                           int v = e[1];
                           int t = e[2];

                           adjList.get(u).add(new Pair(t,v));
                           adjList.get(v).add(new Pair(t,u));
                       }

                       timeArr[src] = 0;
                       ways[src] = 1;
                       pq.add(new Pair(0,src));

                       while (!pq.isEmpty()) {
                           // @a poll
                           Pair curr = pq.poll();
                           long currTime = curr.time;
                           int currNode = curr.node;

                           for (Pair ngbr: adjList.get(currNode)) {
                               long newTime = currTime + ngbr.time;

                               if (newTime < timeArr[ngbr.node]) {
                                   // @a faster
                                   timeArr[ngbr.node] = newTime;
                                   ways[ngbr.node] = ways[currNode];
                                   pq.add(new Pair(timeArr[ngbr.node], ngbr.node));
                               } else if (newTime == timeArr[ngbr.node]) {
                                   // @a tie
                                   ways[ngbr.node] = (ways[ngbr.node] + ways[currNode])%MOD;
                               }
                           }
                       }

                       // @a done
                       return ways[dest];
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        final int MOD = 1_000_000_007;
        Inputs.GraphInput graph = in.getGraph("graph");
        int n = graph.vertices();
        List<List<long[]>> adjList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjList.add(new ArrayList<>());
        }
        for (int[] e : graph.edges()) {
            adjList.get(e[0]).add(new long[]{e[2], e[1]});
            adjList.get(e[1]).add(new long[]{e[2], e[0]});
        }
        long[] timeArr = new long[n];
        int[] ways = new int[n];
        Arrays.fill(timeArr, Long.MAX_VALUE);
        int src = 0;
        int dest = n - 1;
        // {time, node}
        PriorityQueue<long[]> pq = new PriorityQueue<>((x, y) -> Long.compare(x[0], y[0]));
        timeArr[src] = 0;
        ways[src] = 1;
        pq.add(new long[]{0, src});

        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            states.put(i, "unvisited");
        }
        states.put(src, "queued");
        states.put(dest, "target");
        emit.at("init").say("Start at intersection 0 at time 0, with 1 way to be there. ways[] counts the "
                        + "routes that reach each node in its shortest time so far.")
                .var("timeArr", times(timeArr)).var("ways", Arrays.toString(ways))
                .graph(graph).nodes(states).queue(entries(pq)).step();

        while (!pq.isEmpty()) {
            long[] curr = pq.poll();
            long currTime = curr[0];
            int currNode = (int) curr[1];
            boolean stale = currTime > timeArr[currNode];
            if (currNode != dest) {
                states.put(currNode, "visiting");
            }
            emit.at("poll").say(stale
                            ? "Poll " + currNode + " at time " + currTime + ". It was already reached faster (at "
                                    + timeArr[currNode] + "), so no road from here can match a best time - nothing changes."
                            : "Poll " + currNode + " at time " + currTime + ", reached in " + ways[currNode] + " way"
                                    + Narration.s(ways[currNode]) + ". Try each road out of it.")
                    .var("currNode", currNode).var("currTime", currTime)
                    .var("timeArr", times(timeArr)).var("ways", Arrays.toString(ways))
                    .graph(graph).nodes(states).queue(entries(pq)).step();

            for (long[] ngbr : adjList.get(currNode)) {
                int next = (int) ngbr[1];
                long newTime = currTime + ngbr[0];
                if (newTime < timeArr[next]) {
                    timeArr[next] = newTime;
                    ways[next] = ways[currNode];
                    pq.add(new long[]{timeArr[next], next});
                    if (next != dest) {
                        states.put(next, "queued");
                    }
                    emit.at("faster").say("%d -> %d arrives at %d + %d = %d, the fastest so far. Every fastest route "
                                    + "to %d runs through %d now: ways[%d] = ways[%d] = %d.",
                                    currNode, next, currTime, ngbr[0], newTime, next, currNode, next, currNode, ways[next])
                            .var("timeArr", times(timeArr)).var("ways", Arrays.toString(ways))
                            .graph(graph).nodes(states).edges(List.of(edgeKey(graph, currNode, next)))
                            .queue(entries(pq)).step();
                } else if (newTime == timeArr[next]) {
                    ways[next] = (ways[next] + ways[currNode]) % MOD;
                    emit.at("tie").say("%d -> %d also arrives at %d, tying the fastest time. Add %d's routes: "
                                    + "ways[%d] = %d.", currNode, next, newTime, currNode, next, ways[next])
                            .var("timeArr", times(timeArr)).var("ways", Arrays.toString(ways))
                            .graph(graph).nodes(states).edges(List.of(edgeKey(graph, currNode, next)))
                            .queue(entries(pq)).step();
                }
            }
            if (currNode != dest && !stale) {
                states.put(currNode, "done");
            }
        }

        emit.at("done").say("The queue is empty. Intersection %d is reached in %d shortest way%s. Return ways[%d].",
                        dest, ways[dest], Narration.s(ways[dest]), dest)
                .var("timeArr", times(timeArr)).var("ways", Arrays.toString(ways)).var("answer", ways[dest])
                .graph(graph).nodes(states).step();
    }

    /** The undirected edge's label as the canvas names it - in its input orientation. */
    private static String edgeKey(Inputs.GraphInput graph, int a, int b) {
        for (int[] e : graph.edges()) {
            if (e[0] == a && e[1] == b) return a + "-" + b;
            if (e[0] == b && e[1] == a) return b + "-" + a;
        }
        return a + "-" + b;
    }

    private static String times(long[] t) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < t.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(t[i] == Long.MAX_VALUE ? "inf" : String.valueOf(t[i]));
        }
        return sb.append(']').toString();
    }

    private static List<String> entries(PriorityQueue<long[]> pq) {
        List<long[]> items = new ArrayList<>(pq);
        items.sort((x, y) -> Long.compare(x[0], y[0]));
        List<String> out = new ArrayList<>();
        for (long[] t : items) {
            out.add(t[1] + "@" + t[0]);
        }
        return out;
    }
}
