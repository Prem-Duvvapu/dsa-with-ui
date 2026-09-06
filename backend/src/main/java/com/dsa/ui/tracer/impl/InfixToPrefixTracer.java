package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Infix to Prefix, by reduction rather than by a second conversion algorithm.
 *
 * <p>Prefix is postfix read backwards, so the whole problem is solved by reversing the input
 * (swapping the brackets, which point the wrong way once reversed), running the ordinary
 * infix-to-postfix conversion over it, and reversing the result. The one subtlety is
 * associativity: reversing the string reverses the direction "left to right" means, so
 * equal-precedence operators must now be treated as right-associative — the stacked one is
 * only popped when it binds STRICTLY tighter — while {@code ^}, right-associative in the
 * original, becomes left-associative here.
 *
 * <p>Same stack, opposite comparison, three visible phases. Hand-checked against the
 * published example: {@code (A-B/C)*(A/K-L)} converts to {@code *-A/BC-/AKL}.
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
                        .help("Single-character operands, operators + - * / ^, and brackets.")
                        .length(1, 40)
                        .constraint("pattern", "[A-Za-z0-9+\\-*/^()]{1,40}")
                        .constraint("patternHint",
                                "Single-character operands (letters or digits), the operators "
                                        + "+ - * / ^, and round brackets — up to 40 characters.")
                        .defaultValue("(A-B/C)*(A/K-L)")
                        .build());
    }

    /**
     * Bracket-free and a third the length, so the reversal is trivially readable and the
     * whole conversion turns on one precedence comparison rather than on two bracketed
     * groups. Hand-checked: {@code x+y*z} converts to {@code +x*yz}.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("expression", "x+y*z");
    }

    @Override
    public String annotatedCode() {
        return """
               public String toPrefix(String s) {
                   // @a reverse
                   String flipped = reverseAndSwapBrackets(s);
                   StringBuilder out = new StringBuilder();
                   Deque<Character> ops = new ArrayDeque<>();
                   for (char c : flipped.toCharArray()) {
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
                           // Reversed: equal precedence must NOT pop, except for ^.
                           while (!ops.isEmpty() && ops.peek() != '('
                                   && bindsReversed(ops.peek(), c)) {
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
                   // @a reverseBack
                   String prefix = out.reverse().toString();
                   // @a done
                   return prefix;
               }""";
    }

    private static char swap(char c) {
        return c == '(' ? ')' : c == ')' ? '(' : c;
    }

    /** Strictly tighter only — equal precedence keeps its place, because the scan is reversed. */
    private static boolean bindsReversed(char stacked, char arriving) {
        int s = InfixToPostfixTracer.precedence(stacked);
        int a = InfixToPostfixTracer.precedence(arriving);
        return s > a || (s == a && arriving == '^');
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("expression");

        StringBuilder flippedBuilder = new StringBuilder();
        for (int i = s.length() - 1; i >= 0; i--) {
            flippedBuilder.append(swap(s.charAt(i)));
        }
        String flipped = flippedBuilder.toString();

        emit.at("reverse")
                .say("Prefix is postfix read backwards, so start by reversing \"%s\" into "
                        + "\"%s\" — brackets swapped, because a '(' read from the other end "
                        + "is a ')'. Everything after this is the ordinary postfix conversion.",
                        s, flipped)
                .var("input", s).var("reversed", flipped)
                .chars(flipped, -1).stack(new ArrayDeque<>()).step();

        StringBuilder out = new StringBuilder();
        Deque<Character> ops = new ArrayDeque<>();

        for (int i = 0; i < flipped.length(); i++) {
            char c = flipped.charAt(i);

            if (InfixToPostfixTracer.isOperand(c)) {
                out.append(c);
                emit.at("operand")
                        .say("'%c' is an operand — append it to the working output.", c)
                        .var("i", i).var("working", out.toString())
                        .chars(flipped, i).stack(ops).step();
            } else if (c == '(') {
                ops.push(c);
                emit.at("lparen")
                        .say("'(' in the reversed string opens a group. Push it as a floor.")
                        .var("i", i).var("working", out.toString())
                        .chars(flipped, i).stack(ops).step();
            } else if (c == ')') {
                while (!ops.isEmpty() && ops.peek() != '(') {
                    char popped = ops.pop();
                    out.append(popped);
                    emit.at("rparen")
                            .say("')' closes the group, so '%c' comes off now — it belongs "
                                    + "inside the brackets.", popped)
                            .var("i", i).var("working", out.toString())
                            .chars(flipped, i).stack(ops).step();
                }
                if (ops.isEmpty()) {
                    throw new InputValidationException(Map.of("expression",
                            "The brackets in this expression are not balanced."));
                }
                ops.pop();
            } else {
                while (!ops.isEmpty() && ops.peek() != '(' && bindsReversed(ops.peek(), c)) {
                    char popped = ops.pop();
                    out.append(popped);
                    emit.at("popHigher")
                            .say("'%c' binds tighter than '%c', so it comes off before '%c' "
                                    + "goes on. (Equal precedence would NOT pop here — the "
                                    + "scan is reversed, so ties keep their place.)",
                                    popped, c, c)
                            .var("i", i).var("working", out.toString())
                            .chars(flipped, i).stack(ops).step();
                }
                ops.push(c);
                emit.at("pushOp")
                        .say("Push '%c' and carry on.", c)
                        .var("i", i).var("working", out.toString())
                        .chars(flipped, i).stack(ops).step();
            }
        }

        while (!ops.isEmpty()) {
            char popped = ops.pop();
            if (popped == '(') {
                throw new InputValidationException(Map.of("expression",
                        "The brackets in this expression are not balanced."));
            }
            out.append(popped);
            emit.at("flush")
                    .say("Reversed string exhausted — flush '%c'.", popped)
                    .var("working", out.toString())
                    .chars(flipped, -1).stack(ops).step();
        }

        String working = out.toString();
        String prefix = new StringBuilder(working).reverse().toString();

        emit.at("reverseBack")
                .say("The postfix of the reversed expression is \"%s\". Reverse it once more "
                        + "to undo the flip: \"%s\".", working, prefix)
                .var("working", working).var("prefix", prefix)
                .chars(prefix, -1).stack(ops).step();

        emit.at("done")
                .say("Prefix form of \"%s\" is \"%s\".", s, prefix)
                .var("answer", prefix)
                .chars(prefix, -1).stack(ops).step();
    }
}
