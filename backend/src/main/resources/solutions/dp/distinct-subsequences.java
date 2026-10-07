public class Solution {
    public long solve(String source, String target) {
        long[][] dp = new long[source.length() + 1][target.length() + 1];
        // There is exactly one way to choose the empty target.
        // @a init
        for (int i = 0; i <= source.length(); i++) dp[i][0] = 1;
        for (int i = 1; i <= source.length(); i++) {
            for (int j = 1; j <= target.length(); j++) {
                // Skip this source character, or take it when it matches.
                // @a fill
                dp[i][j] = dp[i - 1][j]
                        + (source.charAt(i - 1) == target.charAt(j - 1) ? dp[i - 1][j - 1] : 0);
            }
        }
        // @a done
        return dp[source.length()][target.length()];
    }
}
