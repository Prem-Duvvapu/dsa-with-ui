# Implementation Plan - Master DSA Sheet (Complete 249 Questions)

Implement full step-by-step interactive visualizers, Java reference implementations (LeetCode / Striver A2Z Sheet), complexity analysis proofs, and dynamic UI renderings for all **249 Master DSA Sheet Algorithms** in `dsa-with-ui`.

## Proposed Algorithms

### Module Breakdown (249 Questions)
1. **Binary Search Suite** (1D Arrays, BS on Answers, 2D Matrices) [Q1 - Q32]
2. **String Algorithms Suite** (Easy & Medium String Manipulation) [Q33 - Q47]
3. **LinkedList & Doubly LL Suite** (1D/2D LL, Cycle, Reverse, Sort, Intersections, Flattening) [Q48 - Q78]
4. **Recursion & Backtracking Suite** (Subsequences, N-Queens, Sudoku, Rat in Maze, Phone Keypad) [Q79 - Q103]
5. **Bit Manipulation & Advanced Maths Suite** (Bit Tricks, Single Number, Primes, Divisors, Pow(x,n)) [Q104 - Q121]
6. **Stack & Queues Suite** (Conversions, Monotonic Stack, Trapping Rainwater, Histogram Area, LRU/LFU) [Q122 - Q151]
7. **Sliding Window & Two Pointer Suite** (Longest Substring, Subarrays with K Distinct, Min Window) [Q152 - Q163]
8. **Heaps & Priority Queue Suite** (Kth Largest, Task Scheduler, Twitter Design, Median Stream) [Q164 - Q180]
9. **Greedy Algorithms Suite** (Fractional Knapsack, N Meetings, Railway Platforms, Candy, Jump Game) [Q181 - Q195]
10. **Binary Trees Suite** (Traversals, Level-order, Views, Diameter, LCA, Burn Tree, Morris Traversal) [Q196 - Q233]
11. **Binary Search Trees (BST) Suite** (Insert, Delete, Floor/Ceil, Swap Fix, Merge 2 BSTs, Largest BST) [Q234 - Q249]

## Proposed Changes

### Backend - Spring Boot Services (`com.dsa.ui.service`)

#### [MODIFY] [BinarySearchService.java](file:///c:/Users/Hp/OneDrive/Desktop/dsa-with-ui/backend/src/main/java/com/dsa/ui/service/BinarySearchService.java)
- Populate problem details & step trace generators for all 32 Binary Search algorithms.

#### [MODIFY] [StringService.java](file:///c:/Users/Hp/OneDrive/Desktop/dsa-with-ui/backend/src/main/java/com/dsa/ui/service/StringService.java)
- Populate problem details & step trace generators for all 15 String algorithms.

#### [MODIFY] [LinkedListService.java](file:///c:/Users/Hp/OneDrive/Desktop/dsa-with-ui/backend/src/main/java/com/dsa/ui/service/LinkedListService.java)
- Populate problem details & step trace generators for all 31 LinkedList & Doubly LL algorithms.

#### [MODIFY] [RecursionBacktrackingService.java](file:///c:/Users/Hp/OneDrive/Desktop/dsa-with-ui/backend/src/main/java/com/dsa/ui/service/RecursionBacktrackingService.java)
- Populate problem details & step trace generators for all 25 Recursion & Backtracking algorithms.

#### [MODIFY] [BitManipulationService.java](file:///c:/Users/Hp/OneDrive/Desktop/dsa-with-ui/backend/src/main/java/com/dsa/ui/service/BitManipulationService.java) & [BasicMathService.java](file:///c:/Users/Hp/OneDrive/Desktop/dsa-with-ui/backend/src/main/java/com/dsa/ui/service/BasicMathService.java)
- Populate problem details & step trace generators for all 18 Bit & Math algorithms.

#### [MODIFY] [StackQueueService.java](file:///c:/Users/Hp/OneDrive/Desktop/dsa-with-ui/backend/src/main/java/com/dsa/ui/service/StackQueueService.java)
- Populate problem details & step trace generators for all 30 Stack & Queue algorithms.

#### [MODIFY] [SlidingWindowService.java](file:///c:/Users/Hp/OneDrive/Desktop/dsa-with-ui/backend/src/main/java/com/dsa/ui/service/SlidingWindowService.java)
- Populate problem details & step trace generators for all 12 Sliding Window algorithms.

#### [MODIFY] [HeapService.java](file:///c:/Users/Hp/OneDrive/Desktop/dsa-with-ui/backend/src/main/java/com/dsa/ui/service/HeapService.java) & [GreedyService.java](file:///c:/Users/Hp/OneDrive/Desktop/dsa-with-ui/backend/src/main/java/com/dsa/ui/service/GreedyService.java)
- Populate problem details & step trace generators for all 32 Heap & Greedy algorithms.

#### [MODIFY] [TreeService.java](file:///c:/Users/Hp/OneDrive/Desktop/dsa-with-ui/backend/src/main/java/com/dsa/ui/service/TreeService.java)
- Populate problem details & step trace generators for all 54 Binary Tree & BST algorithms.

## Verification Plan

### Automated Tests
- Run full suite unit tests for all services via `wsl mvn test`.

### Manual Verification
- Launch backend and frontend dev servers.
- Select algorithms from each of the 11 domain modules in the UI sidebar.
- Step through visualizer steps to verify line highlights, pointer states, tree nodes, stacks, queues, and array animations.
