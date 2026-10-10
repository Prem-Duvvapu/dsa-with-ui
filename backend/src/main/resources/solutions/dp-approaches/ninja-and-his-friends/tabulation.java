public class Solution {
    public int solve(int[][] grid) {
        int rows = grid.length, cols = grid[0].length;
        int[][][] dp = new int[rows][cols][cols];
        for (int col1 = 0; col1 < cols; col1++) {
            for (int col2 = 0; col2 < cols; col2++) {
                // @a base
                dp[rows - 1][col1][col2] = grid[rows - 1][col1]
                        + (col1 == col2 ? 0 : grid[rows - 1][col2]);
            }
        }
        for (int row = rows - 2; row >= 0; row--) {
            // @a rowStart
            for (int col1 = 0; col1 < cols; col1++) {
                for (int col2 = 0; col2 < cols; col2++) {
                    int best = 0;
                    for (int d1 = -1; d1 <= 1; d1++) {
                        for (int d2 = -1; d2 <= 1; d2++) {
                            int next1 = col1 + d1, next2 = col2 + d2;
                            if (next1 >= 0 && next1 < cols && next2 >= 0 && next2 < cols) {
                                best = Math.max(best, dp[row + 1][next1][next2]);
                            }
                        }
                    }
                    int collected = grid[row][col1] + (col1 == col2 ? 0 : grid[row][col2]);
                    // @a fill
                    dp[row][col1][col2] = collected + best;
                }
            }
        }
        // @a done
        return dp[0][0][cols - 1];
    }
}
