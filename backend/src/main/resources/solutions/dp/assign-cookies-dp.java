import java.util.Arrays;

public class Solution {
    public long solve(int[] greed, int[] cookies) {
        Arrays.sort(greed);
        Arrays.sort(cookies);
        // Empty prefixes satisfy zero children.
        // @a init
        long[][] dp = new long[greed.length + 1][cookies.length + 1];
        for (int i = 1; i <= greed.length; i++) {
            for (int j = 1; j <= cookies.length; j++) {
                long skipCookie = dp[i][j - 1], skipChild = dp[i - 1][j];
                long match = cookies[j - 1] >= greed[i - 1] ? 1 + dp[i - 1][j - 1] : -1;
                // @a fill
                dp[i][j] = Math.max(Math.max(skipCookie, skipChild), match);
            }
        }
        // @a done
        return dp[greed.length][cookies.length];
    }
}
