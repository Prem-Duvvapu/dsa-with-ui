package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * How many distinct shortest routes run from intersection 0 to intersection n - 1
 * (LeetCode 1976), counted modulo 1e9 + 7.
 *
 * <p>Dijkstra already finds the shortest time; the counting rides along beside it in a
 * second array, and the whole problem lives in getting the two cases right:
 *
 * <ul>
 *   <li>a STRICTLY shorter time to a node invalidates everything counted for it so far,
 *       so the count is REPLACED by the predecessor's count, not added to;</li>
 *   <li>an EQUAL time arriving over a different road is a genuinely new family of routes,
 *       so the predecessor's count is ADDED.</li>
 * </ul>
 *
 * <p>Getting those two backwards is the classic wrong answer here, so each is narrated
 * with the arithmetic that distinguishes them rather than with the word "update".
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
               public int countPaths(int n, List<List<int[]>> adj) {
                   // @a init
                   long[] time = new long[n];
                   long[] ways = new long[n];
                   Arrays.fill(time, Long.MAX_VALUE);
                   time[0] = 0;
                   ways[0] = 1;
                   PriorityQueue<long[]> pq = new PriorityQueue<>((a, b) -> Long.compare(a[1], b[1]));
                   pq.add(new long[]{0, 0});

                   while (!pq.isEmpty()) {
                       // @a extract
                       long[] top = pq.poll();
                       int node = (int) top[0];
                       long t = top[1];
                       if (t > time[node]) continue;
                       for (int[] road : adj.get(node)) {
                           int next = road[0], cost = road[1];
                           if (t + cost < time[next]) {
                               // @a shorter
                               time[next] = t + cost;
                               ways[next] = ways[node];
                               pq.add(new long[]{next, time[next]});
                           } else if (t + cost == time[next]) {
                               // @a tie
                               ways[next] = (ways[next] + ways[node]) % 1000000007;
                           } else {
                               // @a slower
                               continue;
                           }
                       }
                   }
                   // @a answer
                   return (int) (ways[n - 1] % 1000000007);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int n = graph.vertices();
        int destination = n - 1;

        List<List<Inputs.GraphInput.Neighbor>> adj = graph.weightedAdjacency();

        long[] time = new long[n];
        long[] ways = new long[n];
        Arrays.fill(time, Long.MAX_VALUE);
        time[0] = 0;
        ways[0] = 1;

        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            states.put(i, "unvisited");
        }
        states.put(0, "queued");
        states.put(destination, "target");

        PriorityQueue<long[]> pq = new PriorityQueue<>(Comparator.comparingLong(a -> a[1]));
        pq.add(new long[]{0, 0});

        emit.at("init").say(
                        "%d intersections. Start at 0 with time 0 and exactly one way to be standing there; "
                                + "every other intersection has an unknown time and zero known ways. "
                                + "The destination is intersection %d.",
                        n, destination)
                .var("time", timeString(time)).var("ways", waysString(ways))
                .graph(graph).nodes(states).queue(pqSnapshot(pq)).step();

        while (!pq.isEmpty()) {
            long[] top = pq.poll();
            int node = (int) top[0];
            long t = top[1];
            if (t > time[node]) {
                continue;
            }

            emit.at("extract").say(
                            "Settle intersection %d: %d is the shortest time to it, reachable in %d way(s). "
                                    + "Its roads can now be counted.",
                            node, t, ways[node])
                    .var("node", node).var("time", timeString(time)).var("ways", waysString(ways))
                    .graph(graph).nodes(states).queue(pqSnapshot(pq)).step();

            states.put(node, "visiting");

            for (Inputs.GraphInput.Neighbor road : adj.get(node)) {
                int next = road.to();
                int cost = road.weight();
                long candidate = t + cost;
                String edgeKey = node + "-" + next;

                if (candidate < time[next]) {
                    String old = timeLabel(time[next]);
                    time[next] = candidate;
                    ways[next] = ways[node];
                    pq.add(new long[]{next, candidate});
                    if (next != destination && !"visiting".equals(states.get(next))) {
                        states.put(next, "queued");
                    }
                    emit.at("shorter").say(
                                    "Road %d-%d takes %d: %d + %d = %d beats %s. Everything counted for "
                                            + "intersection %d used a slower route, so its count is REPLACED by "
                                            + "%d's %d way(s), not added to.",
                                    node, next, cost, t, cost, candidate, old, next, node, ways[node])
                            .var("road", edgeKey).var("time", timeString(time)).var("ways", waysString(ways))
                            .graph(graph).nodes(states).edges(List.of(edgeKey)).queue(pqSnapshot(pq)).step();
                } else if (candidate == time[next]) {
                    long before = ways[next];
                    ways[next] = (before + ways[node]) % MOD;
                    emit.at("tie").say(
                                    "Road %d-%d takes %d: %d + %d = %d ties the best known time to %d. "
                                            + "These are different routes arriving equally fast, so ADD: %d + %d = %d way(s).",
                                    node, next, cost, t, cost, candidate, next, before, ways[node], ways[next])
                            .var("road", edgeKey).var("time", timeString(time)).var("ways", waysString(ways))
                            .graph(graph).nodes(states).edges(List.of(edgeKey)).queue(pqSnapshot(pq)).step();
                } else {
                    emit.at("slower").say(
                                    "Road %d-%d takes %d: %d + %d = %d is slower than %s's best %s. "
                                            + "A slower route is never part of a shortest one - no count changes.",
                                    node, next, cost, t, cost, candidate, "intersection " + next,
                                    timeLabel(time[next]))
                            .var("road", edgeKey).var("time", timeString(time)).var("ways", waysString(ways))
                            .graph(graph).nodes(states).edges(List.of(edgeKey)).queue(pqSnapshot(pq)).step();
                }
            }

            states.put(node, node == destination ? "target" : "done");
        }

        long answer = time[destination] == Long.MAX_VALUE ? 0 : ways[destination] % MOD;
        emit.at("answer").say(
                        "Queue empty. The shortest time from 0 to %d is %s, and %d distinct route(s) achieve it.",
                        destination, timeLabel(time[destination]), answer)
                .var("answer", answer).var("time", timeString(time)).var("ways", waysString(ways))
                .graph(graph).nodes(states).queue(pqSnapshot(pq)).step();
    }

    private static List<String> pqSnapshot(PriorityQueue<long[]> pq) {
        List<String> snapshot = new ArrayList<>(pq.size());
        for (long[] entry : pq) {
            snapshot.add(entry[0] + " in " + entry[1]);
        }
        return snapshot;
    }

    private static String timeLabel(long value) {
        return value == Long.MAX_VALUE ? "unknown" : String.valueOf(value);
    }

    private static String timeString(long[] time) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < time.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(time[i] == Long.MAX_VALUE ? "?" : String.valueOf(time[i]));
        }
        return sb.append(']').toString();
    }

    private static String waysString(long[] ways) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ways.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(ways[i]);
        }
        return sb.append(']').toString();
    }
}
