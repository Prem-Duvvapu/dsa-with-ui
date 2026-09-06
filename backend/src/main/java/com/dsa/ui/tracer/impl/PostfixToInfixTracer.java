package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Postfix to Infix conversion. Scan left to right: operands push as strings,
 * operators pop two operands and push "(op2 operator op1)".
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
                        .help("Single uppercase letters as operands, +, -, *, / as operators. No spaces.")
                        .length(1, 40)
                        .constraint("pattern", "[A-Z+\\-*/^]{1,40}")
                        .constraint("patternHint",
                                "Use A-Z as operands, +, -, *, /, ^ as operators. E.g. \"ABC*+\"")
                        .defaultValue("ABC*+")
                        .build());
    }

    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("expression", "AB+CD-*");
    }

    @Override
    public String annotatedCode() {
        return """
               public String postfixToInfix(String s) {
                   // @a init
                   Deque<String> stack = new ArrayDeque<>();
                   for (char c : s.toCharArray()) {
                       if (Character.isLetter(c)) {
                           // @a pushOperand
                           stack.push(String.valueOf(c));
                       } else {
                           String op1 = stack.pop();
                           String op2 = stack.pop();
                           // @a combine
                           stack.push("(" + op2 + c + op1 + ")");
                       }
                   }
                   // @a done
                   return stack.peek();
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("expression");
        Deque<String> stack = new ArrayDeque<>();

        emit.at("init")
                .say("Convert postfix \"%s\" to infix. Scan left to right.", s)
                .var("expression", s)
                .chars(s, -1).stack(stack).step();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (Character.isLetter(c)) {
                stack.push(String.valueOf(c));
                emit.at("pushOperand")
                        .say("s[%d] = '%c' is an operand — push \"%c\".", i, c, c)
                        .var("i", i).var("pushed", c)
                        .chars(s, i).stack(stack).step();
            } else {
                String op1 = stack.pop();
                String op2 = stack.pop();
                String combined = "(" + op2 + c + op1 + ")";
                stack.push(combined);
                emit.at("combine")
                        .say("s[%d] = '%c' is an operator — pop \"%s\" and \"%s\", push \"%s\".",
                                i, c, op1, op2, combined)
                        .var("i", i).var("op1", op1).var("op2", op2).var("combined", combined)
                        .chars(s, i).stack(stack).step();
            }
        }

        emit.at("done")
                .say("Conversion complete. Infix: \"%s\".", stack.peek())
                .var("answer", stack.peek())
                .chars(s).stack(stack).step();
    }
}
