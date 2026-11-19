public class Solution {
    public int solve(String first, String second) {
        Integer[][] memo = new Integer[first.length() + 1][second.length() + 1];
        // @a start
        int answer = lcs(first, second, first.length(), second.length(), memo);
        // @a done
        return answer;
    }

    private int lcs(String first, String second, int i, int j, Integer[][] memo) {
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
        if (first.charAt(i - 1) == second.charAt(j - 1)) {
            answer = 1 + lcs(first, second, i - 1, j - 1, memo);
        } else {
            int withoutFirst = lcs(first, second, i - 1, j, memo);
            int withoutSecond = lcs(first, second, i, j - 1, memo);
            answer = Math.max(withoutFirst, withoutSecond);
        }
        // @a combine
        return memo[i][j] = answer;
    }
}
