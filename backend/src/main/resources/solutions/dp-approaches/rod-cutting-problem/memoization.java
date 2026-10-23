public class Solution {
    public long solve(int[] prices) {
        Long[][] memo = new Long[prices.length + 1][prices.length + 1];
        // @a start
        long answer = best(prices, prices.length, prices.length, memo);
        // @a done
        return answer;
    }

    private long best(int[] prices, int n, int length, Long[][] memo) {
        // @a enter
        if (memo[n][length] != null) {
            // @a hit
            return memo[n][length];
        }
        if (n == 0 || length == 0) {
            // @a base
            return memo[n][length] = 0L;
        }
        // @a branch
        long skip = best(prices, n - 1, length, memo);
        long take = n <= length ? prices[n - 1] + best(prices, n, length - n, memo) : 0L;
        // @a combine
        return memo[n][length] = Math.max(skip, take);
    }
}
