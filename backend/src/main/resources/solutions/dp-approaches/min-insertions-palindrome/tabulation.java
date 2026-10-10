public class Solution {
    public int solve(String text) {
        int n = text.length();
        // @a base
        int[][] dp = new int[n][n]; // Every single-character interval needs zero.
        // Increasing interval length is the reverse of recursive dependency descent.
        for (int length = 2; length <= n; length++) {
            for (int left = 0; left + length <= n; left++) {
                int right = left + length - 1;
                // @a fill
                dp[left][right] = text.charAt(left) == text.charAt(right)
                        ? length == 2 ? 0 : dp[left + 1][right - 1]
                        : 1 + Math.min(dp[left + 1][right], dp[left][right - 1]);
            }
        }
        // @a done
        return dp[0][n - 1];
    }
}
