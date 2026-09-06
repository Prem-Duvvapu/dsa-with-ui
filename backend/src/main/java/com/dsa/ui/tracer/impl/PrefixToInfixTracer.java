package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Prefix to Infix.
 *
 * <p>The stack holds finished sub-expressions, not operators. Scanning RIGHT to left means
 * an operator is only ever reached once both of its operands have already been read and
 * reduced, so it can be applied on the spot: pop two, wrap them in brackets around it, push
 * the result back as a single operand for whatever operator comes next.
 *
 * <p>Order matters and is easy to get backwards. In prefix the operator precedes its
 * operands, so scanning from the right the FIRST value popped is the left operand — the
 * result is {@code (first OP second)}. Postfix-to-infix pops them the other way round for
 * exactly the same reason.
 *
 * <p>Every bracket in the output is emitted deliberately; the result is fully parenthesised
 * rather than minimally, so no precedence rule is needed to read it back.
 */
@Component
public class PrefixToInfixTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "prefix-to-infix";
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
                        .help("Operator before its operands, e.g. \"*+AB-CD\". No brackets.")
                        .length(1, 30)
                        .constraint("pattern", "[A-Za-z0-9+\\-*/^]{1,30}")
                        .constraint("patternHint",
                                "Single-character operands (letters or digits) and the "
                                        + "operators + - * / ^ — no brackets, up to 30 characters.")
                        .defaultValue("*+AB-CD")
                        .build());
    }

    /**
     * Digits and an unbalanced shape: one operand sits at the very top level while the other
     * is a sub-expression, so the two operators nest instead of sitting side by side as they
     * do in the symmetric default. Hand-checked: {@code +9*26} is {@code (9+(2*6))}, which
     * evaluates to 21.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("expression", "+9*26");
    }

    @Override
    public String annotatedCode() {
        return """
               public String toInfix(String prefix) {
                   // @a init
                   Deque<String> operands = new ArrayDeque<>();
                   for (int i = prefix.length() - 1; i >= 0; i--) {
                       char c = prefix.charAt(i);
                       if (isOperand(c)) {
                           // @a operand
                           operands.push(String.valueOf(c));
                       } else {
                           // @a combine
                           String left = operands.pop();
                           String right = operands.pop();
                           operands.push("(" + left + c + right + ")");
                       }
                   }
                   // @a done
                   return operands.pop();
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String prefix = in.getString("expression");
        Deque<String> operands = new ArrayDeque<>();

        emit.at("init")
                .say("Read \"%s\" from the RIGHT. In prefix an operator sits before its "
                        + "operands, so scanning backwards guarantees both operands are "
                        + "already on the stack by the time the operator is reached.", prefix)
                .var("expression", prefix)
                .chars(prefix, -1).stack(operands).step();

        for (int i = prefix.length() - 1; i >= 0; i--) {
            char c = prefix.charAt(i);

            if (InfixToPostfixTracer.isOperand(c)) {
                operands.push(String.valueOf(c));
                emit.at("operand")
                        .say("'%c' is an operand. Push it — a one-character sub-expression, "
                                + "already complete.", c)
                        .var("i", i).var("depth", operands.size())
                        .chars(prefix, i).stack(operands).step();
            } else {
                if (operands.size() < 2) {
                    throw new InputValidationException(Map.of("expression",
                            "Operator '" + c + "' at position " + i + " has fewer than two "
                                    + "operands, so this is not a well-formed prefix expression."));
                }
                String left = operands.pop();
                String right = operands.pop();
                String combined = "(" + left + c + right + ")";
                operands.push(combined);
                emit.at("combine")
                        .say("'%c' takes the two sub-expressions on top. Scanning from the "
                                + "right, the first popped (%s) is its LEFT operand and the "
                                + "second (%s) its right, so they wrap as %s.",
                                c, left, right, combined)
                        .var("i", i).var("built", combined).var("depth", operands.size())
                        .chars(prefix, i).stack(operands).step();
            }
        }

        if (operands.size() != 1) {
            throw new InputValidationException(Map.of("expression",
                    "This leaves " + operands.size() + " separate sub-expressions, so it is "
                            + "not a well-formed prefix expression."));
        }

        emit.at("done")
                .say("One sub-expression left, so the parse is complete: \"%s\" in infix is "
                        + "%s.", prefix, operands.peek())
                .var("answer", operands.peek())
                .chars(prefix, -1).stack(operands).step();
    }
}
