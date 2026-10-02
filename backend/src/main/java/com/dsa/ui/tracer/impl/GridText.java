package com.dsa.ui.tracer.impl;

/**
 * A whole matrix as one compact variable value, {@code [[1,0],[0,1]]}.
 *
 * <p>Step variables are strings. A problem whose answer is a grid (Flood Fill, 01 Matrix,
 * Surrounded Regions) needs that answer written the same way everywhere it is compared - the
 * statement examples in {@code statements/*.json} are checked against it - so it is formatted
 * here once rather than by hand in each tracer.
 */
final class GridText {

    private GridText() {
    }

    static String of(int[][] grid) {
        StringBuilder out = new StringBuilder("[");
        for (int r = 0; r < grid.length; r++) {
            if (r > 0) {
                out.append(',');
            }
            out.append('[');
            for (int c = 0; c < grid[r].length; c++) {
                if (c > 0) {
                    out.append(',');
                }
                out.append(grid[r][c]);
            }
            out.append(']');
        }
        return out.append(']').toString();
    }

    /** The board of a char-grid problem, 'X'/'O', written as text the same way. */
    static String ofChars(char[][] grid) {
        StringBuilder out = new StringBuilder("[");
        for (int r = 0; r < grid.length; r++) {
            if (r > 0) {
                out.append(',');
            }
            out.append('[');
            for (int c = 0; c < grid[r].length; c++) {
                if (c > 0) {
                    out.append(',');
                }
                out.append('"').append(grid[r][c]).append('"');
            }
            out.append(']');
        }
        return out.append(']').toString();
    }
}
