public class Solution {
    public int climbStairs(int n) {
        // Null means this state has not been computed.
        // @a init
        Integer[] memo = new Integer[n + 1];
        // @a start
        return ways(n, memo);
    }

    private int ways(int n, Integer[] memo) {
        // @a enter
        if (memo[n] != null) {
            // @a hit
            return memo[n];
        }
        if (n <= 1) {
            // @a base
            memo[n] = 1;
            // @a base-return
            return 1;
        }
        // @a left
        int oneStep = ways(n - 1, memo);
        // @a right
        int twoSteps = ways(n - 2, memo);
        // @a store
        memo[n] = oneStep + twoSteps;
        // @a result
        return memo[n];
    }
}
