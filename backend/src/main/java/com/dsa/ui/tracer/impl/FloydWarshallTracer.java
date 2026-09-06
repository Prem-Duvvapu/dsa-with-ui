package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * All-pairs shortest paths by dynamic programming over intermediate vertices.
 *
 * <p>The single idea is the meaning of the outer loop, which is the part pseudocode hides:
 * after round k finishes, {@code dist[i][j]} is the best route from i to j that is allowed
 * to pass through vertices 0..k and nothing else. Round k + 1 unlocks one more waypoint and
 * asks a single question of every pair — is going i -> k -> j cheaper than what you had?
 * That is why k must be the OUTERMOST loop: swap it inside and a pair is asked about a
 * waypoint whose own best routes have not been computed yet, and the table silently
 * under-relaxes.
 *
 * <p>The matrix is the input and the output: {@code -1} means "no edge / not reachable",
 * on the way in and on the way out. Weights are non-negative, so the diagonal stays 0 and
 * no negative cycle can form.
 */
@Component
public class FloydWarshallTracer implements AlgorithmTracer {

    private static final int INF = Integer.MAX_VALUE;

    @Override
    public String id() {
        return "floyd-warshall";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("matrix", FieldType.INT_GRID)
                        .label("Adjacency matrix")
                        .help("Square matrix of edge weights for a directed graph. Cell [i][j] is the "
                                + "weight of the edge i -> j, or -1 when there is no such edge. The "
                                + "diagonal is treated as 0.")
                        .constraint("maxRows", 6)
                        .constraint("maxCols", 6)
                        .values(-1, 1000)
                        .defaultValue(List.of(
                                List.of(0, 4, -1, 5),
                                List.of(-1, 0, 1, -1),
                                List.of(-1, -1, 0, 3),
                                List.of(-1, -1, 1, 0)))
                        .build());
    }

    /** A smaller, far sparser graph where two of the three vertices can reach almost nothing. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("matrix", List.of(
                List.of(0, 3, -1),
                List.of(-1, 0, -1),
                List.of(4, -1, 0)));
    }

    @Override
    public String annotatedCode() {
        return """
               public void shortestDistance(int[][] matrix) {
                   int n = matrix.length;
                   // @a init
                   for (int i = 0; i < n; i++) {
                       for (int j = 0; j < n; j++) {
                           if (matrix[i][j] == -1) matrix[i][j] = INF;
                           if (i == j) matrix[i][j] = 0;
                       }
                   }

                   for (int k = 0; k < n; k++) {
                       for (int i = 0; i < n; i++) {
                           for (int j = 0; j < n; j++) {
                               if (i == j) continue;
                               if (matrix[i][k] == INF || matrix[k][j] == INF) continue;
                               if (matrix[i][k] + matrix[k][j] < matrix[i][j]) {
                                   // @a viaK
                                   matrix[i][j] = matrix[i][k] + matrix[k][j];
                               } else {
                                   // @a keep
                                   continue;
                               }
                           }
                       }
                       // @a roundDone
                   }

                   // @a done
                   for (int i = 0; i < n; i++) {
                       for (int j = 0; j < n; j++) {
                           if (matrix[i][j] == INF) matrix[i][j] = -1;
                       }
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] raw = in.getGrid("matrix");
        int n = raw.length;
        for (int[] row : raw) {
            if (row.length != n) {
                throw new InputValidationException(Map.of("matrix",
                        "An adjacency matrix must be square; this one has " + n + " rows but a row of "
                                + row.length + " values."));
            }
        }

        int[][] dist = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                dist[i][j] = i == j ? 0 : (raw[i][j] == -1 ? INF : raw[i][j]);
            }
        }

        emit.at("init").say(
                        "%d vertices. Read the matrix as a distance table: -1 becomes 'no route known "
                                + "yet' and the diagonal is 0. Right now every entry is a single edge, "
                                + "because no waypoints are allowed at all.",
                        n)
                .var("n", n).var("waypoints", "none")
                .grid(display(dist)).step();

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j || dist[i][k] == INF || dist[k][j] == INF) {
                        continue;
                    }
                    int toWaypoint = dist[i][k];
                    int fromWaypoint = dist[k][j];
                    int candidate = toWaypoint + fromWaypoint;

                    if (candidate < dist[i][j]) {
                        String old = label(dist[i][j]);
                        dist[i][j] = candidate;
                        emit.at("viaK").say(
                                        "k = %d: %d -> %d -> %d costs %d + %d = %d, beating %s. dist[%d][%d] = %d.",
                                        k, i, k, j, toWaypoint, fromWaypoint, candidate, old, i, j, candidate)
                                .var("k", k).var("i", i).var("j", j).var("candidate", candidate)
                                .grid(display(dist)).step();
                    } else {
                        emit.at("keep").say(
                                        "k = %d: %d -> %d -> %d costs %d + %d = %d, which does not beat the "
                                                + "known dist[%d][%d] = %s. Leave it.",
                                        k, i, k, j, toWaypoint, fromWaypoint, candidate, i, j, label(dist[i][j]))
                                .var("k", k).var("i", i).var("j", j).var("candidate", candidate)
                                .grid(display(dist)).step();
                    }
                }
            }

            emit.at("roundDone").say(
                            "Vertex %d is now an allowed waypoint. Every entry is the cheapest route that "
                                    + "may pass through vertices 0..%d only.",
                            k, k)
                    .var("k", k).var("waypoints", "0.." + k)
                    .grid(display(dist)).step();
        }

        emit.at("done").say(
                        "Every vertex has served as a waypoint, so the table is the all-pairs shortest "
                                + "path matrix: %s. Remaining -1 entries are pairs with no route at all.",
                        rowsString(display(dist)))
                .var("result", rowsString(display(dist)))
                .grid(display(dist)).step();
    }

    /** The wire form of the table: unreachable pairs go back to -1, as they came in. */
    private static int[][] display(int[][] dist) {
        int n = dist.length;
        int[][] out = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                out[i][j] = dist[i][j] == INF ? -1 : dist[i][j];
            }
        }
        return out;
    }

    private static String label(int value) {
        return value == INF ? "no route" : String.valueOf(value);
    }

    private static String rowsString(int[][] grid) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < grid.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(Arrays.toString(grid[i]));
        }
        return sb.append(']').toString();
    }
}
