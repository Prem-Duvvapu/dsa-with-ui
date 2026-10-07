public class Solution {
    // text is nonempty.
    public long solve(String text) {
        int n = text.length();
        // @a init
        boolean[][] palindrome = new boolean[n][n];
        long[] cuts = new long[n];
        for (int gap = 0; gap < n; gap++) {
            for (int i = 0; i + gap < n; i++) {
                int j = i + gap;
                // @a palindrome
                palindrome[i][j] = text.charAt(i) == text.charAt(j)
                        && (gap < 2 || palindrome[i + 1][j - 1]);
            }
        }
        for (int end = 0; end < n; end++) {
            long best = end;
            if (palindrome[0][end]) {
                best = 0;
            } else {
                for (int start = 1; start <= end; start++) {
                    if (palindrome[start][end]) best = Math.min(best, cuts[start - 1] + 1);
                }
            }
            // @a cuts
            cuts[end] = best;
        }
        // @a done
        return cuts[n - 1];
    }
}
