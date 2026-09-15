package com.dsa.ui.service;

import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphBfsDfsService implements ProblemProvider {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public GraphBfsDfsService() {
        initProblems();
    }

    public List<ProblemDetail> getAllProblems() {
        return new ArrayList<>(problems.values());
    }

    public ProblemDetail getProblemById(String id) {
        return problems.get(id);
    }

    private void initProblems() {
        // 1. BFS Traversal
        problems.put("bfs-traversal", new ProblemDetail(
            "bfs-traversal", "BFS Traversal of Graph", "Graphs - BFS/DFS Problems", "Graphs", "Easy",
            "Given a connected undirected graph with V vertices and E edges, perform a Breadth First Search (BFS) starting from vertex 0.",
            """
            // Java BFS Implementation (Striver A2Z Sheet)
            public ArrayList<Integer> bfsOfGraph(int V, ArrayList<ArrayList<Integer>> adj) {
                ArrayList<Integer> bfs = new ArrayList<>();
                boolean vis[] = new boolean[V];
                Queue<Integer> q = new LinkedList<>();

                q.add(0);
                vis[0] = true;

                while (!q.isEmpty()) {
                    Integer node = q.poll();
                    bfs.add(node);

                    for (Integer it : adj.get(node)) {
                        if (!vis[it]) {
                            vis[it] = true;
                            q.add(it);
                        }
                    }
                }
                return bfs;
            }
            """,
            createDefaultNodes(), createDefaultEdges(), null, null,
            new ComplexityDetail(
                "O(V + 2E)",
                "Time Complexity: Every vertex V is pushed into the queue once and polled once O(V). For each vertex, we iterate over all its adjacent edges. In an undirected graph, the total sum of degrees is 2E, so inner loop runs 2E times.",
                "Why O(V + 2E)? BFS visits every node once and inspects each undirected edge twice (once from each endpoint). Thus, Total Operations = V + 2E.",
                "O(V)",
                "Space Complexity: O(V) for the visited array 'vis[]', O(V) for the Queue 'q', and O(V) for the result list 'bfs'.",
                "Why O(V) space? In the worst case (star graph or complete graph), all V vertices might be stored in the queue at the same level.",
                "Auxiliary Space: O(V) (Queue & Visited Array)",
                "Adjacency List Space: O(V + 2E)"
            ),
            "Graph"
        ));

        // 2. DFS Traversal
        // 3. Number of Provinces
        problems.put("number-of-provinces", new ProblemDetail(
            "number-of-provinces", "Number of Provinces", "Graphs - BFS/DFS Problems", "Graphs", "Medium",
            "Given an N x N matrix isConnected where isConnected[i][j] = 1 if the ith city and jth city are directly connected. Find the total number of connected components (provinces).",
            """
            // Java Solution: Number of Provinces (LeetCode 547)
            public int findCircleNum(int[][] isConnected) {
                int V = isConnected.length;
                boolean[] vis = new boolean[V];
                int provinces = 0;

                for (int i = 0; i < V; i++) {
                    if (!vis[i]) {
                        provinces++;
                        dfs(i, isConnected, vis);
                    }
                }
                return provinces;
            }

            private void dfs(int node, int[][] isConnected, boolean[] vis) {
                vis[node] = true;
                for (int j = 0; j < isConnected.length; j++) {
                    if (isConnected[node][j] == 1 && !vis[j]) {
                        dfs(j, isConnected, vis);
                    }
                }
            }
            """,
            createProvinceNodes(), createProvinceEdges(), null, null,
            new ComplexityDetail(
                "O(V^2)",
                "Time Complexity: We iterate over V outer nodes. For each unvisited node, DFS visits all connected nodes. Since adjacency matrix is V x V, checking neighbours takes O(V) for each vertex, leading to O(V^2) total operations.",
                "Why O(V^2)? Reading through row i in matrix isConnected[i][j] checks all V columns for every node.",
                "O(V)",
                "Space Complexity: Visited array of size O(V) + recursion stack space O(V) for a graph with V vertices.",
                "Why O(V)? Max recursion stack height equals number of cities in the largest connected province (<= V).",
                "Auxiliary Space: O(V)",
                "Matrix Input Space: O(V^2)"
            ),
            DsType.GRAPH.wireValue()
        ));

        // 4. Number of Islands
        // 5. Rotting Oranges
        problems.put("rotting-oranges", new ProblemDetail(
            "rotting-oranges", "Rotting Oranges", "Graphs - BFS/DFS Problems", "Graphs", "Medium",
            "Given a grid where 0=empty, 1=fresh orange, 2=rotten orange. Every minute, any fresh orange adjacent to a rotten orange becomes rotten. Return minimum minutes to rot all oranges, or -1.",
            """
            // Java Solution: Rotting Oranges (LeetCode 994 - Multi-Source BFS)
            public int orangesRotting(int[][] grid) {
                int n = grid.length, m = grid[0].length;
                Queue<int[]> q = new LinkedList<>();
                int cntFresh = 0;

                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < m; j++) {
                        if (grid[i][j] == 2) q.add(new int[]{i, j, 0});
                        else if (grid[i][j] == 1) cntFresh++;
                    }
                }

                int tm = 0, cnt = 0;
                int[] dRow = {-1, 0, 1, 0}, dCol = {0, 1, 0, -1};

                while (!q.isEmpty()) {
                    int r = q.peek()[0], c = q.peek()[1], t = q.peek()[2];
                    tm = Math.max(tm, t);
                    q.poll();

                    for (int i = 0; i < 4; i++) {
                        int nrow = r + dRow[i], ncol = c + dCol[i];
                        if (nrow >= 0 && nrow < n && ncol >= 0 && ncol < m && grid[nrow][ncol] == 1) {
                            grid[nrow][ncol] = 2;
                            q.add(new int[]{nrow, ncol, t + 1});
                            cnt++;
                        }
                    }
                }
                return cnt == cntFresh ? tm : -1;
            }
            """,
            null, null, null, createRottingGrid(),
            new ComplexityDetail(
                "O(N x M)",
                "Time Complexity: We first scan all N x M cells to push initial rotten oranges into the Queue and count fresh ones O(N x M). Multi-source BFS processes each cell at most once.",
                "Why Multi-Source BFS is O(N x M)? All rotten oranges are inserted into the queue at time t=0. Level-by-level processing guarantees that every cell is reached in the minimum possible time.",
                "O(N x M)",
                "Space Complexity: The queue stores rotten oranges. In the worst case, all N x M cells are rotten, so Queue memory is O(N x M).",
                "Why O(N x M)? Queue size corresponds to the maximum number of simultaneously rotting oranges at any time level.",
                "Auxiliary Space: O(N x M) (Queue)",
                "Grid Space: O(N x M)"
            ),
            DsType.MATRIX.wireValue()
        ));

        // 6. Flood Fill
        // 7. Undirected Cycle BFS
        problems.put("undirected-cycle-bfs", new ProblemDetail(
            "undirected-cycle-bfs", "Detect Cycle in Undirected Graph (BFS)", "Graphs - BFS/DFS Problems", "Graphs", "Medium",
            "Given an undirected graph with V vertices and E edges, check whether it contains a cycle using Breadth First Search.",
            """
            // Java Solution: Detect Cycle in Undirected Graph (BFS)
            class NodePair {
                int node, parent;
                NodePair(int node, int parent) { this.node = node; this.parent = parent; }
            }

            public boolean isCycle(int V, ArrayList<ArrayList<Integer>> adj) {
                boolean vis[] = new boolean[V];
                for (int i = 0; i < V; i++) {
                    if (!vis[i]) {
                        if (checkForCycle(i, V, adj, vis)) return true;
                    }
                }
                return false;
            }

            private boolean checkForCycle(int src, int V, ArrayList<ArrayList<Integer>> adj, boolean[] vis) {
                vis[src] = true;
                Queue<NodePair> q = new LinkedList<>();
                q.add(new NodePair(src, -1));

                while (!q.isEmpty()) {
                    int node = q.peek().node;
                    int parent = q.peek().parent;
                    q.poll();

                    for (int adjacentNode : adj.get(node)) {
                        if (!vis[adjacentNode]) {
                            vis[adjacentNode] = true;
                            q.add(new NodePair(adjacentNode, node));
                        } else if (parent != adjacentNode) {
                            return true; // Cycle detected!
                        }
                    }
                }
                return false;
            }
            """,
            createCyclicGraphNodes(), createCyclicGraphEdges(), null, null,
            new ComplexityDetail(
                "O(V + 2E)",
                "Time Complexity: We loop through all components O(V). For each component, BFS processes all vertices and checks edges. Total edge operations across undirected graph sum to 2E.",
                "Why Parent Check Detects Cycle? In BFS, if an adjacent node is already visited AND is NOT the parent of current node, it means there exists another path to that node -> Cycle exists!",
                "O(V)",
                "Space Complexity: Visited array takes O(V) and Queue stores pairs of (node, parent) taking up to O(V) memory.",
                "Why O(V)? Max queue length is bounded by number of vertices V.",
                "Auxiliary Space: O(V) (Queue & Visited array)",
                "Adjacency List Space: O(V + 2E)"
            ),
            DsType.GRAPH.wireValue()
        ));

        // 8. Undirected Cycle DFS
        problems.put("undirected-cycle-dfs", new ProblemDetail(
            "undirected-cycle-dfs", "Detect Cycle in Undirected Graph (DFS - parent check)", "Graphs - BFS/DFS Problems", "Graphs", "Medium",
            "Given an undirected graph with V vertices and E edges, check whether it contains a cycle using Depth First Search (Recursion).",
            """
            // Java Solution: Detect Cycle in Undirected Graph (DFS)
            public boolean isCycle(int V, ArrayList<ArrayList<Integer>> adj) {
                boolean vis[] = new boolean[V];
                for (int i = 0; i < V; i++) {
                    if (!vis[i]) {
                        if (dfs(i, -1, vis, adj)) return true;
                    }
                }
                return false;
            }

            private boolean dfs(int node, int parent, boolean vis[], ArrayList<ArrayList<Integer>> adj) {
                vis[node] = true;
                for (int adjacentNode : adj.get(node)) {
                    if (!vis[adjacentNode]) {
                        if (dfs(adjacentNode, node, vis, adj)) return true;
                    } else if (adjacentNode != parent) {
                        return true; // Cycle detected!
                    }
                }
                return false;
            }
            """,
            createCyclicGraphNodes(), createCyclicGraphEdges(), null, null,
            new ComplexityDetail(
                "O(V + 2E)",
                "Time Complexity: DFS traverses all V nodes and 2E edges across all connected components.",
                "Why DFS finds cycle? If during recursive traversal, we encounter an adjacent node that is already marked as visited AND is NOT the immediate parent node, we have detected a back-edge creating a loop/cycle.",
                "O(V)",
                "Space Complexity: Visited array O(V) + auxiliary recursion call stack space O(V).",
                "Why O(V)? Max recursion stack depth is equal to the longest path in the graph component.",
                "Auxiliary Space: O(V)",
                "Adjacency List Space: O(V + 2E)"
            ),
            DsType.GRAPH.wireValue()
        ));

        // 9. Directed Cycle DFS
        problems.put("directed-cycle-dfs", new ProblemDetail(
            "directed-cycle-dfs", "Detect Cycle in Directed Graph (DFS - recursion path)", "Graphs - BFS/DFS Problems", "Graphs", "Medium",
            "Given a directed graph with V vertices and E edges, check whether it contains a cycle using DFS with pathVisited array / recursion stack tracking.",
            """
            // Java Solution: Detect Cycle in Directed Graph (DFS)
            public boolean isCyclic(int V, ArrayList<ArrayList<Integer>> adj) {
                boolean vis[] = new boolean[V];
                boolean pathVis[] = new boolean[V];

                for (int i = 0; i < V; i++) {
                    if (!vis[i]) {
                        if (dfsCheck(i, adj, vis, pathVis)) return true;
                    }
                }
                return false;
            }

            private boolean dfsCheck(int node, ArrayList<ArrayList<Integer>> adj, boolean vis[], boolean pathVis[]) {
                vis[node] = true;
                pathVis[node] = true;

                for (int it : adj.get(node)) {
                    if (!vis[it]) {
                        if (dfsCheck(it, adj, vis, pathVis)) return true;
                    } else if (pathVis[it]) {
                        return true; // Directed Cycle detected on current path!
                    }
                }

                pathVis[node] = false; // Backtrack!
                return false;
            }
            """,
            createDirectedGraphNodes(), createDirectedGraphEdges(), null, null,
            new ComplexityDetail(
                "O(V + E)",
                "Time Complexity: In a directed graph, each edge is processed once O(E). Each node is visited once overall O(V). Total time O(V + E).",
                "Why pathVis[] is needed for Directed Graphs? In directed graphs, reaching an already visited node doesn't imply a cycle UNLESS that node lies on the CURRENT recursion path (i.e. pathVis[it] == true). Backtracking sets pathVis[node] = false upon function return.",
                "O(V)",
                "Space Complexity: Visited array O(V), Path Visited array O(V), and recursion stack O(V).",
                "Why O(V)? Max path length in directed graph is at most V vertices.",
                "Auxiliary Space: O(V) (vis[], pathVis[], Recursion Call Stack)",
                "Adjacency List Space: O(V + E)"
            ),
            DsType.GRAPH.wireValue()
        ));

        // 10. Distance of Nearest 1 (0/1 Matrix)
        problems.put("distance-nearest-1", new ProblemDetail(
            "distance-nearest-1", "0/1 Matrix - Distance of Nearest 1", "Graphs - BFS/DFS Problems", "Graphs", "Medium",
            "Given an m x n binary matrix grid, return a matrix dist where dist[i][j] is the distance of the nearest 1 from cell (i, j).",
            """
            // Java Solution: 0/1 Matrix (LeetCode 542 - Multi-Source BFS)
            public int[][] nearest(int[][] grid) {
                int n = grid.length, m = grid[0].length;
                int[][] vis = new int[n][m];
                int[][] dist = new int[n][m];
                Queue<int[]> q = new LinkedList<>();

                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < m; j++) {
                        if (grid[i][j] == 1) {
                            q.add(new int[]{i, j, 0});
                            vis[i][j] = 1;
                        }
                    }
                }

                int delrow[] = {-1, 0, +1, 0};
                int delcol[] = {0, +1, 0, -1};

                while (!q.isEmpty()) {
                    int row = q.peek()[0], col = q.peek()[1], steps = q.peek()[2];
                    q.poll();
                    dist[row][col] = steps;

                    for (int i = 0; i < 4; i++) {
                        int nrow = row + delrow[i], ncol = col + delcol[i];
                        if (nrow >= 0 && nrow < n && ncol >= 0 && ncol < m && vis[nrow][ncol] == 0) {
                            vis[nrow][ncol] = 1;
                            q.add(new int[]{nrow, ncol, steps + 1});
                        }
                    }
                }
                return dist;
            }
            """,
            null, null, null, createNearest1Grid(),
            new ComplexityDetail(
                "O(N x M)",
                "Time Complexity: Initial grid scan O(N x M) pushes all 1-cells into queue at distance 0. Multi-source BFS visits each unvisited neighbor cell exactly once.",
                "Why Multi-source BFS guarantees shortest distance? Because BFS expands outward level-by-level. First time cell (i,j) is reached from any 1-cell gives the absolute minimum steps.",
                "O(N x M)",
                "Space Complexity: dist[][] matrix O(N x M), vis[][] matrix O(N x M), and Queue O(N x M).",
                "Why O(N x M)? Queue size can store at most N x M elements.",
                "Auxiliary Space: O(N x M)",
                "Distance Output Space: O(N x M)"
            ),
            DsType.MATRIX.wireValue()
        ));

        // 11. Surrounded Regions
    }

    // Helper builders
    private List<GraphNode> createDefaultNodes() {
        return List.of(
            new GraphNode(0, "0", 150, 80, "unvisited"),
            new GraphNode(1, "1", 80, 180, "unvisited"),
            new GraphNode(2, "2", 220, 180, "unvisited"),
            new GraphNode(3, "3", 50, 290, "unvisited"),
            new GraphNode(4, "4", 180, 290, "unvisited"),
            new GraphNode(5, "5", 280, 290, "unvisited")
        );
    }

    private List<GraphEdge> createDefaultEdges() {
        return List.of(
            new GraphEdge(0, 1, false),
            new GraphEdge(0, 2, false),
            new GraphEdge(1, 3, false),
            new GraphEdge(2, 4, false),
            new GraphEdge(2, 5, false)
        );
    }

    private List<GraphNode> createProvinceNodes() {
        return List.of(
            new GraphNode(0, "0", 100, 100, "unvisited"),
            new GraphNode(1, "1", 200, 100, "unvisited"),
            new GraphNode(2, "2", 100, 250, "unvisited"),
            new GraphNode(3, "3", 200, 250, "unvisited")
        );
    }

    private List<GraphEdge> createProvinceEdges() {
        return List.of(
            new GraphEdge(0, 1, false),
            new GraphEdge(2, 3, false)
        );
    }

    private List<GraphNode> createCyclicGraphNodes() {
        return List.of(
            new GraphNode(0, "0", 150, 70, "unvisited"),
            new GraphNode(1, "1", 70, 170, "unvisited"),
            new GraphNode(2, "2", 230, 170, "unvisited"),
            new GraphNode(3, "3", 150, 270, "unvisited")
        );
    }

    private List<GraphEdge> createCyclicGraphEdges() {
        return List.of(
            new GraphEdge(0, 1, false),
            new GraphEdge(0, 2, false),
            new GraphEdge(1, 3, false),
            new GraphEdge(2, 3, false)
        );
    }

    private List<GraphNode> createDirectedGraphNodes() {
        return List.of(
            new GraphNode(0, "0", 100, 80, "unvisited"),
            new GraphNode(1, "1", 240, 80, "unvisited"),
            new GraphNode(2, "2", 170, 220, "unvisited")
        );
    }

    private List<GraphEdge> createDirectedGraphEdges() {
        return List.of(
            new GraphEdge(0, 1, true),
            new GraphEdge(1, 2, true),
            new GraphEdge(2, 0, true)
        );
    }

    private int[][] createIslandGrid() {
        return new int[][]{
            {1, 1, 0, 0},
            {1, 0, 0, 1},
            {0, 0, 1, 1},
            {0, 0, 0, 0}
        };
    }

    private int[][] createRottingGrid() {
        return new int[][]{
            {2, 1, 1},
            {1, 1, 0},
            {0, 1, 1}
        };
    }

    private int[][] createFloodFillGrid() {
        return new int[][]{
            {1, 1, 1},
            {1, 1, 0},
            {1, 0, 1}
        };
    }

    private int[][] createNearest1Grid() {
        return new int[][]{
            {0, 0, 0},
            {0, 1, 0},
            {1, 0, 1}
        };
    }

    private int[][] createSurroundedGrid() {
        return new int[][]{
            {1, 1, 1, 1},
            {1, 0, 0, 1},
            {1, 1, 0, 1},
            {1, 0, 1, 1}
        };
    }

    private int[][] copyGrid(int[][] grid) {
        if (grid == null) return null;
        int[][] res = new int[grid.length][grid[0].length];
        for (int i = 0; i < grid.length; i++) {
            System.arraycopy(grid[i], 0, res[i], 0, grid[i].length);
        }
        return res;
    }
}
