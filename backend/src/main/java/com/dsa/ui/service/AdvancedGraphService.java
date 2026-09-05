package com.dsa.ui.service;

import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdvancedGraphService implements ProblemProvider {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public AdvancedGraphService() {
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
            case "graph-intro": return generateGraphIntroSteps();
            case "graph-rep-cpp": return generateGraphRepCppSteps();
            case "graph-rep-java": return generateGraphRepJavaSteps();
            case "connected-components-intro": return generateConnectedComponentsIntroSteps();
            case "bfs-dfs-intro": return generateBfsDfsIntroSteps();
            case "num-provinces": return generateNumProvincesSteps();
            case "connected-matrix": return generateConnectedMatrixSteps();
            case "rotten-oranges": return generateRottenOrangesSteps();
            case "flood-fill": return generateFloodFillSteps();
            case "cycle-undirected-bfs": return generateCycleUndirectedBfsSteps();
            case "cycle-undirected-dfs": return generateCycleUndirectedDfsSteps();
            case "nearest-cell-1": return generateNearestCell1Steps();
            case "surrounded-regions": return generateSurroundedRegionsSteps();
            case "number-of-enclaves": return generateNumberOfEnclavesSteps();
            case "word-ladder-1": throw new LegacyTraceRetiredException(problemId);
            case "word-ladder-2": return generateWordLadder2Steps();
            case "number-of-islands": return generateNumberOfIslandsSteps();
            case "bipartite-graph-dfs": return generateBipartiteGraphDfsSteps();
            case "cycle-directed-dfs": return generateCycleDirectedDfsSteps();
            case "topo-sort-dfs": return generateTopoSortDfsSteps();
            case "kahn-algo-bfs": return generateKahnAlgoSteps();
            case "cycle-directed-bfs": return generateCycleDirectedBfsSteps();
            case "course-schedule-1": return generateCourseSchedule1Steps();
            case "course-schedule-2": return generateCourseSchedule2Steps();
            case "find-eventual-safe-states": return generateFindEventualSafeStatesSteps();
            case "alien-dictionary": throw new LegacyTraceRetiredException(problemId);
            case "shortest-path-undirected": return generateShortestPathUndirectedSteps();
            case "shortest-path-dag": return generateShortestPathDagSteps();
            case "dijkstra-pq-theory": return generateDijkstraPqTheorySteps();
            case "shortest-path-binary-maze": return generateShortestPathBinaryMazeSteps();
            case "path-min-effort": return generatePathMinEffortSteps();
            case "cheapest-flights-k-stops": return generateCheapestFlightsKStopsSteps();
            case "network-delay-time": return generateNetworkDelayTimeSteps();
            case "number-of-ways-destination": return generateNumberOfWaysDestinationSteps();
            case "min-multiplications-reach-end": return generateMinMultiplicationsReachEndSteps();
            case "bellman-ford": throw new LegacyTraceRetiredException(problemId);
            case "floyd-warshall": return generateFloydWarshallSteps();
            case "city-smallest-neighbors": return generateCitySmallestNeighborsSteps();
            case "mst-theory": return generateMstTheorySteps();
            case "prims-mst": return generatePrimsSteps();
            case "disjoint-set-dsu": return generateDisjointSetDsuSteps();
            case "kruskals-mst": return generateKruskalsSteps();
            case "network-connected-ops": return generateNetworkConnectedOpsSteps();
            case "most-stones-removed": return generateMostStonesRemovedSteps();
            case "accounts-merge": return generateAccountsMergeSteps();
            case "number-of-islands-2": return generateNumberOfIslands2Steps();
            case "making-large-island": return generateMakingLargeIslandSteps();
            case "swim-in-rising-water": return generateSwimInRisingWaterSteps();
            case "tarjan-bridges": return generateTarjanBridgesSteps();
            case "articulation-points": return generateArticulationPointsSteps();
            case "kosaraju-scc": throw new LegacyTraceRetiredException(problemId);
            case "bracket-reversals": return generateBracketReversalsSteps();
            case "count-and-say": return generateCountAndSaySteps();
            case "string-hashing-theory": return generateStringHashingTheorySteps();
            case "rabin-karp-algo": return generateRabinKarpSteps();
            // z-function-algo, kmp-lps-algo, shortest-palindrome and
            // longest-happy-prefix have real tracers now (tracer/impl). Their
            // generators are gone; refusing loudly beats serving this service's
            // graph-intro animation under a string algorithm's name.
            case "z-function-algo":
            case "kmp-lps-algo":
            case "shortest-palindrome":
            case "longest-happy-prefix":
                throw new LegacyTraceRetiredException(problemId);
            case "count-palindromic-subsequences": return generateCountPalindromicSubsequencesSteps();
            default: return generateGraphIntroSteps();
        }
    }

    private void initProblems() {
        // 1. Introduction to Graph
        problems.put("graph-intro", new ProblemDetail(
            "graph-intro", "1. Introduction to Graph", "Graphs - Basics", "Advanced Graphs", "Easy",
            "A Graph is a non-linear data structure consisting of Vertices (Nodes) and Edges connecting pairs of vertices.",
            """
            // Graph Definition in Java
            // V = Number of Vertices, E = Number of Edges
            // Graph types: Directed vs Undirected, Weighted vs Unweighted, Cyclic vs Acyclic
            """,
            createBasicGraphNodes(), createBasicGraphEdges(), null, null, null, null, null,
            new ComplexityDetail("O(V + E)", "Time Complexity: Traversal over V vertices and E edges.", "Graph Size", "O(V + E)", "Space Complexity: Storage for adjacency list representation.", "Adjacency List", "Auxiliary Space: O(V + E)", "Storage"), "Graph"
        ));

        // 2. Graph Representation C++
        problems.put("graph-rep-cpp", new ProblemDetail(
            "graph-rep-cpp", "2. Graph Representation | C++", "Graphs - Basics", "Advanced Graphs", "Easy",
            "Graph representation using Adjacency Matrix and Adjacency List in C++.",
            """
            // C++ Adjacency List Representation
            vector<int> adj[V + 1];
            adj[u].push_back(v);
            adj[v].push_back(u); // for undirected graph
            """,
            createBasicGraphNodes(), createBasicGraphEdges(), null, null, null, null, null,
            new ComplexityDetail("O(V + E)", "Space efficient adjacency list representation.", "Adjacency List", "O(V + E)", "Auxiliary space for adjacency list.", "Adjacency List", "Space: O(V + E)", "Memory"), "Graph"
        ));

        // 3. Graph Representation Java
        problems.put("graph-rep-java", new ProblemDetail(
            "graph-rep-java", "3. Graph Representation | Java", "Graphs - Basics", "Advanced Graphs", "Easy",
            "Graph representation using ArrayList of ArrayLists in Java.",
            """
            // Java Adjacency List Representation
            List<List<Integer>> adj = new ArrayList<>();
            for (int i = 0; i < V; i++) adj.add(new ArrayList<>());
            adj.get(u).add(v);
            adj.get(v).add(u);
            """,
            createBasicGraphNodes(), createBasicGraphEdges(), null, null, null, null, null,
            new ComplexityDetail("O(V + E)", "Java ArrayList representation.", "Adjacency List", "O(V + E)", "Auxiliary space for ArrayList structure.", "ArrayList", "Space: O(V + E)", "Memory"), "Graph"
        ));

        // 4. Connected Components
        problems.put("connected-components-intro", new ProblemDetail(
            "connected-components-intro", "4. Connected Components", "Graphs - Basics", "Advanced Graphs", "Easy",
            "A connected component of an undirected graph is a maximal connected subgraph.",
            """
            // Java Connected Components Count
            int components = 0;
            boolean[] vis = new boolean[V];
            for (int i = 0; i < V; i++) {
                if (!vis[i]) {
                    components++;
                    dfs(i, vis, adj);
                }
            }
            """,
            createProvinceNodes(), createProvinceEdges(), null, null, null, null, null,
            new ComplexityDetail("O(V + E)", "Time Complexity: Single pass outer loop visits each vertex once.", "DFS Outer Pass", "O(V)", "Space Complexity: Boolean visited array of size V.", "Visited Array", "Auxiliary Space: O(V)", "Memory"), "Graph"
        ));

        // 5. Traversal Techniques
        problems.put("bfs-dfs-intro", new ProblemDetail(
            "bfs-dfs-intro", "5. Traversal Techniques (BFS vs DFS)", "Graphs - Basics", "Advanced Graphs", "Easy",
            "Breadth First Search (BFS) explores level-by-level using a Queue. Depth First Search (DFS) explores as deep as possible using a Stack/Recursion.",
            """
            // BFS Uses Queue (FIFO), DFS Uses Recursion/Stack (LIFO)
            """,
            createBasicGraphNodes(), createBasicGraphEdges(), null, null, null, null, null,
            new ComplexityDetail("O(V + E)", "Time Complexity: Traverses all V vertices and E edges.", "Traversal", "O(V)", "Space Complexity: Queue/Stack memory.", "Queue/Stack", "Auxiliary Space: O(V)", "Memory"), "Graph"
        ));

        // 6. DFS Traversal
        problems.put("dfs-traversal", new ProblemDetail(
            "dfs-traversal", "6. Depth First Search Traversal", "Graphs - Basics", "Advanced Graphs", "Easy",
            "Traverse an undirected graph using Depth First Search (DFS) recursion.",
            """
            // Java DFS Traversal (Striver A2Z Sheet)
            public void dfs(int node, boolean vis[], ArrayList<ArrayList<Integer>> adj, ArrayList<Integer> ls) {
                vis[node] = true;
                ls.add(node);
                for (int it : adj.get(node)) {
                    if (!vis[it]) dfs(it, vis, adj, ls);
                }
            }
            """,
            createBasicGraphNodes(), createBasicGraphEdges(), null, null, null, null, null,
            new ComplexityDetail("O(V + E)", "Time Complexity: Every vertex and edge processed once.", "DFS Recursion", "O(V)", "Space Complexity: Recursion call stack O(V).", "Call Stack", "Auxiliary Space: O(V)", "Memory"), "Graph"
        ));

        // Populate remaining problems 7 to 62...
        populateGraphProblems7To62();
    }

    private void populateGraphProblems7To62() {
        // 7. Number of Provinces
        problems.put("num-provinces", new ProblemDetail(
            "num-provinces", "7. Number of Provinces", "Graphs - BFS/DFS Problems", "Advanced Graphs", "Medium",
            "Given an N x N matrix isConnected where isConnected[i][j] = 1 if city i and city j are directly connected, return total number of provinces.",
            """
            // Java Number of Provinces (LeetCode 547)
            public int findCircleNum(int[][] isConnected) {
                int n = isConnected.length, provinces = 0;
                boolean[] vis = new boolean[n];
                for (int i = 0; i < n; i++) {
                    if (!vis[i]) { provinces++; dfs(i, isConnected, vis); }
                }
                return provinces;
            }
            """,
            createProvinceNodes(), createProvinceEdges(), null, null, null, null, null,
            new ComplexityDetail("O(V^2)", "Time Complexity: Traverses adjacency matrix of size V x V.", "Matrix Scan", "O(V)", "Space Complexity: Visited array.", "Visited Array", "Auxiliary Space: O(V)", "Memory"), "Graph"
        ));

        // 8. Connected Components in Matrix
        problems.put("connected-matrix", new ProblemDetail(
            "connected-matrix", "8. Connected Components Problem in Matrix", "Graphs - BFS/DFS Problems", "Advanced Graphs", "Medium",
            "Find number of connected components in a 2D matrix grid.",
            """
            // Java Connected Components Matrix
            public int numComponents(int[][] grid) {
                int n = grid.length, m = grid[0].length, cnt = 0;
                boolean[][] vis = new boolean[n][m];
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < m; j++) {
                        if (grid[i][j] == 1 && !vis[i][j]) { cnt++; dfs(i, j, grid, vis); }
                    }
                }
                return cnt;
            }
            """,
            null, null, null, null, null, null, createDefaultMatrix(),
            new ComplexityDetail("O(N * M)", "Time Complexity: Iterates through N x M grid.", "Grid Traversal", "O(N * M)", "Space Complexity: Visited grid matrix.", "Visited Matrix", "Auxiliary Space: O(N * M)", "Memory"), "Matrix"
        ));

        // 9. Rotten Oranges
        problems.put("rotten-oranges", new ProblemDetail(
            "rotten-oranges", "9. Rotten Oranges", "Graphs - BFS/DFS Problems", "Advanced Graphs", "Medium",
            "Given an N x M grid where 0=empty, 1=fresh, 2=rotten, find minimum minutes to rot all fresh oranges using Multi-Source BFS.",
            """
            // Java Rotten Oranges Multi-Source BFS (LeetCode 994)
            public int orangesRotting(int[][] grid) {
                Queue<int[]> q = new LinkedList<>();
                int fresh = 0, time = 0;
                // Add all initial rotten oranges (val 2) to BFS queue...
                return fresh == 0 ? time : -1;
            }
            """,
            null, null, null, null, null, null, createOrangesGrid(),
            new ComplexityDetail("O(N * M)", "Time Complexity: Multi-Source BFS visits each cell at most once.", "Multi-Source BFS", "O(N * M)", "Space Complexity: Queue storage for grid cells.", "Queue Space", "Auxiliary Space: O(N * M)", "Memory"), "Matrix"
        ));

        // 10. Flood Fill
        problems.put("flood-fill", new ProblemDetail(
            "flood-fill", "10. Flood Fill Algorithm", "Graphs - BFS/DFS Problems", "Advanced Graphs", "Easy",
            "Perform flood fill on image grid starting from cell (sr, sc) replacing connected matching pixels with newColor.",
            """
            // Java Flood Fill DFS (LeetCode 733)
            public int[][] floodFill(int[][] image, int sr, int sc, int newColor) {
                int iniColor = image[sr][sc];
                if (iniColor != newColor) dfs(sr, sc, image, iniColor, newColor);
                return image;
            }
            """,
            null, null, null, null, null, null, createDefaultMatrix(),
            new ComplexityDetail("O(N * M)", "Time Complexity: Visits each pixel connected to starting cell.", "Pixel DFS", "O(N * M)", "Space Complexity: Recursion call stack.", "Call Stack", "Auxiliary Space: O(N * M)", "Memory"), "Matrix"
        ));

        // 11 to 62 dynamic definitions
        addProblems11To62();
    }

    private void addProblems11To62() {
        // 11. Cycle Detection Undirected BFS
        problems.put("cycle-undirected-bfs", new ProblemDetail(
            "cycle-undirected-bfs", "11. Cycle Detection in Undirected Graph (BFS)", "Graphs - BFS/DFS Problems", "Advanced Graphs", "Medium",
            "Detect cycle in an undirected graph using Breadth First Search (BFS) tracking (node, parent) pairs in queue.",
            """
            // Java Undirected Cycle Detection BFS (Striver A2Z)
            public boolean checkForCycle(int src, int V, ArrayList<ArrayList<Integer>> adj, boolean vis[]) {
                Queue<int[]> q = new LinkedList<>();
                q.add(new int[]{src, -1});
                vis[src] = true;
                while (!q.isEmpty()) {
                    int node = q.peek()[0], parent = q.peek()[1]; q.poll();
                    for (int adjacentNode : adj.get(node)) {
                        if (!vis[adjacentNode]) {
                            vis[adjacentNode] = true; q.add(new int[]{adjacentNode, node});
                        } else if (parent != adjacentNode) return true; // Cycle detected!
                    }
                }
                return false;
            }
            """,
            createCyclicGraphNodes(), createCyclicGraphEdges(), null, null, null, null, null,
            new ComplexityDetail("O(V + E)", "Time Complexity: BFS visits each node and edge.", "BFS Traversal", "O(V)", "Space Complexity: Queue and visited array.", "Queue & Visited", "Auxiliary Space: O(V)", "Memory"), "Graph"
        ));

        // 12. Cycle Detection Undirected DFS
        problems.put("cycle-undirected-dfs", new ProblemDetail(
            "cycle-undirected-dfs", "12. Cycle Detection in Undirected Graph (DFS)", "Graphs - BFS/DFS Problems", "Advanced Graphs", "Medium",
            "Detect cycle in an undirected graph using Depth First Search (DFS) tracking parent node.",
            """
            // Java Undirected Cycle Detection DFS
            public boolean dfs(int node, int parent, boolean vis[], ArrayList<ArrayList<Integer>> adj) {
                vis[node] = true;
                for (int adjacentNode : adj.get(node)) {
                    if (!vis[adjacentNode]) {
                        if (dfs(adjacentNode, node, vis, adj)) return true;
                    } else if (adjacentNode != parent) return true;
                }
                return false;
            }
            """,
            createCyclicGraphNodes(), createCyclicGraphEdges(), null, null, null, null, null,
            new ComplexityDetail("O(V + E)", "Time Complexity: DFS traversal.", "DFS Traversal", "O(V)", "Space Complexity: Visited array and recursion call stack.", "Call Stack", "Auxiliary Space: O(V)", "Memory"), "Graph"
        ));

        // 13 to 62 bulk registrations
        addProblems13To62Bulk();
    }

    private void addProblems13To62Bulk() {
        String[][] list = new String[][]{
            {"nearest-cell-1", "13. Distance of Nearest Cell Having 1", "Graphs - BFS/DFS Problems", "Medium", "0/1 Matrix BFS shortest distance to nearest 1."},
            {"surrounded-regions", "14. Surrounded Regions", "Graphs - BFS/DFS Problems", "Medium", "Replace 'O' with 'X' if completely surrounded by 'X' using Boundary DFS."},
            {"number-of-enclaves", "15. Number of Enclaves", "Graphs - BFS/DFS Problems", "Medium", "Count land cells from which cannot walk off boundary."},
            {"word-ladder-1", "16. Word Ladder I", "Graphs - BFS/DFS Problems", "Hard", "Shortest transformation sequence from startWord to targetWord using BFS."},
            {"word-ladder-2", "17. Word Ladder II", "Graphs - BFS/DFS Problems", "Hard", "Find all shortest transformation sequences using BFS + Backtracking."},
            {"number-of-islands", "18. Number of Islands", "Graphs - BFS/DFS Problems", "Medium", "Count connected land components (1s) in 2D grid."},
            {"bipartite-graph-dfs", "19. Bipartite Graph (DFS)", "Graphs - BFS/DFS Problems", "Medium", "Check if graph is bipartite using 2-Coloring DFS."},
            {"cycle-directed-dfs", "20. Cycle Detection in Directed Graph (DFS)", "Graphs - BFS/DFS Problems", "Medium", "Detect cycle in directed graph using DFS path visited array."},
            {"topo-sort-dfs", "21. Topo Sort (DFS)", "Graphs - Topo Sort", "Medium", "Linear ordering of DAG vertices using DFS stack."},
            {"kahn-algo-bfs", "22. Topological Sort (Kahn's BFS)", "Graphs - Topo Sort", "Medium", "Kahn's BFS Indegree Topological Sort algorithm."},
            {"cycle-directed-bfs", "23. Cycle Detection in Directed Graph (Kahn's BFS)", "Graphs - Topo Sort", "Medium", "If topo sort contains < V vertices, directed cycle exists!"},
            {"course-schedule-1", "24. Course Schedule I", "Graphs - Topo Sort", "Medium", "Check if possible to finish all courses using Cycle Detection."},
            {"course-schedule-2", "25. Course Schedule II", "Graphs - Topo Sort", "Medium", "Return course ordering using Topological Sort."},
            {"find-eventual-safe-states", "26. Find Eventual Safe States", "Graphs - Topo Sort", "Medium", "Find safe nodes using terminal reverse edge Topo Sort."},
            {"alien-dictionary", "27. Alien Dictionary", "Graphs - Topo Sort", "Hard", "Order characters of alien language using Character Dependency Topo Sort."},
            {"shortest-path-undirected", "28. Shortest Path in Undirected Graph", "Graphs - Shortest Path", "Medium", "Unit weight shortest path using Queue BFS."},
            {"shortest-path-dag", "29. Shortest Path in DAG", "Graphs - Shortest Path", "Medium", "Shortest path in DAG using Topo Sort edge relaxation."},
            {"dijkstra-min-heap", "30. Dijkstra's Algorithm", "Graphs - Shortest Path", "Medium", "Single-source shortest path using Min-Heap / PriorityQueue."},
            {"dijkstra-pq-theory", "31. Why Priority Queue in Dijkstra?", "Graphs - Shortest Path", "Easy", "Priority Queue greedily selects minimum distance vertex first."},
            {"shortest-path-binary-maze", "32. Shortest Distance in Binary Maze", "Graphs - Shortest Path", "Medium", "BFS shortest distance in 2D binary matrix grid."},
            {"path-min-effort", "33. Path with Minimum Effort", "Graphs - Shortest Path", "Medium", "Find path with min-max height difference effort using Dijkstra."},
            {"cheapest-flights-k-stops", "34. Cheapest Flights Within K Stops", "Graphs - Shortest Path", "Medium", "Cheapest price with at most K stops using BFS / Bellman-Ford."},
            {"network-delay-time", "35. Network Delay Time", "Graphs - Shortest Path", "Medium", "Signal travel time over directed network using Dijkstra."},
            {"number-of-ways-destination", "36. Number of Ways to Destination", "Graphs - Shortest Path", "Medium", "Count shortest paths using Dijkstra path array."},
            {"min-multiplications-reach-end", "37. Min Multiplications to Reach End", "Graphs - Shortest Path", "Medium", "BFS modular arithmetic shortest multiplication steps."},
            {"bellman-ford", "38. Bellman Ford Algorithm", "Graphs - Shortest Path", "Medium", "Shortest path algorithm handling negative weight edges & cycles."},
            {"floyd-warshall", "39. Floyd Warshall Algorithm", "Graphs - Shortest Path", "Medium", "All-Pairs Shortest Path dynamic programming algorithm O(V^3)."},
            {"city-smallest-neighbors", "40. City With Smallest Neighbors", "Graphs - Shortest Path", "Medium", "City with smallest reachable neighbors at threshold distance using Floyd-Warshall."},
            {"mst-theory", "41. Minimum Spanning Tree Theory", "Graphs - MST & DSU", "Easy", "MST connects all vertices with minimum total edge weight."},
            {"prims-mst", "42. Prim's Algorithm for MST", "Graphs - MST & DSU", "Medium", "Greedy MST algorithm using Min-Heap."},
            {"disjoint-set-dsu", "43. Disjoint Set (DSU Implementation)", "Graphs - MST & DSU", "Medium", "DSU with Path Compression and Union by Rank / Size."},
            {"kruskals-mst", "44. Find MST Weight (Kruskal's)", "Graphs - MST & DSU", "Medium", "Sort edges + DSU union for Minimum Spanning Tree weight."},
            {"network-connected-ops", "45. Operations to Make Network Connected", "Graphs - MST & DSU", "Medium", "Min cables to connect all computers using DSU components."},
            {"most-stones-removed", "46. Most Stones Removed", "Graphs - MST & DSU", "Medium", "Max stones removed in same row or col using DSU component counting."},
            {"accounts-merge", "47. Accounts Merge", "Graphs - MST & DSU", "Medium", "Merge accounts sharing common email addresses using DSU."},
            {"number-of-islands-2", "48. Number of Islands II", "Graphs - MST & DSU", "Hard", "Dynamic island count updates over 2D grid using DSU."},
            {"making-large-island", "49. Making A Large Island", "Graphs - MST & DSU", "Hard", "Change at most one 0 to 1 to maximize island area using DSU."},
            {"swim-in-rising-water", "50. Swim in Rising Water", "Graphs - MST & DSU", "Hard", "Min time to swim from top-left to bottom-right using Dijkstra / DSU."},
            {"tarjan-bridges", "51. Bridges in Graph (Tarjan's)", "Graphs - Hard", "Hard", "Find critical connections (bridges) using Insertion & Low time arrays."},
            {"articulation-points", "52. Articulation Points in Graph", "Graphs - Hard", "Hard", "Find cut vertices whose removal increases connected components."},
            {"kosaraju-scc", "53. Kosaraju's Algorithm (SCC)", "Graphs - Hard", "Hard", "Find Strongly Connected Components using Topo order + Transpose graph DFS."},
            {"bracket-reversals", "54. Min Bracket Reversals", "Strings - Algorithms", "Medium", "Min reversals to balance string of curly brackets '{' and '}'."},
            {"count-and-say", "55. Count and Say", "Strings - Algorithms", "Medium", "Generate nth term of Count and Say look-and-say sequence."},
            {"string-hashing-theory", "56. String Hashing Theory", "Strings - Algorithms", "Easy", "Polynomial rolling hash technique for O(1) substring equality testing."},
            {"rabin-karp-algo", "57. Rabin Karp Algorithm", "Strings - Algorithms", "Medium", "Pattern searching algorithm using rolling hash matching."},
            {"z-function-algo", "58. Z Function Algorithm", "Strings - Algorithms", "Medium", "Z-array algorithm matching pattern prefixes in linear O(N) time."},
            {"kmp-lps-algo", "59. KMP Algorithm / LPS Array", "Strings - Algorithms", "Medium", "Knuth-Morris-Pratt pattern matching using Longest Prefix Suffix array."},
            {"shortest-palindrome", "60. Shortest Palindrome", "Strings - Algorithms", "Hard", "Convert string to shortest palindrome by adding characters in front using KMP LPS."},
            {"longest-happy-prefix", "61. Longest Happy Prefix", "Strings - Algorithms", "Hard", "Find longest prefix that is also a non-overlapping suffix using LPS array."},
            {"count-palindromic-subsequences", "62. Count Palindromic Subsequences", "Strings - Algorithms", "Hard", "Count total palindromic subsequences using 2D DP matrix."}
        };

        for (String[] p : list) {
            String id = p[0];
            String title = p[1];
            String cat = p[2];
            String diff = p[3];
            String desc = p[4];

            problems.put(id, new ProblemDetail(
                id, title, cat, cat.startsWith("Strings") ? "Strings" : "Advanced Graphs", diff, desc,
                String.format("// Java Implementation for %s\npublic void solve() {\n    // Striver A2Z Sheet Implementation\n}", title),
                createBasicGraphNodes(), createBasicGraphEdges(), null, null, null, null, null,
                new ComplexityDetail("O(V + E)", "Time Complexity: Standard optimal graph traversal.", "Graph Algorithm", "O(V + E)", "Space Complexity: Visited arrays and recursion call stack.", "Memory", "Auxiliary Space: O(V)", "Memory"),
                bulkDsType(id, cat).wireValue()
            ));
        }
    }

    private static DsType bulkDsType(String id, String category) {
        if (category.startsWith("Strings")) {
            return DsType.STRING;
        }
        return switch (id) {
            case "disjoint-set-dsu" -> DsType.DSU;
            case "number-of-islands" -> DsType.MATRIX;
            default -> DsType.GRAPH;
        };
    }

    // Step Generators
    private List<ExecutionStep> generateGraphIntroSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> nodeStates = Map.of(0, "visited", 1, "visiting", 2, "unvisited", 3, "unvisited");
        List<String> activeEdges = List.of("0-1", "0-2");

        steps.add(new ExecutionStep(1, 1, "Graph Intro: A Graph consists of V vertices and E edges.", List.of(), nodeStates, activeEdges, Map.of("V", "4", "E", "3"), "Graph", null));
        steps.add(new ExecutionStep(2, 4, "Graph Intro Complete! All nodes initialized.", List.of(), nodeStates, activeEdges, Map.of("Status", "INITIALIZED"), "Graph", null));
        return steps;
    }

    private List<ExecutionStep> generateGraphRepCppSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateGraphRepJavaSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateConnectedComponentsIntroSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateBfsDfsIntroSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateNumProvincesSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateConnectedMatrixSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateRottenOrangesSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateFloodFillSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateCycleUndirectedBfsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateCycleUndirectedDfsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateNearestCell1Steps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateSurroundedRegionsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateNumberOfEnclavesSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateWordLadder2Steps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateNumberOfIslandsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateBipartiteGraphDfsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateCycleDirectedDfsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateTopoSortDfsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateKahnAlgoSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateCycleDirectedBfsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateCourseSchedule1Steps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateCourseSchedule2Steps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateFindEventualSafeStatesSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateShortestPathUndirectedSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateShortestPathDagSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateDijkstraPqTheorySteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateShortestPathBinaryMazeSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generatePathMinEffortSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateCheapestFlightsKStopsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateNetworkDelayTimeSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateNumberOfWaysDestinationSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateMinMultiplicationsReachEndSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateFloydWarshallSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateCitySmallestNeighborsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateMstTheorySteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generatePrimsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateDisjointSetDsuSteps() {
        List<ExecutionStep> steps = new ArrayList<>();

        steps.add(new ExecutionStep(
            1, 4,
            "Initialize Disjoint Set (DSU) with N=7 elements: parent = [0, 1, 2, 3, 4, 5, 6, 7], rank = [0, 0, 0, 0, 0, 0, 0, 0]. Each element is its own root.",
            List.of(), Map.of(), List.of(),
            Map.of("parent[]", "[0, 1, 2, 3, 4, 5, 6, 7]", "rank[]", "[0, 0, 0, 0, 0, 0, 0, 0]", "Disjoint Sets", "{1}, {2}, {3}, {4}, {5}, {6}, {7}", "Operation", "Initialize DSU(7)"),
            "Graph", null
        ));

        steps.add(new ExecutionStep(
            2, 12,
            "Operation: union(1, 2). find(1)=1, find(2)=2. Ranks equal (0==0). Attach 2 to 1 (parent[2]=1, rank[1]=1).",
            List.of("1", "2"), Map.of(1, "visiting", 2, "visited"), List.of("1-2"),
            Map.of("parent[]", "[0, 1, 1, 3, 4, 5, 6, 7]", "rank[]", "[0, 1, 0, 0, 0, 0, 0, 0]", "Disjoint Sets", "{1, 2}, {3}, {4}, {5}, {6}, {7}", "Operation", "union(1, 2)"),
            "Graph", null
        ));

        steps.add(new ExecutionStep(
            3, 18,
            "Operation: union(2, 3). find(2)=1, find(3)=3. rank[1] (1) > rank[3] (0). Attach 3 under 1 (parent[3]=1).",
            List.of("1", "2", "3"), Map.of(1, "visiting", 2, "visited", 3, "visited"), List.of("1-2", "1-3"),
            Map.of("parent[]", "[0, 1, 1, 1, 4, 5, 6, 7]", "rank[]", "[0, 1, 0, 0, 0, 0, 0, 0]", "Disjoint Sets", "{1, 2, 3}, {4}, {5}, {6}, {7}", "Operation", "union(2, 3)"),
            "Graph", null
        ));

        steps.add(new ExecutionStep(
            4, 24,
            "Operation: union(4, 5). find(4)=4, find(5)=5. Ranks equal (0==0). Attach 5 under 4 (parent[5]=4, rank[4]=1).",
            List.of("4", "5"), Map.of(4, "visiting", 5, "visited"), List.of("4-5"),
            Map.of("parent[]", "[0, 1, 1, 1, 4, 4, 6, 7]", "rank[]", "[0, 1, 0, 0, 1, 0, 0, 0]", "Disjoint Sets", "{1, 2, 3}, {4, 5}, {6}, {7}", "Operation", "union(4, 5)"),
            "Graph", null
        ));

        steps.add(new ExecutionStep(
            5, 30,
            "Operation: union(6, 7). find(6)=6, find(7)=7. Ranks equal (0==0). Attach 7 under 6 (parent[7]=6, rank[6]=1).",
            List.of("6", "7"), Map.of(6, "visiting", 7, "visited"), List.of("6-7"),
            Map.of("parent[]", "[0, 1, 1, 1, 4, 4, 6, 6]", "rank[]", "[0, 1, 0, 0, 1, 0, 1, 0]", "Disjoint Sets", "{1, 2, 3}, {4, 5}, {6, 7}", "Operation", "union(6, 7)"),
            "Graph", null
        ));

        steps.add(new ExecutionStep(
            6, 36,
            "Operation: union(5, 6). find(5)=4 (rank 1), find(6)=6 (rank 1). Equal ranks! Attach 6 under 4 (parent[6]=4, rank[4]=2).",
            List.of("4", "5", "6", "7"), Map.of(4, "visiting", 5, "visited", 6, "visited", 7, "visited"), List.of("4-5", "4-6", "6-7"),
            Map.of("parent[]", "[0, 1, 1, 1, 4, 4, 4, 6]", "rank[]", "[0, 1, 0, 0, 2, 0, 1, 0]", "Disjoint Sets", "{1, 2, 3}, {4, 5, 6, 7}", "Operation", "union(5, 6)"),
            "Graph", null
        ));

        steps.add(new ExecutionStep(
            7, 42,
            "Operation: find(7) with Path Compression! Node 7 parent is 6, 6 parent is 4. Path compression updates parent[7] = 4 directly!",
            List.of("4", "6", "7"), Map.of(4, "visiting", 6, "visited", 7, "visited"), List.of("4-7"),
            Map.of("parent[]", "[0, 1, 1, 1, 4, 4, 4, 4]", "rank[]", "[0, 1, 0, 0, 2, 0, 1, 0]", "Disjoint Sets", "{1, 2, 3}, {4, 5, 6, 7}", "Operation", "find(7) Path Compression"),
            "Graph", null
        ));

        steps.add(new ExecutionStep(
            8, 50,
            "Operation: union(3, 7). find(3)=1 (rank 1), find(7)=4 (rank 2). rank[4] > rank[1] -> Attach root 1 under root 4 (parent[1]=4).",
            List.of("1", "2", "3", "4", "5", "6", "7"), Map.of(4, "visiting", 1, "visited", 2, "visited", 3, "visited", 5, "visited", 6, "visited", 7, "visited"), List.of("4-1", "1-2", "1-3", "4-5", "4-6", "4-7"),
            Map.of("parent[]", "[0, 4, 1, 1, 4, 4, 4, 4]", "rank[]", "[0, 1, 0, 0, 2, 0, 1, 0]", "Disjoint Sets", "{1, 2, 3, 4, 5, 6, 7} (Single Component!)", "Operation", "union(3, 7) Complete"),
            "Graph", null
        ));

        return steps;
    }
    private List<ExecutionStep> generateKruskalsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateNetworkConnectedOpsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateMostStonesRemovedSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateAccountsMergeSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateNumberOfIslands2Steps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateMakingLargeIslandSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateSwimInRisingWaterSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateTarjanBridgesSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateArticulationPointsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateBracketReversalsSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateCountAndSaySteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateStringHashingTheorySteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateRabinKarpSteps() { return generateGraphIntroSteps(); }
    private List<ExecutionStep> generateCountPalindromicSubsequencesSteps() { return generateGraphIntroSteps(); }

    // Graph helper builders
    private List<GraphNode> createBasicGraphNodes() {
        return List.of(
            new GraphNode(0, "0", 100, 80, "unvisited"),
            new GraphNode(1, "1", 220, 80, "unvisited"),
            new GraphNode(2, "2", 100, 200, "unvisited"),
            new GraphNode(3, "3", 220, 200, "unvisited")
        );
    }

    private List<GraphEdge> createBasicGraphEdges() {
        return List.of(
            new GraphEdge(0, 1, false),
            new GraphEdge(0, 2, false),
            new GraphEdge(1, 3, false)
        );
    }

    private List<GraphNode> createProvinceNodes() {
        return List.of(
            new GraphNode(0, "0", 100, 100, "unvisited"),
            new GraphNode(1, "1", 200, 100, "unvisited"),
            new GraphNode(2, "2", 100, 200, "unvisited"),
            new GraphNode(3, "3", 200, 200, "unvisited")
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
            new GraphEdge(1, 2, false),
            new GraphEdge(1, 3, false)
        );
    }

    private int[][] createDefaultMatrix() {
        return new int[][]{
            {1, 1, 0},
            {1, 1, 0},
            {0, 0, 1}
        };
    }

    private int[][] createOrangesGrid() {
        return new int[][]{
            {2, 1, 1},
            {1, 1, 0},
            {0, 1, 1}
        };
    }
}
