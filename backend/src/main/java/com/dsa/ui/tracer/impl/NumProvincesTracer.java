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
 * edge-list graph; this one recurses straight over the matrix rows, as LeetCode 547 is posed.
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
     * Five cities chained 0-1-2-3-4, so the whole map is a single province and one DFS has
     * to recurse down the chain end to end - the opposite profile from the default, where the
     * first DFS returns quickly and a second one has to be started.
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

    /**
     * The owner's own accepted LeetCode 547 solution, shown and traced as written: recursive
     * DFS straight off the matrix row. (It replaced a queue-based BFS so the code on screen is
     * the code the owner submitted - see docs/ui-revamp REVAMP_TRACKER decision log.)
     */
    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public int findCircleNum(int[][] isConnected) {
                       // @a init
                       int n=isConnected.length;
                       boolean[] visited=new boolean[n];
                       int numOfProvinces=0;

                       for (int i=0;i<n;i++) {
                           // @a check
                           if (!visited[i]) {
                               // @a newProvince
                               dfs(i,visited,isConnected,n);
                               numOfProvinces++;
                           }
                       }

                       // @a done
                       return numOfProvinces;
                   }

                   public void dfs(int currNode,boolean[] visited,int[][] isConnected,int n) {
                       // @a visit
                       visited[currNode]=true;

                       for (int j=0;j<n;j++)
                           // @a recurse
                           if (isConnected[currNode][j]==1 && !visited[j])
                               dfs(j,visited,isConnected,n);
                   // @a return
                   }
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

        emit.at("init").say("%d cities and no province counted yet. A city's neighbours are its own "
                        + "row of isConnected, so each DFS call scans one full row.", n)
                .var("n", n).var("numOfProvinces", 0)
                .graph(topology).nodes(states).step();

        for (int i = 0; i < n; i++) {
            if (visited[i]) {
                emit.at("check").say("City %d was already reached by an earlier DFS, so it belongs to a "
                                + "province that is already counted - skip it.", i)
                        .var("i", i).var("numOfProvinces", provinces)
                        .graph(topology).nodes(states).step();
                continue;
            }
            emit.at("newProvince").say("City %d is unvisited, so it starts a new province: run dfs(%d) to "
                            + "mark every city it can reach.", i, i)
                    .var("i", i).var("numOfProvinces", provinces)
                    .graph(topology).nodes(states).step();
            dfs(i, visited, matrix, n, states, topology, emit);
            provinces++;
        }

        emit.at("done").say("Every city has been visited. The cities form %d province%s.", provinces, Narration.s(provinces))
                .var("numOfProvinces", provinces)
                .graph(topology).nodes(states).step();
    }

    private static void dfs(int currNode, boolean[] visited, int[][] matrix, int n,
                            Map<Integer, String> states, Inputs.GraphInput topology, StepEmitter emit) {
        emit.push("dfs(" + currNode + ")");
        visited[currNode] = true;
        states.put(currNode, "visiting");
        emit.at("visit").say("dfs(%d): mark city %d visited, then scan row %d for unvisited neighbours.",
                        currNode, currNode, currNode)
                .var("currNode", currNode)
                .graph(topology).nodes(states).step();

        for (int j = 0; j < n; j++) {
            if (matrix[currNode][j] == 1 && !visited[j]) {
                emit.at("recurse").say("isConnected[%d][%d] = 1 and city %d is unvisited - recurse into dfs(%d).",
                                currNode, j, j, j)
                        .var("currNode", currNode).var("j", j)
                        .graph(topology).nodes(states).edges(edgeLabel(currNode, j)).step();
                dfs(j, visited, matrix, n, states, topology, emit);
            }
        }

        states.put(currNode, "visited");
        emit.at("return").say("dfs(%d) is done: every unvisited city joined to %d has been reached. "
                        + "Return to the caller.", currNode, currNode)
                .var("currNode", currNode)
                .graph(topology).nodes(states).step();
        emit.pop();
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
