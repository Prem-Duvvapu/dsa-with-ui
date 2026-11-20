public class Solution {
    public int solve(String text) {
        String reversed = new StringBuilder(text).reverse().toString();
        // LCS length against the reversal equals LPS length; this does not reconstruct a sequence.
        // @a start
        int answer = lps(text, reversed, text.length(), reversed.length());
        // @a done
        return answer;
    }

    // i and j are prefix lengths of the fixed text and reversal, not interval endpoints.
    private int lps(String text, String reversed, int i, int j) {
        // @a enter
        if (i == 0 || j == 0) {
            // @a base
            return 0;
        }
        int answer;
        // @a branch
        if (text.charAt(i - 1) == reversed.charAt(j - 1)) {
            answer = 1 + lps(text, reversed, i - 1, j - 1);
        } else {
            int withoutText = lps(text, reversed, i - 1, j);
            int withoutReversed = lps(text, reversed, i, j - 1);
            answer = Math.max(withoutText, withoutReversed);
        }
        // @a combine
        return answer;
    }
}
