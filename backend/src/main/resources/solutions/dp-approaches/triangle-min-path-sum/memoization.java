public class Solution {
    public int solve(int[][] triangle) {
        // @a start
        int n = Math.min(triangle.length, triangle[0].length);
        Integer[][] memo = new Integer[n][n];
        // @a done
        return cost(0, 0, triangle, n, memo);
    }
    private int cost(int row, int col, int[][] triangle, int n, Integer[][] memo) {
        // @a enter
        if (memo[row][col] != null) {
            // @a hit
            return memo[row][col];
        }
        if (row == n - 1) {
            // @a base
            return memo[row][col] = triangle[row][col];
        }
        // @a branch
        int left = cost(row + 1, col, triangle, n, memo);
        int right = cost(row + 1, col + 1, triangle, n, memo);
        // @a combine
        return memo[row][col] = triangle[row][col] + Math.min(left, right);
    }
}
