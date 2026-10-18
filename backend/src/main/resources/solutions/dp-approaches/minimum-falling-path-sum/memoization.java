public class Solution {
    public int solve(int[][] matrix) {
        // @a start
        Integer[][] memo = new Integer[matrix.length][matrix[0].length];
        int answer = Integer.MAX_VALUE;
        for (int col = 0; col < matrix[0].length; col++) {
            // @a reduce
            answer = Math.min(answer, cost(matrix.length - 1, col, matrix, memo));
        }
        // @a done
        return answer;
    }
    private int cost(int row, int col, int[][] matrix, Integer[][] memo) {
        // @a enter
        if (memo[row][col] != null) {
            // @a hit
            return memo[row][col];
        }
        if (row == 0) {
            // @a base
            return memo[row][col] = matrix[0][col];
        }
        // @a branch
        int best = cost(row - 1, col, matrix, memo);
        if (col > 0) best = Math.min(best, cost(row - 1, col - 1, matrix, memo));
        if (col + 1 < matrix[0].length) best = Math.min(best, cost(row - 1, col + 1, matrix, memo));
        // @a combine
        return memo[row][col] = matrix[row][col] + best;
    }
}
