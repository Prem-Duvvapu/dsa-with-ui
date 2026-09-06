package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Prefix to Postfix.
 *
 * <p>Both notations are bracket-free and encode the same tree; only the operator's position
 * relative to its operands differs. So the conversion never needs to know about precedence
 * at all — it rebuilds the tree bottom-up with an operand stack and re-emits each node with
 * the operator moved from the front to the back.
 *
 * <p>Scanned right to left, as prefix must be. The first value popped is the LEFT operand,
 * so the rebuilt piece is {@code left + right + operator} — swap those two pops and the
 * output is still a valid-looking postfix string for a different expression, which is
 * exactly the kind of error the golden file exists to pin.
 */
@Component
public class PrefixToPostfixTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "prefix-to-postfix";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("expression", FieldType.STRING)
                        .label("Prefix expression")
                        .help("Operator before its operands, e.g. \"*-A/BC-/AKL\". No brackets.")
                        .length(1, 30)
                        .constraint("pattern", "[A-Za-z0-9+\\-*/^]{1,30}")
                        .constraint("patternHint",
                                "Single-character operands (letters or digits) and the "
                                        + "operators + - * / ^ — no brackets, up to 30 characters.")
                        .defaultValue("*-A/BC-/AKL")
                        .build());
    }

    /**
     * Five characters against the default's eleven, and only two operators against five, so
     * the stack never holds more than two entries. Hand-checked: {@code +p*qr} is
     * {@code pqr*+}.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("expression", "+p*qr");
    }

    @Override
    public String annotatedCode() {
        return """
               public String toPostfix(String prefix) {
                   // @a init
                   Deque<String> pieces = new ArrayDeque<>();
                   for (int i = prefix.length() - 1; i >= 0; i--) {
                       char c = prefix.charAt(i);
                       if (isOperand(c)) {
                           // @a operand
                           pieces.push(String.valueOf(c));
                       } else {
                           // @a rebuild
                           String left = pieces.pop();
                           String right = pieces.pop();
                           pieces.push(left + right + c);
                       }
                   }
                   // @a done
                   return pieces.pop();
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String prefix = in.getString("expression");
        Deque<String> pieces = new ArrayDeque<>();

        emit.at("init")
                .say("\"%s\" is prefix, so scan it from the right. No precedence rules are "
                        + "involved: both notations are bracket-free, and the only difference "
                        + "is whether the operator sits in front of its operands or behind them.",
                        prefix)
                .var("expression", prefix)
                .chars(prefix, -1).stack(pieces).step();

        for (int i = prefix.length() - 1; i >= 0; i--) {
            char c = prefix.charAt(i);

            if (InfixToPostfixTracer.isOperand(c)) {
                pieces.push(String.valueOf(c));
                emit.at("operand")
                        .say("'%c' is an operand and is already valid postfix on its own. Push it.", c)
                        .var("i", i).var("depth", pieces.size())
                        .chars(prefix, i).stack(pieces).step();
            } else {
                if (pieces.size() < 2) {
                    throw new InputValidationException(Map.of("expression",
                            "Operator '" + c + "' at position " + i + " has fewer than two "
                                    + "operands, so this is not a well-formed prefix expression."));
                }
                String left = pieces.pop();
                String right = pieces.pop();
                String rebuilt = left + right + c;
                pieces.push(rebuilt);
                emit.at("rebuild")
                        .say("'%c' owns the two pieces on top. Right-to-left, the first popped "
                                + "(%s) is the left operand and the second (%s) the right, so "
                                + "moving the operator behind them gives %s.",
                                c, left, right, rebuilt)
                        .var("i", i).var("built", rebuilt).var("depth", pieces.size())
                        .chars(prefix, i).stack(pieces).step();
            }
        }

        if (pieces.size() != 1) {
            throw new InputValidationException(Map.of("expression",
                    "This leaves " + pieces.size() + " separate pieces, so it is not a "
                            + "well-formed prefix expression."));
        }

        emit.at("done")
                .say("\"%s\" in postfix is \"%s\" — the same tree, every operator moved to "
                        + "the far side of its operands.", prefix, pieces.peek())
                .var("answer", pieces.peek())
                .chars(prefix, -1).stack(pieces).step();
    }
}
