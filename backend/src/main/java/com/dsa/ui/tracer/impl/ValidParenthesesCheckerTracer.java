package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Valid-parenthesis-string greedy: track the minimum and maximum possible open count. */
@Component
public class ValidParenthesesCheckerTracer implements AlgorithmTracer {
    @Override public String id() { return "valid-parentheses-checker"; }
    @Override public DsType dsType() { return DsType.STRING; }
    @Override public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("s", FieldType.STRING).label("Parenthesis string")
                .help("Use only (, ), and *. A star may become either parenthesis or empty.")
                .length(1, 80).constraint("pattern", "[()*]+").defaultValue("(*))").build());
    }
    @Override public Map<String, Object> alternateInput() { return Map.of("s", "(*)))"); }
    @Override public String annotatedCode() {
        return """
               public boolean checkValidString(String s) {
                   // @a init
                   int low = 0, high = 0;
                   for (int i = 0; i < s.length(); i++) {
                       // @a read
                       char ch = s.charAt(i);
                       if (ch == '(') {
                           // @a open
                           low++; high++;
                       } else if (ch == ')') {
                           // @a close
                           low = Math.max(0, low - 1); high--;
                       } else {
                           // @a wildcard
                           low = Math.max(0, low - 1); high++;
                       }
                       if (high < 0) {
                           // @a invalid
                           return false;
                       }
                   }
                   // @a done
                   return low == 0;
               }""";
    }
    @Override public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int low = 0, high = 0;
        emit.at("init").say("Open-count possibilities start at [0, 0].")
                .var("low", low).var("high", high).chars(s).step();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            emit.at("read").say("Read '%s' at index %d with possible opens [%d, %d].", ch, i, low, high)
                    .var("i", i).var("low", low).var("high", high).chars(s, i).step();
            String anchor;
            if (ch == '(') { low++; high++; anchor = "open"; }
            else if (ch == ')') { low = Math.max(0, low - 1); high--; anchor = "close"; }
            else { low = Math.max(0, low - 1); high++; anchor = "wildcard"; }
            emit.at(anchor).say("After '%s', the feasible open-count range is [%d, %d].", ch, low, high)
                    .var("i", i).var("low", low).var("high", high).chars(s, i).step();
            if (high < 0) {
                emit.at("invalid").say("Even the most optimistic interpretation has too many closing parentheses.")
                        .var("i", i).var("low", low).var("high", high).chars(s, i).step();
                return;
            }
        }
        String conclusion = low == 0
                ? "Zero opens is still feasible, so the string is valid."
                : String.format("At least %d opens remain in every interpretation, so the string is invalid.", low);
        emit.at("done").say(conclusion)
                .var("valid", low == 0).var("low", low).var("high", high).chars(s).step();
    }
}
