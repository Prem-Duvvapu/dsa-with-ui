public class Solution {
    public int solve(int[][] grid) {
        int rows = grid.length, cols = grid[0].length;
        Integer[][][] memo = new Integer[rows][cols][cols];
        // @a start
        int answer = solve(0, 0, cols - 1, grid, memo);
        // @a done
        return answer;
    }

    private int solve(int row, int col1, int col2, int[][] grid, Integer[][][] memo) {
        // @a enter
        if (memo[row][col1][col2] != null) {
            // @a hit
            return memo[row][col1][col2];
        }
        int collected = grid[row][col1] + (col1 == col2 ? 0 : grid[row][col2]);
        if (row == grid.length - 1) {
            // @a base
            return memo[row][col1][col2] = collected;
        }
        // @a branch
        int best = 0;
        for (int d1 = -1; d1 <= 1; d1++) {
            for (int d2 = -1; d2 <= 1; d2++) {
                int next1 = col1 + d1, next2 = col2 + d2;
                if (next1 >= 0 && next1 < grid[0].length && next2 >= 0 && next2 < grid[0].length) {
                    best = Math.max(best, solve(row + 1, next1, next2, grid, memo));
                }
            }
        }
        // @a combine
        return memo[row][col1][col2] = collected + best;
    }
}
