import java.util.ArrayDeque;
import java.util.Deque;

public class Solution {
    public boolean solve(String expression) {
        // @a init
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : expression.toCharArray()) {
            if (c == '(' || c == '[' || c == '{') {
                // @a push
                stack.push(c);
                continue;
            }
            if (stack.isEmpty() || stack.peek() != opener(c)) {
                // @a mismatch
                return false;
            }
            // @a match
            stack.pop();
        }
        // @a done
        return stack.isEmpty();
    }

    // The problem's alphabet is ()[]{} only.
    private char opener(char closer) {
        return switch (closer) {
            case ')' -> '(';
            case ']' -> '[';
            case '}' -> '{';
            default -> throw new IllegalArgumentException("Not a closing bracket: " + closer);
        };
    }
}
