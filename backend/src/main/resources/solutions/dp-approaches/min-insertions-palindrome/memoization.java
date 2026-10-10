public class Solution {
    public int solve(String text) {
        int n = text.length();
        Integer[][] memo = new Integer[n + 1][n + 1];
        // @a start
        int answer = solve(0, n - 1, text, memo);
        // @a done
        return answer;
    }

    private int solve(int left, int right, String text, Integer[][] memo) {
        // @a enter
        if (memo[left][right + 1] != null) {
            // @a hit
            return memo[left][right + 1];
        }
        if (left >= right) {
            // @a base
            return memo[left][right + 1] = 0;
        }
        // @a branch
        int value;
        if (text.charAt(left) == text.charAt(right)) {
            value = solve(left + 1, right - 1, text, memo);
        } else {
            int removeLeft = solve(left + 1, right, text, memo);
            int removeRight = solve(left, right - 1, text, memo);
            value = 1 + Math.min(removeLeft, removeRight);
        }
        // @a combine
        return memo[left][right + 1] = value;
    }
}
