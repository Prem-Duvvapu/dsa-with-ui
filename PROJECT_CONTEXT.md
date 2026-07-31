# Project Context: DSA Visualizer (Interactive Platform)

## 📌 Project Overview
**DSA Visualizer** is a full-stack interactive web application built to assist software engineers and students in mastering core Data Structures and Algorithms.

The platform pairs step-by-step animated execution with:
- **Production-ready Java Solutions** (commented & structured for technical interviews).
- **Synchronized Code Execution Tracing** (highlighting Java lines as nodes/edges/pointers are processed).
- **Animated Data Structure State** (Call Stack for DFS, Queue for BFS, PriorityQueue for Min-Heap, Array Bars, Linked List Nodes).
- **Comprehensive Time & Space Complexity Analysis** explaining the **How** and **Why** behind theoretical bounds ($O(V+E)$, $O(E \log V)$, $O(N \log N)$, $O(N)$, etc.).

---

## 🎯 Algorithm Categories

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
- [x] Binary Tree Level Order Traversal (BFS Queue)
- [x] Maximum Depth / Height of Binary Tree
- [x] Check if Binary Tree is Height-Balanced
- [x] Diameter of Binary Tree
- [x] Maximum Path Sum in Binary Tree (LeetCode 124)
- [x] Lowest Common Ancestor (LCA) in Binary Tree (LeetCode 236)
- [x] Minimum Time to Burn Binary Tree from Target Node
- [x] Search in Binary Search Tree (BST)
- [x] Validate Binary Search Tree (LeetCode 98)
- [x] Kth Smallest Element in BST

### 4. **Sorting Algorithms**
- [x] Selection Sort ($O(N^2)$)
- [x] Bubble Sort ($O(N^2)$)
- [x] Insertion Sort ($O(N^2)$)
- [x] Merge Sort ($O(N \log N)$ Divide & Conquer)
- [x] Quick Sort ($O(N \log N)$ Partitioning)

### 5. **Arrays & Math**
- [x] Two Sum (HashMap / 2 Pointers)
- [x] Sort An Array of 0s, 1s and 2s (Dutch National Flag Algorithm)
- [x] Majority Element (Moore's Voting Algorithm)
- [x] Kadane's Algorithm (Maximum Subarray Sum)
- [x] Best Time to Buy and Sell Stock

### 6. **Linked Lists**
- [x] Reverse Linked List (3 Pointers)
- [x] Middle of Linked List (Fast & Slow Pointers)
- [x] Detect Loop in Linked List (Floyd's Cycle Detection)
- [x] Delete Node in a Linked List (O(1))
- [x] Merge Two Sorted Linked Lists

### 7. **Binary Search**
- [x] Binary Search on 1D Array
- [x] Search in Rotated Sorted Array
- [x] Find Peak Element
- [x] Koko Eating Bananas (Binary Search on Answer)

### 8. **Dynamic Programming**
- [x] Climbing Stairs (1D DP)
- [x] Frog Jump / Min Cost Climbing (1D DP)
- [x] 0/1 Knapsack Problem (2D DP Matrix)
- [x] Longest Common Subsequence (LCS 2D DP Matrix)

---

## 🏗️ Architecture & Stack
- **Backend Framework**: Spring Boot 3 (Java 17, Maven)
- **Frontend Framework**: React 18 + Vite
- **Styling**: Modern Vanilla CSS with Dark Mode Glassmorphism Theme
- **Visualization Engine**:
  - `GraphCanvas.jsx`: SVG Network graph visualizer
  - `TreeCanvas.jsx`: SVG Binary Tree & BST layout visualizer
  - `ArrayCanvas.jsx`: Dynamic bar graph & element array visualizer
  - `LinkedListCanvas.jsx`: Node & link visualizer
