package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Two counters instead of a validity check after the fact: an open paren can be added
 * whenever fewer than n have been placed, and a close paren only when fewer closes than
 * opens have been placed so far — that second rule is exactly what keeps every prefix
 * balanced, so a leaf at length 2n is guaranteed well-formed without ever verifying it.
 */
@Component
public class GenerateParenthesesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "generate-parentheses";
    }

    @Override
    public DsType dsType() {
        return DsType.STRING;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("n", FieldType.INT)
                        .label("Pairs (N)")
                        .range(1, 6)
                        .defaultValue(3)
                        .build());
    }

    /** One pair fewer - two combinations instead of five. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               public List<String> generateParenthesis(int n) {
                   List<String> res = new ArrayList<>();
                   backtrack(n, 0, 0, "", res);
                   // @a done
                   return res;
               }

               private void backtrack(int n, int open, int close, String cur, List<String> res) {
                   if (cur.length() == 2 * n) {
                       // @a capture
                       res.add(cur);
                       return;
                   }
                   if (open < n) {
                       // @a addOpen
                       backtrack(n, open + 1, close, cur + '(', res);
                   }
                   if (close < open) {
                       // @a addClose
                       backtrack(n, open, close + 1, cur + ')', res);
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        List<String> res = new ArrayList<>();
        backtrack(n, 0, 0, "", res, emit);

        emit.at("done")
                .say("Every position filled at every leaf. %d well-formed combination%s of %d pair(s) found.",
                        res.size(), res.size() == 1 ? "" : "s", n)
                .var("count", res.size()).chars("").step();
    }

    private void backtrack(int n, int open, int close, String cur, List<String> res, StepEmitter emit) {
        emit.push("backtrack(open=" + open + ",close=" + close + ")");

        if (cur.length() == 2 * n) {
            res.add(cur);
            emit.at("capture")
                    .say("Length %d reached - capture \"%s\" as combination #%d.", cur.length(), cur, res.size())
                    .var("combination", cur).var("count", res.size()).chars(cur).step();
            emit.pop();
            return;
        }

        if (open < n) {
            emit.at("addOpen")
                    .say("%d/%d opens placed - add '(' to \"%s\" and recurse.", open, n, cur)
                    .var("prefix", cur + "(").chars(cur + "(").step();
            backtrack(n, open + 1, close, cur + "(", res, emit);
        }

        if (close < open) {
            emit.at("addClose")
                    .say("%d closes trail %d opens - add ')' to \"%s\" and recurse.", close, open, cur)
                    .var("prefix", cur + ")").chars(cur + ")").step();
            backtrack(n, open, close + 1, cur + ")", res, emit);
        }

        emit.pop();
    }
}
