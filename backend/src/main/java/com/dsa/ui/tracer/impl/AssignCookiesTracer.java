package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Assign Cookies — the smallest-fit greedy. Sort both sides, then never waste a cookie on
 * a child it does not need to satisfy: hand the least-greedy unsatisfied child the
 * smallest cookie that still meets their greed, and move on either way.
 */
@Component
public class AssignCookiesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "assign-cookies";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("g", FieldType.INT_ARRAY)
                        .label("Greed factors")
                        .help("g[i] is the smallest cookie size that satisfies child i.")
                        .length(1, 40).values(1, 30)
                        .defaultValue(List.of(1, 2, 3))
                        .build(),
                InputField.of("s", FieldType.INT_ARRAY)
                        .label("Cookie sizes")
                        .help("Size of each cookie available to hand out.")
                        .length(0, 40).values(1, 30)
                        .defaultValue(List.of(1, 1))
                        .build());
    }

    /** Enough cookies for every child this time, so the greedy never runs out early. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "g", List.of(1, 2),
                "s", List.of(1, 2, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public int findContentChildren(int[] g, int[] s) {
                   // @a sort
                   Arrays.sort(g);
                   Arrays.sort(s);
                   // @a init
                   int i = 0, j = 0, count = 0;
                   while (i < g.length && j < s.length) {
                       // @a compare
                       if (s[j] >= g[i]) {
                           // @a satisfy
                           count++;
                           i++;
                       }
                       // @a advance
                       j++;
                   }
                   // @a done
                   return count;
               }""";
    }

    private List<ArrayElement> board(int[] vals, int satisfiedUpTo, int cursor) {
        List<ArrayElement> state = new ArrayList<>(vals.length);
        for (int k = 0; k < vals.length; k++) {
            String s = k < satisfiedUpTo ? "sorted" : k == cursor ? "current" : "target";
            state.add(new ArrayElement(k, vals[k], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] g = in.getIntArray("g");
        int[] s = in.getIntArray("s");

        Arrays.sort(g);
        Arrays.sort(s);

        emit.at("sort").say("Sort greed factors %s and cookie sizes %s ascending — the smallest child and the smallest cookie meet first.",
                        Arrays.toString(g), Arrays.toString(s))
                .var("g", Arrays.toString(g)).var("s", Arrays.toString(s))
                .array(g).step();

        int i = 0, j = 0, count = 0;

        emit.at("init").say("Two pointers: i walks the children, j walks the cookies. Nobody satisfied yet.")
                .var("count", count).var("i", i).var("j", j)
                .array(g, 0).step();

        while (i < g.length && j < s.length) {
            emit.at("compare").say("Child %d needs at least %d. Cookie %d is size %d.", i, g[i], j, s[j])
                    .var("count", count).var("i", i).var("j", j)
                    .array(g, i).step();

            if (s[j] >= g[i]) {
                count++;
                emit.at("satisfy").say("%d >= %d — this cookie satisfies child %d. Move to the next child.",
                                s[j], g[i], i)
                        .var("count", count).var("i", i + 1).var("j", j)
                        .array(g, Math.min(i + 1, g.length - 1)).step();
                i++;
            } else {
                emit.at("advance").say("%d < %d — cookie %d is too small for child %d; it is wasted on nobody.",
                                s[j], g[i], j, i)
                        .var("count", count).var("i", i).var("j", j + 1)
                        .array(g, i).step();
            }
            j++;
        }

        emit.at("done").say("Ran out of %s. %d of %d children got a cookie big enough for them.",
                        i >= g.length ? "children" : "cookies", count, g.length)
                .var("count", count)
                .array(g, -1, -1).step();
    }
}
