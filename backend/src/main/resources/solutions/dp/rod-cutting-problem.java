public class Solution {
    public long solve(int[] prices) {
        int n = prices.length;
        // @a init
        long[][] dp = new long[n + 1][n + 1];
        for (int piece = 1; piece <= n; piece++) {
            for (int length = 1; length <= n; length++) {
                long skip = dp[piece - 1][length];
                // Reuse the same row because a piece length may be taken again.
                long take = length >= piece
                        ? prices[piece - 1] + dp[piece][length - piece] : Long.MIN_VALUE / 4;
                // @a fill
                dp[piece][length] = Math.max(skip, take);
            }
        }
        // @a done
        return dp[n][n];
    }
}
