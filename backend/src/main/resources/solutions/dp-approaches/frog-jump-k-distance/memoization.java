public class Solution {
    public int solve(int[] heights, int k) {
        // @a start
        Integer[] memo = new Integer[heights.length];
        // @a done
        return energy(heights.length - 1, heights, k, memo);
    }
    private int energy(int i, int[] heights, int k, Integer[] memo) {
        // @a enter
        if (memo[i] != null) {
            // @a hit
            return memo[i];
        }
        if (i == 0) {
            // @a base
            return memo[i] = 0;
        }
        // @a branch
        int best = Integer.MAX_VALUE;
        for (int d = 1; d <= k && d <= i; d++) {
            best = Math.min(best, energy(i - d, heights, k, memo) + Math.abs(heights[i] - heights[i - d]));
        }
        // @a combine
        return memo[i] = best;
    }
}
