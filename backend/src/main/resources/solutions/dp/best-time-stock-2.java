public class Solution {
    public long solve(int[] prices) {
        // Column 0: may buy. Column 1: holding a share, may sell.
        // @a init
        long[][] dp = new long[prices.length + 1][2];
        for (int day = prices.length - 1; day >= 0; day--) {
            // @a buy
            dp[day][0] = Math.max(dp[day + 1][0], -prices[day] + dp[day + 1][1]);
            // @a sell
            dp[day][1] = Math.max(dp[day + 1][1], prices[day] + dp[day + 1][0]);
        }
        // @a done
        return dp[0][0];
    }
}
