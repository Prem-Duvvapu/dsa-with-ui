package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Appending '0' is always legal - it never creates a "11". Appending '1' is legal only when
 * the character just placed was not itself '1', which is exactly the one bit of state the
 * recursion needs to carry forward: not the whole prefix, just what its last character was.
 */
@Component
public class GenerateBinaryStringsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "generate-binary-strings";
    }

    @Override
    public DsType dsType() {
        return DsType.STRING;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("n", FieldType.INT)
                        .label("Length (N)")
                        .range(1, 12)
                        .defaultValue(3)
                        .build());
    }

    /** One shorter - three strings instead of five. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               public List<String> generate(int n) {
                   List<String> res = new ArrayList<>();
                   backtrack(n, '0', "", res);
                   // @a done
                   return res;
               }

               private void backtrack(int remaining, char last, String cur, List<String> res) {
                   if (remaining == 0) {
                       // @a capture
                       res.add(cur);
                       return;
                   }
                   // @a appendZero
                   backtrack(remaining - 1, '0', cur + '0', res);
                   if (last != '1') {
                       // @a appendOne
                       backtrack(remaining - 1, '1', cur + '1', res);
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        List<String> res = new ArrayList<>();
        backtrack(n, '0', "", res, emit);

        emit.at("done")
                .say("Every position filled at every leaf. %d string%s of length %d generated with no two "
                        + "consecutive 1s.", res.size(), res.size() == 1 ? "" : "s", n)
                .var("count", res.size()).chars("").step();
    }

    private void backtrack(int remaining, char last, String cur, List<String> res, StepEmitter emit) {
        emit.push("backtrack(remaining=" + remaining + ")");

        if (remaining == 0) {
            res.add(cur);
            emit.at("capture")
                    .say("Length reached - capture \"%s\" as string #%d.", cur, res.size())
                    .var("string", cur).var("count", res.size()).chars(cur).step();
            emit.pop();
            return;
        }

        emit.at("appendZero")
                .say("Append '0' to \"%s\" - always legal, recurse with %d position(s) left.", cur, remaining - 1)
                .var("prefix", cur + "0").chars(cur + "0").step();
        backtrack(remaining - 1, '0', cur + "0", res, emit);

        if (last != '1') {
            emit.at("appendOne")
                    .say("Last character was not '1', so appending '1' to \"%s\" is legal too - recurse.", cur)
                    .var("prefix", cur + "1").chars(cur + "1").step();
            backtrack(remaining - 1, '1', cur + "1", res, emit);
        }

        emit.pop();
    }
}
