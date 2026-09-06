package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Infix to Postfix (the Shunting-yard conversion).
 *
 * <p>Postfix needs no brackets because an operator's operands are decided by position alone.
 * Getting there means emitting operands the moment they are read and holding operators back
 * until everything that must bind tighter has already been written — which is what the stack
 * is for. An arriving operator flushes every stacked operator that binds at least as tightly
 * (strictly more tightly for right-associative {@code ^}, so {@code a^b^c} groups as
 * {@code a^(b^c)}), and a {@code (} is an impassable floor that {@code )} removes.
 *
 * <p>Neither bracket ever reaches the output: the grouping they expressed is carried by the
 * order the operators come off the stack instead.
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
                        .help("Single-character operands, operators + - * / ^, and brackets.")
                        .length(1, 40)
                        .constraint("pattern", "[A-Za-z0-9+\\-*/^()]{1,40}")
                        .constraint("patternHint",
                                "Single-character operands (letters or digits), the operators "
                                        + "+ - * / ^, and round brackets — up to 40 characters.")
                        .defaultValue("a+b*(c^d-e)^(f+g*h)-i")
                        .build());
    }

    /**
     * Bracket-first and division-heavy rather than the default's nested exponentiation: it
     * never touches the right-associative {@code ^} branch, is half the length, and its
     * bracketed group closes before the first stacked operator is ever compared.
     * Hand-checked: {@code (p+q)/r-s} converts to {@code pq+r/s-}.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("expression", "(p+q)/r-s");
    }

    @Override
    public String annotatedCode() {
        return """
               public String toPostfix(String s) {
                   // @a init
                   StringBuilder out = new StringBuilder();
                   Deque<Character> ops = new ArrayDeque<>();
                   for (char c : s.toCharArray()) {
                       if (isOperand(c)) {
                           // @a operand
                           out.append(c);
                       } else if (c == '(') {
                           // @a lparen
                           ops.push(c);
                       } else if (c == ')') {
                           while (ops.peek() != '(') {
                               // @a rparen
                               out.append(ops.pop());
                           }
                           ops.pop();
                       } else {
                           while (!ops.isEmpty() && ops.peek() != '('
                                   && binds(ops.peek(), c)) {
                               // @a popHigher
                               out.append(ops.pop());
                           }
                           // @a pushOp
                           ops.push(c);
                       }
                   }
                   while (!ops.isEmpty()) {
                       // @a flush
                       out.append(ops.pop());
                   }
                   // @a done
                   return out.toString();
               }""";
    }

    static boolean isOperand(char c) {
        return Character.isLetterOrDigit(c);
    }

    static int precedence(char op) {
        return switch (op) {
            case '^' -> 3;
            case '*', '/' -> 2;
            default -> 1;
        };
    }

    /** Whether the stacked operator must come off before the arriving one goes on. */
    static boolean binds(char stacked, char arriving) {
        int s = precedence(stacked);
        int a = precedence(arriving);
        return s > a || (s == a && arriving != '^');
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("expression");
        StringBuilder out = new StringBuilder();
        Deque<Character> ops = new ArrayDeque<>();

        emit.at("init")
                .say("Convert \"%s\". Operands go straight to the output; operators wait on "
                        + "the stack until everything that binds at least as tightly has "
                        + "already been written.", s)
                .var("output", "").var("expression", s)
                .chars(s, -1).stack(ops).step();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (isOperand(c)) {
                out.append(c);
                emit.at("operand")
                        .say("'%c' is an operand. Its position in the output is already "
                                + "settled, so append it immediately.", c)
                        .var("i", i).var("output", out.toString())
                        .chars(s, i).stack(ops).step();
            } else if (c == '(') {
                ops.push(c);
                emit.at("lparen")
                        .say("'(' opens a group. Push it as a floor — nothing below it may be "
                                + "popped until the matching ')' arrives.")
                        .var("i", i).var("output", out.toString())
                        .chars(s, i).stack(ops).step();
            } else if (c == ')') {
                while (!ops.isEmpty() && ops.peek() != '(') {
                    char popped = ops.pop();
                    out.append(popped);
                    emit.at("rparen")
                            .say("')' closes the group: '%c' belongs inside it, so it comes "
                                    + "off now, ahead of anything outside the brackets.", popped)
                            .var("i", i).var("output", out.toString())
                            .chars(s, i).stack(ops).step();
                }
                if (ops.isEmpty()) {
                    throw new InputValidationException(Map.of("expression",
                            "A ')' at position " + i + " has no matching '('."));
                }
                ops.pop();
            } else {
                while (!ops.isEmpty() && ops.peek() != '(' && binds(ops.peek(), c)) {
                    char popped = ops.pop();
                    out.append(popped);
                    emit.at("popHigher")
                            .say("'%c' is on the stack and binds %s '%c', so its operands are "
                                    + "already complete — write it out before '%c' goes on.",
                                    popped,
                                    precedence(popped) > precedence(c) ? "tighter than"
                                            : "as tightly as (and left to right, so first before)",
                                    c, c)
                            .var("i", i).var("output", out.toString())
                            .chars(s, i).stack(ops).step();
                }
                ops.push(c);
                emit.at("pushOp")
                        .say("Push '%c'. It waits until something binding less tightly arrives, "
                                + "or the expression ends.", c)
                        .var("i", i).var("output", out.toString())
                        .chars(s, i).stack(ops).step();
            }
        }

        while (!ops.isEmpty()) {
            char popped = ops.pop();
            if (popped == '(') {
                throw new InputValidationException(Map.of("expression",
                        "A '(' was never closed."));
            }
            out.append(popped);
            emit.at("flush")
                    .say("The expression is finished, so every operator still waiting is "
                            + "complete. Write '%c' out.", popped)
                    .var("output", out.toString())
                    .chars(s, -1).stack(ops).step();
        }

        emit.at("done")
                .say("Postfix form of \"%s\" is \"%s\" — no brackets needed; the order alone "
                        + "records the grouping.", s, out)
                .var("answer", out.toString())
                .chars(s, -1).stack(ops).step();
    }
}
