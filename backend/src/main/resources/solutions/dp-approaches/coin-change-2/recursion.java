public class Solution {
    public int change(int[] coins, int amount) {
        // @a start
        int answer = ways(coins, coins.length, amount);
        // @a done
        return answer;
    }

    private int ways(int[] coins, int n, int a) {
        // @a enter
        if (n == 0 || a == 0) {
            // @a base
            return a == 0 ? 1 : 0;
        }
        // @a branch
        int skip = ways(coins, n - 1, a);
        int take = coins[n - 1] <= a ? ways(coins, n, a - coins[n - 1]) : 0;
        // @a combine
        return skip + take;
    }
}
