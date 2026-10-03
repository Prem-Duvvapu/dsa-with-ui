package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Find the City With the Smallest Number of Neighbors at a Threshold Distance (LeetCode 1334),
 * traced on the owner's own accepted submission: Floyd-Warshall for every pair's shortest
 * distance, then count, for each city, the others within the threshold. {@code <=} in the final
 * comparison keeps the LARGEST-numbered city on a tie, as the problem asks. O(n^3), the expected
 * answer for n up to 100.
 *
 * <p>The canvas is the {@code dist} matrix; -1 marks a pair with no route yet (the code's 1e8).
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
               class Solution {
                   public int findTheCity(int n, int[][] edges, int distanceThreshold) {
                       // @a init
                       int res = -1;
                       int minCitiesCnt = n+1;
                       int[][] dist = new int[n][n];

                       for (int i=0;i<n;i++) {
                           for (int j=0;j<n;j++) {
                               if (i != j)
                                   dist[i][j] = (int)1e8;
                           }
                       }

                       for (int[] e: edges) {
                           int u = e[0];
                           int v = e[1];
                           int wt = e[2];

                           dist[u][v] = wt;
                           dist[v][u] = wt;
                       }

                       for (int k=0;k<n;k++) {
                           // @a via
                           for (int i=0;i<n;i++) {
                               for (int j=0;j<n;j++) {
                                   // @a relax
                                   dist[i][j] = Math.min(dist[i][j], dist[i][k] + dist[k][j]);
                               }
                           }
                       }

                       for (int i=0;i<n;i++) {
                           int currCitiesCnt = 0;
                           for (int j=0;j<n;j++) {
                               if (i != j && dist[i][j] <= distanceThreshold) {
                                   currCitiesCnt++;
                               }
                           }

                           // @a count
                           if (currCitiesCnt <= minCitiesCnt) {
                               // @a best
                               minCitiesCnt = currCitiesCnt;
                               res = i;
                           }
                       }

                       // @a done
                       return res;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int distanceThreshold = in.getInt("threshold");
        int n = graph.vertices();
        int res = -1;
        int minCitiesCnt = n + 1;
        int[][] dist = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) dist[i][j] = INF;
            }
        }
        for (int[] e : graph.edges()) {
            dist[e[0]][e[1]] = e[2];
            dist[e[1]][e[0]] = e[2];
        }

        emit.at("init").say("dist[i][j] starts as the direct road between i and j (-1 on the canvas when there is "
                        + "none). Floyd-Warshall then lets each city in turn act as a stop in the middle.")
                .var("n", n).var("distanceThreshold", distanceThreshold).grid(display(dist)).step();

        for (int k = 0; k < n; k++) {
            emit.at("via").say("Allow city %d as a stop: any pair i, j may now go i -> %d -> j if that is shorter.", k, k)
                    .var("k", k).grid(display(dist)).step();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int through = dist[i][k] + dist[k][j];
                    if (through < dist[i][j]) {
                        String before = dist[i][j] >= INF ? "no route" : String.valueOf(dist[i][j]);
                        dist[i][j] = through;
                        emit.at("relax").say("%d -> %d -> %d costs %d + %d = %d, better than %s: dist[%d][%d] = %d.",
                                        i, k, j, dist[i][k], dist[k][j], through, before, i, j, through)
                                .var("k", k).var("i", i).var("j", j).grid(display(dist)).step();
                    }
                }
            }
        }

        for (int i = 0; i < n; i++) {
            int currCitiesCnt = 0;
            for (int j = 0; j < n; j++) {
                if (i != j && dist[i][j] <= distanceThreshold) {
                    currCitiesCnt++;
                }
            }
            boolean better = currCitiesCnt <= minCitiesCnt;
            emit.at("count").say("City %d reaches %d other cit%s within %d.%s", i, currCitiesCnt,
                            currCitiesCnt == 1 ? "y" : "ies", distanceThreshold,
                            better ? "" : " More than the best so far (" + minCitiesCnt + "), so it is not the answer.")
                    .var("i", i).var("currCitiesCnt", currCitiesCnt).var("minCitiesCnt", minCitiesCnt)
                    .grid(display(dist)).step();
            if (better) {
                boolean tie = currCitiesCnt == minCitiesCnt;
                minCitiesCnt = currCitiesCnt;
                res = i;
                emit.at("best").say(tie
                                ? "That ties the fewest so far, and the problem breaks ties toward the larger city "
                                        + "number - which is " + i + " (that is what <= does). res = " + i + "."
                                : "That is the fewest so far: res = " + i + ".")
                        .var("res", res).var("minCitiesCnt", minCitiesCnt).grid(display(dist)).step();
            }
        }

        emit.at("done").say("Return res = %d.", res)
                .var("res", res).var("answer", res).grid(display(dist)).step();
    }

    /** -1 for "no route", which the code holds as 1e8. */
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
}
