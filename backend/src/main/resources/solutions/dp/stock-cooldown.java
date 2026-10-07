public class Solution {
    public long solve(int[] prices) {
        // Two terminal rows allow a sale on the last day to skip a day safely.
        // @a init
        long[][] dp = new long[prices.length + 2][2];
        for (int day = prices.length - 1; day >= 0; day--) {
            // @a buy
            dp[day][0] = Math.max(dp[day + 1][0], -prices[day] + dp[day + 1][1]);
            // After selling, buying is forbidden on the next day.
            // @a sell
            dp[day][1] = Math.max(dp[day + 1][1], prices[day] + dp[day + 2][0]);
        }
        // @a done
        return dp[0][0];
    }
}
