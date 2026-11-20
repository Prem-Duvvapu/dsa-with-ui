public class Solution {
    public int solve(String text) {
        String reversed = new StringBuilder(text).reverse().toString();
        Integer[][] memo = new Integer[text.length() + 1][reversed.length() + 1];
        // LCS length against the reversal equals LPS length; this does not reconstruct a sequence.
        // @a start
        int answer = lps(text, reversed, text.length(), reversed.length(), memo);
        // @a done
        return answer;
    }

    // i and j are prefix lengths of the fixed text and reversal, not interval endpoints.
    private int lps(String text, String reversed, int i, int j, Integer[][] memo) {
        // @a enter
        if (memo[i][j] != null) {
            // @a hit
            return memo[i][j];
        }
        if (i == 0 || j == 0) {
            // @a base
            return memo[i][j] = 0;
        }
        int answer;
        // @a branch
        if (text.charAt(i - 1) == reversed.charAt(j - 1)) {
            answer = 1 + lps(text, reversed, i - 1, j - 1, memo);
        } else {
            int withoutText = lps(text, reversed, i - 1, j, memo);
            int withoutReversed = lps(text, reversed, i, j - 1, memo);
            answer = Math.max(withoutText, withoutReversed);
        }
        // @a combine
        return memo[i][j] = answer;
    }
}
