package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Prefix to Postfix conversion. Scan right to left: operands push as strings,
 * operators pop two operands and push "op1 op2 operator".
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
                        .help("Single uppercase letters as operands, +, -, *, / as operators. No spaces.")
                        .length(1, 40)
                        .constraint("pattern", "[A-Z+\\-*/^]{1,40}")
                        .constraint("patternHint",
                                "Use A-Z as operands, +, -, *, /, ^ as operators. E.g. \"+A*BC\"")
                        .defaultValue("+A*BC")
                        .build());
    }

    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("expression", "*-AB+CD");
    }

    @Override
    public String annotatedCode() {
        return """
               public String prefixToPostfix(String s) {
                   // @a init
                   Deque<String> stack = new ArrayDeque<>();
                   for (int i = s.length() - 1; i >= 0; i--) {
                       char c = s.charAt(i);
                       if (Character.isLetter(c)) {
                           // @a pushOperand
                           stack.push(String.valueOf(c));
                       } else {
                           String op1 = stack.pop();
                           String op2 = stack.pop();
                           // @a combine
                           stack.push(op1 + op2 + c);
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
                .say("Convert prefix \"%s\" to postfix. Scan right to left.", s)
                .var("expression", s)
                .chars(s, -1).stack(stack).step();

        for (int i = s.length() - 1; i >= 0; i--) {
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
                String combined = op1 + op2 + c;
                stack.push(combined);
                emit.at("combine")
                        .say("s[%d] = '%c' is an operator — pop \"%s\" and \"%s\", push \"%s\".",
                                i, c, op1, op2, combined)
                        .var("i", i).var("op1", op1).var("op2", op2).var("combined", combined)
                        .chars(s, i).stack(stack).step();
            }
        }

        emit.at("done")
                .say("Conversion complete. Postfix: \"%s\".", stack.peek())
                .var("answer", stack.peek())
                .chars(s).stack(stack).step();
    }
}
