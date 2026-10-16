public class Solution {
    public int solve(int rows, int cols) {
        // @a start
        int row = rows - 1, col = cols - 1;
        // @a done
        return paths(row, col);
    }
    private int paths(int row, int col) {
        // @a enter
        if (row == 0 || col == 0) {
            // @a base
            return 1;
        }
        // @a branch
        int above = paths(row - 1, col);
        int left = paths(row, col - 1);
        // @a combine
        return above + left;
    }
}
