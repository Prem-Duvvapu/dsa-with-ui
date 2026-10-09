public class Solution {
    public int frogJump(int n, int[] heights) {
        // Null means unknown. Zero is a valid minimum energy and must be cached too.
        // @a init
        Integer[] memo = new Integer[n];
        // @a start
        return energy(n - 1, heights, memo);
    }

    private int energy(int i, int[] heights, Integer[] memo) {
        // @a enter
        if (memo[i] != null) {
            // @a hit
            return memo[i];
        }
        if (i == 0) {
            // @a base
            memo[i] = 0;
            // @a base-return
            return 0;
        }
        // @a one
        int jumpOne = energy(i - 1, heights, memo) + Math.abs(heights[i] - heights[i - 1]);
        // @a two
        int jumpTwo = i > 1
                ? energy(i - 2, heights, memo) + Math.abs(heights[i] - heights[i - 2])
                : Integer.MAX_VALUE;
        // @a store
        memo[i] = Math.min(jumpOne, jumpTwo);
        // @a result
        return memo[i];
    }
}
