public class Solution {
    public long solve(String first, String second) {
        long best = 0;
        // @a init
        long[][] dp = new long[first.length() + 1][second.length() + 1];
        for (int i = 1; i <= first.length(); i++) {
            for (int j = 1; j <= second.length(); j++) {
                // A mismatch ends a contiguous substring.
                // @a fill
                dp[i][j] = first.charAt(i - 1) == second.charAt(j - 1) ? 1 + dp[i - 1][j - 1] : 0;
                best = Math.max(best, dp[i][j]);
            }
        }
        // @a done
        return best;
    }
}
