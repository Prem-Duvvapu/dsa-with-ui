public class Solution {
    public String solve(String first, String second) {
        int m = first.length(), n = second.length();
        // @a init
        long[][] dp = new long[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                // @a fill
                dp[i][j] = first.charAt(i - 1) == second.charAt(j - 1)
                        ? 1 + dp[i - 1][j - 1] : Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        StringBuilder reversed = new StringBuilder();
        int i = m, j = n;
        while (i > 0 && j > 0) {
            // Shared characters appear once; ties consume from the first string.
            // @a reconstruct
            if (first.charAt(i - 1) == second.charAt(j - 1)) {
                reversed.append(first.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                reversed.append(first.charAt(--i));
            } else {
                reversed.append(second.charAt(--j));
            }
        }
        while (i > 0) reversed.append(first.charAt(--i));
        while (j > 0) reversed.append(second.charAt(--j));
        // @a done
        return reversed.reverse().toString();
    }
}
