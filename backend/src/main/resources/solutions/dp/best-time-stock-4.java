public class Solution {
    public long solve(int[] prices, int k) {
        int actions = 2 * k;
        // Even actions buy; odd actions sell. The last column means no actions left.
        // @a init
        long[][] dp = new long[prices.length + 1][actions + 1];
        for (int day = prices.length - 1; day >= 0; day--) {
            for (int action = actions - 1; action >= 0; action--) {
                long skip = dp[day + 1][action];
                long trade = (action % 2 == 0 ? -prices[day] : prices[day]) + dp[day + 1][action + 1];
                // @a fill
                dp[day][action] = Math.max(skip, trade);
            }
        }
        // @a done
        return dp[0][0];
    }
}
