public class Solution {
    public long solve(String text) {
        String reversed = new StringBuilder(text).reverse().toString();
        int n = text.length();
        // The LCS length of a string and its reverse equals its LPS length.
        // @a init
        long[][] dp = new long[n + 1][n + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                // @a fill
                dp[i][j] = text.charAt(i - 1) == reversed.charAt(j - 1)
                        ? 1 + dp[i - 1][j - 1] : Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        // @a done
        return dp[n][n];
    }
}
