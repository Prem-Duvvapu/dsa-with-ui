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
public class InfixToPrefixTracer implements AlgorithmTracer {

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
                        .help("Single uppercase letters as operands, +, -, *, /, ^, and parentheses.")
                        .length(1, 40)
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

    @Override
    public String annotatedCode() {
        return """
               public String infixToPrefix(String s) {
                   // @a init
                   String reversed = reverseAndSwapParens(s);
                   StringBuilder result = new StringBuilder();
                   Deque<Character> stack = new ArrayDeque<>();
                   for (char c : reversed.toCharArray()) {
                       if (Character.isLetter(c)) {
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
                           while (!stack.isEmpty() && precedence(stack.peek()) > precedence(c)) {
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
               }""";
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
                // Strictly greater (not >=) for right-to-left associativity in reverse pass
                while (!stack.isEmpty() && stack.peek() != '(' && precedence(stack.peek()) > precedence(c)) {
                    char popped = stack.pop();
                    result.append(popped);
                    emit.at("higherPrec")
                            .say("'%c' (prec %d) < '%c' (prec %d) on top — pop '%c'.",
                                    c, precedence(c), popped, precedence(popped), popped)
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
