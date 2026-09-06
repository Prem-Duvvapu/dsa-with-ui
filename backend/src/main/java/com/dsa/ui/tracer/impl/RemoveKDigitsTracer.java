package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Remove K Digits (LeetCode 402). Build the smallest number by maintaining a monotonic
 * increasing stack of digit characters. When a smaller digit arrives, pop larger digits
 * from the top (up to k removals), then push the new digit. Leading zeros and remaining
 * quota are handled at the end.
 */
@Component
public class RemoveKDigitsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "remove-k-digits";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("num", FieldType.STRING)
                        .label("Number string")
                        .help("A non-negative integer as a digit string.")
                        .length(1, 20)
                        .constraint("pattern", "[0-9]{1,20}")
                        .constraint("patternHint", "Digits only, e.g. \"1432219\".")
                        .defaultValue("1432219")
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Digits to remove")
                        .help("Remove exactly k digits to make the smallest possible number.")
                        .range(0, 19)
                        .defaultValue(3)
                        .build());
    }

    /** Monotonically increasing digits: forces all removals to happen from the tail via trimTail. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("num", "12345", "k", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               public String removeKdigits(String num, int k) {
                   // @a init
                   Deque<Character> stack = new ArrayDeque<>();
                   for (char c : num.toCharArray()) {
                       while (k > 0 && !stack.isEmpty() && stack.peek() > c) {
                           // @a pop
                           stack.pop();
                           k--;
                       }
                       // @a push
                       stack.push(c);
                   }
                   while (k > 0) {
                       // @a trimTail
                       stack.pop();
                       k--;
                   }
                   // @a build
                   StringBuilder sb = new StringBuilder();
                   for (char c : stack) sb.append(c);
                   String result = sb.reverse().toString().replaceFirst("^0+", "");
                   // @a done
                   return result.isEmpty() ? "0" : result;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String num = in.getString("num");
        int k = in.getInt("k");
        Deque<Character> stack = new ArrayDeque<>();

        emit.at("init")
                .say("Remove %d digits from \"%s\" to form the smallest number. Use a monotonic increasing stack.",
                        k, num)
                .var("num", num).var("k", k)
                .chars(num, -1).stack(stack).step();

        int remaining = k;
        for (int i = 0; i < num.length(); i++) {
            char c = num.charAt(i);

            while (remaining > 0 && !stack.isEmpty() && stack.peek() > c) {
                char popped = stack.pop();
                remaining--;
                emit.at("pop")
                        .say("'%c' > '%c' — pop '%c' (removal %d of %d). Smaller digit ahead makes this one wasteful.",
                                popped, c, popped, k - remaining, k)
                        .var("popped", popped).var("remaining", remaining)
                        .chars(num, i).stack(stack).step();
            }

            stack.push(c);
            emit.at("push")
                    .say("Push '%c'. Stack maintains increasing order from bottom to top.", c)
                    .var("pushed", c).var("stackSize", stack.size())
                    .chars(num, i).stack(stack).step();
        }

        while (remaining > 0) {
            char popped = stack.pop();
            remaining--;
            emit.at("trimTail")
                    .say("Still %d removals left — pop '%c' from the tail.", remaining + 1, popped)
                    .var("popped", popped).var("remaining", remaining)
                    .chars(num).stack(stack).step();
        }

        StringBuilder sb = new StringBuilder();
        for (char c : stack) sb.append(c);
        String raw = sb.reverse().toString();
        String result = raw.replaceFirst("^0+", "");
        if (result.isEmpty()) result = "0";

        emit.at("build")
                .say("Build result from stack: \"%s\". Strip leading zeros: \"%s\".", raw, result)
                .var("raw", raw).var("result", result)
                .chars(num).stack(stack).step();

        emit.at("done")
                .say("Smallest number after removing %d digits: \"%s\".", k, result)
                .var("answer", result)
                .chars(num).stack(stack).step();
    }
}
