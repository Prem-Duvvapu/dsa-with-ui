package com.dsa.ui.service;

import com.dsa.ui.algorithm.backtracking.*;
import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import com.dsa.ui.trace.ListTraceRecorder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RecursionBacktrackingService implements ProblemProvider {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public RecursionBacktrackingService() {
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
            // Every id below now has a real tracer (tracer/impl). Refuse rather than let
            // default: serve an unrelated legacy generator's steps under these ids.
            case "n-queens":
            case "sudoku-solver":
            case "subsets-i":
            case "combination-sum-i":
            case "rat-in-a-maze":
            case "m-coloring":
            case "palindrome-partitioning":
            case "permutations":
            case "word-search":
            case "atoi-recursive":
            case "pow-x-n-recursive":
            case "count-good-numbers":
            case "sort-stack-recursion":
            case "reverse-stack-recursion":
            case "generate-binary-strings":
            case "generate-parentheses":
            case "power-set":
            case "subsequences-patterns-theory":
            case "count-subsequences-sum-k":
            case "check-subsequence-sum-k":
            case "combination-sum-2":
            case "subsets-2":
            case "combination-sum-3":
            case "letter-combinations-phone":
            case "word-break":
                throw new LegacyTraceRetiredException(problemId);
            default: return generateNQueensSteps();
        }
    }

    private void initProblems() {
        // 1. N-Queens Problem
        problems.put("n-queens", new ProblemDetail(
            "n-queens", "N-Queens Problem", "Recursion - Hard", "Recursion & Backtracking", "Hard",
            "Place N non-attacking queens on an N x N chessboard such that no two queens attack each other.",
            """
            // Java N-Queens Backtracking with Hash Arrays (LeetCode 51)
            public List<List<String>> solveNQueens(int n) {
                List<List<String>> ans = new ArrayList<>();
                char[][] board = new char[n][n];
                for (int i = 0; i < n; i++) Arrays.fill(board[i], '.');
                solve(0, board, ans, new int[n], new int[2*n-1], new int[2*n-1], n);
                return ans;
            }
            """,
            null, null, createNQueensTreeNodes(), null, null, null, createEmpty4x4Grid(),
            new ComplexityDetail("O(N!)", "Time Complexity: N choices for first queen, N-2 for second, etc.", "Backtracking N!", "O(N^2)", "Space Complexity: Board storage O(N^2) + Hash arrays O(N).", "Grid & Hash Arrays", "Auxiliary Space: O(N)", "Memory"), "Matrix"
        ));

        // 2. Rat in a Maze
        problems.put("rat-in-a-maze", new ProblemDetail(
            "rat-in-a-maze", "Rat in a Maze", "Recursion - Hard", "Recursion & Backtracking", "Medium",
            "Find all possible paths for a rat from (0,0) to (N-1,N-1) in a 2D binary grid using directions D, L, R, U.",
            """
            // Java Rat in a Maze (Striver A2Z Sheet)
            public ArrayList<String> findPath(int[][] m, int n) {
                ArrayList<String> ans = new ArrayList<>();
                if (m[0][0] == 1) solve(0, 0, m, n, ans, "", new int[n][n]);
                return ans;
            }
            """,
            null, null, createRatTreeNodes(), null, null, null, createMazeGrid(),
            new ComplexityDetail("O(4^(N*N))", "Time Complexity: At each cell, 4 directional choices.", "Grid DFS", "O(N*N)", "Space Complexity: Visited array + Recursion stack depth.", "Call Stack", "Auxiliary Space: O(N*N)", "Memory"), "Matrix"
        ));

        // 3. Sudoku Solver
        problems.put("sudoku-solver", new ProblemDetail(
            "sudoku-solver", "Sudoku Solver", "Recursion - Hard", "Recursion & Backtracking", "Hard",
            "Solve a Sudoku puzzle by filling empty cells ('.') with digits '1'-'9'.",
            """
            // Java Sudoku Solver (LeetCode 37)
            public void solveSudoku(char[][] board) { solve(board); }
            """,
            null, null, createSudokuTreeNodes(), null, null, null, createSudokuGrid(),
            new ComplexityDetail("O(9^(81))", "Time Complexity: Worst case tries 9 digits per cell.", "Backtracking", "O(1)", "Space Complexity: In-place 9x9 board modification.", "Memory", "Auxiliary Space: O(81)", "Memory"), "Matrix"
        ));

        // 4. M-Coloring
        problems.put("m-coloring", new ProblemDetail(
            "m-coloring", "M-Coloring Problem", "Recursion - Hard", "Recursion & Backtracking", "Medium",
            "Color N vertices with M colors such that no adjacent vertices share color.",
            """
            // Java M-Coloring
            public boolean graphColoring(boolean[][] graph, int m, int n) {
                return solve(0, graph, new int[n], m, n);
            }
            """,
            null, null, createColoringTreeNodes(), null, null, null, null,
            new ComplexityDetail("O(M^N)", "Time Complexity: M choices for each of N nodes.", "Backtracking", "O(N)", "Space Complexity: Color array and recursion stack.", "Memory", "Auxiliary Space: O(N)", "Memory"), "Graph"
        ));

        // 5. Palindrome Partitioning
        problems.put("palindrome-partitioning", new ProblemDetail(
            "palindrome-partitioning", "Palindrome Partitioning", "Recursion - Hard", "Recursion & Backtracking", "Medium",
            "Partition string s such that every substring is a palindrome.",
            """
            // Java Palindrome Partitioning (LeetCode 131)
            public List<List<String>> partition(String s) {
                List<List<String>> res = new ArrayList<>();
                solve(0, s, new ArrayList<>(), res);
                return res;
            }
            """,
            null, null, createPalindromeTreeNodes(), null, null, null, null,
            new ComplexityDetail("O(2^N * N)", "Time Complexity: 2^(N-1) partition cuts * O(N) palindrome check.", "Backtracking", "O(N)", "Space Complexity: Call stack depth.", "Memory", "Auxiliary Space: O(N)", "Memory"), "Stack"
        ));

        // 6. Subsets
        problems.put("subsets-i", new ProblemDetail(
            "subsets-i", "Subsets / Subset Sums", "Recursion - Subsequences", "Recursion & Backtracking", "Medium",
            "Generate all possible subsets (the power set) of a unique integer array.",
            """
            // Java Subsets (LeetCode 78)
            public List<List<Integer>> subsets(int[] nums) {
                List<List<Integer>> ans = new ArrayList<>();
                solve(0, nums, new ArrayList<>(), ans);
                return ans;
            }
            """,
            null, null, createSubsetsTreeNodes(), null, null, null, null,
            new ComplexityDetail("O(2^N)", "Time Complexity: 2 choices per element (Pick / Non-Pick).", "Binary Tree Recursion", "O(N)", "Space Complexity: Recursion stack depth.", "Call Stack", "Auxiliary Space: O(N)", "Memory"), "Stack"
        ));

        // Bulk register remaining 19 recursion algorithms
        populateRemainingRecursionProblems();
    }

    /**
     * The bulk-registered ids previously hardcoded {@code "Stack"} regardless of what each
     * tracer actually emits — the same category of gap {@code BitManipulationService.bulkDsType}
     * and {@code StringService.bulkDsType} had before their own batches fixed it. Ids not
     * listed here keep reporting {@code Stack} unchanged, which matches their tracer's real
     * include/exclude-path visualization.
     */
    private static DsType bulkDsType(String id) {
        return switch (id) {
            case "atoi-recursive", "generate-binary-strings", "generate-parentheses",
                    "letter-combinations-phone", "word-break" -> DsType.STRING;
            case "count-good-numbers", "pow-x-n-recursive" -> DsType.BITS;
            case "permutations" -> DsType.ARRAY;
            case "word-search" -> DsType.MATRIX;
            default -> DsType.STACK;
        };
    }

    private void populateRemainingRecursionProblems() {
        String[][] list = new String[][]{
            {"combination-sum-i", "Combination Sum I", "Recursion - Subsequences", "Medium", "Find combinations reaching target sum with infinite element reuse."},
            {"permutations", "Permutations of Array / String", "Recursion - Hard", "Medium", "Generate all N! arrangements using swapping backtracking."},
            {"word-search", "Word Search in 2D Board", "Recursion - Hard", "Medium", "Search word in 2D board using 4-directional DFS."},
            {"atoi-recursive", "Recursive Implementation of atoi()", "Recursion - Basics", "Easy", "Convert string to 32-bit integer recursively."},
            {"pow-x-n-recursive", "Pow(x, n) Recursive", "Recursion - Basics", "Medium", "Calculate x^n using Binary Exponentiation recursion O(log N)."},
            {"count-good-numbers", "Count Good Numbers", "Recursion - Basics", "Medium", "Count good digit strings using modular exponentiation."},
            {"sort-stack-recursion", "Sort a Stack Using Recursion", "Recursion - Basics", "Medium", "Sort stack elements in-place using recursion without loops."},
            {"reverse-stack-recursion", "Reverse a Stack Using Recursion", "Recursion - Basics", "Medium", "Reverse stack elements using recursion call stack."},
            {"generate-binary-strings", "Generate Binary Strings Without Consecutive 1s", "Recursion - Subsequences", "Medium", "Generate binary strings avoiding '11' substrings."},
            {"generate-parentheses", "Generate Parentheses", "Recursion - Subsequences", "Medium", "Generate N pairs of valid well-formed parentheses."},
            {"power-set", "Power Set", "Recursion - Subsequences", "Medium", "Generate all 2^N subsequences using bit manipulation or recursion."},
            {"subsequences-patterns-theory", "Learn All Patterns of Subsequences", "Recursion - Subsequences", "Easy", "Pick / Non-Pick recursive pattern theory."},
            {"count-subsequences-sum-k", "Count Subsequences With Sum K", "Recursion - Subsequences", "Medium", "Count total subsequences whose sum equals K."},
            {"check-subsequence-sum-k", "Check if Subsequence With Sum K Exists", "Recursion - Subsequences", "Medium", "Return true if any subsequence sums to K."},
            {"combination-sum-2", "Combination Sum II", "Recursion - Subsequences", "Medium", "Find unique combinations with array containing duplicates (use each once)."},
            {"subsets-2", "Subsets II", "Recursion - Subsequences", "Medium", "Generate unique subsets from array containing duplicates."},
            {"combination-sum-3", "Combination Sum III", "Recursion - Subsequences", "Medium", "Find combinations of K numbers (1-9) that add up to N."},
            {"letter-combinations-phone", "Letter Combinations of a Phone Number", "Recursion - Subsequences", "Medium", "Map digit keypad string to letter combinations."},
            {"word-break", "Word Break", "Recursion - Hard", "Medium", "Check if string can be segmented into dictionary words."}
        };

        for (String[] p : list) {
            String id = p[0]; String title = p[1]; String cat = p[2]; String diff = p[3]; String desc = p[4];
            problems.put(id, new ProblemDetail(
                id, title, cat, "Recursion & Backtracking", diff, desc,
                String.format("// Java Implementation for %s\npublic void solve() {\n    // Backtracking Striver A2Z Implementation\n}", title),
                null, null, createSubsetsTreeNodes(), null, null, null, null,
                new ComplexityDetail("O(2^N)", "Time Complexity: Exponential backtracking tree exploration.", "Recursion", "O(N)", "Space Complexity: Recursion stack depth.", "Call Stack", "Auxiliary Space: O(N)", "Memory"), bulkDsType(id).wireValue()
            ));
        }
    }

    // Step Generators
    private List<ExecutionStep> generateNQueensSteps() {
        ListTraceRecorder recorder = new ListTraceRecorder();
        new NQueens().solve(4, recorder);
        return recorder.toExecutionSteps();
    }

    // Helper Recursion Tree Nodes
    private List<TreeNode> createNQueensTreeNodes() {
        return List.of(
            new TreeNode(1, "Q(col=0)", 190, 40, 2, 3, "unvisited"),
            new TreeNode(2, "row=0", 100, 110, 4, 5, "unvisited"),
            new TreeNode(3, "row=1", 280, 110, 6, 7, "unvisited"),
            new TreeNode(4, "Q(col=1)", 60, 180, null, null, "unvisited"),
            new TreeNode(5, "backtrack", 140, 180, null, null, "unvisited"),
            new TreeNode(6, "Q(col=1)", 240, 180, null, null, "unvisited"),
            new TreeNode(7, "backtrack", 320, 180, null, null, "unvisited")
        );
    }

    private List<TreeNode> createRatTreeNodes() {
        return List.of(
            new TreeNode(1, "rat(0,0)", 190, 40, 2, 3, "unvisited"),
            new TreeNode(2, "Down(1,0)", 110, 110, 4, null, "unvisited"),
            new TreeNode(3, "Right(0,1)", 270, 110, null, 5, "unvisited"),
            new TreeNode(4, "Down(2,0)", 70, 180, null, null, "unvisited"),
            new TreeNode(5, "Right(0,2)", 310, 180, null, null, "unvisited")
        );
    }

    private List<TreeNode> createSudokuTreeNodes() {
        return List.of(
            new TreeNode(1, "solve(0,0)", 190, 40, 2, 3, "unvisited"),
            new TreeNode(2, "try '1'", 110, 110, null, null, "unvisited"),
            new TreeNode(3, "try '5'", 270, 110, null, null, "unvisited")
        );
    }

    private List<TreeNode> createColoringTreeNodes() {
        return List.of(
            new TreeNode(1, "node 0", 190, 40, 2, 3, "unvisited"),
            new TreeNode(2, "color: Red", 110, 110, null, null, "unvisited"),
            new TreeNode(3, "color: Green", 270, 110, null, null, "unvisited")
        );
    }

    private List<TreeNode> createPalindromeTreeNodes() {
        return List.of(
            new TreeNode(1, "\"aab\"", 190, 40, 2, 3, "unvisited"),
            new TreeNode(2, "cut \"a\"", 110, 110, null, null, "unvisited"),
            new TreeNode(3, "cut \"aa\"", 270, 110, null, null, "unvisited")
        );
    }

    private List<TreeNode> createSubsetsTreeNodes() {
        return List.of(
            new TreeNode(1, "ind=0 (1)", 190, 40, 2, 3, "unvisited"),
            new TreeNode(2, "Pick", 110, 110, null, null, "unvisited"),
            new TreeNode(3, "Non-Pick", 270, 110, null, null, "unvisited")
        );
    }

    // Grid Helpers
    private int[][] createEmpty4x4Grid() {
        return new int[][]{{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0}};
    }
    private int[][] createMazeGrid() {
        return new int[][]{{1,0,0,0},{1,1,0,1},{1,1,0,0},{0,1,1,1}};
    }
    private int[][] createSudokuGrid() {
        return new int[][]{{5,3,0,0,7,0,0,0,0},{6,0,0,1,9,5,0,0,0},{0,9,8,0,0,0,0,6,0},{8,0,0,0,6,0,0,0,3},{4,0,0,8,0,3,0,0,1},{7,0,0,0,2,0,0,0,6},{0,6,0,0,0,0,2,8,0},{0,0,0,4,1,9,0,0,5},{0,0,0,0,8,0,0,7,9}};
    }
}
