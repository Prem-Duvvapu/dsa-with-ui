> **Superseded.** This document describes the project as it was planned before the
> tracing rewrite, and its numbers are wrong: it headlines 406 algorithms, its own
> itemised list sums to 428, and the code registers 433 unique ids. It also predates the
> finding that 303 catalogued problems returned another algorithm's animation.
>
> Kept as a historical record. For the current architecture see `plan.md` and the README;
> for live coverage see `GET /api/problems/stats`.

# Implementation Plan - Master DSA Sheet Interactive Visualizer

Complete architectural plan and execution blueprint for the 406 algorithm interactive visualizer application (`dsa-with-ui`).

---

## 🎯 Architecture Overview

```
                        +-------------------------------------+
                        |          Vite React Frontend        |
                        |   - Sidebar.jsx (A-Z Accordions)    |
                        |   - Canvas Stages (Graph, Tree, etc)|
                        |   - Controls & Diagnostic Dashboard |
                        +------------------+------------------+
                                           | HTTP REST API
                                           v
                        +-------------------------------------+
                        |        Spring Boot 3 Backend        |
                        |   - 18 REST Controller Endpoints    |
                        |   - 18 Algorithm Domain Services    |
                        |   - Step Trace Generators & Models  |
                        +-------------------------------------+
```

---

## 🧩 Proposed Component Architecture

### Backend Services & Controllers (18 REST Modules)
1. **`AdvancedGraphService.java` & `AdvancedGraphController.java`**: 62 Graph & String algorithms (DSU, Dijkstra, Bellman-Ford, Floyd-Warshall, Prim's, Kruskal's, Tarjan's, Kosaraju's, KMP, Z-Function, Rabin-Karp).
2. **`DpService.java` & `DpController.java`**: 55 Dynamic Programming algorithms (Basic DP, Grids, Subsequences, Strings, Stocks, LIS, MCM, Squares).
3. **`TreeService.java` & `TreeController.java`**: 54 Binary Tree & BST algorithms (Traversals, Views, LCA, Burn Tree, Morris, BST Operations).
4. **`BinarySearchService.java` & `BinarySearchController.java`**: 32 Binary Search algorithms (1D, BS on Answers, 2D Matrix).
5. **`LinkedListService.java` & `LinkedListController.java`**: 31 Singly & Doubly LinkedList algorithms.
6. **`StackQueueService.java` & `StackQueueController.java`**: 30 Stack & Queue algorithms (Conversions, Monotonic Stack, LRU/LFU).
7. **`ArrayService.java` & `ArrayController.java`**: 26 Array & Matrix algorithms.
8. **`RecursionBacktrackingService.java` & `RecursionBacktrackingController.java`**: 25 Recursion & Backtracking algorithms.
9. **`BitManipulationService.java` & `BitManipulationController.java`**: 18 Bit Manipulation & Advanced Maths algorithms.
10. **`HeapService.java` & `HeapController.java`**: 17 Heap & PriorityQueue algorithms.
11. **`StringService.java` & `StringController.java`**: 16 String algorithms.
12. **`GreedyService.java` & `GreedyController.java`**: 15 Greedy algorithms.
13. **`SlidingWindowService.java` & `SlidingWindowController.java`**: 12 Sliding Window algorithms.
14. **`GraphBfsDfsService.java` & `GraphBfsDfsController.java`**: 11 Graph BFS & DFS algorithms.
15. **`BasicMathService.java` & `BasicMathController.java`**: 6 Basic Math algorithms.
16. **`BasicRecursionService.java` & `BasicRecursionController.java`**: 8 Basic Recursion algorithms.
17. **`TrieService.java` & `TrieController.java`**: 5 Trie algorithms.
18. **`SortingService.java` & `SortingController.java`**: 5 Sorting algorithms.

---

### Frontend Components (`frontend/src/`)
- **`Sidebar.jsx`**: Alphabetical (A-Z) Topic Accordions with expandable/collapsible category cards and real-time search filtering.
- **`GraphCanvas.jsx`**: Multi-mode visualizer stage supporting SVG Graph Networks, 2D Grids, Chessboards (N-Queens), Sudoku, and **Disjoint Set Union (DSU) Component Forest Cards & Array Tables**.
- **`TreeCanvas.jsx`**: Dynamic SVG Binary Tree & BST Topology renderer.
- **`ArrayCanvas.jsx`**: Height-based bar chart and array element visualizer with spring animations.
- **`LinkedListCanvas.jsx`**: Linear node pointer and link visualizer.
- **`RecursionTreeCanvas.jsx`**: Dynamic recursion call tree visualizer stage.

---

## 🧪 Verification Plan

### Backend Unit Tests
- Execute full JUnit test suite: `wsl mvn test`
- Expected: `Tests run: 90, Failures: 0, Errors: 0` (`BUILD SUCCESS`).

### Frontend Production Build
- Execute Vite production build: `wsl npm run build`
- Expected: `✓ built in ~35s` (0 errors).
