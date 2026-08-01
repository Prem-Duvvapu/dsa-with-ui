# 🌐 DSA Visualizer - Interactive Data Structures & Algorithms Engine

Welcome to **DSA Visualizer**! A full-stack interactive web application built for mastering core Data Structures & Algorithms with visual elegance.

The application pairs step-by-step animated execution with:
- ☕ **Production-Ready Java Solutions** (clean interview-grade code with commented lines).
- 🎯 **Line-by-Line Code Execution Tracing** (synchronized Java code line highlighting as nodes/edges/cells process).
- 📊 **Animated Data Structure Panels** (Live Queue FIFO for BFS, Call Stack LIFO for DFS, PriorityQueue for Min-Heap, Array Bars, Linked List Nodes).
- ⚡ **Time & Space Complexity Deep-Dive** (In-depth **How and Why** mathematical proofs for $O(V+E)$, $O(E \log V)$, $O(N \log N)$, $O(N)$, auxiliary space, etc.).

---

## 🏗️ Architecture & Technology Stack

| Tier | Technology | Description |
| :--- | :--- | :--- |
| **Backend** | **Spring Boot (Java 17)** | REST APIs (`http://localhost:8923/api/*`), algorithm step engine |
| **Frontend** | **React 18 + Vite** | SVG Graph, Tree, Array & Linked List renderers, glassmorphism dark mode, player controls |
| **Testing** | **JUnit 5 & Vitest** | End-to-end unit & integration test coverage for backend and frontend components |
| **DevOps** | **Docker & Docker Compose** | Single-command deployment (`docker-compose up`) |

---

## 🚀 How to Run the Application

### Option 1: Using Docker (One Command for FE & BE)

Run both the Spring Boot backend (`port 8923`) and React frontend (`port 5174`) in one command:

```bash
docker-compose up --build
```
Open your browser at **`http://localhost:5174`**!

---

### Option 2: Running Locally

#### 1. Start the Spring Boot Backend (Java)
```bash
# In WSL or bash
cd backend
mvn spring-boot:run
```
*(Backend runs on `http://localhost:8923`)*

#### 2. Start the React Frontend
```bash
cd frontend
npm install
npm run dev
```
*(Frontend runs on `http://localhost:5180` with proxy configured to Spring Boot on `8923`)*

---

## 🧪 Running Tests

### Run Backend Java Tests (JUnit 5 + MockMvc)
```bash
cd backend
mvn test
```

### Run Frontend React Tests (Vitest)
```bash
cd frontend
npm run test
```

---

## 📋 Algorithm Library Progress

### 1. **Graphs: BFS & DFS**
- [x] **BFS Traversal of Graph**
- [x] **DFS Traversal of Graph**
- [x] **Number of Provinces**
- [x] **Number of Islands**
- [x] **Rotting Oranges**
- [x] **Flood Fill Algorithm**
- [x] **Detect Cycle in Undirected Graph**
- [x] **Detect Cycle in Directed Graph**
- [x] **0/1 Matrix - Distance of Nearest 1**
- [x] **Surrounded Regions**

### 2. **Advanced Graph Algorithms**
- [x] **Topological Sort** (DFS & Kahn's Algorithm BFS)
- [x] **Shortest Path in Undirected Graph** (Unit Weights BFS)
- [x] **Dijkstra's Shortest Path** (PriorityQueue Min-Heap)
- [x] **Bellman-Ford Algorithm** (Edge relaxation & Negative Cycle check)
- [x] **Floyd-Warshall Algorithm** (All-Pairs Shortest Path Matrix DP)
- [x] **Prim's Minimum Spanning Tree (MST)**
- [x] **Kruskal's MST** (Disjoint Set / Union-Find with Rank & Path Compression)
- [x] **Kosaraju's Strongly Connected Components (SCC)**
- [x] **Tarjan's Bridges in Graph** (tin & low arrays)

### 3. **Binary Trees & BST**
- [x] **Preorder, Inorder, Postorder Traversals**
- [x] **Level Order Traversal**
- [x] **Maximum Depth / Height of Binary Tree**
- [x] **Check if Binary Tree is Height-Balanced**
- [x] **Diameter of Binary Tree**
- [x] **Maximum Path Sum in Binary Tree**
- [x] **Lowest Common Ancestor (LCA)**
- [x] **Minimum Time to Burn Binary Tree**
- [x] **Search in Binary Search Tree (BST)**
- [x] **Validate Binary Search Tree**
- [x] **Kth Smallest Element in BST**

### 4. **Sorting Algorithms**
- [x] **Selection Sort**
- [x] **Bubble Sort**
- [x] **Insertion Sort**
- [x] **Merge Sort**
- [x] **Quick Sort**

### 5. **Arrays & Math**
- [x] **Two Sum**
- [x] **Sort 0s, 1s, 2s (Dutch National Flag)**
- [x] **Majority Element (Moore's Voting)**
- [x] **Kadane's Algorithm**
- [x] **Stock Buy & Sell**

### 6. **Linked Lists**
- [x] **Reverse Linked List**
- [x] **Middle of Linked List**
- [x] **Detect Loop in Linked List**
- [x] **Delete Node in Linked List (O(1))**
- [x] **Merge Two Sorted Linked Lists**

### 7. **Binary Search**
- [x] **Binary Search 1D**
- [x] **Search in Rotated Sorted Array**
- [x] **Find Peak Element**
- [x] **Koko Eating Bananas**

### 8. **Dynamic Programming**
- [x] **Climbing Stairs**
- [x] **Frog Jump / Min Cost Climbing**
- [x] **0/1 Knapsack Problem**
- [x] **Longest Common Subsequence (LCS)**