public class Solution {
    public int frogJump(int n, int[] heights) {
        // @a start
        return energy(n - 1, heights);
    }

    private int energy(int i, int[] heights) {
        // @a enter
        if (i == 0) {
            // @a base
            return 0;
        }
        // @a one
        int jumpOne = energy(i - 1, heights) + Math.abs(heights[i] - heights[i - 1]);
        // Stair 1 has no two-step predecessor. The sentinel is never added to a cost.
        // @a two
        int jumpTwo = i > 1
                ? energy(i - 2, heights) + Math.abs(heights[i] - heights[i - 2])
                : Integer.MAX_VALUE;
        // @a combine
        return Math.min(jumpOne, jumpTwo);
    }
}
