public class Solution {
    private static final int INFINITY = 1_000_000;

    public int coinChange(int[] coins, int amount) {
        // @a start
        int result = fewest(coins, coins.length, amount);
        // @a done
        return result >= INFINITY ? -1 : result;
    }

    private int fewest(int[] coins, int n, int a) {
        // @a enter
        if (n == 0 || a == 0) {
            // @a base
            return a == 0 ? 0 : INFINITY;
        }
        // @a branch
        int skip = fewest(coins, n - 1, a);
        int take = INFINITY;
        if (coins[n - 1] <= a) {
            int reused = fewest(coins, n, a - coins[n - 1]);
            if (reused < INFINITY) take = reused + 1;
        }
        // @a combine
        return Math.min(skip, take);
    }
}
