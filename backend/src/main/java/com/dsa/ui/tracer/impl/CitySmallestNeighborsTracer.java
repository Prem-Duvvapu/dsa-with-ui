package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * The city that can reach the fewest other cities within a distance threshold
 * (LeetCode 1334), ties going to the largest city number.
 *
 * <p>The question is about every city at once, not about one source, which is what makes
 * all-pairs Floyd-Warshall the natural engine rather than n separate Dijkstra runs. Once
 * the distance table is complete the answer is a single scan: count, per row, how many
 * other cities sit at or under the threshold.
 *
 * <p>Two details decide right from wrong here and are narrated as such: the threshold is
 * compared against the SHORTEST distance, not against any single road, so a city with one
 * long road may still be well connected through a chain of short ones; and the tie-break
 * wants the LARGEST index, which is why the scan keeps accepting a count equal to the best
 * so far instead of only a strictly smaller one.
 */
@Component
public class CitySmallestNeighborsTracer implements AlgorithmTracer {

    /** Large enough to mean "unreachable", small enough that INF + INF cannot overflow. */
    private static final int INF = 100_000_000;

    @Override
    public String id() {
        return "city-smallest-neighbors";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("City network")
                        .help("City count plus bidirectional roads [from, to, distance]. The default is "
                                + "LeetCode 1334's first example, whose answer is city 3.")
                        .weighted()
                        .weights(1, 10_000)
                        .constraint("maxVertices", 8)
                        .constraint("maxEdges", 20)
                        .defaultValue(Map.of(
                                "vertices", 4,
                                "edges", List.of(
                                        List.of(0, 1, 3), List.of(1, 2, 1),
                                        List.of(1, 3, 4), List.of(2, 3, 1))))
                        .build(),
                InputField.of("threshold", FieldType.INT)
                        .label("Distance threshold")
                        .help("A city counts as a neighbour when the shortest route to it is at most this.")
                        .range(0, 100_000)
                        .defaultValue(4)
                        .build());
    }

    /**
     * LeetCode 1334's second example: five cities and a threshold so tight that most roads
     * are useless, and the winner is decided outright rather than on the tie-break.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "graph", Map.of(
                        "vertices", 5,
                        "edges", List.of(
                                List.of(0, 1, 2), List.of(0, 4, 8), List.of(1, 2, 3),
                                List.of(1, 4, 2), List.of(2, 3, 1), List.of(3, 4, 1))),
                "threshold", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               public int findTheCity(int n, int[][] edges, int distanceThreshold) {
                   // @a init
                   int[][] dist = new int[n][n];
                   for (int[] row : dist) Arrays.fill(row, INF);
                   for (int i = 0; i < n; i++) dist[i][i] = 0;
                   for (int[] e : edges) {
                       dist[e[0]][e[1]] = Math.min(dist[e[0]][e[1]], e[2]);
                       dist[e[1]][e[0]] = Math.min(dist[e[1]][e[0]], e[2]);
                   }

                   for (int k = 0; k < n; k++) {
                       for (int i = 0; i < n; i++) {
                           for (int j = 0; j < n; j++) {
                               if (dist[i][k] + dist[k][j] < dist[i][j]) {
                                   // @a viaK
                                   dist[i][j] = dist[i][k] + dist[k][j];
                               }
                           }
                       }
                       // @a roundDone
                   }

                   int best = -1, fewest = n + 1;
                   for (int city = 0; city < n; city++) {
                       // @a count
                       int reachable = 0;
                       for (int j = 0; j < n; j++) {
                           if (j != city && dist[city][j] <= distanceThreshold) reachable++;
                       }
                       if (reachable <= fewest) {
                           // @a newBest
                           fewest = reachable;
                           best = city;
                       }
                   }
                   // @a answer
                   return best;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int threshold = in.getInt("threshold");
        int n = graph.vertices();

        int[][] dist = new int[n][n];
        for (int[] row : dist) {
            Arrays.fill(row, INF);
        }
        for (int i = 0; i < n; i++) {
            dist[i][i] = 0;
        }
        for (int[] e : graph.edges()) {
            dist[e[0]][e[1]] = Math.min(dist[e[0]][e[1]], e[2]);
            dist[e[1]][e[0]] = Math.min(dist[e[1]][e[0]], e[2]);
        }

        emit.at("init").say(
                        "%d cities, %d road(s), threshold %d. Seed the distance table with the roads "
                                + "themselves; -1 marks a pair with no route known yet. Floyd-Warshall will "
                                + "fill in the rest before any counting happens.",
                        n, graph.edges().length, threshold)
                .var("n", n).var("threshold", threshold)
                .grid(display(dist)).step();

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int candidate = dist[i][k] + dist[k][j];
                    if (candidate < dist[i][j]) {
                        String old = label(dist[i][j]);
                        int toWaypoint = dist[i][k];
                        int fromWaypoint = dist[k][j];
                        dist[i][j] = candidate;
                        emit.at("viaK").say(
                                        "Routing %d to %d through city %d costs %d + %d = %d, better than %s.",
                                        i, j, k, toWaypoint, fromWaypoint, candidate, old)
                                .var("k", k).var("i", i).var("j", j).var("distance", candidate)
                                .grid(display(dist)).step();
                    }
                }
            }
            emit.at("roundDone").say(
                            "City %d has been offered as a waypoint to every pair. The table now holds the "
                                    + "shortest routes that use only cities 0..%d in between.",
                            k, k)
                    .var("k", k)
                    .grid(display(dist)).step();
        }

        int best = -1;
        int fewest = n + 1;
        int[] counts = new int[n];

        for (int city = 0; city < n; city++) {
            int reachable = 0;
            StringBuilder within = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j != city && dist[city][j] <= threshold) {
                    reachable++;
                    if (within.length() > 0) {
                        within.append(", ");
                    }
                    within.append(j).append(" at ").append(dist[city][j]);
                }
            }
            counts[city] = reachable;

            emit.at("count").say(
                            "City %d reaches %d other city/cities within %d: %s.",
                            city, reachable, threshold,
                            reachable == 0 ? "none" : within.toString())
                    .var("city", city).var("reachable", reachable).var("counts", Arrays.toString(counts))
                    .grid(display(dist)).step();

            if (reachable <= fewest) {
                String why = reachable < fewest
                        ? String.format("%d is fewer than the previous best of %s", reachable,
                                fewest > n ? "nothing yet" : String.valueOf(fewest))
                        : String.format("%d ties the previous best, and a tie goes to the larger city number",
                                reachable);
                fewest = reachable;
                best = city;
                emit.at("newBest").say("City %d becomes the answer so far: %s.", city, why)
                        .var("best", best).var("fewest", fewest).var("counts", Arrays.toString(counts))
                        .grid(display(dist)).step();
            }
        }

        emit.at("answer").say(
                        "Neighbour counts are %s. City %d has the smallest count (%d), so it is the answer.",
                        Arrays.toString(counts), best, fewest)
                .var("answer", best).var("counts", Arrays.toString(counts))
                .grid(display(dist)).step();
    }

    /** Unreachable pairs render as -1 rather than as the sentinel. */
    private static int[][] display(int[][] dist) {
        int n = dist.length;
        int[][] out = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                out[i][j] = dist[i][j] >= INF ? -1 : dist[i][j];
            }
        }
        return out;
    }

    private static String label(int value) {
        return value >= INF ? "no route" : String.valueOf(value);
    }
}
