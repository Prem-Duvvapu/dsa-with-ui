public class Solution {
    public int solve(int[] heights, int k) {
        // @a start
        int last = heights.length - 1;
        // @a done
        return energy(last, heights, k);
    }
    private int energy(int i, int[] heights, int k) {
        // @a enter
        if (i == 0) {
            // @a base
            return 0;
        }
        // @a branch
        int best = Integer.MAX_VALUE;
        for (int d = 1; d <= k && d <= i; d++) {
            best = Math.min(best, energy(i - d, heights, k) + Math.abs(heights[i] - heights[i - d]));
        }
        // @a combine
        return best;
    }
}
