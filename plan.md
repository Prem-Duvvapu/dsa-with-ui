# DSA Visualizer v2: Complete Execution Tracing Architecture

## Problem Statement

The current DSA Visualizer has **71 problems implemented**, but the vast majority trace only 2-4 "summary" steps instead of the algorithm's full execution. Concrete examples from the existing codebase:

| Problem | Actual steps needed | Steps generated | What's missing |
|---|---|---|---|
| Sudoku Solver | ~200+ (53 empty cells x multiple digit attempts) | **3** | Every cell-fill attempt, validity check, backtrack |
| Permutations [1,2,3] | ~30+ (6 permutations x swap/recurse/backtrack) | **4** | Individual swap, recurse, capture, backtrack events |
| 0/1 Knapsack | ~15+ (3 items x 5 capacities = 15 DP cells) | **2** | Every DP cell computation |
| LCS | ~25+ (5x5 matrix fill) | **2** | Every cell comparison and fill |
| M-Coloring | ~15+ | **4** | Color attempt, conflict check, backtrack |
| Word Search | ~20+ | **3** | Each directional probe, match, backtrack |
| Combination Sum | ~15+ | **4** | Each branch pick, remaining target update, backtrack |

Only `SelectionSort`, `BubbleSort`, `InsertionSort`, `BFS`, `DFS`, and `NQueens` (partially) actually run the algorithm loop and emit per-iteration steps. Everything else is hand-written narrative that skips to the answer.

**The root cause**: Step generation is done by writing `new ExecutionStep(...)` calls manually, separate from the actual algorithm code. The developer writes a few "representative" steps as prose, not a faithful execution log.

---

## 1. Core Architecture for Correct Full Tracing

### 1.1 The Fundamental Design Principle

> **The algorithm itself must emit trace events as it runs. The trace IS the algorithm's execution log, not an approximation written by a human.**

This means: the Java code that solves the problem is the same code that produces the trace. There is no separate "step generator" that tries to simulate what the algorithm would do.

### 1.2 The `TraceRecorder` Callback Interface

Introduce a **listener/callback interface** that the algorithm calls at each meaningful state change. The algorithm code remains clean and readable - it just calls `recorder.record(...)` at instrumentation points.

```java
// Clean separation: algorithm logic vs. trace instrumentation
public interface TraceRecorder {
    void record(TraceEvent event);
}

// Immutable event - one per meaningful state change
public record TraceEvent(
    String operation,           // "compare", "swap", "push", "pop", "fill_cell", 
                                // "recurse_enter", "recurse_exit", "backtrack",
                                // "mark_visited", "enqueue", "dequeue", "memo_hit",
                                // "place", "remove", "found_solution", "prune"
    int codeLine,               // Line number in the displayed Java code
    String description,         // Human-readable: "Compare arr[2]=24 < arr[5]=9: true, update mini=5"
    Map<String, String> variables,  // Current variable values: {"i":"2", "mini":"5", "arr[mini]":"9"}
    Object snapshot              // Deep-cloned data structure state at this moment
) {}
```

### 1.3 How the Instrumented Algorithm Looks

The algorithm reads like real interview code, with minimal `recorder.record()` calls at decision points. Example for Selection Sort:

```java
public class SelectionSortTraced {
    
    public void solve(int[] arr, TraceRecorder recorder) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int mini = i;
            for (int j = i + 1; j < n; j++) {
                boolean smaller = arr[j] < arr[mini];
                recorder.record(new TraceEvent("compare", 5,
                    String.format("Compare arr[%d]=%d < arr[%d]=%d -> %s",
                        j, arr[j], mini, arr[mini], smaller),
                    Map.of("i", str(i), "j", str(j), "mini", str(mini)),
                    cloneArray(arr)));
                if (smaller) mini = j;
            }
            if (mini != i) {
                swap(arr, i, mini);
                recorder.record(new TraceEvent("swap", 8,
                    String.format("Swap arr[%d] <-> arr[%d]", i, mini),
                    Map.of("i", str(i), "mini", str(mini)),
                    cloneArray(arr)));
            }
        }
    }
}
```

**Key principle**: The algorithm code itself is clean - `recorder.record()` is no more intrusive than a logging statement. Removing all `record()` calls would leave you with valid, interview-quality Java.

### 1.4 How the Trace is Collected and Served

```
+------------------------------------------------------+
|  Frontend: GET /api/{category}/execute/{problemId}   |
+---------------------------+--------------------------+
                            |
                            v
+------------------------------------------------------+
|  Controller -> Service.generateSteps(problemId)      |
|                                                      |
|  1. Create ListTraceRecorder (implements recorder)   |
|  2. Create input (small, visualization-friendly)     |
|  3. Run algorithm: algo.solve(input, recorder)       |
|  4. Convert recorder.getEvents() -> List<ExecStep>   |
|  5. Return JSON to frontend                          |
+------------------------------------------------------+
```

The `ListTraceRecorder` simply collects events into an `ArrayList<TraceEvent>`. After the algorithm finishes (full run, every step), the service converts the events into the frontend's `ExecutionStep` format.

**This is the critical difference from v1**: In v1, the developer writes `steps.add(new ExecutionStep(...))` manually, trying to narrate what the algorithm would do. In v2, the algorithm runs and the trace is the output.

### 1.5 The `ExecutionStep` Data Structure (v2 - Enhanced)

The existing `ExecutionStep` model is mostly fine, but needs enhancements to support richer tracing:

```java
public class ExecutionStep {
    int stepNumber;                      // Sequential 1-indexed
    int activeLine;                      // Line in displayed Java code being executed
    String operation;                    // NEW: "compare", "swap", "recurse_enter", etc.
    String description;                  // Human-readable narrative of what just happened
    List<String> queueOrStackState;      // Queue/Stack contents
    Map<Integer, String> nodeStates;     // Graph/tree node -> visual state
    List<String> activeEdges;            // Currently highlighted edges
    Map<String, String> variables;       // All relevant variable values
    String dsType;                       // "Array", "Stack", "Queue", "Matrix", etc.
    int[][] gridState;                   // 2D grid (DP tables, maze, sudoku, etc.)
    List<ArrayElement> arrayState;       // Array with element states
    List<ListNode> listState;            // Linked list nodes
    List<TreeNode> treeState;            // Tree/recursion tree nodes  
    List<TrieNodeModel> trieState;       // Trie nodes
    List<String> callStack;             // NEW: explicit recursion call stack
    Map<String, Integer> highlightCells; // NEW: for grid - which cell(s) are active
}
```

### 1.6 Full Completion Guarantee

> **The tracer runs the algorithm to complete termination.** There is no step limit, no truncation, no "representative subset."

- The algorithm's `solve()` method returns normally after full execution.
- The `ListTraceRecorder` captures every event - 15 steps or 500 steps, it doesn't matter.
- The REST API returns the **complete** list.
- The frontend's player supports the full sequence: play/pause, step-forward, step-back, speed slider, jump-to-step scrubber, reset. The progress bar and step counter already exist in the current `Controls.jsx` and work correctly - they just need a longer step list to traverse.

---

## 2. Topic and Pattern Breakdown (Striver A2Z Sheet)

For each pattern, I list representative problems to build first. The first problem in each group is the **prototype candidate** (most useful for validating that pattern's tracing).

### 2.1 Arrays and Hashing
| # | Problem | Difficulty | Key Operation to Trace |
|---|---------|-----------|----------------------|
| 1 | Two Sum (HashMap) | Easy | Map put/get, found pair |
| 2 | Sort 0s 1s 2s (Dutch National Flag) | Medium | Pointer movement, swap |
| 3 | Kadane's Algorithm | Medium | maxSoFar/maxEndingHere update |

### 2.2 Sorting
| # | Problem | Difficulty | Key Operation to Trace |
|---|---------|-----------|----------------------|
| 1 | Merge Sort | Medium | Split, recursive call tree, merge comparisons |
| 2 | Quick Sort | Medium | Partition pivot selection, pointer sweep, swap |
| 3 | Selection Sort | Easy | Min scan, swap (already well-traced in v1) |

### 2.3 Binary Search
| # | Problem | Difficulty | Key Operation to Trace |
|---|---------|-----------|----------------------|
| 1 | Search in Rotated Sorted Array | Medium | mid calc, which-half decision |
| 2 | Koko Eating Bananas (BS on answer) | Medium | Feasibility check per mid value |
| 3 | Find Peak Element | Medium | Slope comparison, half elimination |

### 2.4 Recursion and Backtracking
| # | Problem | Difficulty | Key Operation to Trace |
|---|---------|-----------|----------------------|
| 1 | **N-Queens (4x4)** | Hard | Place queen, safety check, recurse, backtrack |
| 2 | Subsets (Power Set) | Medium | Pick/non-pick decision tree |
| 3 | Sudoku Solver (9x9) | Hard | Cell fill, row/col/box validity, backtrack |

### 2.5 Linked Lists
| # | Problem | Difficulty | Key Operation to Trace |
|---|---------|-----------|----------------------|
| 1 | Reverse Linked List | Easy | Pointer rewiring (prev, curr, next) |
| 2 | Detect Cycle (Floyd's) | Medium | Slow/fast pointer movement |
| 3 | Merge Two Sorted Lists | Easy | Comparison, pointer splice |

### 2.6 Stacks, Queues and Monotonic Structures
| # | Problem | Difficulty | Key Operation to Trace |
|---|---------|-----------|----------------------|
| 1 | Next Greater Element (Monotonic Stack) | Medium | Push, pop, assign result |
| 2 | Valid Parentheses | Easy | Push open, pop and match close |
| 3 | Sliding Window Maximum (Monotonic Deque) | Hard | Deque front/back operations |

### 2.7 Binary Trees and BST
| # | Problem | Difficulty | Key Operation to Trace |
|---|---------|-----------|----------------------|
| 1 | Inorder Traversal | Easy | Recurse left, visit, recurse right |
| 2 | Maximum Depth | Easy | Recursive depth comparison |
| 3 | Lowest Common Ancestor | Medium | Path divergence |

### 2.8 Heaps and Priority Queues
| # | Problem | Difficulty | Key Operation to Trace |
|---|---------|-----------|----------------------|
| 1 | Kth Largest Element | Medium | Heap insert, extract-min, size check |
| 2 | Merge K Sorted Lists | Hard | PQ poll, insert next node |
| 3 | Top K Frequent Elements | Medium | Frequency map + heap |

### 2.9 Graphs (BFS/DFS)
| # | Problem | Difficulty | Key Operation to Trace |
|---|---------|-----------|----------------------|
| 1 | BFS Traversal | Easy | Enqueue, dequeue, visit, mark |
| 2 | Number of Islands (grid BFS/DFS) | Medium | Grid cell visit, 4-directional expansion |
| 3 | Detect Cycle in Directed Graph | Medium | DFS color states (white/gray/black) |

### 2.10 Advanced Graphs
| # | Problem | Difficulty | Key Operation to Trace |
|---|---------|-----------|----------------------|
| 1 | Dijkstra's Algorithm | Medium | PQ extract-min, edge relaxation |
| 2 | Topological Sort (Kahn's BFS) | Medium | In-degree decrement, enqueue |
| 3 | Kruskal's MST (Union-Find) | Hard | Sort edges, union, find with path compression |

### 2.11 Dynamic Programming
| # | Problem | Difficulty | Key Operation to Trace |
|---|---------|-----------|----------------------|
| 1 | **0/1 Knapsack (2D table)** | Medium | Cell-by-cell DP fill, include/exclude decision |
| 2 | Longest Common Subsequence | Medium | Character match vs mismatch, cell fill |
| 3 | Climbing Stairs | Easy | dp[i] = dp[i-1] + dp[i-2] fill |

### 2.12 Tries
| # | Problem | Difficulty | Key Operation to Trace |
|---|---------|-----------|----------------------|
| 1 | Implement Trie | Medium | Node creation, character traversal |
| 2 | Word Break (Trie-based) | Medium | Prefix matching, DP memoization |
| 3 | Longest Common Prefix | Easy | Character-by-character comparison |

### 2.13 Greedy
| # | Problem | Difficulty | Key Operation to Trace |
|---|---------|-----------|----------------------|
| 1 | N Meetings in One Room | Easy | Sort by end time, greedy selection |
| 2 | Job Sequencing | Medium | Deadline slot allocation |
| 3 | Jump Game | Medium | Reachability update |

---

## 3. Visualization Design Per Pattern

### 3.1 Visual Elements by Data Structure

| Data Structure | Visual Representation | State Cues |
|---|---|---|
| **Array** | Horizontal bar chart with index labels | `comparing` (yellow border), `swapping` (orange pulse), `sorted` (green), `active` (blue highlight), `pivot` (red) |
| **Linked List** | Horizontal chain of nodes with arrows | `current` (blue glow), `prev` (gray), `next` (dashed arrow preview), `cycle-detected` (red loop arrow) |
| **Stack** | Vertical LIFO column (top = top of stack) | `push` (slide-in from top, green), `pop` (slide-out, red), `peek` (glow top) |
| **Queue** | Horizontal FIFO row (enqueue right, dequeue left) | `enqueue` (slide-in right, green), `dequeue` (slide-out left, blue), `front` (glow) |
| **Binary Tree** | SVG tree with edges, positioned nodes | `visiting` (blue pulse), `visited` (green), `target` (gold), `backtracking` (red fade) |
| **Recursion Call Tree** | SVG tree mirroring call stack depth | `active_call` (bright node), `returned` (dimmed), `pruned` (strikethrough/red) |
| **Graph** | SVG with nodes + edges | `unvisited` (gray), `in_queue/stack` (yellow), `visiting` (blue pulse), `visited` (green), `cycle_edge` (red dashed) |
| **2D Grid / Matrix** | Table of cells | `current_cell` (blue), `filled` (green), `conflict` (red), `backtracked` (gray strikethrough), `path` (gold) |
| **DP Table** | Numbered grid with cell values | `computing` (yellow), `computed` (green), `reading_from` (blue arrow), `optimal` (gold border) |
| **Trie** | Tree with character-labeled edges | `traversing` (blue), `inserted` (green), `end_of_word` (gold dot) |
| **Heap** | Array bar chart + tree overlay | `sifting_up` (orange arrow up), `sifting_down` (orange arrow down), `extracted` (red) |

### 3.2 Operation Visual Cues

| Operation | Visual Cue |
|---|---|
| **Comparison** | Both elements get yellow highlight + "?" icon; then green check or red X |
| **Swap** | Elements animate position exchange with crossing arc |
| **Recursive Enter** | New node appears in call tree, call stack pushes frame |
| **Recursive Exit** | Call tree node dims, stack frame pops |
| **Backtrack** | Call tree node turns red, undone state change animates reversal |
| **Memoization Hit** | Cell flashes gold with "CACHE HIT" badge, skip arrow shown |
| **DP Cell Fill** | Cell transitions from empty to computing (yellow) to filled (green) with value |
| **Queue Enqueue** | Element slides into right end with green glow |
| **Queue Dequeue** | Element slides out from left end with blue glow |
| **Graph Edge Relaxation** | Edge flashes, distance label updates with animation |
| **Pruning** | Subtree/branch grays out with "PRUNED" overlay |

### 3.3 Synchronized Code Highlighting

Every `ExecutionStep` carries an `activeLine` field. The `CodeViewer` component (already exists) highlights that line in the displayed Java code. This creates the effect of a debugger stepping through the code in sync with the data structure animation.

---

## 4. Code Quality Requirements

### 4.1 Separation of Algorithm Logic from Trace Instrumentation

```
com.dsa.ui/
  algorithm/                    # Pure algorithm implementations
    sorting/
      SelectionSort.java        # Clean algorithm + recorder.record() calls
      MergeSort.java
      QuickSort.java
    graph/
      BfsTraversal.java
      DijkstraShortestPath.java
    dp/
      Knapsack01.java
      LongestCommonSubsequence.java
    backtracking/
      NQueens.java
      SudokuSolver.java
    ...
  trace/                        # Tracing infrastructure
    TraceRecorder.java          # Interface
    ListTraceRecorder.java      # Collects events into a list
    TraceEvent.java             # Single trace event record
    SnapshotUtil.java           # Deep-clone utilities for arrays, grids, etc.
  model/                        # Existing models (ExecutionStep, ProblemDetail, etc.)
  service/                      # Services that wire algorithm + recorder + input
    SortingService.java         # Creates input, runs algo, converts trace to steps
    ...
  controller/                   # REST endpoints (unchanged pattern)
```

### 4.2 Algorithm Code Standards

Every algorithm class follows this structure:

```java
/**
 * Problem: N-Queens (LeetCode 51)
 * 
 * Place N non-attacking queens on an NxN chessboard.
 * No two queens share the same row, column, or diagonal.
 * 
 * Approach: Column-by-column backtracking with O(1) safety check
 * using three boolean arrays (leftRow, lowerDiag, upperDiag).
 * 
 * Time:  O(N!) - at most N choices for col 0, N-1 for col 1, etc.
 * Space: O(N^2) for the board + O(N) recursion stack depth.
 */
public class NQueens {

    public List<List<String>> solve(int n, TraceRecorder recorder) {
        // ... clean, interview-quality Java code
        // ... with recorder.record() at each decision point
    }
}
```

- Algorithm logic is **readable without understanding the trace system** - `recorder.record()` calls are as unobtrusive as log statements.
- Removing all `recorder.record()` calls yields valid, compilable, interview-ready code.
- Each file includes: Javadoc with problem statement, approach summary, and complexity.

### 4.3 Problem Module Structure

Every problem is described by a `ProblemDetail` (metadata) + an algorithm class (traced implementation). The service wires them together:

```java
// In SortingService.java
public List<ExecutionStep> generateSteps(String problemId) {
    switch (problemId) {
        case "selection-sort": {
            int[] input = {13, 46, 24, 52, 20, 9};
            ListTraceRecorder recorder = new ListTraceRecorder();
            new SelectionSort().solve(input, recorder);
            return recorder.toExecutionSteps("Array");
        }
        // ...
    }
}
```

---

## 5. Complexity Explanation Requirements

### 5.1 Enhanced `ComplexityDetail` Model

The existing model has `timeComplexity`, `timeExplanation`, `timeWhy`, `spaceComplexity`, `spaceExplanation`, `spaceWhy`. This is already solid. Proposed additions:

```java
public class ComplexityDetail {
    // Existing fields (keep all)
    String timeComplexity;       // "O(N^2)"
    String timeExplanation;      // "Outer loop N-1 times, inner loop N-i-1 times..."
    String timeWhy;              // "Why O(N^2) always? Selection Sort always scans..."

    String spaceComplexity;      // "O(1)"
    String spaceExplanation;
    String spaceWhy;

    String auxiliarySpace;
    String dataStructureSpace;

    // NEW fields
    String bruteForceComplexity;     // "O(N^3)" (if applicable)
    String bruteForceExplanation;    // "Brute force checks all triplets..."
    String optimizationInsight;      // "How optimization is visible in the trace: 
                                     //  DP memoization reduces fib(5) from 15 calls to 9"
    int expectedTraceSteps;          // Approximate step count for the default input, so
                                     // the user can correlate theory with observed trace length
}
```

### 5.2 Tying Complexity to the Trace

Every complexity explanation must reference what the user will **see** in the trace:

> **Example (Merge Sort)**:
> - Time: O(N log N). "You will see log2(6) = 3 levels of recursive splitting in the call tree. At each level, the merge phase compares and places all N elements. Total: N x 3 = 18 merge comparisons in the trace."
> - Brute force comparison: "Bubble Sort on the same input produces ~15 comparison steps (N^2/2). Merge Sort produces ~18 merge-comparison steps but completes in 3 levels instead of 5 passes - the trace visually shows the divide-and-conquer advantage."

### 5.3 Where Brute Force vs. Optimized Matters

| Problem | Brute Force | Optimized | What the trace shows |
|---|---|---|---|
| Fibonacci | O(2^N) - 15 calls for fib(5) | O(N) with memo - 9 calls | Memoized trace skips repeated subtrees (gold "CACHE HIT" cells) |
| 0/1 Knapsack | O(2^N) subset enumeration | O(NxW) DP table | DP trace fills table left-to-right, no redundant recomputation |
| Two Sum | O(N^2) nested loops | O(N) hash map | Hash map trace shows single-pass with O(1) lookups |

---

## 6. Architecture Proposal

### 6.1 Existing Stack (Keep)

| Layer | Technology | Status |
|---|---|---|
| **Backend** | Spring Boot 3.2 + Java 17 | Keep |
| **Frontend** | React 18 + Vite | Keep |
| **Styling** | Custom CSS (glassmorphism dark mode) | Keep |
| **Visualization** | Inline SVG (GraphCanvas, TreeCanvas, etc.) | Keep |
| **Build/Deploy** | Docker Compose | Keep |

### 6.2 Execution Model: Precomputed JSON Trace

The backend **precomputes the full trace** on each `GET /api/{category}/execute/{id}` request. This is the right choice because:

1. Inputs are intentionally small (N=4 to 9) for visualization clarity - trace generation takes less than 100ms.
2. The frontend can freely seek forward/backward without re-executing.
3. No WebSocket complexity or live execution coordination needed.
4. The existing API contract and frontend player logic remain unchanged.

### 6.3 Reusable "Problem Module" Structure

Every problem follows this consistent pattern:

```
Backend:
  algorithm/{category}/{ProblemName}.java    - Traced algorithm implementation
  service/{Category}Service.java             - Wires input + algorithm + recorder
  model/ProblemDetail                        - Metadata (reuse existing)
  model/ExecutionStep                        - Trace step (reuse existing, enhanced)
  controller/{Category}Controller.java       - REST endpoints (reuse existing)

Conceptual mapping per problem:
  +-----------------------------------------------------+
  |  ProblemDetail (metadata)                            |
  |    - id, title, category, difficulty                 |
  |    - description (problem statement)                 |
  |    - javaCode (displayed in CodeViewer)              |
  |    - complexity (ComplexityDetail)                    |
  |    - defaultInput (graph nodes, array, grid, etc.)   |
  |    - dsType (determines which canvas renders)        |
  |                                                      |
  |  Algorithm class (traced implementation)              |
  |    - solve(input, TraceRecorder)                     |
  |    - Produces complete trace via recorder callbacks   |
  |                                                      |
  |  Service (orchestrator)                              |
  |    - Creates input, runs algorithm, returns steps    |
  +-----------------------------------------------------+
```

### 6.4 Frontend Architecture (Minimal Changes)

The frontend architecture is already well-structured. Required changes:

1. **Controls.jsx** - Add a step scrubber/slider for jumping to any step (currently only prev/next buttons). Already has progress bar.
2. **CodeViewer.jsx** - Already highlights `activeLine`. No changes needed.
3. **Canvas components** - Already render from step state. May need minor enhancements for new visual cues (e.g., "backtrack" state coloring).
4. **App.jsx** - Playback logic already works with arbitrary-length step arrays. No changes needed.

---

## 7. Build Sequencing

### 7.1 Phase 0: Infrastructure (Do First)

Build the tracing infrastructure that all problems will use:

1. `TraceRecorder` interface
2. `ListTraceRecorder` implementation
3. `TraceEvent` record
4. `SnapshotUtil` (deep clone helpers for arrays, 2D grids, lists)
5. Enhanced `ExecutionStep` model (add `operation`, `callStack`, `highlightCells` fields - backward-compatible additions)

### 7.2 Phase 1: End-to-End Prototype - N-Queens (4x4)

**Why N-Queens as the prototype**: It is the hardest tracing case because it involves:
- Recursive call tree growth
- Safety constraint checking (3 hash arrays)
- Queen placement (grid mutation)
- Backtracking (grid reversal + hash array reset)
- Multiple solutions (must trace the algorithm finding ALL solutions, not just one)
- Non-trivial step count: 4x4 N-Queens explores ~40-60 steps to find both solutions

**If N-Queens traces correctly and completely, every simpler pattern (sorting, DP table fill, BFS queue) is guaranteed to work with the same infrastructure.**

Deliverable:
- `algorithm/backtracking/NQueens.java` - Full traced implementation
- Updated `RecursionBacktrackingService.java` - Wires NQueens with recorder
- Frontend verifies: all ~50 steps render, grid updates, call stack shows, code line highlights, play/pause/step all work

### 7.3 Phase 2: One Problem Per Pattern (Validate Coverage)

After N-Queens is approved, build one problem from each remaining pattern to prove the tracing infra handles every data structure type:

| Order | Problem | Why This One |
|---|---|---|
| 1 | 0/1 Knapsack (2D DP) | Validates 2D grid cell-by-cell fill |
| 2 | Merge Sort | Validates recursion tree + array merge animation |
| 3 | BFS Traversal | Validates queue + graph node states (already partially done, upgrade to recorder pattern) |
| 4 | Reverse Linked List | Validates linked list pointer animation |
| 5 | Search in Rotated Sorted Array | Validates binary search pointer narrowing |
| 6 | Kth Largest Element | Validates heap operations |
| 7 | Implement Trie | Validates trie node insertion |
| 8 | N Meetings in One Room | Validates greedy selection |
| 9 | Next Greater Element | Validates monotonic stack |

### 7.4 Phase 3: Bulk Migration

Migrate remaining 60+ problems to the recorder pattern. Each migration is mechanical:
1. Extract the algorithm logic into an `algorithm/` class
2. Add `recorder.record()` calls at each decision point
3. Remove the hand-written `generateXxxSteps()` method from the service
4. Wire the new algorithm class in the service's `generateSteps()` switch

### 7.5 Phase 4: Polish

- Add step scrubber/slider to `Controls.jsx`
- Add "jump to step" input field
- Add keyboard shortcuts (left/right arrow for step, space for play/pause)
- Add step filtering (e.g., "show only swaps" or "show only recursive calls")

---

## 8. Open Questions and Edge Cases

### 8.1 Step Count Limits for Large Inputs

| Problem | Input Size | Approx Steps | Concern? |
|---|---|---|---|
| Selection Sort (N=6) | 6 elements | ~25 | Fine |
| Merge Sort (N=6) | 6 elements | ~30 | Fine |
| BFS (7 nodes) | 7V, 8E | ~20 | Fine |
| N-Queens (N=4) | 4x4 | ~50 | Fine |
| Sudoku Solver (9x9) | 53 empty cells | ~200-400 | Many steps |
| Permutations (N=4) | 4 elements | ~100+ | Many steps |

**Proposed handling**:
- Use **intentionally small inputs** for visualization (N=4 for queens, N=6 for sorting, 4x4 for DP, 7 nodes for graphs). Document this as: *"Small inputs for understanding the algorithm's mechanics, not performance benchmarking."*
- For problems like Sudoku where even the standard input (9x9) produces hundreds of steps: use a **partially-filled board** (e.g., only 10-15 empty cells) to keep steps under ~80.
- For Permutations: use N=3 (6 permutations, ~30 steps) instead of N=4.
- The UI already supports arbitrary step counts (play/pause/scrub), so even 200+ steps are navigable. The concern is cognitive overload, not technical limitation.

### 8.2 Backward Compatibility

> [!IMPORTANT]
> The existing 71 problems must continue to work during the migration. The new `TraceRecorder` infrastructure is **additive** - existing hand-written step generators will still compile and run. Migration from hand-written to traced will be done problem-by-problem, with the service's `generateSteps()` switch statement pointing to either the old or new implementation.

### 8.3 Should the Frontend Send Input to the Backend?

Currently, inputs are hardcoded in the service (e.g., `int[] arr = {13, 46, 24, 52, 20, 9}`). A future enhancement could let the user choose/edit input, and the backend traces it dynamically.

**Recommendation**: Defer this to a later phase. Hardcoded small inputs are sufficient for learning purposes and keep the system simple. The architecture supports dynamic input naturally (the `solve()` method takes input as a parameter), so this can be added later without refactoring.

### 8.4 Performance Concern for Very Large Traces

If a trace produces 500+ steps, the JSON payload could be large (each step includes a full data structure snapshot).

**Mitigation**:
- Snapshots use primitive types (int arrays, 2D int arrays) which serialize efficiently.
- For the recommended small inputs, payloads will be under 500KB even with 200 steps.
- If needed later: implement delta-based snapshots (only send what changed), but this is premature optimization for now.

---

## Verification Plan

### Automated Tests
- `mvn test` - JUnit tests for each traced algorithm verifying:
  - Step count matches expected range for the given input
  - First step and last step contain correct initial/final state
  - Every step has a non-null description, valid activeLine, and non-null snapshot
  - The final snapshot matches the algorithm's expected output

### Manual Verification
- For the N-Queens prototype: manually step through all ~50 steps in the UI, verifying:
  - Grid state matches expected queen placements at each step
  - Call stack shows correct recursion depth
  - Code viewer highlights the correct line
  - Backtrack steps correctly remove queens and reset the grid
  - Both solutions are found (the algorithm doesn't stop after the first)

---

## Summary of What Changes vs. What Stays

| Component | Change? | Details |
|---|---|---|
| `TraceRecorder`, `TraceEvent`, `ListTraceRecorder` | **NEW** | Core tracing infrastructure |
| `algorithm/` package | **NEW** | Traced algorithm implementations |
| `SnapshotUtil` | **NEW** | Deep clone utilities |
| `ExecutionStep` model | **MODIFY** | Add `operation`, `callStack`, `highlightCells` fields |
| `ComplexityDetail` model | **MODIFY** | Add brute-force comparison fields |
| Service classes | **MODIFY** | Rewire `generateSteps()` to use recorder pattern |
| Controller classes | **NO CHANGE** | Same REST API contract |
| Frontend components | **MINOR** | Add step scrubber; visual cue enhancements |
| `App.jsx` playback logic | **NO CHANGE** | Already handles arbitrary step arrays |
| CSS / design system | **NO CHANGE** | Keep existing glassmorphism dark theme |
