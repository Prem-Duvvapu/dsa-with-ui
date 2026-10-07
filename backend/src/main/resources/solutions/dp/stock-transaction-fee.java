public class Solution {
    public long solve(int[] prices, int fee) {
        // @a init
        long[][] dp = new long[prices.length + 1][2];
        for (int day = prices.length - 1; day >= 0; day--) {
            // @a buy
            dp[day][0] = Math.max(dp[day + 1][0], -prices[day] + dp[day + 1][1]);
            // Charge the fee once, on the sale.
            // @a sell
            dp[day][1] = Math.max(dp[day + 1][1], prices[day] - fee + dp[day + 1][0]);
        }
        // @a done
        return dp[0][0];
    }
}
