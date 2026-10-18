public class Solution {
    public int solve(int[][] triangle) {
        // @a start
        int n = Math.min(triangle.length, triangle[0].length);
        // @a done
        return cost(0, 0, triangle, n);
    }
    private int cost(int row, int col, int[][] triangle, int n) {
        // @a enter
        if (row == n - 1) {
            // @a base
            return triangle[row][col];
        }
        // @a branch
        int left = cost(row + 1, col, triangle, n);
        int right = cost(row + 1, col + 1, triangle, n);
        // @a combine
        return triangle[row][col] + Math.min(left, right);
    }
}
