public class Solution {
    public int solve(String text) {
        int n = text.length();
        // @a start
        int answer = solve(0, n - 1, text);
        // @a done
        return answer;
    }

    private int solve(int left, int right, String text) {
        // @a enter
        if (left >= right) {
            // @a base
            return 0;
        }
        // @a branch
        int value;
        if (text.charAt(left) == text.charAt(right)) {
            value = solve(left + 1, right - 1, text);
        } else {
            int removeLeft = solve(left + 1, right, text);
            int removeRight = solve(left, right - 1, text);
            value = 1 + Math.min(removeLeft, removeRight);
        }
        // @a combine
        return value;
    }
}
