public class Solution {
    public int solve(int[][] grid) {
        // @a start
        int row = grid.length - 1, col = grid[0].length - 1;
        // @a done
        return paths(row, col, grid);
    }
    private int paths(int row, int col, int[][] grid) {
        // @a enter
        if (grid[row][col] == 1 || row == 0 && col == 0) {
            // @a base
            return grid[row][col] == 1 ? 0 : 1;
        }
        // @a branch
        int above = row > 0 ? paths(row - 1, col, grid) : 0;
        int left = col > 0 ? paths(row, col - 1, grid) : 0;
        // @a combine
        return above + left;
    }
}
