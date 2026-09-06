package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * How long a signal broadcast from one node takes to reach the whole network
 * (LeetCode 743).
 *
 * <p>The shortest-path work is ordinary Dijkstra, but the question asked at the end is
 * not "how far is node x": it is "when is the LAST node lit up", which is the maximum
 * over every finalized distance — and -1 the moment a single node was never reached at
 * all. That asymmetry is the point of the problem, so the trace ends by walking the
 * finished distance array and naming the node that decides the answer.
 *
 * <p>Vertices are numbered from 0 here rather than LeetCode's 1..n, because every graph
 * input in this catalogue is 0-based; the algorithm and the answers are unchanged.
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
               public int networkDelayTime(int n, List<List<int[]>> adj, int source) {
                   // @a init
                   int[] time = new int[n];
                   Arrays.fill(time, Integer.MAX_VALUE);
                   time[source] = 0;
                   PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[1] - b[1]);
                   pq.add(new int[]{source, 0});

                   while (!pq.isEmpty()) {
                       // @a extract
                       int[] top = pq.poll();
                       int node = top[0], t = top[1];
                       if (t > time[node]) {
                           // @a stale
                           continue;
                       }
                       for (int[] link : adj.get(node)) {
                           int next = link[0], travel = link[1];
                           if (t + travel < time[next]) {
                               // @a relax
                               time[next] = t + travel;
                               pq.add(new int[]{next, time[next]});
                           } else {
                               // @a slower
                               continue;
                           }
                       }
                   }

                   // @a answer
                   int slowest = 0;
                   for (int t : time) {
                       if (t == Integer.MAX_VALUE) return -1;
                       slowest = Math.max(slowest, t);
                   }
                   return slowest;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int source = in.getInt("source");
        int n = graph.vertices();
        if (source >= n) {
            throw new InputValidationException(Map.of("source",
                    "This network only has nodes 0.." + (n - 1) + "."));
        }

        List<List<Inputs.GraphInput.Neighbor>> adj = graph.weightedAdjacency(true);
        GraphLayout.Layout layout = GraphLayout.directed(graph);

        int[] time = new int[n];
        Arrays.fill(time, Integer.MAX_VALUE);
        time[source] = 0;

        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            states.put(i, "unvisited");
        }
        states.put(source, "queued");

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        pq.add(new int[]{source, 0});

        emit.at("init").say(
                        "Node %d transmits at t = 0. Every other node's arrival time starts unknown, "
                                + "and the queue holds the one node the signal has definitely reached.",
                        source)
                .var("source", source).var("time", timeString(time))
                .graph(layout.nodes(), layout.edges()).nodes(states).queue(pqSnapshot(pq)).step();

        while (!pq.isEmpty()) {
            int[] top = pq.poll();
            int node = top[0];
            int t = top[1];

            emit.at("extract").say("Take the earliest pending arrival: node %d at t = %d.", node, t)
                    .var("node", node).var("arrival", t).var("time", timeString(time))
                    .graph(layout.nodes(), layout.edges()).nodes(states).queue(pqSnapshot(pq)).step();

            if (t > time[node]) {
                emit.at("stale").say(
                                "The signal already reached node %d at t = %d, earlier than this entry's %d. "
                                        + "This entry was queued before that shortcut was found - drop it.",
                                node, time[node], t)
                        .var("node", node).var("arrival", t).var("time", timeString(time))
                        .graph(layout.nodes(), layout.edges()).nodes(states).queue(pqSnapshot(pq)).step();
                continue;
            }

            states.put(node, "visiting");

            for (Inputs.GraphInput.Neighbor link : adj.get(node)) {
                int next = link.to();
                int travel = link.weight();
                String edgeKey = node + "-" + next;

                if (t + travel < time[next]) {
                    String old = timeLabel(time[next]);
                    time[next] = t + travel;
                    pq.add(new int[]{next, time[next]});
                    if (!"done".equals(states.get(next))) {
                        states.put(next, "queued");
                    }
                    emit.at("relax").say(
                                    "Link %d -> %d takes %d. The signal would reach %d at %d + %d = %d, "
                                            + "earlier than %s. Record it and queue node %d.",
                                    node, next, travel, next, t, travel, time[next], old, next)
                            .var("node", node).var("link", edgeKey).var("arrival", time[next])
                            .var("time", timeString(time))
                            .graph(layout.nodes(), layout.edges()).nodes(states)
                            .edges(List.of(edgeKey)).queue(pqSnapshot(pq)).step();
                } else {
                    emit.at("slower").say(
                                    "Link %d -> %d takes %d, arriving at %d + %d = %d. Node %d already hears "
                                            + "the signal at %d, so this route is no faster.",
                                    node, next, travel, t, travel, t + travel, next, time[next])
                            .var("node", node).var("link", edgeKey).var("time", timeString(time))
                            .graph(layout.nodes(), layout.edges()).nodes(states)
                            .edges(List.of(edgeKey)).queue(pqSnapshot(pq)).step();
                }
            }

            states.put(node, "done");
        }

        int unreached = -1;
        int slowest = 0;
        int slowestNode = source;
        for (int i = 0; i < n; i++) {
            if (time[i] == Integer.MAX_VALUE) {
                unreached = i;
                break;
            }
            if (time[i] > slowest) {
                slowest = time[i];
                slowestNode = i;
            }
        }

        String verdict;
        if (unreached >= 0) {
            states.put(unreached, "target");
            verdict = String.format(
                    "node %d never hears it, so no finite delay covers the whole network: the answer is -1",
                    unreached);
        } else {
            states.put(slowestNode, "target");
            verdict = String.format(
                    "the last node to hear it is %d at t = %d, so the network delay time is %d",
                    slowestNode, slowest, slowest);
        }
        emit.at("answer").say("Queue empty; arrival times are final at %s. Scanning them, %s.",
                        timeString(time), verdict)
                .var("time", timeString(time))
                .var("answer", unreached >= 0 ? -1 : slowest)
                .graph(layout.nodes(), layout.edges()).nodes(states).queue(pqSnapshot(pq)).step();
    }

    private static List<String> pqSnapshot(PriorityQueue<int[]> pq) {
        List<String> snapshot = new ArrayList<>(pq.size());
        for (int[] entry : pq) {
            snapshot.add(entry[0] + "@" + entry[1]);
        }
        return snapshot;
    }

    private static String timeLabel(int value) {
        return value == Integer.MAX_VALUE ? "never" : "t = " + value;
    }

    private static String timeString(int[] time) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < time.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(time[i] == Integer.MAX_VALUE ? "never" : String.valueOf(time[i]));
        }
        return sb.append(']').toString();
    }
}
