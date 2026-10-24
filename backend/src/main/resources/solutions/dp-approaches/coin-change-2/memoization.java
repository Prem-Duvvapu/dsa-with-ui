public class Solution {
    public int change(int[] coins, int amount) {
        Integer[][] memo = new Integer[coins.length + 1][amount + 1];
        // @a start
        int answer = ways(coins, coins.length, amount, memo);
        // @a done
        return answer;
    }

    private int ways(int[] coins, int n, int a, Integer[][] memo) {
        // @a enter
        if (memo[n][a] != null) {
            // @a hit
            return memo[n][a];
        }
        if (n == 0 || a == 0) {
            // @a base
            return memo[n][a] = a == 0 ? 1 : 0;
        }
        // @a branch
        int skip = ways(coins, n - 1, a, memo);
        int take = coins[n - 1] <= a ? ways(coins, n, a - coins[n - 1], memo) : 0;
        // @a combine
        return memo[n][a] = skip + take;
    }
}
