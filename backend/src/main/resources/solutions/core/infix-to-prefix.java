import java.util.ArrayDeque;
import java.util.Deque;

public class Solution {
    public String solve(String expression) {
        requireInfix(expression);
        // @a init
        String reversed = reverseAndSwap(expression);
        StringBuilder result = new StringBuilder();
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : reversed.toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                // @a operand
                result.append(c);
            } else if (c == '(') {
                // @a openParen
                stack.push(c);
            } else if (c == ')') {
                while (stack.peek() != '(') {
                    // @a closeParen
                    result.append(stack.pop());
                }
                stack.pop();
            } else {
                while (!stack.isEmpty() && stack.peek() != '(' && popsBefore(stack.peek(), c)) {
                    // @a higherPrec
                    result.append(stack.pop());
                }
                // @a pushOp
                stack.push(c);
            }
        }
        while (!stack.isEmpty()) {
            // @a flush
            result.append(stack.pop());
        }
        // @a done
        return result.reverse().toString();
    }

    // Reversing the scan reverses associativity: equal precedence pops ONLY for '^'.
    private static boolean popsBefore(char top, char arriving) {
        return precedence(top) > precedence(arriving)
                || (precedence(top) == precedence(arriving) && arriving == '^');
    }

    private static int precedence(char operator) {
        return switch (operator) {
            case '+', '-' -> 1;
            case '*', '/' -> 2;
            case '^' -> 3;
            default -> 0;
        };
    }

    private static String reverseAndSwap(String expression) {
        StringBuilder reversed = new StringBuilder();
        for (int index = expression.length() - 1; index >= 0; index--) {
            char token = expression.charAt(index);
            reversed.append(token == '(' ? ')' : token == ')' ? '(' : token);
        }
        return reversed.toString();
    }

    // The visualizer additionally caps input at 40 characters; this solver has no UI size cap.
    // Grammar: single A-Z operands, binary + - * / ^, balanced groups; no spaces or unary operators.
    private static void requireInfix(String expression) {
        if (expression == null || expression.isEmpty()) throw new IllegalArgumentException("Expression is required.");
        boolean needsOperand = true;
        int depth = 0;
        for (char token : expression.toCharArray()) {
            if (token >= 'A' && token <= 'Z') {
                if (!needsOperand) throw new IllegalArgumentException("Expected a binary operator.");
                needsOperand = false;
            } else if (token == '(') {
                if (!needsOperand) throw new IllegalArgumentException("Write an operator before '('.");
                depth++;
            } else if (token == ')') {
                if (needsOperand || depth == 0) throw new IllegalArgumentException("Invalid closing parenthesis.");
                depth--;
            } else if ("+-*/^".indexOf(token) >= 0) {
                if (needsOperand) throw new IllegalArgumentException("Expected an operand.");
                needsOperand = true;
            } else throw new IllegalArgumentException("Unsupported character.");
        }
        if (needsOperand || depth != 0) throw new IllegalArgumentException("Incomplete expression or unmatched parentheses.");
    }
}
