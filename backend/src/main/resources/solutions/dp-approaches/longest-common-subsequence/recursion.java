public class Solution {
    public int solve(String first, String second) {
        // @a start
        int answer = lcs(first, second, first.length(), second.length());
        // @a done
        return answer;
    }

    private int lcs(String first, String second, int i, int j) {
        // @a enter
        if (i == 0 || j == 0) {
            // @a base
            return 0;
        }
        int answer;
        // @a branch
        if (first.charAt(i - 1) == second.charAt(j - 1)) {
            answer = 1 + lcs(first, second, i - 1, j - 1);
        } else {
            int withoutFirst = lcs(first, second, i - 1, j);
            int withoutSecond = lcs(first, second, i, j - 1);
            answer = Math.max(withoutFirst, withoutSecond);
        }
        // @a combine
        return answer;
    }
}
