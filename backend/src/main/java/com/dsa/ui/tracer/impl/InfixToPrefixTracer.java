package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Infix to Prefix (Polish Notation) conversion.
 *
 * <p>The trick: reverse the infix string (swapping parentheses), run a modified
 * infix-to-postfix where precedence is strictly-greater instead of greater-or-equal
 * for left-associative operators, then reverse the result.
 */
@Component
public class InfixToPrefixTracer extends CompleteSourceTracer {

    @Override
    public String id() {
        return "infix-to-prefix";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("expression", FieldType.STRING)
                        .label("Infix expression")
                        .help("Single A-Z operands with binary +, -, *, /, ^ and balanced parentheses. No unary operators, spaces or implicit multiplication (write A*B, not AB or A(B)).")
                        .length(1, 40)
                        .constraint("requireInfixExpression", true)
                        .constraint("pattern", "[A-Z+\\-*/^()]{1,40}")
                        .constraint("patternHint",
                                "Use A-Z as operands, +, -, *, /, ^ as operators, and ( ) for grouping.")
                        .defaultValue("A+B*C")
                        .build());
    }

    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("expression", "(A-B)*(C+D)");
    }

    /**
     * The reverse-scan method mirrors associativity. Higher precedence always pops. Equal precedence
     * pops only for '^': scanning right to left, the left-associative operators must stay stacked
     * (A-B-C becomes --ABC) while the right-associative '^' must leave (A^B^C becomes ^A^BC).
     */
    private static boolean popsBefore(char top, char arriving) {
        return precedence(top) > precedence(arriving)
                || (precedence(top) == precedence(arriving) && arriving == '^');
    }

    private static int precedence(char op) {
        return switch (op) {
            case '+', '-' -> 1;
            case '*', '/' -> 2;
            case '^' -> 3;
            default -> 0;
        };
    }

    private static String reverseAndSwap(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (c == '(') sb.append(')');
            else if (c == ')') sb.append('(');
            else sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("expression");
        String reversed = reverseAndSwap(s);
        StringBuilder result = new StringBuilder();
        Deque<Character> stack = new ArrayDeque<>();

        emit.at("init")
                .say("Convert infix \"%s\" to prefix. Step 1: reverse and swap parens → \"%s\".",
                        s, reversed)
                .var("original", s).var("reversed", reversed)
                .chars(reversed, -1).stack(stack).step();

        for (int i = 0; i < reversed.length(); i++) {
            char c = reversed.charAt(i);

            if (Character.isLetter(c)) {
                result.append(c);
                emit.at("operand")
                        .say("'%c' is an operand — append to intermediate result.", c)
                        .var("char", c).var("result", result.toString())
                        .chars(reversed, i).stack(stack).step();
            } else if (c == '(') {
                stack.push(c);
                emit.at("openParen")
                        .say("'(' (was ')' in original) — push as precedence floor.")
                        .var("char", c)
                        .chars(reversed, i).stack(stack).step();
            } else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    char popped = stack.pop();
                    result.append(popped);
                    emit.at("closeParen")
                            .say("')' (was '(' in original) — pop '%c' to result.", popped)
                            .var("popped", popped).var("result", result.toString())
                            .chars(reversed, i).stack(stack).step();
                }
                if (!stack.isEmpty()) stack.pop();
            } else {
                // The reverse pass mirrors associativity: equal precedence pops only for '^'.
                while (!stack.isEmpty() && stack.peek() != '(' && popsBefore(stack.peek(), c)) {
                    char popped = stack.pop();
                    result.append(popped);
                    String reason = precedence(c) == precedence(popped)
                            ? "Equal '^' precedence in the reversed scan — pop '^' so exponentiation stays right-associative."
                            : String.format("'%c' (prec %d) < '%c' (prec %d) on top — pop '%c'.",
                                    c, precedence(c), popped, precedence(popped), popped);
                    emit.at("higherPrec")
                            .say("%s", reason)
                            .var("popped", popped).var("result", result.toString())
                            .chars(reversed, i).stack(stack).step();
                }
                stack.push(c);
                emit.at("pushOp")
                        .say("Push operator '%c' (precedence %d).", c, precedence(c))
                        .var("operator", c)
                        .chars(reversed, i).stack(stack).step();
            }
        }

        while (!stack.isEmpty()) {
            char popped = stack.pop();
            result.append(popped);
            emit.at("flush")
                    .say("Flush '%c' from remaining stack.", popped)
                    .var("popped", popped).var("result", result.toString())
                    .chars(reversed).stack(stack).step();
        }

        String prefix = result.reverse().toString();
        emit.at("done")
                .say("Reverse intermediate \"%s\" → prefix: \"%s\".", result.reverse().toString(), prefix)
                .var("answer", prefix)
                .chars(s).stack(stack).step();
    }
}
