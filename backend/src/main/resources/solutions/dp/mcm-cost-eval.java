public class Solution {
    public long solve(int[] dimensions) {
        int n = dimensions.length - 1;
        // One matrix requires no scalar multiplications.
        // @a init
        long[][] dp = new long[n][n];
        for (int gap = 1; gap < n; gap++) {
            for (int i = 0; i + gap < n; i++) {
                int j = i + gap;
                long best = Long.MAX_VALUE;
                for (int k = i; k < j; k++) {
                    long candidate = dp[i][k] + dp[k + 1][j]
                            + (long) dimensions[i] * dimensions[k + 1] * dimensions[j + 1];
                    // @a fill
                    best = Math.min(best, candidate);
                }
                dp[i][j] = best;
            }
        }
        // @a done
        return dp[0][n - 1];
    }
}
