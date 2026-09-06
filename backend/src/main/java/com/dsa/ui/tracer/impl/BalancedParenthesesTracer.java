package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Valid Parentheses (LeetCode 20). The stack exists because closing brackets must match in
 * reverse order of opening: the most recently opened bracket is the only one that may close
 * next, which is exactly "top of the stack".
 *
 * <p>Two ways to be unbalanced, and the trace shows both: a closer that does not match the
 * top (or arrives with nothing open at all), and openers still sitting on the stack when the
 * string runs out. The first stops the scan; the second is only visible at the end.
 */
@Component
public class BalancedParenthesesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "balanced-parentheses";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("expression", FieldType.STRING)
                        .label("Bracket string")
                        .help("Round, square and curly brackets only, e.g. \"{[()]}()\".")
                        .length(1, 30)
                        .constraint("pattern", "[()\\[\\]{}]{1,30}")
                        .constraint("patternHint",
                                "Use only ( ) [ ] { } — up to 30 characters, e.g. \"{[()]}()\".")
                        .defaultValue("{[()]}()")
                        .build());
    }

    /**
     * Unbalanced by crossing rather than by count: "([)]" has one of each bracket and closes
     * ')' while '[' is still open, so it fails on the mismatch branch the balanced default
     * never reaches.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("expression", "([)]");
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isValid(String s) {
                   // @a init
                   Deque<Character> stack = new ArrayDeque<>();
                   for (char c : s.toCharArray()) {
                       if (c == '(' || c == '[' || c == '{') {
                           // @a push
                           stack.push(c);
                           continue;
                       }
                       if (stack.isEmpty() || stack.peek() != opener(c)) {
                           // @a mismatch
                           return false;
                       }
                       // @a match
                       stack.pop();
                   }
                   // @a done
                   return stack.isEmpty();
               }""";
    }

    private static char opener(char closer) {
        return switch (closer) {
            case ')' -> '(';
            case ']' -> '[';
            default -> '{';
        };
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("expression");
        Deque<Character> stack = new ArrayDeque<>();

        emit.at("init")
                .say("Scan \"%s\" left to right with an empty stack. Every opener waits on "
                        + "the stack until its own closer arrives.", s)
                .var("expression", s).var("length", s.length())
                .chars(s, -1).stack(stack).step();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);
                emit.at("push")
                        .say("s[%d] = '%c' opens. Push it; it is now the only bracket that "
                                + "may be closed next.", i, c)
                        .var("i", i).var("pushed", c).var("depth", stack.size())
                        .chars(s, i).stack(stack).step();
                continue;
            }

            char needed = opener(c);
            if (stack.isEmpty()) {
                emit.at("mismatch")
                        .say("s[%d] = '%c' closes, but nothing is open — the stack is empty, "
                                + "so this closer has no partner. Not balanced.", i, c)
                        .var("i", i).var("closer", c).var("answer", false)
                        .chars(s, i).stack(stack).step();
                return;
            }
            if (stack.peek() != needed) {
                emit.at("mismatch")
                        .say("s[%d] = '%c' needs '%c' on top, but the top is '%c' — the "
                                + "brackets cross instead of nesting. Not balanced.",
                                i, c, needed, stack.peek())
                        .var("i", i).var("closer", c).var("top", stack.peek()).var("answer", false)
                        .chars(s, i).stack(stack).step();
                return;
            }

            stack.pop();
            emit.at("match")
                    .say("s[%d] = '%c' matches the '%c' on top. Pop it — that pair is closed.",
                            i, c, needed)
                    .var("i", i).var("matched", needed).var("depth", stack.size())
                    .chars(s, i).stack(stack).step();
        }

        boolean balanced = stack.isEmpty();
        emit.at("done")
                .say(balanced
                        ? String.format("End of the string with nothing left open. \"%s\" is balanced.", s)
                        : String.format("End of the string, but %d opener(s) never closed. \"%s\" is not balanced.",
                                stack.size(), s))
                .var("answer", balanced).var("leftOpen", stack.size())
                .chars(s, -1).stack(stack).step();
    }
}
