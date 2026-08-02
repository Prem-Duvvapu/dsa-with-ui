package com.dsa.ui.service;

import com.dsa.ui.algorithm.graph.*;
import com.dsa.ui.model.*;
import com.dsa.ui.trace.ListTraceRecorder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphBfsDfsService {

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

    public List<ExecutionStep> generateSteps(String problemId) {
        switch (problemId) {
            case "bfs-traversal": return generateBfsSteps();
            case "dfs-traversal": return generateDfsSteps();
            case "number-of-provinces": return generateProvincesSteps();
            case "number-of-islands": return generateIslandsSteps();
            case "rotting-oranges": return generateRottingOrangesSteps();
            case "flood-fill": return generateFloodFillSteps();
            case "undirected-cycle-bfs": return generateUndirectedCycleBfsSteps();
            case "undirected-cycle-dfs": return generateUndirectedCycleDfsSteps();
            case "directed-cycle-dfs": return generateDirectedCycleDfsSteps();
            case "distance-nearest-1": return generateDistanceNearest1Steps();
            case "surrounded-regions": return generateSurroundedRegionsSteps();
            default: return generateBfsSteps();
        }
    }

    private void initProblems() {
        // 1. BFS Traversal
        problems.put("bfs-traversal", new ProblemDetail(
            "bfs-traversal", "BFS Traversal of Graph", "Graphs - BFS/DFS Problems", "Graph BFS/DFS", "Easy",
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
            "Queue"
        ));

        // 2. DFS Traversal
        problems.put("dfs-traversal", new ProblemDetail(
            "dfs-traversal", "DFS Traversal of Graph", "Graphs - BFS/DFS Problems", "Graph BFS/DFS", "Easy",
            "Given a connected undirected graph with V vertices and E edges, perform a Depth First Search (DFS) starting from vertex 0 using recursion/call stack.",
            """
            // Java DFS Implementation (Striver A2Z Sheet)
            public ArrayList<Integer> dfsOfGraph(int V, ArrayList<ArrayList<Integer>> adj) {
                boolean vis[] = new boolean[V];
                ArrayList<Integer> ls = new ArrayList<>();
                dfs(0, vis, adj, ls);
                return ls;
            }

            private void dfs(int node, boolean vis[], ArrayList<ArrayList<Integer>> adj, ArrayList<Integer> ls) {
                vis[node] = true;
                ls.add(node);

                for (Integer it : adj.get(node)) {
                    if (!vis[it]) {
                        dfs(it, vis, adj, ls);
                    }
                }
            }
            """,
            createDefaultNodes(), createDefaultEdges(), null, null,
            new ComplexityDetail(
                "O(V + 2E)",
                "Time Complexity: The recursive DFS function is called exactly once for each vertex O(V). Inside each call, we iterate through its adjacency list. For all vertices combined, edges are checked 2E times in undirected graph.",
                "Why O(V + 2E)? Every vertex is marked visited upon entry, so recursive call runs V times. Each edge is traversed from both direction endpoints.",
                "O(V)",
                "Space Complexity: O(V) auxiliary recursion call stack space in the worst case (skewed/linear graph) plus O(V) for visited array.",
                "Why O(V) space? If the graph is a single line 0-1-2-3-...-V-1, the maximum depth of the call stack reaches V frames.",
                "Auxiliary Space: O(V) (Recursion Call Stack & Visited Array)",
                "Adjacency List Space: O(V + 2E)"
            ),
            "Stack"
        ));

        // 3. Number of Provinces
        problems.put("number-of-provinces", new ProblemDetail(
            "number-of-provinces", "Number of Provinces", "Graphs - BFS/DFS Problems", "Graph BFS/DFS", "Medium",
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
            "Stack"
        ));

        // 4. Number of Islands
        problems.put("number-of-islands", new ProblemDetail(
            "number-of-islands", "Number of Islands", "Graphs - BFS/DFS Problems", "Graph BFS/DFS", "Medium",
            "Given an m x n 2D binary grid grid where '1' represents land and '0' represents water, return the number of islands.",
            """
            // Java Solution: Number of Islands (LeetCode 200)
            public int numIslands(char[][] grid) {
                int n = grid.length, m = grid[0].length;
                boolean[][] vis = new boolean[n][m];
                int count = 0;

                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < m; j++) {
                        if (grid[i][j] == '1' && !vis[i][j]) {
                            count++;
                            bfs(i, j, grid, vis);
                        }
                    }
                }
                return count;
            }

            private void bfs(int r, int c, char[][] grid, boolean[][] vis) {
                vis[r][c] = true;
                Queue<int[]> q = new LinkedList<>();
                q.add(new int[]{r, c});
                int[] dr = {-1, 1, 0, 0};
                int[] dc = {0, 0, -1, 1};

                while (!q.isEmpty()) {
                    int[] cell = q.poll();
                    for (int i = 0; i < 4; i++) {
                        int nr = cell[0] + dr[i], nc = cell[1] + dc[i];
                        if (nr >= 0 && nr < grid.length && nc >= 0 && nc < grid[0].length
                            && grid[nr][nc] == '1' && !vis[nr][nc]) {
                            vis[nr][nc] = true;
                            q.add(new int[]{nr, nc});
                        }
                    }
                }
            }
            """,
            null, null, null, createIslandGrid(),
            new ComplexityDetail(
                "O(N x M)",
                "Time Complexity: Outer nested loops iterate over all N x M cells. Each land cell ('1') is visited at most once by BFS/DFS. For each cell, we inspect 4 directional neighbors.",
                "Why O(N x M)? Total grid cells = N x M. Each cell undergoes 4 boundary & land checks, making total work proportional to 4 * N * M = O(N x M).",
                "O(N x M)",
                "Space Complexity: Visited boolean matrix vis[N][M] takes O(N x M). The Queue can hold up to O(N x M) cells in the worst case (e.g. grid filled with all '1's).",
                "Why O(N x M)? In worst-case diagonal traversal of a full land grid, queue size can grow up to O(min(N, M)) or O(N x M).",
                "Auxiliary Space: O(N x M) (Queue & Visited Grid)",
                "Grid Space: O(N x M)"
            ),
            "Queue"
        ));

        // 5. Rotting Oranges
        problems.put("rotting-oranges", new ProblemDetail(
            "rotting-oranges", "Rotting Oranges", "Graphs - BFS/DFS Problems", "Graph BFS/DFS", "Medium",
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
            "Queue"
        ));

        // 6. Flood Fill
        problems.put("flood-fill", new ProblemDetail(
            "flood-fill", "Flood Fill Algorithm", "Graphs - BFS/DFS Problems", "Graph BFS/DFS", "Easy",
            "An image is represented by an m x n grid of integers. Perform a flood fill on image starting from pixel (sr, sc) with color newColor.",
            """
            // Java Solution: Flood Fill (LeetCode 733)
            public int[][] floodFill(int[][] image, int sr, int sc, int newColor) {
                int iniColor = image[sr][sc];
                if (iniColor != newColor) {
                    dfs(sr, sc, image, iniColor, newColor);
                }
                return image;
            }

            private void dfs(int r, int c, int[][] image, int iniColor, int newColor) {
                if (r < 0 || r >= image.length || c < 0 || c >= image[0].length || image[r][c] != iniColor) {
                    return;
                }
                image[r][c] = newColor;
                dfs(r - 1, c, image, iniColor, newColor);
                dfs(r + 1, c, image, iniColor, newColor);
                dfs(r, c - 1, image, iniColor, newColor);
                dfs(r, c + 1, image, iniColor, newColor);
            }
            """,
            null, null, null, createFloodFillGrid(),
            new ComplexityDetail(
                "O(N x M)",
                "Time Complexity: In the worst case, all pixels in the image have the initial color. The DFS algorithm visits each connected pixel of the same color exactly once.",
                "Why O(N x M)? At most N x M pixels are repainted. For each pixel, 4 recursive calls check boundaries and matching initial color.",
                "O(N x M)",
                "Space Complexity: Call stack depth can reach O(N x M) in the worst case (e.g. a long snake-like region of matching color pixels).",
                "Why O(N x M)? Max call stack depth equals the maximum path length of same-colored adjacent pixels.",
                "Auxiliary Space: O(N x M) (Recursion Stack)",
                "Grid Space: Modified in-place O(1)"
            ),
            "Stack"
        ));

        // 7. Undirected Cycle BFS
        problems.put("undirected-cycle-bfs", new ProblemDetail(
            "undirected-cycle-bfs", "Detect Cycle in Undirected Graph (BFS)", "Graphs - BFS/DFS Problems", "Graph BFS/DFS", "Medium",
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
            "Queue"
        ));

        // 8. Undirected Cycle DFS
        problems.put("undirected-cycle-dfs", new ProblemDetail(
            "undirected-cycle-dfs", "Detect Cycle in Undirected Graph (DFS)", "Graphs - BFS/DFS Problems", "Graph BFS/DFS", "Medium",
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
            "Stack"
        ));

        // 9. Directed Cycle DFS
        problems.put("directed-cycle-dfs", new ProblemDetail(
            "directed-cycle-dfs", "Detect Cycle in Directed Graph (DFS)", "Graphs - BFS/DFS Problems", "Graph BFS/DFS", "Medium",
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
            "Stack"
        ));

        // 10. Distance of Nearest 1 (0/1 Matrix)
        problems.put("distance-nearest-1", new ProblemDetail(
            "distance-nearest-1", "0/1 Matrix - Distance of Nearest 1", "Graphs - BFS/DFS Problems", "Graph BFS/DFS", "Medium",
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
            "Queue"
        ));

        // 11. Surrounded Regions
        problems.put("surrounded-regions", new ProblemDetail(
            "surrounded-regions", "Surrounded Regions (Replace 'O' with 'X')", "Graphs - BFS/DFS Problems", "Graph BFS/DFS", "Medium",
            "Given an m x n matrix board containing 'X' and 'O', capture all regions that are 4-directionally surrounded by 'X'.",
            """
            // Java Solution: Surrounded Regions (LeetCode 130 - Boundary Traversal)
            public void solve(char[][] board) {
                int n = board.length, m = board[0].length;
                boolean[][] vis = new boolean[n][m];

                // Check boundary rows
                for (int j = 0; j < m; j++) {
                    if (!vis[0][j] && board[0][j] == 'O') dfs(0, j, board, vis);
                    if (!vis[n - 1][j] && board[n - 1][j] == 'O') dfs(n - 1, j, board, vis);
                }
                // Check boundary columns
                for (int i = 0; i < n; i++) {
                    if (!vis[i][0] && board[i][0] == 'O') dfs(i, 0, board, vis);
                    if (!vis[i][m - 1] && board[i][m - 1] == 'O') dfs(i, m - 1, board, vis);
                }

                // Replace unvisited 'O's with 'X'
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < m; j++) {
                        if (!vis[i][j] && board[i][j] == 'O') board[i][j] = 'X';
                    }
                }
            }

            private void dfs(int r, int c, char[][] board, boolean[][] vis) {
                vis[r][c] = true;
                int[] dr = {-1, 1, 0, 0}, dc = {0, 0, -1, 1};
                for (int i = 0; i < 4; i++) {
                    int nr = r + dr[i], nc = c + dc[i];
                    if (nr >= 0 && nr < board.length && nc >= 0 && nc < board[0].length
                        && !vis[nr][nc] && board[nr][nc] == 'O') {
                        dfs(nr, nc, board, vis);
                    }
                }
            }
            """,
            null, null, null, createSurroundedGrid(),
            new ComplexityDetail(
                "O(N x M)",
                "Time Complexity: Traversing 4 boundaries takes O(N + M). DFS starting from boundary 'O's visits each connected 'O' once O(N x M). Final grid scan takes O(N x M).",
                "Why Boundary Traversal key? Any 'O' connected to a boundary 'O' CANNOT be surrounded by 'X's! Thus, marking boundary-connected 'O's leaves only truly surrounded interior 'O's.",
                "O(N x M)",
                "Space Complexity: Visited boolean matrix O(N x M) + recursion stack memory depth up to O(N x M).",
                "Why O(N x M)? Maximum connected component of boundary-touching 'O's can span up to N x M cells.",
                "Auxiliary Space: O(N x M)",
                "Grid Space: Modified in-place O(1)"
            ),
            "Stack"
        ));
    }

    // Step Generators
    private List<ExecutionStep> generateBfsSteps() {
        int v = 6;
        Map<Integer, List<Integer>> adj = Map.of(
            0, List.of(1, 2),
            1, List.of(0, 3),
            2, List.of(0, 4, 5),
            3, List.of(1),
            4, List.of(2),
            5, List.of(2)
        );
        ListTraceRecorder recorder = new ListTraceRecorder();
        new BfsTraversal().solve(v, adj, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generateDfsSteps() {
        int v = 6;
        Map<Integer, List<Integer>> adj = Map.of(
            0, List.of(1, 2),
            1, List.of(0, 3),
            2, List.of(0, 4, 5),
            3, List.of(1),
            4, List.of(2),
            5, List.of(2)
        );
        ListTraceRecorder recorder = new ListTraceRecorder();
        new DfsTraversal().solve(v, adj, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generateProvincesSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> nodeStates = new HashMap<>();
        for (int i = 0; i < 4; i++) nodeStates.put(i, "unvisited");

        nodeStates.put(0, "visiting");
        steps.add(new ExecutionStep(1, 8, "i = 0 is unvisited. Increment provinces = 1. Start DFS(0)", List.of("dfs(0)"), new HashMap<>(nodeStates), List.of(), Map.of("provinces", "1"), "Stack", null));

        nodeStates.put(1, "visiting");
        steps.add(new ExecutionStep(2, 16, "isConnected[0][1] == 1. Invoke dfs(1)", List.of("dfs(0)", "dfs(1)"), new HashMap<>(nodeStates), List.of("0-1"), Map.of("provinces", "1"), "Stack", null));

        nodeStates.put(1, "visited"); nodeStates.put(0, "visited");
        steps.add(new ExecutionStep(3, 9, "Finish Province 1 (Nodes 0, 1)", List.of(), new HashMap<>(nodeStates), List.of(), Map.of("provinces", "1"), "Stack", null));

        nodeStates.put(2, "visiting");
        steps.add(new ExecutionStep(4, 8, "i = 2 is unvisited. Increment provinces = 2. Start DFS(2)", List.of("dfs(2)"), new HashMap<>(nodeStates), List.of(), Map.of("provinces", "2"), "Stack", null));

        nodeStates.put(3, "visiting");
        steps.add(new ExecutionStep(5, 16, "isConnected[2][3] == 1. Invoke dfs(3)", List.of("dfs(2)", "dfs(3)"), new HashMap<>(nodeStates), List.of("2-3"), Map.of("provinces", "2"), "Stack", null));

        nodeStates.put(3, "visited"); nodeStates.put(2, "visited");
        steps.add(new ExecutionStep(6, 11, "All nodes processed. Total Provinces = 2", List.of(), new HashMap<>(nodeStates), List.of(), Map.of("provinces", "2"), "Stack", null));

        return steps;
    }

    private List<ExecutionStep> generateIslandsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[][] grid = createIslandGrid();

        steps.add(new ExecutionStep(1, 7, "Start grid traversal. Scan cell (0,0)", List.of(), Map.of(), List.of(), Map.of("count", "0"), "Matrix", copyGrid(grid)));
        grid[0][0] = 3;
        steps.add(new ExecutionStep(2, 9, "Found land at (0,0) & unvisited. Found Island 1! Launch BFS(0,0)", List.of("(0,0)"), Map.of(), List.of(), Map.of("count", "1"), "Matrix", copyGrid(grid)));
        grid[0][1] = 3; grid[1][0] = 3;
        steps.add(new ExecutionStep(3, 21, "BFS expands: mark connected land cells (0,1) and (1,0)", List.of("(0,1)", "(1,0)"), Map.of(), List.of(), Map.of("count", "1"), "Matrix", copyGrid(grid)));
        grid[0][0] = 4; grid[0][1] = 4; grid[1][0] = 4;
        steps.add(new ExecutionStep(4, 11, "Completed BFS for Island 1. Continue grid search...", List.of(), Map.of(), List.of(), Map.of("count", "1"), "Matrix", copyGrid(grid)));
        grid[2][2] = 3;
        steps.add(new ExecutionStep(5, 9, "Found land at (2,2). Found Island 2! Launch BFS(2,2)", List.of("(2,2)"), Map.of(), List.of(), Map.of("count", "2"), "Matrix", copyGrid(grid)));
        grid[2][3] = 3;
        steps.add(new ExecutionStep(6, 21, "BFS expands: mark connected land cell (2,3)", List.of("(2,3)"), Map.of(), List.of(), Map.of("count", "2"), "Matrix", copyGrid(grid)));
        grid[2][2] = 4; grid[2][3] = 4;
        steps.add(new ExecutionStep(7, 13, "Grid scan finished. Total Islands = 2", List.of(), Map.of(), List.of(), Map.of("Total Islands", "2"), "Matrix", copyGrid(grid)));

        return steps;
    }

    private List<ExecutionStep> generateRottingOrangesSteps() {
        int[][] grid = {
            {2, 1, 1},
            {1, 1, 0},
            {0, 1, 1}
        };
        ListTraceRecorder recorder = new ListTraceRecorder();
        new RottingOranges().solve(grid, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generateFloodFillSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[][] grid = createFloodFillGrid();

        steps.add(new ExecutionStep(1, 3, "Start Flood Fill at (1,1) with newColor = 2. Initial color = 1", List.of("dfs(1,1)"), Map.of(), List.of(), Map.of("iniColor", "1", "newColor", "2"), "Matrix", copyGrid(grid)));
        grid[1][1] = 2;
        steps.add(new ExecutionStep(2, 11, "Repaint (1,1) -> 2. Recurse 4 directions...", List.of("dfs(1,1)"), Map.of(), List.of(), Map.of("pixel", "(1,1)"), "Matrix", copyGrid(grid)));
        grid[0][1] = 2; grid[1][0] = 2; grid[1][2] = 2; grid[2][1] = 2;
        steps.add(new ExecutionStep(3, 12, "Repaint connected color 1 pixels at (0,1), (1,0), (1,2), (2,1)", List.of("dfs(0,1)", "dfs(1,0)", "dfs(1,2)", "dfs(2,1)"), Map.of(), List.of(), Map.of("pixel", "connected"), "Matrix", copyGrid(grid)));
        steps.add(new ExecutionStep(4, 5, "Flood Fill algorithm completed successfully!", List.of(), Map.of(), List.of(), Map.of("Status", "Complete"), "Matrix", copyGrid(grid)));

        return steps;
    }

    private List<ExecutionStep> generateUndirectedCycleBfsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> nodeStates = new HashMap<>();
        for (int i = 0; i < 4; i++) nodeStates.put(i, "unvisited");

        nodeStates.put(0, "queued");
        steps.add(new ExecutionStep(1, 15, "Start BFS from 0. Queue pair (node=0, parent=-1)", List.of("(0, parent:-1)"), new HashMap<>(nodeStates), List.of(), Map.of("vis[0]", "true"), "Queue", null));

        nodeStates.put(0, "visited"); nodeStates.put(1, "queued"); nodeStates.put(2, "queued");
        steps.add(new ExecutionStep(2, 22, "Poll 0. Add neighbors 1 and 2 to Queue with parent=0", List.of("(1, parent:0)", "(2, parent:0)"), new HashMap<>(nodeStates), List.of("0-1", "0-2"), Map.of("node", "0"), "Queue", null));

        nodeStates.put(1, "visited"); nodeStates.put(3, "queued");
        steps.add(new ExecutionStep(3, 22, "Poll 1. Add neighbor 3 to Queue with parent=1", List.of("(2, parent:0)", "(3, parent:1)"), new HashMap<>(nodeStates), List.of("1-3"), Map.of("node", "1"), "Queue", null));

        nodeStates.put(2, "visiting"); nodeStates.put(3, "cycle");
        steps.add(new ExecutionStep(4, 25, "Poll 2. Inspect adjacent node 3. vis[3] == true AND parent (0) != 3! CYCLE DETECTED!", List.of("(2, parent:0)"), new HashMap<>(nodeStates), List.of("2-3"), Map.of("Cycle Found", "TRUE"), "Queue", null));

        return steps;
    }

    private List<ExecutionStep> generateUndirectedCycleDfsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> nodeStates = new HashMap<>();
        for (int i = 0; i < 4; i++) nodeStates.put(i, "unvisited");

        nodeStates.put(0, "visiting");
        steps.add(new ExecutionStep(1, 13, "Start DFS(0, parent=-1)", List.of("dfs(0, -1)"), new HashMap<>(nodeStates), List.of(), Map.of("vis[0]", "true"), "Stack", null));

        nodeStates.put(1, "visiting");
        steps.add(new ExecutionStep(2, 16, "DFS(0) -> DFS(1, parent=0)", List.of("dfs(0, -1)", "dfs(1, 0)"), new HashMap<>(nodeStates), List.of("0-1"), Map.of("vis[1]", "true"), "Stack", null));

        nodeStates.put(3, "visiting");
        steps.add(new ExecutionStep(3, 16, "DFS(1) -> DFS(3, parent=1)", List.of("dfs(0, -1)", "dfs(1, 0)", "dfs(3, 1)"), new HashMap<>(nodeStates), List.of("1-3"), Map.of("vis[3]", "true"), "Stack", null));

        nodeStates.put(2, "cycle");
        steps.add(new ExecutionStep(4, 18, "DFS(3) inspects neighbor 2. vis[2] == true AND adjacent (2) != parent (1)! CYCLE DETECTED!", List.of("dfs(0, -1)", "dfs(1, 0)", "dfs(3, 1)"), new HashMap<>(nodeStates), List.of("3-2", "2-0"), Map.of("Cycle Found", "TRUE"), "Stack", null));

        return steps;
    }

    private List<ExecutionStep> generateDirectedCycleDfsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> nodeStates = new HashMap<>();
        for (int i = 0; i < 4; i++) nodeStates.put(i, "unvisited");

        nodeStates.put(0, "visiting");
        steps.add(new ExecutionStep(1, 16, "dfsCheck(0). Set vis[0]=true, pathVis[0]=true", List.of("dfs(0)"), new HashMap<>(nodeStates), List.of(), Map.of("pathVis", "[0]"), "Stack", null));

        nodeStates.put(1, "visiting");
        steps.add(new ExecutionStep(2, 19, "dfsCheck(0) -> dfsCheck(1). Set vis[1]=true, pathVis[1]=true", List.of("dfs(0)", "dfs(1)"), new HashMap<>(nodeStates), List.of("0-1"), Map.of("pathVis", "[0, 1]"), "Stack", null));

        nodeStates.put(2, "visiting");
        steps.add(new ExecutionStep(3, 19, "dfsCheck(1) -> dfsCheck(2). Set vis[2]=true, pathVis[2]=true", List.of("dfs(0)", "dfs(1)", "dfs(2)"), new HashMap<>(nodeStates), List.of("1-2"), Map.of("pathVis", "[0, 1, 2]"), "Stack", null));

        nodeStates.put(0, "cycle");
        steps.add(new ExecutionStep(4, 21, "dfsCheck(2) inspects directed edge 2 -> 0. vis[0] == true AND pathVis[0] == true! DIRECTED CYCLE DETECTED!", List.of("dfs(0)", "dfs(1)", "dfs(2)"), new HashMap<>(nodeStates), List.of("2-0"), Map.of("Cycle Detected", "TRUE"), "Stack", null));

        return steps;
    }

    private List<ExecutionStep> generateDistanceNearest1Steps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[][] distGrid = new int[][]{
            {0, 1, 2},
            {1, 0, 1},
            {2, 1, 2}
        };

        steps.add(new ExecutionStep(1, 8, "Multi-source BFS: Insert all cells with value 1 into Queue at distance 0", List.of("(0,0,d=0)", "(1,1,d=0)"), Map.of(), List.of(), Map.of("queue_size", "2"), "Matrix", distGrid));
        steps.add(new ExecutionStep(2, 22, "Multi-source BFS finished: computed shortest distance from nearest 1 for all grid cells", List.of(), Map.of(), List.of(), Map.of("Status", "Complete"), "Matrix", distGrid));

        return steps;
    }

    private List<ExecutionStep> generateSurroundedRegionsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[][] grid = new int[][]{
            {1, 1, 1, 1},
            {1, 0, 0, 1},
            {1, 1, 0, 1},
            {1, 0, 1, 1}
        };

        steps.add(new ExecutionStep(1, 6, "Traverse boundary cells looking for 'O's...", List.of(), Map.of(), List.of(), Map.of("phase", "Boundary Search"), "Matrix", copyGrid(grid)));
        grid[3][1] = 2;
        steps.add(new ExecutionStep(2, 9, "Found boundary 'O' at (3,1). DFS marks it and its connected 'O's as un-capturable safe 'O's", List.of("dfs(3,1)"), Map.of(), List.of(), Map.of("safe", "(3,1)"), "Matrix", copyGrid(grid)));
        steps.add(new ExecutionStep(3, 16, "Flip remaining unvisited interior 'O's to 'X's. Surrounded regions captured!", List.of(), Map.of(), List.of(), Map.of("status", "Complete"), "Matrix", copyGrid(grid)));

        return steps;
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
