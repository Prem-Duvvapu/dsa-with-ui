package com.dsa.ui.service;

import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdvancedGraphService {

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
            case "topo-sort-dfs": return generateTopoSortDfsSteps();
            case "kahn-algo-bfs": return generateKahnAlgoSteps();
            case "shortest-path-undirected": return generateShortestPathUndirectedSteps();
            case "dijkstra-min-heap": return generateDijkstraSteps();
            case "bellman-ford": return generateBellmanFordSteps();
            case "floyd-warshall": return generateFloydWarshallSteps();
            case "prims-mst": return generatePrimsSteps();
            case "kruskals-mst": return generateKruskalsSteps();
            case "kosaraju-scc": return generateKosarajuSteps();
            case "tarjan-bridges": return generateTarjanBridgesSteps();
            default: return generateTopoSortDfsSteps();
        }
    }

    private void initProblems() {
        // 1. Topological Sort (DFS)
        problems.put("topo-sort-dfs", new ProblemDetail(
            "topo-sort-dfs", "Topological Sort (DFS)", "Graphs - Topo Sort", "Advanced Graphs", "Medium",
            "Given a Directed Acyclic Graph (DAG) with V vertices and E edges, return a linear ordering of vertices such that for every directed edge u -> v, u appears before v.",
            """
            // Java Topological Sort DFS (Striver A2Z Sheet)
            public int[] topoSort(int V, ArrayList<ArrayList<Integer>> adj) {
                boolean vis[] = new boolean[V];
                Stack<Integer> st = new Stack<>();

                for (int i = 0; i < V; i++) {
                    if (!vis[i]) {
                        findTopoSort(i, vis, st, adj);
                    }
                }

                int topo[] = new int[V];
                int i = 0;
                while (!st.isEmpty()) {
                    topo[i++] = st.pop();
                }
                return topo;
            }

            private void findTopoSort(int node, boolean vis[], Stack<Integer> st, ArrayList<ArrayList<Integer>> adj) {
                vis[node] = true;
                for (int it : adj.get(node)) {
                    if (!vis[it]) {
                        findTopoSort(it, vis, st, adj);
                    }
                }
                st.push(node); // Push node to stack AFTER visiting all adjacent nodes!
            }
            """,
            createDagNodes(), createDagEdges(), null, null,
            new ComplexityDetail(
                "O(V + E)",
                "Time Complexity: DFS visit runs once for every vertex O(V) and traverses each directed edge once O(E).",
                "Why O(V + E)? Every vertex is pushed to stack exactly once upon function call finish.",
                "O(V)",
                "Space Complexity: Visited array O(V), Auxiliary Recursion Call Stack O(V), and Output Stack O(V).",
                "Why O(V)? Output stack stores all V vertices.",
                "Auxiliary Space: O(V)",
                "Adjacency List Space: O(V + E)"
            ),
            "Stack"
        ));

        // 2. Kahn's Algorithm (BFS Topo Sort)
        problems.put("kahn-algo-bfs", new ProblemDetail(
            "kahn-algo-bfs", "Kahn's Algorithm (BFS Topo Sort)", "Graphs - Topo Sort", "Advanced Graphs", "Medium",
            "Topological Sorting using Indegree Array & BFS Queue.",
            """
            // Java Kahn's Algorithm (Striver A2Z Sheet)
            public int[] topoSortKahn(int V, ArrayList<ArrayList<Integer>> adj) {
                int indegree[] = new int[V];
                for (int i = 0; i < V; i++) {
                    for (int it : adj.get(i)) indegree[it]++;
                }

                Queue<Integer> q = new LinkedList<>();
                for (int i = 0; i < V; i++) {
                    if (indegree[i] == 0) q.add(i);
                }

                int topo[] = new int[V];
                int i = 0;
                while (!q.isEmpty()) {
                    int node = q.poll();
                    topo[i++] = node;

                    for (int it : adj.get(node)) {
                        indegree[it]--;
                        if (indegree[it] == 0) q.add(it);
                    }
                }
                return topo;
            }
            """,
            createDagNodes(), createDagEdges(), null, null,
            new ComplexityDetail(
                "O(V + E)",
                "Time Complexity: Indegree calculation takes O(V + E). Queue processes nodes with indegree 0.",
                "Why Kahn's works? A node with indegree 0 has no prerequisite incoming edges, so it can safely be placed first in topological order.",
                "O(V)",
                "Space Complexity: Indegree array O(V) + Queue memory O(V).",
                "Why O(V)? Bounded by number of graph vertices V.",
                "Auxiliary Space: O(V)",
                "Adjacency List Space: O(V + E)"
            ),
            "Queue"
        ));

        // 3. Shortest Path in Undirected Graph
        problems.put("shortest-path-undirected", new ProblemDetail(
            "shortest-path-undirected", "Shortest Path in Undirected Graph (Unit Weights)", "Graphs - Shortest Path", "Advanced Graphs", "Medium",
            "Find shortest path distance from source node 0 to all vertices in an unweighted undirected graph using BFS.",
            """
            // Java Shortest Path BFS (Striver A2Z Sheet)
            public int[] shortestPath(int[][] edges, int N, int M, int src) {
                ArrayList<ArrayList<Integer>> adj = new ArrayList<>();
                for (int i = 0; i < N; i++) adj.add(new ArrayList<>());
                for (int i = 0; i < M; i++) {
                    adj.get(edges[i][0]).add(edges[i][1]);
                    adj.get(edges[i][1]).add(edges[i][0]);
                }

                int dist[] = new int[N];
                Arrays.fill(dist, (int) 1e9);
                dist[src] = 0;

                Queue<Integer> q = new LinkedList<>();
                q.add(src);

                while (!q.isEmpty()) {
                    int node = q.poll();
                    for (int it : adj.get(node)) {
                        if (dist[node] + 1 < dist[it]) {
                            dist[it] = dist[node] + 1;
                            q.add(it);
                        }
                    }
                }
                return dist;
            }
            """,
            createShortestPathNodes(), createShortestPathEdges(), null, null,
            new ComplexityDetail(
                "O(V + 2E)",
                "Time Complexity: Level-by-level BFS queue traversal.",
                "Why BFS yields shortest path for unit weights? First time BFS reaches any node is guaranteed to be via minimum edges.",
                "O(V)",
                "Space Complexity: Distance array dist[] of size O(V) + Queue O(V).",
                "Why O(V)? Max queue capacity is V.",
                "Auxiliary Space: O(V)",
                "Adjacency List Space: O(V + 2E)"
            ),
            "Queue"
        ));

        // 4. Dijkstra's Algorithm
        problems.put("dijkstra-min-heap", new ProblemDetail(
            "dijkstra-min-heap", "Dijkstra's Shortest Path Algorithm", "Graphs - Shortest Path", "Advanced Graphs", "Medium",
            "Find shortest path distance from source vertex to all vertices in a weighted graph using PriorityQueue (Min-Heap).",
            """
            // Java Dijkstra Implementation (LeetCode / Striver A2Z)
            class Pair {
                int distance, node;
                Pair(int distance, int node) { this.distance = distance; this.node = node; }
            }

            public int[] dijkstra(int V, ArrayList<ArrayList<ArrayList<Integer>>> adj, int S) {
                PriorityQueue<Pair> pq = new PriorityQueue<>((x, y) -> x.distance - y.distance);
                int dist[] = new int[V];
                Arrays.fill(dist, (int) 1e9);

                dist[S] = 0;
                pq.add(new Pair(0, S));

                while (!pq.isEmpty()) {
                    int dis = pq.peek().distance;
                    int node = pq.peek().node;
                    pq.poll();

                    for (ArrayList<Integer> it : adj.get(node)) {
                        int adjNode = it.get(0);
                        int edgeWeight = it.get(1);

                        if (dis + edgeWeight < dist[adjNode]) {
                            dist[adjNode] = dis + edgeWeight;
                            pq.add(new Pair(dist[adjNode], adjNode));
                        }
                    }
                }
                return dist;
            }
            """,
            createWeightedNodes(), createWeightedEdges(), null, null,
            new ComplexityDetail(
                "O(E log V)",
                "Time Complexity: Every vertex can be pushed to Min-Heap up to its degree times. Total heap insertions = E, each taking O(log V).",
                "Why Min-Heap vs Array? Min-Heap retrieves min distance vertex in O(log V) instead of O(V) array scan, improving overall runtime from O(V^2) to O(E log V).",
                "O(V + E)",
                "Space Complexity: PriorityQueue stores pairs up to O(E) elements + dist[] array O(V).",
                "Why PriorityQueue can store E elements? Multiple path updates for same vertex can coexist in queue before stale entries poll.",
                "Auxiliary Space: O(V + E)",
                "Adjacency List Space: O(V + E)"
            ),
            "PriorityQueue"
        ));

        // 5. Bellman-Ford Algorithm
        problems.put("bellman-ford", new ProblemDetail(
            "bellman-ford", "Bellman-Ford Algorithm (Negative Cycles)", "Graphs - Shortest Path", "Advanced Graphs", "Medium",
            "Shortest path algorithm handling negative edge weights and detecting negative weight cycles.",
            """
            // Java Bellman-Ford (Striver A2Z Sheet)
            public int[] bellmanFord(int V, ArrayList<ArrayList<Integer>> edges, int S) {
                int dist[] = new int[V];
                Arrays.fill(dist, (int) 1e8);
                dist[S] = 0;

                // Relax all edges V - 1 times
                for (int i = 0; i < V - 1; i++) {
                    for (ArrayList<Integer> it : edges) {
                        int u = it.get(0), v = it.get(1), wt = it.get(2);
                        if (dist[u] != 1e8 && dist[u] + wt < dist[v]) {
                            dist[v] = dist[u] + wt;
                        }
                    }
                }

                // Vth relaxation check for negative cycle
                for (ArrayList<Integer> it : edges) {
                    int u = it.get(0), v = it.get(1), wt = it.get(2);
                    if (dist[u] != 1e8 && dist[u] + wt < dist[v]) {
                        return new int[]{-1}; // Negative cycle detected!
                    }
                }
                return dist;
            }
            """,
            createWeightedNodes(), createWeightedEdges(), null, null,
            new ComplexityDetail(
                "O(V x E)",
                "Time Complexity: We relax all E edges V-1 times = (V-1) * E operations. One additional pass checks for negative cycles.",
                "Why V-1 iterations? In a graph with V vertices, the longest simple path contains at most V-1 edges.",
                "O(V)",
                "Space Complexity: Distance array dist[] of size V.",
                "Why O(V)? Only distance array is maintained.",
                "Auxiliary Space: O(V)",
                "Edge List Space: O(E)"
            ),
            "Queue"
        ));

        // 6. Floyd-Warshall Algorithm
        problems.put("floyd-warshall", new ProblemDetail(
            "floyd-warshall", "Floyd-Warshall All-Pairs Shortest Path", "Graphs - Shortest Path", "Advanced Graphs", "Medium",
            "Find shortest distances between every pair of vertices in a weighted directed graph using DP Matrix.",
            """
            // Java Floyd-Warshall (Striver A2Z Sheet)
            public void floydWarshall(int[][] matrix) {
                int n = matrix.length;
                for (int k = 0; k < n; k++) {
                    for (int i = 0; i < n; i++) {
                        for (int j = 0; j < n; j++) {
                            if (matrix[i][k] != -1 && matrix[k][j] != -1) {
                                int dist = matrix[i][k] + matrix[k][j];
                                if (matrix[i][j] == -1 || dist < matrix[i][j]) {
                                    matrix[i][j] = dist;
                                }
                            }
                        }
                    }
                }
            }
            """,
            createProvinceNodes(), createProvinceEdges(), null, null,
            new ComplexityDetail(
                "O(V^3)",
                "Time Complexity: 3 nested loops from k=0 to V, i=0 to V, j=0 to V.",
                "Why k is outer loop? Node k acts as intermediate waypoint. Matrix[i][j] updates if path via k is shorter.",
                "O(1)",
                "Space Complexity: Updates input adjacency matrix in-place.",
                "Why O(1) auxiliary? Uses original N x N input matrix.",
                "Auxiliary Space: O(1)",
                "Matrix Input Space: O(V^2)"
            ),
            "Matrix"
        ));

        // 7. Prim's MST Algorithm
        problems.put("prims-mst", new ProblemDetail(
            "prims-mst", "Prim's Minimum Spanning Tree (MST)", "Graphs - MST & Disjoint Set", "Advanced Graphs", "Medium",
            "Find Minimum Spanning Tree weight sum using Greedy Min-Heap approach.",
            """
            // Java Prim's MST (Striver A2Z Sheet)
            class Pair {
                int node, distance;
                Pair(int node, int distance) { this.node = node; this.distance = distance; }
            }

            public int spanningTree(int V, ArrayList<ArrayList<ArrayList<Integer>>> adj) {
                PriorityQueue<Pair> pq = new PriorityQueue<>((x, y) -> x.distance - y.distance);
                boolean[] vis = new boolean[V];
                pq.add(new Pair(0, 0));
                int sum = 0;

                while (!pq.isEmpty()) {
                    int wt = pq.peek().distance;
                    int node = pq.peek().node;
                    pq.poll();

                    if (vis[node]) continue;
                    vis[node] = true;
                    sum += wt;

                    for (ArrayList<Integer> it : adj.get(node)) {
                        int adjNode = it.get(0);
                        int edW = it.get(1);
                        if (!vis[adjNode]) {
                            pq.add(new Pair(adjNode, edW));
                        }
                    }
                }
                return sum;
            }
            """,
            createWeightedNodes(), createWeightedEdges(), null, null,
            new ComplexityDetail(
                "O(E log V)",
                "Time Complexity: Priority queue stores up to E edge pairs. Heap push/pop takes O(log V).",
                "Why Prim's is Greedy? Always expands MST by choosing the smallest weight edge connecting an unvisited vertex.",
                "O(V + E)",
                "Space Complexity: Visited array O(V) + Min-Heap O(E).",
                "Why O(V+E)? Min-Heap contains edge pairs.",
                "Auxiliary Space: O(V + E)",
                "Adjacency List Space: O(V + E)"
            ),
            "PriorityQueue"
        ));

        // 8. Kruskal's MST Algorithm
        problems.put("kruskals-mst", new ProblemDetail(
            "kruskals-mst", "Kruskal's MST (Disjoint Set / Union-Find)", "Graphs - MST & Disjoint Set", "Advanced Graphs", "Medium",
            "Sort all graph edges by weight and pick non-cyclic edges using Disjoint Set (Union-Find).",
            """
            // Java Kruskal's MST with Disjoint Set (Striver A2Z Sheet)
            class DisjointSet {
                int[] parent, rank;
                DisjointSet(int n) {
                    parent = new int[n + 1]; rank = new int[n + 1];
                    for (int i = 0; i <= n; i++) parent[i] = i;
                }
                int findUPar(int node) {
                    if (node == parent[node]) return node;
                    return parent[node] = findUPar(parent[node]); // Path compression!
                }
                void unionByRank(int u, int v) {
                    int ulp_u = findUPar(u), ulp_v = findUPar(v);
                    if (ulp_u == ulp_v) return;
                    if (rank[ulp_u] < rank[ulp_v]) parent[ulp_u] = ulp_v;
                    else if (rank[ulp_v] < rank[ulp_u]) parent[ulp_v] = ulp_u;
                    else { parent[ulp_v] = ulp_u; rank[ulp_u]++; }
                }
            }

            public int kruskalMST(int V, List<int[]> edges) {
                edges.sort((a, b) -> a[2] - b[2]); // Sort by edge weight
                DisjointSet ds = new DisjointSet(V);
                int mstWeight = 0;

                for (int[] edge : edges) {
                    int u = edge[0], v = edge[1], wt = edge[2];
                    if (ds.findUPar(u) != ds.findUPar(v)) {
                        mstWeight += wt;
                        ds.unionByRank(u, v);
                    }
                }
                return mstWeight;
            }
            """,
            createWeightedNodes(), createWeightedEdges(), null, null,
            new ComplexityDetail(
                "O(E log E)",
                "Time Complexity: Sorting E edges takes O(E log E). Union-Find operations take near-constant O(4 alpha) amortized time.",
                "Why Disjoint Set Path Compression is fast? Collapses tree depth so find parent operation takes amortized O(4 alpha) = O(1) time.",
                "O(V + E)",
                "Space Complexity: Parent & Rank arrays in Disjoint Set of size O(V) + Edge list O(E).",
                "Why O(V+E)? Bounded by vertex count and edge list storage.",
                "Auxiliary Space: O(V)",
                "Edge List Space: O(E)"
            ),
            "Stack"
        ));

        // 9. Kosaraju's SCC Algorithm
        problems.put("kosaraju-scc", new ProblemDetail(
            "kosaraju-scc", "Kosaraju's Strongly Connected Components", "Graphs - Hard Problems", "Advanced Graphs", "Hard",
            "Find all Strongly Connected Components (SCCs) in a Directed Graph using 3-Step DFS & Transpose.",
            """
            // Java Kosaraju's Algorithm (Striver A2Z Sheet)
            public int kosaraju(int V, ArrayList<ArrayList<Integer>> adj) {
                boolean vis[] = new boolean[V];
                Stack<Integer> st = new Stack<>();
                for (int i = 0; i < V; i++) {
                    if (!vis[i]) dfs1(i, vis, st, adj);
                }

                // Step 2: Transpose Graph
                ArrayList<ArrayList<Integer>> adjT = new ArrayList<>();
                for (int i = 0; i < V; i++) adjT.add(new ArrayList<>());
                for (int i = 0; i < V; i++) {
                    vis[i] = false;
                    for (int it : adj.get(i)) adjT.get(it).add(i);
                }

                // Step 3: DFS on transposed graph in stack order
                int scc = 0;
                while (!st.isEmpty()) {
                    int node = st.pop();
                    if (!vis[node]) {
                        scc++;
                        dfs2(node, vis, adjT);
                    }
                }
                return scc;
            }
            """,
            createDirectedGraphNodes(), createDirectedGraphEdges(), null, null,
            new ComplexityDetail(
                "O(V + E)",
                "Time Complexity: Step 1 DFS1 O(V+E), Step 2 Transpose Graph O(V+E), Step 3 DFS2 O(V+E). Total = 3 * O(V+E) = O(V+E).",
                "Why Transpose reverses edges? Reversing directed edges prevents DFS from spilling across SCC boundaries during step 3.",
                "O(V + E)",
                "Space Complexity: Transposed graph adjacency list O(V+E) + Stack O(V) + Visited array O(V).",
                "Why O(V+E)? Stores transposed graph.",
                "Auxiliary Space: O(V)",
                "Transposed Adjacency List: O(V + E)"
            ),
            "Stack"
        ));

        // 10. Tarjan's Bridges
        problems.put("tarjan-bridges", new ProblemDetail(
            "tarjan-bridges", "Bridges in Graph (Tarjan's Algorithm)", "Graphs - Hard Problems", "Advanced Graphs", "Hard",
            "An edge is a bridge if removing it increases the number of connected components. Find all bridges using tin[] and low[] arrays.",
            """
            // Java Tarjan's Bridge Finding (LeetCode 1192)
            private int timer = 1;
            public List<List<Integer>> criticalConnections(int n, List<List<Integer>> connections) {
                ArrayList<ArrayList<Integer>> adj = new ArrayList<>();
                for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
                for (List<Integer> edge : connections) {
                    adj.get(edge.get(0)).add(edge.get(1));
                    adj.get(edge.get(1)).add(edge.get(0));
                }

                int[] tin = new int[n];
                int[] low = new int[n];
                boolean[] vis = new boolean[n];
                List<List<Integer>> bridges = new ArrayList<>();

                dfs(0, -1, vis, adj, tin, low, bridges);
                return bridges;
            }

            private void dfs(int node, int parent, boolean[] vis, ArrayList<ArrayList<Integer>> adj, int[] tin, int[] low, List<List<Integer>> bridges) {
                vis[node] = true;
                tin[node] = low[node] = timer++;

                for (int it : adj.get(node)) {
                    if (it == parent) continue;
                    if (!vis[it]) {
                        dfs(it, node, vis, adj, tin, low, bridges);
                        low[node] = Math.min(low[node], low[it]);
                        if (low[it] > tin[node]) {
                            bridges.add(Arrays.asList(node, it)); // Bridge found!
                        }
                    } else {
                        low[node] = Math.min(low[node], tin[it]);
                    }
                }
            }
            """,
            createCyclicGraphNodes(), createCyclicGraphEdges(), null, null,
            new ComplexityDetail(
                "O(V + 2E)",
                "Time Complexity: Single DFS traversal tracks discovery time `tin[]` and lowest reach time `low[]` for every node and edge.",
                "Why low[it] > tin[node] condition indicates bridge? If lowest reachable time from node `it` is greater than insertion time of `node`, there is NO BACK-EDGE to node or ancestors -> Edge is a critical bridge!",
                "O(V + 2E)",
                "Space Complexity: Visited array O(V), tin[] O(V), low[] O(V), recursion call stack O(V), adjacency list O(V+2E).",
                "Why O(V)? Arrays tin[] and low[] take size V.",
                "Auxiliary Space: O(V)",
                "Adjacency List Space: O(V + 2E)"
            ),
            "Stack"
        ));
    }

    // Step Generators
    private List<ExecutionStep> generateTopoSortDfsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> nodeStates = new HashMap<>();
        for (int i = 0; i < 4; i++) nodeStates.put(i, "unvisited");

        nodeStates.put(0, "visiting");
        steps.add(new ExecutionStep(1, 19, "Start DFS from 0. Mark vis[0]=true", List.of("dfs(0)"), new HashMap<>(nodeStates), List.of(), Map.of("Stack", "[]"), "Stack", null));

        nodeStates.put(1, "visiting");
        steps.add(new ExecutionStep(2, 21, "DFS(0) -> DFS(1). Mark vis[1]=true", List.of("dfs(0)", "dfs(1)"), new HashMap<>(nodeStates), List.of("0-1"), Map.of("Stack", "[]"), "Stack", null));

        nodeStates.put(2, "visiting");
        steps.add(new ExecutionStep(3, 21, "DFS(1) -> DFS(2). Mark vis[2]=true", List.of("dfs(0)", "dfs(1)", "dfs(2)"), new HashMap<>(nodeStates), List.of("1-2"), Map.of("Stack", "[]"), "Stack", null));

        nodeStates.put(2, "visited");
        steps.add(new ExecutionStep(4, 24, "DFS(2) completed. Push 2 to Output Stack", List.of("dfs(0)", "dfs(1)"), new HashMap<>(nodeStates), List.of(), Map.of("Output Stack", "[2]"), "Stack", null));

        nodeStates.put(1, "visited");
        steps.add(new ExecutionStep(5, 24, "DFS(1) completed. Push 1 to Output Stack", List.of("dfs(0)"), new HashMap<>(nodeStates), List.of(), Map.of("Output Stack", "[2, 1]"), "Stack", null));

        nodeStates.put(0, "visited");
        steps.add(new ExecutionStep(6, 24, "DFS(0) completed. Push 0 to Output Stack", List.of(), new HashMap<>(nodeStates), List.of(), Map.of("Output Stack", "[2, 1, 0]"), "Stack", null));

        nodeStates.put(3, "visited");
        steps.add(new ExecutionStep(7, 14, "Pop Stack: Topological Order = [0, 1, 2, 3]", List.of(), new HashMap<>(nodeStates), List.of(), Map.of("Topo Order", "[0, 1, 2, 3]"), "Stack", null));

        return steps;
    }

    private List<ExecutionStep> generateKahnAlgoSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> nodeStates = new HashMap<>();
        for (int i = 0; i < 4; i++) nodeStates.put(i, "unvisited");

        nodeStates.put(0, "queued");
        steps.add(new ExecutionStep(1, 10, "Calculate Indegrees: [0:0, 1:1, 2:1, 3:1]. Push node 0 (indegree=0) into Queue", List.of("0"), new HashMap<>(nodeStates), List.of(), Map.of("indegree[0]", "0"), "Queue", null));

        nodeStates.put(0, "visited"); nodeStates.put(1, "queued");
        steps.add(new ExecutionStep(2, 19, "Poll 0. Decrement indegree of neighbor 1 (1->0). Push 1 to Queue", List.of("1"), new HashMap<>(nodeStates), List.of("0-1"), Map.of("indegree[1]", "0"), "Queue", null));

        nodeStates.put(1, "visited"); nodeStates.put(2, "queued");
        steps.add(new ExecutionStep(3, 19, "Poll 1. Decrement indegree of neighbor 2 (2->0). Push 2 to Queue", List.of("2"), new HashMap<>(nodeStates), List.of("1-2"), Map.of("indegree[2]", "0"), "Queue", null));

        nodeStates.put(2, "visited"); nodeStates.put(3, "queued");
        steps.add(new ExecutionStep(4, 19, "Poll 2. Decrement indegree of neighbor 3 (3->0). Push 3 to Queue", List.of("3"), new HashMap<>(nodeStates), List.of("2-3"), Map.of("indegree[3]", "0"), "Queue", null));

        nodeStates.put(3, "visited");
        steps.add(new ExecutionStep(5, 22, "Kahn's BFS complete. Topological Order = [0, 1, 2, 3]", List.of(), new HashMap<>(nodeStates), List.of(), Map.of("Result", "[0, 1, 2, 3]"), "Queue", null));

        return steps;
    }

    private List<ExecutionStep> generateShortestPathUndirectedSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> nodeStates = new HashMap<>();
        for (int i = 0; i < 4; i++) nodeStates.put(i, "unvisited");

        nodeStates.put(0, "queued");
        steps.add(new ExecutionStep(1, 12, "Initialize dist[] = [0, INF, INF, INF]. Push source 0 to Queue", List.of("0"), new HashMap<>(nodeStates), List.of(), Map.of("dist[0]", "0"), "Queue", null));

        nodeStates.put(0, "visited"); nodeStates.put(1, "queued"); nodeStates.put(2, "queued");
        steps.add(new ExecutionStep(2, 18, "Poll 0. Update dist[1]=1, dist[2]=1. Push 1 and 2 to Queue", List.of("1", "2"), new HashMap<>(nodeStates), List.of("0-1", "0-2"), Map.of("dist", "[0, 1, 1, INF]"), "Queue", null));

        nodeStates.put(1, "visited"); nodeStates.put(3, "queued");
        steps.add(new ExecutionStep(3, 18, "Poll 1. Update dist[3] = dist[1] + 1 = 2. Push 3 to Queue", List.of("2", "3"), new HashMap<>(nodeStates), List.of("1-3"), Map.of("dist", "[0, 1, 1, 2]"), "Queue", null));

        nodeStates.put(2, "visited"); nodeStates.put(3, "visited");
        steps.add(new ExecutionStep(4, 21, "Queue empty. Final shortest distances from source 0: [0, 1, 1, 2]", List.of(), new HashMap<>(nodeStates), List.of(), Map.of("Result dist", "[0, 1, 1, 2]"), "Queue", null));

        return steps;
    }

    private List<ExecutionStep> generateDijkstraSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> nodeStates = new HashMap<>();
        for (int i = 0; i < 4; i++) nodeStates.put(i, "unvisited");

        nodeStates.put(0, "queued");
        steps.add(new ExecutionStep(1, 11, "Initialize dist[0]=0, others=INF. Add Pair(dist=0, node=0) to Min-Heap", List.of("(d:0, n:0)"), new HashMap<>(nodeStates), List.of(), Map.of("dist[0]", "0"), "PriorityQueue", null));

        nodeStates.put(0, "visited"); nodeStates.put(1, "queued"); nodeStates.put(2, "queued");
        steps.add(new ExecutionStep(2, 20, "Poll Min (d:0, n:0). Relax edge 0->1 (wt=4) and 0->2 (wt=1). Push to Min-Heap", List.of("(d:1, n:2)", "(d:4, n:1)"), new HashMap<>(nodeStates), List.of("0-2"), Map.of("dist", "[0, 4, 1, INF]"), "PriorityQueue", null));

        nodeStates.put(2, "visited"); nodeStates.put(3, "queued");
        steps.add(new ExecutionStep(3, 20, "Poll Min (d:1, n:2). Relax edge 2->3 (wt=2). Update dist[3]=1+2=3. Push (d:3, n:3) to Min-Heap", List.of("(d:3, n:3)", "(d:4, n:1)"), new HashMap<>(nodeStates), List.of("2-3"), Map.of("dist", "[0, 4, 1, 3]"), "PriorityQueue", null));

        nodeStates.put(3, "visited"); nodeStates.put(1, "visited");
        steps.add(new ExecutionStep(4, 23, "Dijkstra Min-Heap complete. Shortest Distances: [0, 4, 1, 3]", List.of(), new HashMap<>(nodeStates), List.of(), Map.of("Final dist", "[0, 4, 1, 3]"), "PriorityQueue", null));

        return steps;
    }

    private List<ExecutionStep> generateBellmanFordSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        steps.add(new ExecutionStep(1, 4, "Initialize dist[0]=0, dist[1..V-1]=INF. Start V-1 = 3 relaxation passes", List.of(), Map.of(), List.of(), Map.of("pass", "0", "dist", "[0, INF, INF, INF]"), "Queue", null));
        steps.add(new ExecutionStep(2, 10, "Pass 1: Relax all edges -> dist updated to [0, 4, 1, 3]", List.of(), Map.of(), List.of(), Map.of("pass", "1", "dist", "[0, 4, 1, 3]"), "Queue", null));
        steps.add(new ExecutionStep(3, 17, "Pass V check: No edge relaxed further. No negative cycle detected! Final dist = [0, 4, 1, 3]", List.of(), Map.of(), List.of(), Map.of("Status", "Complete - No Negative Cycle"), "Queue", null));
        return steps;
    }

    private List<ExecutionStep> generateFloydWarshallSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[][] matrix = new int[][]{
            {0, 4, 1, 3},
            {4, 0, 2, 5},
            {1, 2, 0, 2},
            {3, 5, 2, 0}
        };

        steps.add(new ExecutionStep(1, 4, "Floyd-Warshall 3-Loop DP: Matrix[i][j] = Math.min(Matrix[i][j], Matrix[i][k] + Matrix[k][j])", List.of(), Map.of(), List.of(), Map.of("k", "0..V-1"), "Matrix", matrix));
        steps.add(new ExecutionStep(2, 12, "All-Pairs Shortest Path matrix computed successfully!", List.of(), Map.of(), List.of(), Map.of("Status", "Complete"), "Matrix", matrix));
        return steps;
    }

    private List<ExecutionStep> generatePrimsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> nodeStates = new HashMap<>();
        for (int i = 0; i < 4; i++) nodeStates.put(i, "unvisited");

        nodeStates.put(0, "queued");
        steps.add(new ExecutionStep(1, 10, "Initialize Prim's Min-Heap with Pair(wt=0, node=0)", List.of("(wt:0, node:0)"), new HashMap<>(nodeStates), List.of(), Map.of("MST sum", "0"), "PriorityQueue", null));

        nodeStates.put(0, "visited"); nodeStates.put(2, "queued");
        steps.add(new ExecutionStep(2, 18, "Poll (wt:0, node:0). Add weight 0 to MST. Push unvisited neighbors 2 (wt=1) and 1 (wt=4)", List.of("(wt:1, node:2)", "(wt:4, node:1)"), new HashMap<>(nodeStates), List.of("0-2"), Map.of("MST sum", "1"), "PriorityQueue", null));

        nodeStates.put(2, "visited"); nodeStates.put(3, "queued");
        steps.add(new ExecutionStep(3, 18, "Poll Min (wt:1, node:2). Add weight 1 to MST. Push neighbor 3 (wt=2)", List.of("(wt:2, node:3)", "(wt:4, node:1)"), new HashMap<>(nodeStates), List.of("2-3"), Map.of("MST sum", "3"), "PriorityQueue", null));

        nodeStates.put(3, "visited"); nodeStates.put(1, "visited");
        steps.add(new ExecutionStep(4, 21, "All V vertices included in MST! Minimum Spanning Tree Weight Sum = 6", List.of(), new HashMap<>(nodeStates), List.of(), Map.of("MST Weight Sum", "6"), "PriorityQueue", null));

        return steps;
    }

    private List<ExecutionStep> generateKruskalsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        steps.add(new ExecutionStep(1, 19, "Sort all edges by weight: [(0-2, wt=1), (2-3, wt=2), (1-3, wt=3), (0-1, wt=4)]", List.of(), Map.of(), List.of(), Map.of("sorted_edges", "4"), "Stack", null));
        steps.add(new ExecutionStep(2, 25, "Pick edge (0-2, wt=1): find(0)!=find(2) -> Union(0, 2). Add wt=1. MST sum = 1", List.of(), Map.of(), List.of("0-2"), Map.of("MST sum", "1"), "Stack", null));
        steps.add(new ExecutionStep(3, 25, "Pick edge (2-3, wt=2): find(2)!=find(3) -> Union(2, 3). Add wt=2. MST sum = 3", List.of(), Map.of(), List.of("0-2", "2-3"), Map.of("MST sum", "3"), "Stack", null));
        steps.add(new ExecutionStep(4, 25, "Pick edge (1-3, wt=3): find(1)!=find(3) -> Union(1, 3). Add wt=3. MST sum = 6", List.of(), Map.of(), List.of("0-2", "2-3", "1-3"), Map.of("MST sum", "6"), "Stack", null));
        steps.add(new ExecutionStep(5, 29, "Kruskal's MST Complete! Total MST Weight = 6", List.of(), Map.of(), List.of(), Map.of("Final MST Weight", "6"), "Stack", null));
        return steps;
    }

    private List<ExecutionStep> generateKosarajuSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        steps.add(new ExecutionStep(1, 6, "Step 1: Perform DFS1 on graph and store finish order in Stack [0, 1, 2]", List.of("2", "1", "0"), Map.of(), List.of(), Map.of("Step", "1 - Topo Finish Order"), "Stack", null));
        steps.add(new ExecutionStep(2, 12, "Step 2: Transpose Graph (Reverse direction of all directed edges)", List.of(), Map.of(), List.of(), Map.of("Step", "2 - Transpose Graph"), "Stack", null));
        steps.add(new ExecutionStep(3, 21, "Step 3: Pop stack and launch DFS2 on transposed graph -> Found 1 Strongly Connected Component!", List.of(), Map.of(), List.of(), Map.of("SCC Count", "1"), "Stack", null));
        return steps;
    }

    private List<ExecutionStep> generateTarjanBridgesSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        Map<Integer, String> nodeStates = new HashMap<>();
        for (int i = 0; i < 4; i++) nodeStates.put(i, "unvisited");

        nodeStates.put(0, "visiting");
        steps.add(new ExecutionStep(1, 18, "DFS(0): set tin[0]=1, low[0]=1", List.of("dfs(0)"), new HashMap<>(nodeStates), List.of(), Map.of("tin[0]", "1", "low[0]", "1"), "Stack", null));

        nodeStates.put(1, "visiting");
        steps.add(new ExecutionStep(2, 22, "DFS(1): set tin[1]=2, low[1]=2", List.of("dfs(0)", "dfs(1)"), new HashMap<>(nodeStates), List.of("0-1"), Map.of("tin[1]", "2", "low[1]", "2"), "Stack", null));

        nodeStates.put(3, "visiting");
        steps.add(new ExecutionStep(3, 22, "DFS(3): set tin[3]=3, low[3]=3", List.of("dfs(0)", "dfs(1)", "dfs(3)"), new HashMap<>(nodeStates), List.of("1-3"), Map.of("tin[3]", "3", "low[3]", "3"), "Stack", null));

        nodeStates.put(3, "visited");
        steps.add(new ExecutionStep(4, 25, "Backtrack to 1. low[3] > tin[1] (3 > 2) is TRUE -> Edge (1-3) IS A CRITICAL BRIDGE!", List.of("dfs(0)", "dfs(1)"), new HashMap<>(nodeStates), List.of("1-3"), Map.of("Bridge Found", "1-3"), "Stack", null));

        return steps;
    }

    // Helper node builders
    private List<GraphNode> createDagNodes() {
        return List.of(
            new GraphNode(0, "0", 80, 100, "unvisited"),
            new GraphNode(1, "1", 170, 100, "unvisited"),
            new GraphNode(2, "2", 260, 100, "unvisited"),
            new GraphNode(3, "3", 350, 100, "unvisited")
        );
    }

    private List<GraphEdge> createDagEdges() {
        return List.of(
            new GraphEdge(0, 1, true),
            new GraphEdge(1, 2, true),
            new GraphEdge(2, 3, true)
        );
    }

    private List<GraphNode> createShortestPathNodes() {
        return List.of(
            new GraphNode(0, "0", 80, 80, "unvisited"),
            new GraphNode(1, "1", 200, 80, "unvisited"),
            new GraphNode(2, "2", 80, 220, "unvisited"),
            new GraphNode(3, "3", 200, 220, "unvisited")
        );
    }

    private List<GraphEdge> createShortestPathEdges() {
        return List.of(
            new GraphEdge(0, 1, false),
            new GraphEdge(0, 2, false),
            new GraphEdge(1, 3, false),
            new GraphEdge(2, 3, false)
        );
    }

    private List<GraphNode> createWeightedNodes() {
        return List.of(
            new GraphNode(0, "0", 80, 80, "unvisited"),
            new GraphNode(1, "1", 240, 80, "unvisited"),
            new GraphNode(2, "2", 80, 240, "unvisited"),
            new GraphNode(3, "3", 240, 240, "unvisited")
        );
    }

    private List<GraphEdge> createWeightedEdges() {
        return List.of(
            new GraphEdge(0, 1, 4, true, false),
            new GraphEdge(0, 2, 1, true, false),
            new GraphEdge(2, 3, 2, true, false),
            new GraphEdge(1, 3, 3, true, false)
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
}
