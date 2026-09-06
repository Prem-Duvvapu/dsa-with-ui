package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Shunting-yard conversion from infix to postfix (Reverse Polish Notation).
 *
 * <p>Operators sit on a stack ordered by precedence. An arriving operator pops everything
 * of equal or higher precedence first, because those operators bind more tightly and must
 * appear earlier in the output. Parentheses override that: an opening paren pushes a
 * precedence floor, and a closing paren flushes back to it.
 */
@Component
public class InfixToPostfixTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "infix-to-postfix";
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
                        .defaultValue("A+B*C-D")
                        .build());
    }

    /** Parenthesised: forces a different flush order than the default. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("expression", "(A+B)*(C-D)");
    }

    @Override
    public String annotatedCode() {
        return """
               public String infixToPostfix(String s) {
                   // @a init
                   StringBuilder result = new StringBuilder();
                   Deque<Character> stack = new ArrayDeque<>();
                   for (char c : s.toCharArray()) {
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
                           while (!stack.isEmpty() && precedence(stack.peek()) >= precedence(c)) {
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
                   return result.toString();
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

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("expression");
        StringBuilder result = new StringBuilder();
        Deque<Character> stack = new ArrayDeque<>();

        emit.at("init")
                .say("Convert infix \"%s\" to postfix. Empty operator stack, empty output.", s)
                .var("expression", s).var("result", "")
                .chars(s, -1).stack(stack).step();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (Character.isLetter(c)) {
                result.append(c);
                emit.at("operand")
                        .say("'%c' is an operand — append directly to the output.", c)
                        .var("char", c).var("result", result.toString())
                        .chars(s, i).stack(stack).step();
            } else if (c == '(') {
                stack.push(c);
                emit.at("openParen")
                        .say("'(' opens a group — push it as a precedence floor.", c)
                        .var("char", c)
                        .chars(s, i).stack(stack).step();
            } else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    char popped = stack.pop();
                    result.append(popped);
                    emit.at("closeParen")
                            .say("')' closes the group — pop '%c' to output.", popped)
                            .var("popped", popped).var("result", result.toString())
                            .chars(s, i).stack(stack).step();
                }
                if (!stack.isEmpty()) {
                    stack.pop(); // remove '('
                }
            } else {
                while (!stack.isEmpty() && stack.peek() != '(' && precedence(stack.peek()) >= precedence(c)) {
                    char popped = stack.pop();
                    result.append(popped);
                    emit.at("higherPrec")
                            .say("'%c' (precedence %d) is not higher than '%c' (precedence %d) on top — pop '%c' to output.",
                                    c, precedence(c), popped, precedence(popped), popped)
                            .var("popped", popped).var("result", result.toString())
                            .chars(s, i).stack(stack).step();
                }
                stack.push(c);
                emit.at("pushOp")
                        .say("Push operator '%c' (precedence %d) onto the stack.", c, precedence(c))
                        .var("operator", c).var("precedence", precedence(c))
                        .chars(s, i).stack(stack).step();
            }
        }

        while (!stack.isEmpty()) {
            char popped = stack.pop();
            result.append(popped);
            emit.at("flush")
                    .say("Flush remaining operator '%c' from the stack to output.", popped)
                    .var("popped", popped).var("result", result.toString())
                    .chars(s).stack(stack).step();
        }

        emit.at("done")
                .say("Conversion complete. Postfix: \"%s\".", result.toString())
                .var("answer", result.toString())
                .chars(s).stack(stack).step();
    }
}
