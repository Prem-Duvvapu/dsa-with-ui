public class Solution {
    public int solve(int[][] matrix) {
        // @a start
        int answer = Integer.MAX_VALUE;
        for (int col = 0; col < matrix[0].length; col++) {
            // @a reduce
            answer = Math.min(answer, cost(matrix.length - 1, col, matrix));
        }
        // @a done
        return answer;
    }
    private int cost(int row, int col, int[][] matrix) {
        // @a enter
        if (row == 0) {
            // @a base
            return matrix[0][col];
        }
        // @a branch
        int best = cost(row - 1, col, matrix);
        if (col > 0) best = Math.min(best, cost(row - 1, col - 1, matrix));
        if (col + 1 < matrix[0].length) best = Math.min(best, cost(row - 1, col + 1, matrix));
        // @a combine
        return matrix[row][col] + best;
    }
}
