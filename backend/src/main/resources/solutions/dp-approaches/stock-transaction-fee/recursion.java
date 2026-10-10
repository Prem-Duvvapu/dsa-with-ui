public class Solution {
    public int solve(int[] prices, int fee) {
        int n = prices.length;
        // @a start
        int answer = solve(0, 0, prices, fee);
        // @a done
        return answer;
    }

    private int solve(int day, int holding, int[] prices, int fee) {
        // @a enter
        if (day == prices.length) {
            // @a base
            return 0;
        }
        // @a branch
        int skip = solve(day + 1, holding, prices, fee);
        // Pay the fee once per completed sale, not on both trades.
        int trade = (holding == 0 ? -prices[day] : prices[day] - fee)
                + solve(day + 1, 1 - holding, prices, fee);
        // @a combine
        return Math.max(skip, trade);
    }
}
