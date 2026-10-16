public class Solution {
    public int solve(int[][] grid) {
        // @a start
        Integer[][] memo = new Integer[grid.length][grid[0].length];
        // @a done
        return paths(grid.length - 1, grid[0].length - 1, grid, memo);
    }
    private int paths(int row, int col, int[][] grid, Integer[][] memo) {
        // @a enter
        if (memo[row][col] != null) {
            // @a hit
            return memo[row][col];
        }
        if (grid[row][col] == 1 || row == 0 && col == 0) {
            // @a base
            return memo[row][col] = grid[row][col] == 1 ? 0 : 1;
        }
        // @a branch
        int above = row > 0 ? paths(row - 1, col, grid, memo) : 0;
        int left = col > 0 ? paths(row, col - 1, grid, memo) : 0;
        // @a combine
        return memo[row][col] = above + left;
    }
}
