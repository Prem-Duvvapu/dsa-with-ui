public class Solution {
    public int solve(int rows, int cols) {
        // @a start
        Integer[][] memo = new Integer[rows][cols];
        // @a done
        return paths(rows - 1, cols - 1, memo);
    }
    private int paths(int row, int col, Integer[][] memo) {
        // @a enter
        if (memo[row][col] != null) {
            // @a hit
            return memo[row][col];
        }
        if (row == 0 || col == 0) {
            // @a base
            return memo[row][col] = 1;
        }
        // @a branch
        int above = paths(row - 1, col, memo);
        int left = paths(row, col - 1, memo);
        // @a combine
        return memo[row][col] = above + left;
    }
}
