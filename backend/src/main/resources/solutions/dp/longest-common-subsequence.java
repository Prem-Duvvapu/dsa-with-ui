public class Solution {
    public long solve(String first, String second) {
        int m = first.length(), n = second.length();
        // Empty prefixes have LCS length zero.
        // @a init
        long[][] dp = new long[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                // @a fill
                dp[i][j] = first.charAt(i - 1) == second.charAt(j - 1)
                        ? 1 + dp[i - 1][j - 1]
                        : Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        // @a done
        return dp[m][n];
    }
}
