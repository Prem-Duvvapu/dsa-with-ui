package com.dsa.ui.service;

import com.dsa.ui.algorithm.backtracking.*;
import com.dsa.ui.model.*;
import com.dsa.ui.trace.ListTraceRecorder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RecursionBacktrackingService {

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
            case "n-queens": return generateNQueensSteps();
            case "rat-in-a-maze": return generateRatInMazeSteps();
            case "sudoku-solver": return generateSudokuSteps();
            case "m-coloring": return generateMColoringSteps();
            case "palindrome-partitioning": return generatePalindromePartitioningSteps();
            case "subsets-i": return generateSubsetsSteps();
            case "combination-sum-i": return generateCombinationSumSteps();
            case "permutations": return generatePermutationsSteps();
            case "word-search": return generateWordSearchSteps();
            default: return generateNQueensSteps();
        }
    }

    private void initProblems() {
        // 1. N-Queens Problem
        problems.put("n-queens", new ProblemDetail(
            "n-queens", "N-Queens Problem", "Recursion - Hard", "Recursion & Backtracking", "Hard",
            "Place N non-attacking queens on an N x N chessboard such that no two queens attack each other (same row, column, or diagonal).",
            """
            // Java N-Queens Backtracking with Hash Arrays (LeetCode 51)
            public List<List<String>> solveNQueens(int n) {
                List<List<String>> ans = new ArrayList<>();
                char[][] board = new char[n][n];
                for (int i = 0; i < n; i++) Arrays.fill(board[i], '.');

                int leftRow[] = new int[n];
                int lowerDiagonal[] = new int[2 * n - 1];
                int upperDiagonal[] = new int[2 * n - 1];

                solve(0, board, ans, leftRow, lowerDiagonal, upperDiagonal, n);
                return ans;
            }

            private void solve(int col, char[][] board, List<List<String>> ans, int[] leftRow, int[] lowerDiagonal, int[] upperDiagonal, int n) {
                if (col == n) {
                    ans.add(construct(board));
                    return;
                }
                for (int row = 0; row < n; row++) {
                    if (leftRow[row] == 0 && lowerDiagonal[row + col] == 0 && upperDiagonal[n - 1 + col - row] == 0) {
                        board[row][col] = 'Q';
                        leftRow[row] = 1; lowerDiagonal[row + col] = 1; upperDiagonal[n - 1 + col - row] = 1;

                        solve(col + 1, board, ans, leftRow, lowerDiagonal, upperDiagonal, n);

                        // Backtrack!
                        board[row][col] = '.';
                        leftRow[row] = 0; lowerDiagonal[row + col] = 0; upperDiagonal[n - 1 + col - row] = 0;
                    }
                }
            }
            """,
            null, null, createNQueensTreeNodes(), null, null, null, createEmpty4x4Grid(),
            new ComplexityDetail(
                "O(N!)",
                "Time Complexity: First queen has N choices, second has at most N-2 choices, third N-4, etc., giving O(N!) time complexity.",
                "Why O(N!)? The branching factor reduces at each column as constraints eliminate valid rows and diagonals.",
                "O(N^2)",
                "Space Complexity: Board storage O(N^2) + Hash arrays for fast O(1) safety checks O(N) + Call stack depth O(N).",
                "Why Hash Arrays instead of scanning? `leftRow[]`, `lowerDiagonal[]`, `upperDiagonal[]` check queen safety in O(1) time instead of O(N) board scans.",
                "Auxiliary Space: O(N) (Hash Arrays & Call Stack)",
                "Chessboard Grid: O(N^2)"
            ),
            "Matrix"
        ));

        // 2. Rat in a Maze
        problems.put("rat-in-a-maze", new ProblemDetail(
            "rat-in-a-maze", "Rat in a Maze", "Recursion - Hard", "Recursion & Backtracking", "Medium",
            "Find all possible paths for a rat from (0,0) to (N-1,N-1) in a 2D binary grid using directions D, L, R, U.",
            """
            // Java Rat in a Maze (Striver A2Z Sheet)
            public ArrayList<String> findPath(int[][] m, int n) {
                ArrayList<String> ans = new ArrayList<>();
                int vis[][] = new int[n][n];
                if (m[0][0] == 1) solve(0, 0, m, n, ans, "", vis);
                return ans;
            }

            private void solve(int i, int j, int[][] a, int n, ArrayList<String> ans, String move, int[][] vis) {
                if (i == n - 1 && j == n - 1) {
                    ans.add(move);
                    return;
                }
                // Down, Left, Right, Up
                int di[] = {+1, 0, 0, -1};
                int dj[] = {0, -1, +1, 0};
                String dir = "DLRU";

                for (int ind = 0; ind < 4; ind++) {
                    int nexti = i + di[ind], nextj = j + dj[ind];
                    if (nexti >= 0 && nextj >= 0 && nexti < n && nextj < n && vis[nexti][nextj] == 0 && a[nexti][nextj] == 1) {
                        vis[i][j] = 1;
                        solve(nexti, nextj, a, n, ans, move + dir.charAt(ind), vis);
                        vis[i][j] = 0; // Backtrack!
                    }
                }
            }
            """,
            null, null, createRatTreeNodes(), null, null, null, createMazeGrid(),
            new ComplexityDetail(
                "O(4^(N*N))",
                "Time Complexity: At each cell, rat has 4 directional choices (Down, Left, Right, Up). Worst case visits 4^(N*N) paths.",
                "Why lexicographical order D-L-R-U? Iterating directions D, L, R, U guarantees generated path strings are in alphabetical order.",
                "O(N*N)",
                "Space Complexity: Visited array vis[N][N] + Recursion call stack depth up to O(N*N).",
                "Why O(N*N)? Longest simple path in N x N grid contains N*N cells.",
                "Auxiliary Space: O(N*N) (Recursion Stack)",
                "Grid Space: O(N*N)"
            ),
            "Matrix"
        ));

        // 3. Sudoku Solver
        problems.put("sudoku-solver", new ProblemDetail(
            "sudoku-solver", "Sudoku Solver", "Recursion - Hard", "Recursion & Backtracking", "Hard",
            "Write a program to solve a Sudoku puzzle by filling empty cells ('.') with digits '1'-'9'.",
            """
            // Java Sudoku Solver (LeetCode 37)
            public void solveSudoku(char[][] board) {
                solve(board);
            }

            private boolean solve(char[][] board) {
                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        if (board[i][j] == '.') {
                            for (char c = '1'; c <= '9'; c++) {
                                if (isValid(board, i, j, c)) {
                                    board[i][j] = c;
                                    if (solve(board)) return true;
                                    else board[i][j] = '.'; // Backtrack!
                                }
                            }
                            return false;
                        }
                    }
                }
                return true;
            }

            private boolean isValid(char[][] board, int row, int col, char c) {
                for (int i = 0; i < 9; i++) {
                    if (board[i][col] == c) return false; // Check column
                    if (board[row][i] == c) return false; // Check row
                    if (board[3 * (row / 3) + i / 3][3 * (col / 3) + i % 3] == c) return false; // Check 3x3 box
                }
                return true;
            }
            """,
            null, null, createSudokuTreeNodes(), null, null, null, createSudokuGrid(),
            new ComplexityDetail(
                "O(9^(81))",
                "Time Complexity: 9x9 board has 81 cells. In worst case, each cell tries digits '1'-'9'.",
                "Why fast in practice? Constraints (row, col, 3x3 box) prune invalid branches early, solving valid Sudokus instantly.",
                "O(1)",
                "Space Complexity: Modifies input 9x9 board in-place. Call stack depth at most 81.",
                "Why O(1) auxiliary space? Fixed board size 9x9.",
                "Auxiliary Space: O(81) = O(1)",
                "Board Space: 9x9 matrix"
            ),
            "Matrix"
        ));

        // 4. M-Coloring Problem
        problems.put("m-coloring", new ProblemDetail(
            "m-coloring", "M-Coloring Problem", "Recursion - Hard", "Recursion & Backtracking", "Medium",
            "Given an undirected graph and M colors, color all vertices such that no two adjacent vertices have the same color.",
            """
            // Java M-Coloring (Striver A2Z Sheet)
            public boolean graphColoring(boolean[][] graph, int m, int n) {
                int[] color = new int[n];
                return solve(0, graph, color, m, n);
            }

            private boolean solve(int node, boolean[][] graph, int[] color, int m, int n) {
                if (node == n) return true;

                for (int col = 1; col <= m; col++) {
                    if (isSafe(node, graph, color, col, n)) {
                        color[node] = col;
                        if (solve(node + 1, graph, color, m, n)) return true;
                        color[node] = 0; // Backtrack!
                    }
                }
                return false;
            }
            """,
            null, null, createColoringTreeNodes(), null, null, null, null,
            new ComplexityDetail(
                "O(M^N)",
                "Time Complexity: Each of N nodes can try up to M colors = M^N possible colorings.",
                "Why Backtracking helps? Stops trying color branches as soon as an edge collision is detected.",
                "O(N)",
                "Space Complexity: Color array color[] size N + Recursion stack depth N.",
                "Why O(N)? Bounded by number of graph vertices N.",
                "Auxiliary Space: O(N)",
                "Color Array: O(N)"
            ),
            "Stack"
        ));

        // 5. Palindrome Partitioning
        problems.put("palindrome-partitioning", new ProblemDetail(
            "palindrome-partitioning", "Palindrome Partitioning", "Recursion - Hard", "Recursion & Backtracking", "Medium",
            "Partition a string s such that every substring of the partition is a palindrome.",
            """
            // Java Palindrome Partitioning (LeetCode 131)
            public List<List<String>> partition(String s) {
                List<List<String>> res = new ArrayList<>();
                solve(0, s, new ArrayList<>(), res);
                return res;
            }

            private void solve(int index, String s, List<String> path, List<List<String>> res) {
                if (index == s.length()) {
                    res.add(new ArrayList<>(path));
                    return;
                }
                for (int i = index; i < s.length(); i++) {
                    if (isPalindrome(s, index, i)) {
                        path.add(s.substring(index, i + 1));
                        solve(i + 1, s, path, res);
                        path.remove(path.size() - 1); // Backtrack!
                    }
                }
            }
            """,
            null, null, createPalindromeTreeNodes(), null, null, null, null,
            new ComplexityDetail(
                "O(2^N * N)",
                "Time Complexity: A string of length N has 2^(N-1) possible partition cut points. Checking palindrome takes O(N).",
                "Why 2^N cuts? At every character position, we can either make a partition cut or continue substring.",
                "O(N)",
                "Space Complexity: Path list space O(N) + Call stack depth O(N).",
                "Why O(N)? Bounded by length of string s.",
                "Auxiliary Space: O(N)",
                "Result List: O(2^N)"
            ),
            "Stack"
        ));

        // 6. Subsets / Subset Sums
        problems.put("subsets-i", new ProblemDetail(
            "subsets-i", "Subsets / Subset Sums", "Recursion - Subsequences", "Recursion & Backtracking", "Medium",
            "Given an array of unique integers, return all possible subsets (the power set).",
            """
            // Java Subsets (LeetCode 78)
            public List<List<Integer>> subsets(int[] nums) {
                List<List<Integer>> ans = new ArrayList<>();
                solve(0, nums, new ArrayList<>(), ans);
                return ans;
            }

            private void solve(int ind, int[] nums, List<Integer> ds, List<List<Integer>> ans) {
                if (ind == nums.length) {
                    ans.add(new ArrayList<>(ds));
                    return;
                }
                // Pick element
                ds.add(nums[ind]);
                solve(ind + 1, nums, ds, ans);

                // Don't Pick element (Backtrack)
                ds.remove(ds.size() - 1);
                solve(ind + 1, nums, ds, ans);
            }
            """,
            null, null, createSubsetsTreeNodes(), null, null, null, null,
            new ComplexityDetail(
                "O(2^N)",
                "Time Complexity: At each element index, we have 2 choices: Pick or Don't Pick. Total subsets = 2^N.",
                "Why Pick / Non-Pick recursion tree is binary? Each level doubles the number of active subset branches.",
                "O(N)",
                "Space Complexity: Recursion stack depth O(N) + Data structure ds list O(N).",
                "Why O(N)? Bounded by array size N.",
                "Auxiliary Space: O(N)",
                "Subsets Result: O(2^N)"
            ),
            "Stack"
        ));

        // 7. Combination Sum I
        problems.put("combination-sum-i", new ProblemDetail(
            "combination-sum-i", "Combination Sum I", "Recursion - Subsequences", "Recursion & Backtracking", "Medium",
            "Find all unique combinations of candidates where target sum is achieved (elements can be reused infinitely).",
            """
            // Java Combination Sum I (LeetCode 39)
            public List<List<Integer>> combinationSum(int[] candidates, int target) {
                List<List<Integer>> ans = new ArrayList<>();
                findCombinations(0, candidates, target, ans, new ArrayList<>());
                return ans;
            }

            private void findCombinations(int ind, int[] arr, int target, List<List<Integer>> ans, List<Integer> ds) {
                if (ind == arr.length) {
                    if (target == 0) ans.add(new ArrayList<>(ds));
                    return;
                }
                if (arr[ind] <= target) {
                    ds.add(arr[ind]);
                    findCombinations(ind, arr, target - arr[ind], ans, ds); // Stay at ind for infinite reuse!
                    ds.remove(ds.size() - 1); // Backtrack!
                }
                findCombinations(ind + 1, arr, target, ans, ds);
            }
            """,
            null, null, createCombinationTreeNodes(), null, null, null, null,
            new ComplexityDetail(
                "O(2^T * K)",
                "Time Complexity: T is target sum, K is average length of combination.",
                "Why infinite reuse doesn't cause infinite loop? Target decreases by arr[ind] at each step until target < arr[ind].",
                "O(K)",
                "Space Complexity: Call stack depth proportional to target sum T / min(arr).",
                "Why O(K)? Memory bounded by combination length.",
                "Auxiliary Space: O(K)",
                "Result List: O(2^T)"
            ),
            "Stack"
        ));

        // 8. Permutations
        problems.put("permutations", new ProblemDetail(
            "permutations", "Permutations of Array / String", "Recursion - Hard", "Recursion & Backtracking", "Medium",
            "Generate all permutations of an array of distinct integers using Swapping Backtracking.",
            """
            // Java Permutations (LeetCode 46)
            public List<List<Integer>> permute(int[] nums) {
                List<List<Integer>> ans = new ArrayList<>();
                recurPermute(0, nums, ans);
                return ans;
            }

            private void recurPermute(int index, int[] nums, List<List<Integer>> ans) {
                if (index == nums.length) {
                    List<Integer> ds = new ArrayList<>();
                    for (int i = 0; i < nums.length; i++) ds.add(nums[i]);
                    ans.add(ds);
                    return;
                }
                for (int i = index; i < nums.length; i++) {
                    swap(i, index, nums);
                    recurPermute(index + 1, nums, ans);
                    swap(i, index, nums); // Backtrack!
                }
            }
            """,
            null, null, createPermutationTreeNodes(), null, null, null, null,
            new ComplexityDetail(
                "O(N! * N)",
                "Time Complexity: N! permutations, each taking O(N) to copy array to result list.",
                "Why Swapping eliminates extra visited array? Swapping nums[i] with nums[index] generates all arrangements in-place without auxiliary boolean arrays.",
                "O(N)",
                "Space Complexity: In-place recursion stack depth O(N).",
                "Why O(N)? Stack depth equals array length N.",
                "Auxiliary Space: O(N)",
                "Permutation Output: O(N! * N)"
            ),
            "Stack"
        ));

        // 9. Word Search
        problems.put("word-search", new ProblemDetail(
            "word-search", "Word Search in 2D Board", "Recursion - Hard", "Recursion & Backtracking", "Medium",
            "Check if a given word exists in a 2D board of characters using 4-directional Backtracking.",
            """
            // Java Word Search (LeetCode 79)
            public boolean exist(char[][] board, String word) {
                int n = board.length, m = board[0].length;
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < m; j++) {
                        if (board[i][j] == word.charAt(0)) {
                            if (search(0, i, j, board, word)) return true;
                        }
                    }
                }
                return false;
            }

            private boolean search(int index, int r, int c, char[][] board, String word) {
                if (index == word.length()) return true;
                if (r < 0 || c < 0 || r >= board.length || c >= board[0].length || board[r][c] != word.charAt(index)) return false;

                char temp = board[r][c];
                board[r][c] = '#'; // Mark visited

                boolean found = search(index + 1, r + 1, c, board, word) ||
                                search(index + 1, r - 1, c, board, word) ||
                                search(index + 1, r, c + 1, board, word) ||
                                search(index + 1, r, c - 1, board, word);

                board[r][c] = temp; // Backtrack!
                return found;
            }
            """,
            null, null, createWordSearchTreeNodes(), null, null, null, createWordBoardGrid(),
            new ComplexityDetail(
                "O(N * M * 3^L)",
                "Time Complexity: N*M starting positions. For word of length L, each step explores 3 directions (excluding previous direction).",
                "Why 3^L instead of 4^L? Since we mark cell visited, rat cannot immediately step backward, reducing choices from 4 to 3.",
                "O(L)",
                "Space Complexity: Call stack depth equal to word length L.",
                "Why O(L)? Board modification is done in-place by setting board[r][c] = '#'.",
                "Auxiliary Space: O(L)",
                "Board Space: N x M"
            ),
            "Matrix"
        ));
    }

    // Full Execution Trace N-Queens Generator using TraceRecorder
    private List<ExecutionStep> generateNQueensSteps() {
        ListTraceRecorder recorder = new ListTraceRecorder();
        new NQueens().solve(4, recorder);
        return recorder.toExecutionSteps();
    }

    // Granular Rat in a Maze Step Generator using TraceRecorder
    private List<ExecutionStep> generateRatInMazeSteps() {
        int[][] maze = createMazeGrid();
        ListTraceRecorder recorder = new ListTraceRecorder();
        new RatInMaze().solve(maze, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generateSudokuSteps() {
        int[][] board = createSudokuGrid();
        ListTraceRecorder recorder = new ListTraceRecorder();
        new SudokuSolver().solve(board, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generateMColoringSteps() {
        int[][] graph = {
            {0, 1, 1, 1},
            {1, 0, 1, 0},
            {1, 1, 0, 1},
            {1, 0, 1, 0}
        };
        int m = 3;
        int v = 4;
        ListTraceRecorder recorder = new ListTraceRecorder();
        new MColoring().solve(graph, m, v, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generatePalindromePartitioningSteps() {
        String s = "aab";
        ListTraceRecorder recorder = new ListTraceRecorder();
        new PalindromePartitioning().solve(s, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generateSubsetsSteps() {
        int[] nums = {1, 2, 3};
        ListTraceRecorder recorder = new ListTraceRecorder();
        new Subsets().solve(nums, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generateCombinationSumSteps() {
        int[] candidates = {2, 3, 6, 7};
        int target = 7;
        ListTraceRecorder recorder = new ListTraceRecorder();
        new CombinationSum().solve(candidates, target, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generatePermutationsSteps() {
        int[] nums = {1, 2, 3};
        ListTraceRecorder recorder = new ListTraceRecorder();
        new Permutations().solve(nums, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generateWordSearchSteps() {
        char[][] board = {
            {'A', 'B', 'C', 'E'},
            {'S', 'F', 'C', 'S'},
            {'A', 'D', 'E', 'E'}
        };
        String word = "ABCCED";
        ListTraceRecorder recorder = new ListTraceRecorder();
        new WordSearch().solve(board, word, recorder);
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

    private List<TreeNode> createCombinationTreeNodes() {
        return List.of(
            new TreeNode(1, "target=7", 190, 40, 2, 3, "unvisited"),
            new TreeNode(2, "pick 2", 110, 110, null, null, "unvisited"),
            new TreeNode(3, "pick 7", 270, 110, null, null, "unvisited")
        );
    }

    private List<TreeNode> createPermutationTreeNodes() {
        return List.of(
            new TreeNode(1, "idx=0", 190, 40, 2, 3, "unvisited"),
            new TreeNode(2, "swap(0,0)", 110, 110, null, null, "unvisited"),
            new TreeNode(3, "swap(0,1)", 270, 110, null, null, "unvisited")
        );
    }

    private List<TreeNode> createWordSearchTreeNodes() {
        return List.of(
            new TreeNode(1, "word[0]='A'", 190, 40, 2, 3, "unvisited"),
            new TreeNode(2, "word[1]='B'", 110, 110, null, null, "unvisited"),
            new TreeNode(3, "word[1]='C'", 270, 110, null, null, "unvisited")
        );
    }

    // Grid Helpers
    private int[][] createEmpty4x4Grid() {
        return new int[][]{
            {0, 0, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0}
        };
    }

    private int[][] createMazeGrid() {
        return new int[][]{
            {1, 0, 0, 0},
            {1, 1, 0, 1},
            {1, 1, 0, 0},
            {0, 1, 1, 1}
        };
    }

    private int[][] createSudokuGrid() {
        return new int[][]{
            {5, 3, 0, 0, 7, 0, 0, 0, 0},
            {6, 0, 0, 1, 9, 5, 0, 0, 0},
            {0, 9, 8, 0, 0, 0, 0, 6, 0},
            {8, 0, 0, 0, 6, 0, 0, 0, 3},
            {4, 0, 0, 8, 0, 3, 0, 0, 1},
            {7, 0, 0, 0, 2, 0, 0, 0, 6},
            {0, 6, 0, 0, 0, 0, 2, 8, 0},
            {0, 0, 0, 4, 1, 9, 0, 0, 5},
            {0, 0, 0, 0, 8, 0, 0, 7, 9}
        };
    }

    private int[][] createWordBoardGrid() {
        return new int[][]{
            {1, 2, 3, 4},
            {5, 6, 7, 8},
            {9, 10, 11, 12}
        };
    }

    private int[][] cloneGrid(int[][] original) {
        int[][] copy = new int[original.length][original[0].length];
        for (int i = 0; i < original.length; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, original[i].length);
        }
        return copy;
    }
}
