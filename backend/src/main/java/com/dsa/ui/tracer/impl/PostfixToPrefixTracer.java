package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Postfix to Prefix.
 *
 * <p>The mirror of prefix-to-postfix, and the mirror is in the scan direction: postfix puts
 * the operator AFTER its operands, so reading LEFT to right is what guarantees both operands
 * are already on the stack when the operator turns up.
 *
 * <p>That flips the pop order too. Reading forwards, the last operand pushed is the RIGHT
 * one, so the first pop is the right operand and the second is the left — the rebuilt piece
 * is {@code operator + second + first}. Prefix-to-postfix, scanning backwards, uses the
 * opposite assignment for the same reason.
 */
@Component
public class PostfixToPrefixTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "postfix-to-prefix";
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
                        .help("Operator after its operands, e.g. \"AB+CD-*\". No brackets.")
                        .length(1, 30)
                        .constraint("pattern", "[A-Za-z0-9+\\-*/^]{1,30}")
                        .constraint("patternHint",
                                "Single-character operands (letters or digits) and the "
                                        + "operators + - * / ^ — no brackets, up to 30 characters.")
                        .defaultValue("AB+CD-*")
                        .build());
    }

    /**
     * A different letter alphabet and a subtraction at the root rather than a product, so the
     * two sub-expressions are combined in the opposite order from the default and the final
     * operator's operands are both compound. Hand-checked: {@code wx+yz*-} is {@code -+wx*yz}.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("expression", "wx+yz*-");
    }

    @Override
    public String annotatedCode() {
        return """
               public String toPrefix(String postfix) {
                   // @a init
                   Deque<String> pieces = new ArrayDeque<>();
                   for (char c : postfix.toCharArray()) {
                       if (isOperand(c)) {
                           // @a operand
                           pieces.push(String.valueOf(c));
                       } else {
                           // @a rebuild
                           String right = pieces.pop();
                           String left = pieces.pop();
                           pieces.push(c + left + right);
                       }
                   }
                   // @a done
                   return pieces.pop();
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String postfix = in.getString("expression");
        Deque<String> pieces = new ArrayDeque<>();

        emit.at("init")
                .say("\"%s\" is postfix, so scan it LEFT to right — the operator trails its "
                        + "operands, which means both are already on the stack by the time it "
                        + "appears.", postfix)
                .var("expression", postfix)
                .chars(postfix, -1).stack(pieces).step();

        for (int i = 0; i < postfix.length(); i++) {
            char c = postfix.charAt(i);

            if (InfixToPostfixTracer.isOperand(c)) {
                pieces.push(String.valueOf(c));
                emit.at("operand")
                        .say("'%c' is an operand and is already valid prefix on its own. Push it.", c)
                        .var("i", i).var("depth", pieces.size())
                        .chars(postfix, i).stack(pieces).step();
            } else {
                if (pieces.size() < 2) {
                    throw new InputValidationException(Map.of("expression",
                            "Operator '" + c + "' at position " + i + " has fewer than two "
                                    + "operands, so this is not a well-formed postfix expression."));
                }
                String right = pieces.pop();
                String left = pieces.pop();
                String rebuilt = c + left + right;
                pieces.push(rebuilt);
                emit.at("rebuild")
                        .say("'%c' claims the two pieces on top. Scanning forwards, the piece "
                                + "popped first (%s) is the RIGHT operand and the second (%s) "
                                + "the left, so moving the operator in front gives %s.",
                                c, right, left, rebuilt)
                        .var("i", i).var("built", rebuilt).var("depth", pieces.size())
                        .chars(postfix, i).stack(pieces).step();
            }
        }

        if (pieces.size() != 1) {
            throw new InputValidationException(Map.of("expression",
                    "This leaves " + pieces.size() + " separate pieces, so it is not a "
                            + "well-formed postfix expression."));
        }

        emit.at("done")
                .say("\"%s\" in prefix is \"%s\" — every operator moved from behind its "
                        + "operands to in front of them.", postfix, pieces.peek())
                .var("answer", pieces.peek())
                .chars(postfix, -1).stack(pieces).step();
    }
}
