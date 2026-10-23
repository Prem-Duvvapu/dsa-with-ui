public class Solution {
    public long solve(int[] prices) {
        // @a start
        long answer = best(prices, prices.length, prices.length);
        // @a done
        return answer;
    }

    private long best(int[] prices, int n, int length) {
        // @a enter
        // Best revenue within this length, not an exact-fill feasibility state.
        if (n == 0 || length == 0) {
            // @a base
            return 0L;
        }
        // @a branch
        long skip = best(prices, n - 1, length);
        long take = n <= length ? prices[n - 1] + best(prices, n, length - n) : 0L;
        // @a combine
        return Math.max(skip, take);
    }
}
