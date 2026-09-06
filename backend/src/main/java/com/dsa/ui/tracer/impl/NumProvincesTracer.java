package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Number of Provinces (LeetCode 547) from the {@code isConnected} adjacency MATRIX, which
 * is what the problem actually hands you - not an edge list.
 *
 * <p>That distinction is the whole point of the problem and is why it costs O(n^2) rather
 * than O(V + E): finding city {@code city}'s neighbours means walking the entire row
 * {@code isConnected[city]}, one column at a time, including the diagonal {@code [i][i]}
 * which every city sets to 1 for itself. The trace shows that row walk explicitly.
 *
 * <p>The animation is a GRAPH because a province is a connected component, and a component
 * is far easier to see as coloured vertices than as a lit-up matrix cell. The topology drawn
 * beside the narration is derived from the matrix the caller supplied: an undirected edge
 * i-j wherever {@code isConnected[i][j] == 1} and {@code i < j}.
 *
 * <p>Distinct from {@code number-of-provinces}, which takes the same count over an
 * edge-list graph and recurses with DFS; this one is the matrix-scanning BFS form.
 */
@Component
public class NumProvincesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "num-provinces";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("isConnected", FieldType.INT_GRID)
                        .label("isConnected")
                        .help("N x N adjacency matrix. Row i is city i; isConnected[i][j] = 1 when "
                                + "cities i and j are directly connected. The diagonal is 1.")
                        .constraint("maxRows", 9)
                        .constraint("maxCols", 9)
                        .values(0, 1)
                        // LeetCode 547 example 1: cities 0 and 1 are joined, city 2 is alone -> 2.
                        .defaultValue(List.of(
                                List.of(1, 1, 0),
                                List.of(1, 1, 0),
                                List.of(0, 0, 1)))
                        .build());
    }

    /**
     * Five cities chained 0-1-2-3-4, so the whole map is a single province and one BFS has
     * to walk the chain end to end - the opposite profile from the default, where the first
     * BFS settles immediately and a second one has to be started.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("isConnected", List.of(
                List.of(1, 1, 0, 0, 0),
                List.of(1, 1, 1, 0, 0),
                List.of(0, 1, 1, 1, 0),
                List.of(0, 0, 1, 1, 1),
                List.of(0, 0, 0, 1, 1)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int findCircleNum(int[][] isConnected) {
                   // @a init
                   int n = isConnected.length;
                   boolean[] visited = new boolean[n];
                   int provinces = 0;

                   for (int i = 0; i < n; i++) {
                       if (visited[i]) {
                           // @a settled
                           continue;
                       }
                       // @a newProvince
                       provinces++;
                       Queue<Integer> queue = new LinkedList<>();
                       queue.add(i);
                       visited[i] = true;

                       while (!queue.isEmpty()) {
                           // @a dequeue
                           int city = queue.poll();
                           for (int j = 0; j < n; j++) {
                               if (isConnected[city][j] == 0) {
                                   continue;
                               }
                               if (visited[j]) {
                                   // @a already
                                   continue;
                               }
                               // @a enqueue
                               visited[j] = true;
                               queue.add(j);
                           }
                       }
                   }
                   // @a done
                   return provinces;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] matrix = in.getGrid("isConnected");
        // The matrix is meant to be square; taking the smaller side keeps a ragged one from
        // indexing off the end rather than turning caller error into a 500.
        int n = Math.min(matrix.length, matrix[0].length);

        Inputs.GraphInput topology = topologyOf(matrix, n);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            states.put(i, "unvisited");
        }

        boolean[] visited = new boolean[n];
        int provinces = 0;

        emit.at("init").say("%d cities. A city's neighbours are found by scanning its own row of "
                        + "the matrix, so every lookup costs a full row walk.", n)
                .var("n", n).var("provinces", 0)
                .graph(topology).nodes(states).step();

        for (int i = 0; i < n; i++) {
            if (visited[i]) {
                emit.at("settled").say("City %d already belongs to a counted province - skip it.", i)
                        .var("i", i).var("provinces", provinces)
                        .graph(topology).nodes(states).step();
                continue;
            }

            provinces++;
            visited[i] = true;
            states.put(i, "queued");
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(i);

            emit.at("newProvince").say("City %d has not been reached yet, so it opens province #%d. "
                            + "Seed the queue with it.", i, provinces)
                    .var("i", i).var("provinces", provinces)
                    .graph(topology).nodes(states).queue(queue).step();

            while (!queue.isEmpty()) {
                int city = queue.poll();
                states.put(city, "visiting");

                emit.at("dequeue").say("Walk row %d of isConnected to find every city joined to %d.",
                                city, city)
                        .var("city", city).var("provinces", provinces)
                        .graph(topology).nodes(states).queue(queue).step();

                for (int j = 0; j < n; j++) {
                    if (matrix[city][j] == 0) {
                        continue;
                    }
                    if (visited[j]) {
                        emit.at("already").say("isConnected[%d][%d] = 1, but city %d is already in "
                                        + "province #%d - nothing new.", city, j, j, provinces)
                                .var("city", city).var("j", j)
                                .graph(topology).nodes(states).edges(edgeLabel(city, j))
                                .queue(queue).step();
                        continue;
                    }
                    visited[j] = true;
                    states.put(j, "queued");
                    queue.add(j);
                    emit.at("enqueue").say("isConnected[%d][%d] = 1 and city %d is unseen - it joins "
                                    + "province #%d.", city, j, j, provinces)
                            .var("city", city).var("j", j).var("provinces", provinces)
                            .graph(topology).nodes(states).edges(edgeLabel(city, j))
                            .queue(queue).step();
                }

                states.put(city, "visited");
            }
        }

        emit.at("done").say("Every city assigned. The map splits into %d province(s).", provinces)
                .var("provinces", provinces)
                .graph(topology).nodes(states).step();
    }

    /** Undirected edges implied by the matrix, self-loops on the diagonal dropped. */
    private static Inputs.GraphInput topologyOf(int[][] matrix, int n) {
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (matrix[i][j] == 1) {
                    edges.add(new int[]{i, j});
                }
            }
        }
        return new Inputs.GraphInput(n, edges.toArray(new int[0][]));
    }

    private static List<String> edgeLabel(int from, int to) {
        return from == to ? List.of() : List.of(from + "-" + to);
    }
}
