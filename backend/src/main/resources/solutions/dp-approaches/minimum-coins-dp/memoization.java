public class Solution {
    private static final int INFINITY = 1_000_000;

    public int coinChange(int[] coins, int amount) {
        Integer[][] memo = new Integer[coins.length + 1][amount + 1];
        // @a start
        int result = fewest(coins, coins.length, amount, memo);
        // @a done
        return result >= INFINITY ? -1 : result;
    }

    private int fewest(int[] coins, int n, int a, Integer[][] memo) {
        // @a enter
        if (memo[n][a] != null) {
            // @a hit
            return memo[n][a];
        }
        if (n == 0 || a == 0) {
            // @a base
            return memo[n][a] = a == 0 ? 0 : INFINITY;
        }
        // @a branch
        int skip = fewest(coins, n - 1, a, memo);
        int take = INFINITY;
        if (coins[n - 1] <= a) {
            int reused = fewest(coins, n, a - coins[n - 1], memo);
            if (reused < INFINITY) take = reused + 1;
        }
        // @a combine
        return memo[n][a] = Math.min(skip, take);
    }
}
