public class Solution {
    public int solve(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0] == null || matrix[0].length == 0) {
            throw new IllegalArgumentException("A nonempty matrix is required.");
        }
        int rows = matrix.length, cols = matrix[0].length;
        for (int[] row : matrix) {
            if (row == null || row.length != cols) {
                throw new IllegalArgumentException("Every row must have the same number of columns.");
            }
            for (int column = 1; column < row.length; column++) {
                if (row[column] < row[column - 1]) {
                    throw new IllegalArgumentException("Every row must be sorted left to right.");
                }
            }
        }
        // Odd counts select the middle value; even counts select the lower median.
        int desired = (rows * cols + 1) / 2;
        // @a init
        int low = minOfFirstColumn(matrix), high = maxOfLastColumn(matrix);
        while (low < high) {
            // @a mid
            int mid = low + (high - low) / 2;
            int count = 0;
            for (int[] row : matrix) count += countLessEqual(row, mid);
            if (count < desired) {
                // @a tooFew
                low = mid + 1;
            } else {
                // @a enough
                high = mid;
            }
        }
        // @a done
        return low;
    }

    private int minOfFirstColumn(int[][] matrix) {
        int smallest = matrix[0][0];
        for (int[] row : matrix) smallest = Math.min(smallest, row[0]);
        return smallest;
    }

    private int maxOfLastColumn(int[][] matrix) {
        int largest = matrix[0][matrix[0].length - 1];
        for (int[] row : matrix) largest = Math.max(largest, row[row.length - 1]);
        return largest;
    }

    private int countLessEqual(int[] row, int x) {
        int low = 0, high = row.length;
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (row[mid] <= x) low = mid + 1; else high = mid;
        }
        return low;
    }
}
