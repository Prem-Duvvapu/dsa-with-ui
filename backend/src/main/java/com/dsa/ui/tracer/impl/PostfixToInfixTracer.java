package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Postfix to Infix.
 *
 * <p>Going back to infix is the only one of these conversions that has to ADD something:
 * postfix carries its grouping in the operator order, and infix cannot, so every rebuilt
 * sub-expression is wrapped in brackets. Emitting them unconditionally — rather than working
 * out which are redundant — is what makes the output unambiguous without any precedence rule.
 *
 * <p>Scanned left to right, as postfix must be, so the first pop is the RIGHT operand: the
 * result is {@code (second OP first)}.
 */
@Component
public class PostfixToInfixTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "postfix-to-infix";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("expression", FieldType.STRING)
                        .label("Postfix expression")
                        .help("Operator after its operands, e.g. \"ab*c+\". No brackets.")
                        .length(1, 30)
                        .constraint("pattern", "[A-Za-z0-9+\\-*/^]{1,30}")
                        .constraint("patternHint",
                                "Single-character operands (letters or digits) and the "
                                        + "operators + - * / ^ — no brackets, up to 30 characters.")
                        .defaultValue("ab*c+")
                        .build());
    }

    /**
     * Digits rather than letters, twice the length, and three levels of nesting where the
     * default has two — so the stack reaches depth three and two compound sub-expressions get
     * combined with each other. Hand-checked: {@code 23*54*+9-} is
     * {@code (((2*3)+(5*4))-9)}, which evaluates to 6 + 20 - 9 = 17.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("expression", "23*54*+9-");
    }

    @Override
    public String annotatedCode() {
        return """
               public String toInfix(String postfix) {
                   // @a init
                   Deque<String> operands = new ArrayDeque<>();
                   for (char c : postfix.toCharArray()) {
                       if (isOperand(c)) {
                           // @a operand
                           operands.push(String.valueOf(c));
                       } else {
                           // @a bracket
                           String right = operands.pop();
                           String left = operands.pop();
                           operands.push("(" + left + c + right + ")");
                       }
                   }
                   // @a done
                   return operands.pop();
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String postfix = in.getString("expression");
        Deque<String> operands = new ArrayDeque<>();

        emit.at("init")
                .say("Read \"%s\" left to right. Unlike the other conversions this one has to "
                        + "invent something — infix cannot encode grouping by position, so "
                        + "every sub-expression gets brackets.", postfix)
                .var("expression", postfix)
                .chars(postfix, -1).stack(operands).step();

        for (int i = 0; i < postfix.length(); i++) {
            char c = postfix.charAt(i);

            if (InfixToPostfixTracer.isOperand(c)) {
                operands.push(String.valueOf(c));
                emit.at("operand")
                        .say("'%c' is an operand — push it as a complete sub-expression "
                                + "needing no brackets of its own.", c)
                        .var("i", i).var("depth", operands.size())
                        .chars(postfix, i).stack(operands).step();
            } else {
                if (operands.size() < 2) {
                    throw new InputValidationException(Map.of("expression",
                            "Operator '" + c + "' at position " + i + " has fewer than two "
                                    + "operands, so this is not a well-formed postfix expression."));
                }
                String right = operands.pop();
                String left = operands.pop();
                String combined = "(" + left + c + right + ")";
                operands.push(combined);
                emit.at("bracket")
                        .say("'%c' applies to the top two. Forwards, the first popped (%s) is "
                                + "the right operand and the second (%s) the left; bracket "
                                + "them into %s so the grouping survives.",
                                c, right, left, combined)
                        .var("i", i).var("built", combined).var("depth", operands.size())
                        .chars(postfix, i).stack(operands).step();
            }
        }

        if (operands.size() != 1) {
            throw new InputValidationException(Map.of("expression",
                    "This leaves " + operands.size() + " separate sub-expressions, so it is "
                            + "not a well-formed postfix expression."));
        }

        emit.at("done")
                .say("\"%s\" in infix is %s — fully bracketed, so it reads the same way with "
                        + "or without precedence rules.", postfix, operands.peek())
                .var("answer", operands.peek())
                .chars(postfix, -1).stack(operands).step();
    }
}
