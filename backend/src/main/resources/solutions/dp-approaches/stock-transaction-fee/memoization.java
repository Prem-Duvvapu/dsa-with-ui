public class Solution {
    public int solve(int[] prices, int fee) {
        int n = prices.length;
        Integer[][] memo = new Integer[n + 1][2];
        // @a start
        int answer = solve(0, 0, prices, fee, memo);
        // @a done
        return answer;
    }

    private int solve(int day, int holding, int[] prices, int fee, Integer[][] memo) {
        // @a enter
        if (memo[day][holding] != null) {
            // @a hit
            return memo[day][holding];
        }
        if (day == prices.length) {
            // @a base
            return memo[day][holding] = 0;
        }
        // @a branch
        int skip = solve(day + 1, holding, prices, fee, memo);
        // Pay the fee once per completed sale, not on both trades.
        int trade = (holding == 0 ? -prices[day] : prices[day] - fee)
                + solve(day + 1, 1 - holding, prices, fee, memo);
        // @a combine
        return memo[day][holding] = Math.max(skip, trade);
    }
}
