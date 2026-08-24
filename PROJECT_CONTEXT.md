# Project Context: DSA Visualizer (Interactive Platform)

## 📌 Project Overview
**DSA Visualizer** is a full-stack interactive web application built to assist software engineers and students in mastering core Data Structures and Algorithms.

The platform pairs step-by-step animated execution with:
- **Production-ready Java Solutions** (commented & structured for technical interviews).
- **Synchronized Code Execution Tracing** (highlighting Java lines as nodes/edges/pointers are processed).
- **Granular Execution Traces** (tracing every single loop iteration, recursive call, constraint check, state mutation, and backtrack step thoroughly like Selection Sort).
- **Human-Centric Pedagogical Visualizations** ($4 \times 4$ Alternating Tile Chessboard with Crown icons for N-Queens, $9 \times 9$ Board with $3 \times 3$ sub-box borders for Sudoku, SVG Call Trees for Divide & Conquer).
- **Animated Data Structure State** (Call Stack for DFS & Recursion, Queue for BFS, Recursion Call Tree SVG for Divide & Conquer, 2D Matrices, Array Bars, Linked List Nodes).
- **Comprehensive Time & Space Complexity Analysis** explaining the **How** and **Why** behind theoretical bounds ($O(V+E)$, $O(E \log V)$, $O(N \log N)$, $O(N!)$, etc.).

---

## 🧠 Human-Centric Pedagogical Principles

1. **Human Learner First Perspective**:
   - Always evaluate visualizations from the eyes of a human student or software candidate trying to internalize the algorithm.
   - Ask: *"Does this visual representation make the underlying state transition, decision branch, or constraint violation immediately obvious to a human?"*

2. **Domain-Specific Realistic Canvas Views**:
   - **N-Queens Problem**: Render a real $4 \times 4$ chessboard with alternating light/dark tiles, glowing Queen Crown icons (`👑`), and live diagonal/column conflict warnings.
   - **Sudoku Solver**: Render a full $9 \times 9$ Sudoku grid with bold $3 \times 3$ sub-box borders (`#6366f1`), cell-by-cell digit placements ('1'-'9'), and invalid placement highlights.
   - **Divide & Conquer / Recursion**: Render a dynamic SVG Recursion Call Tree showing active call stack nodes (`ms(0,5)`, `ms(0,2)`), 2-way array merging, and base-case completion states.

3. **Granular Code-Line Synchronization**:
   - Every single forward choice, constraint evaluation (`isSafe` / `isValid`), state mutation, array swap, and backtrack removal step MUST be explicitly traced with line numbers and human-readable narrative explanations.

---

## 🎯 Algorithm Categories

> **Coverage note.** The checklist below was written when the project had roughly 71
> hand-built step generators. It is kept as a record of intent, not as a coverage claim:
> a tick means the problem is catalogued, not that it has a real execution trace.
> `GET /api/problems/stats` is the authoritative number — currently **433 catalogued,
> 10 traced**. See the README for why those differ.

### 1. **Graphs - BFS & DFS Problems**
- [x] BFS Traversal of Graph
- [x] DFS Traversal of Graph
- [x] Number of Provinces (LeetCode 547)
- [x] Number of Islands (LeetCode 200)
- [x] Rotting Oranges (LeetCode 994)
- [x] Flood Fill (LeetCode 733)
- [x] Detect Cycle in Undirected Graph (BFS & DFS)
- [x] Detect Cycle in Directed Graph (DFS & Kahn's BFS)
- [x] 0/1 Matrix / Distance of Nearest Cell Having 1 (LeetCode 542)
- [x] Surrounded Regions (LeetCode 130)

### 2. **Advanced Graph Algorithms**
- [x] Topological Sort (DFS & Kahn's Algorithm BFS)
- [x] Shortest Path in Undirected Graph (Unit Weights BFS)
- [x] Dijkstra's Shortest Path Algorithm (PriorityQueue Min-Heap)
- [x] Bellman-Ford Algorithm (V-1 Edge Relaxations & Negative Cycle Check)
- [x] Floyd-Warshall Algorithm (All-Pairs Shortest Path Matrix DP)
- [x] Minimum Spanning Tree - Prim's Algorithm
- [x] Minimum Spanning Tree - Kruskal's Algorithm (Disjoint Set / Union-Find)
- [x] Kosaraju's Strongly Connected Components (3-Step DFS)
- [x] Tarjan's Algorithm for Bridges in Graph (Critical Connections)

### 3. **Binary Trees & Binary Search Trees (BST)**
- [x] Binary Tree Preorder, Inorder, Postorder Traversals
- [x] Level Order Traversal (BFS Queue)
- [x] Maximum Depth of Binary Tree
- [x] Diameter of Binary Tree
- [x] Lowest Common Ancestor (LCA) in Binary Tree
- [x] Search in Binary Search Tree (BST)

### 4. **Recursion & Backtracking (Striver's A2Z Sheet)**
- [x] N-Queens Problem (LeetCode 51 - 4x4 Chessboard with Queen Crown Icons)
- [x] Rat in a Maze (2D Grid D-L-R-U Pathfinding & Backtracking)
- [x] Sudoku Solver (LeetCode 37 - 9x9 Board with 3x3 Sub-box Borders)
- [x] M-Coloring Problem (Graph Vertex Coloring)
- [x] Palindrome Partitioning (LeetCode 131 - Substring Partitioning)
- [x] Subsets / Subset Sums (LeetCode 78 - Power Set Decision Tree)
- [x] Combination Sum I (LeetCode 39 - Infinite Candidate Reuse)
- [x] Permutations of Array / String (LeetCode 46 - In-place Swapping)
- [x] Word Search in 2D Board (LeetCode 79 - 4-Directional Search)

### 5. **Sorting Algorithms**
- [x] Selection Sort (Step-by-step Minimum Scanning & Swapping)
- [x] Bubble Sort (Optimized Adjacent Swapping)
- [x] Insertion Sort (In-place Shifting)
- [x] Merge Sort (Divide & Conquer Recursion Call Tree)
- [x] Quick Sort (Partitioning & Pivot Swapping)

### 6. **Arrays & Math**
- [x] Two Sum (HashMap O(1) Lookup)
- [x] Sort 0s, 1s, 2s (Dutch National Flag 3-Pointer Algorithm)
- [x] Majority Element (Moore's Voting Algorithm)
- [x] Kadane's Algorithm (Max Subarray Sum)
- [x] Best Time to Buy and Sell Stock (Single Pass Min/Max)

### 7. **Linked List**
- [x] Reverse Linked List (3-Pointer Reversal)
- [x] Middle of Linked List (Fast & Slow Pointers)
- [x] Detect Loop in Linked List (Floyd's Cycle Detection)
- [x] Delete Node in Linked List (O(1) Value Copying)
- [x] Merge Two Sorted Lists (In-place Splicing)

### 8. **Binary Search**
- [x] Binary Search on 1D Array
- [x] Search in Rotated Sorted Array
- [x] Find Peak Element (Slope Search)
- [x] Koko Eating Bananas (BS on Answer Range)

### 9. **Dynamic Programming (DP)**
- [x] Climbing Stairs (1D DP / Space Optimized)
- [x] Frog Jump / Min Energy (1D DP)
- [x] 0/1 Knapsack Problem (2D Subsets DP Matrix)
- [x] Longest Common Subsequence (2D String DP Matrix)

### 10. **Tries & Prefixes**
- [x] Implement Trie (Prefix Tree)
- [x] Longest Common Prefix
- [x] Word Break Problem using Trie

### 11. **Greedy Algorithms**
- [x] N Meetings in One Room
- [x] Jump Game I
- [x] Job Sequencing Problem

### 12. **Strings**
- [x] Longest Substring Without Repeating Characters
- [x] Valid Anagram

### 13. **Bit Manipulation**
- [x] Single Number (XOR Property)
- [x] Subsets using Bitmasking

### 14. **Heaps & PriorityQueue**
- [x] Kth Largest Element in an Array
- [x] Merge K Sorted Lists
