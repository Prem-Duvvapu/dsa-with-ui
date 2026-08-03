# 🌐 DSA Visualizer - Master Interactive Data Structures & Algorithms Engine (406 Algorithms)

Welcome to **DSA Visualizer**! A full-stack interactive web application built for mastering **406 Master Data Structures & Algorithms** (Striver's A2Z DSA Sheet) with visual elegance.

The application pairs step-by-step animated execution with:
- ☕ **Production-Ready Java Solutions**: Clean interview-grade Java implementations with commented lines.
- 🎯 **Line-by-Line Code Execution Tracing**: Synchronized Java code line highlighting as nodes, edges, cells, or pointers process.
- 📊 **Animated Data Structure Panels**: Live Queue FIFO, Call Stack LIFO, PriorityQueue Min-Heap, Array Bars, Linked List Nodes, Trie Trees, and **Disjoint Set Union (DSU) Component Forest Cards**.
- ⚡ **Time & Space Complexity Deep-Dive**: Mathematical proofs for $O(V+E)$, $O(E \log V)$, $O(N \log N)$, $O(N)$, auxiliary space, and best/worst case bounds.
- 🗂️ **A-Z Alphabetical Topic Accordions**: Sortable and collapsible topic cards (A-Z) with instant search and global expand/shrink controls.

---

## 🏗️ Architecture & Technology Stack

| Tier | Technology | Description |
| :--- | :--- | :--- |
| **Backend** | **Spring Boot (Java 17)** | 18 REST Controllers (`http://localhost:8923/api/*`), algorithm execution step engines |
| **Frontend** | **React 18 + Vite** | Multi-mode canvas stages (Graph, Tree, Array, Linked List, Recursion Tree, DSU), glassmorphism dark mode UI |
| **Testing** | **JUnit 5 & Vitest** | 90 backend unit tests (`wsl mvn test`) and frontend test coverage |
| **DevOps** | **Docker & Docker Compose** | Single-command container deployment (`docker-compose up`) |

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
# In WSL or terminal
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
wsl mvn test
```
**Result**: `Tests run: 90, Failures: 0, Errors: 0` (`BUILD SUCCESS`).

### Run Frontend Build
```bash
cd frontend
wsl npm run build
```
**Result**: `✓ built in ~35s` (0 errors).

---

## 📋 Algorithm Catalog (406 Master Algorithms across 17 A-Z Categories)

### 1. **Adv Graphs & Graph Strings (62 Algorithms)**
- Shortest Path, Dijkstra, Bellman-Ford, Floyd-Warshall, Prim's, Kruskal's, Disjoint Set (DSU), Tarjan's Bridges, Articulation Points, Kosaraju's SCC, Rabin-Karp, Z-Function, KMP LPS.

### 2. **Arrays & Matrices (26 Algorithms)**
- Two Sum, Dutch National Flag (Sort 0s,1s,2s), Majority Element, Kadane's Algorithm, 3Sum, 4Sum, Pascal's Triangle, Next Permutation, Inversions.

### 3. **Backtracking & Recursion (25 Algorithms)**
- Subsets, Combination Sum I/II, N-Queens, Sudoku Solver, Rat in a Maze, Word Search, Palindrome Partitioning.

### 4. **Basic Math & Basic Recursion (14 Algorithms)**
- Reverse Digits, Palindrome Check, GCD / HCF, Prime Numbers, Fibonacci, Factorial, Print 1 to N.

### 5. **Binary Search Suite (32 Algorithms)**
- Binary Search 1D, Lower/Upper Bound, Search Insert, Rotated Sorted Array I/II, Min in Rotated Array, Single Element, Peak Element, Square Root, Bananas, Bouquets, Smallest Divisor, Gas Stations, Median of 2 Sorted Arrays.

### 6. **Binary Trees & BST (54 Algorithms)**
- Preorder, Inorder, Postorder, Level Order, Max Depth, Balanced Tree, Diameter, Max Path Sum, LCA, Burn Tree, Morris Traversals, Search/Insert/Delete BST.

### 7. **Bit Logic & Advanced Math (18 Algorithms)**
- Bit Tricks, Count Set Bits, Power of 2, Single Number, Sieve of Eratosthenes, Prime Factors, Binary Exponentiation.

### 8. **Dynamic Programming (55 Algorithms)**
- Climbing Stairs, Frog Jump, House Robber, 0/1 Knapsack, Unbounded Knapsack, Subset Sum, Equal Partition, LCS, Longest Palindromic Subsequence, Edit Distance, LIS, MCM, Count Squares.

### 9. **Graphs: BFS & DFS (11 Algorithms)**
- BFS Traversal, DFS Traversal, Provinces, Islands, Rotting Oranges, Flood Fill, Cycle Detections, Bipartite Graph.

### 10. **Greedy Algorithms (15 Algorithms)**
- Assign Cookies, Fractional Knapsack, N Meetings in 1 Room, Job Sequencing, Railway Platforms, Candy, Jump Game I/II.

### 11. **Heaps & PriorityQueue (17 Algorithms)**
- Min/Max Heap Construction, Kth Largest/Smallest Element, Task Scheduler, Twitter Design, Median Stream.

### 12. **Linked List & Doubly LL (31 Algorithms)**
- Reversals, Middle Node, Loop Detection, Segregate Odd-Even, Merge Sort LL, Y Intersection, Flattening LL, Clone Random LL.

### 13. **Sliding Window & Two Pointer (12 Algorithms)**
- Longest Substring Without Repeating, Consecutive Ones III, Fruit Baskets, Min Window Substring.

### 14. **Sorting Algorithms (5 Algorithms)**
- Bubble Sort, Selection Sort, Insertion Sort, Merge Sort, Quick Sort.

### 15. **Stack & Queue (30 Algorithms)**
- Infix to Postfix/Prefix, Min Stack, Next Greater Element, Trapping Rainwater, Largest Rectangle in Histogram, Sliding Window Max, LRU & LFU Cache.

### 16. **Strings (16 Algorithms)**
- Remove Outermost Parentheses, Reverse Words, Longest Common Prefix, Valid Anagram, Isomorphic Strings, String to Integer (atoi).

### 17. **Tries & Prefixes (5 Algorithms)**
- Implement Trie (Insert/Search/StartsWith), Word Count, Prefix Count, Complete String.