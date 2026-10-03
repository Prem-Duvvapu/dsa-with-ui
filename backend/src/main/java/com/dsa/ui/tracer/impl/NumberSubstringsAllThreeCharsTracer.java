package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Number of Substrings Containing All Three Characters (LeetCode 1358), traced on the owner's own
 * accepted submission. Once a, b and c have all appeared, every substring ending at i that starts
 * at or before the earliest of their last positions contains all three - that is
 * 1 + min(lastSeen) substrings. One pass, O(n).
 *
 * <p>{@code lastSeen[ch - 'a']} has three slots, so the input is LeetCode's: only a, b and c.
 */
@Component
public class NumberSubstringsAllThreeCharsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "number-substrings-all-three-chars";
    }

    @Override
    public DsType dsType() {
        return DsType.WINDOW;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("s", FieldType.STRING)
                        .label("String (a, b, c)")
                        .help("Only the letters a, b and c.")
                        .constraint("pattern", "[abc]{1,40}")
                        .constraint("patternHint", "Only the letters a, b and c.")
                        .length(1, 40)
                        .defaultValue("abcabc")
                        .build());
    }

    /** Alternate string with a run of 'a' before 'c' and 'b'. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "aaacb");
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public int numberOfSubstrings(String s) {
                       // @a init
                       int n=s.length();
                       int[] lastSeen=new int[3];
                       Arrays.fill(lastSeen,-1);
                       int res=0;

                       for (int i=0;i<n;i++) {
                           // @a see
                           char ch=s.charAt(i);
                           lastSeen[ch-'a']=i;

                           if (lastSeen[0]!=-1 && lastSeen[1]!=-1 && lastSeen[2]!=-1) {
                               // @a count
                               int startIndex=Math.min(lastSeen[0],Math.min(lastSeen[1],lastSeen[2]));
                               res+=(1+startIndex);
                           }
                       }

                       // @a done
                       return res;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int n = s.length();
        int[] lastSeen = new int[3];
        Arrays.fill(lastSeen, -1);
        int res = 0;
        emit.at("init").say("lastSeen holds the last index of a, b and c (-1 = not seen yet).")
                .var("lastSeen", Arrays.toString(lastSeen)).var("res", 0).arrayState(WindowCells.of(s, -1, -1)).step();
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            lastSeen[ch - 'a'] = i;
            if (lastSeen[0] != -1 && lastSeen[1] != -1 && lastSeen[2] != -1) {
                int startIndex = Math.min(lastSeen[0], Math.min(lastSeen[1], lastSeen[2]));
                res += 1 + startIndex;
                emit.at("count").say("'%c' at %d. All three have appeared; the earliest of their last positions is %d, so the "
                                + "%d substring%s ending at %d and starting at 0..%d contain all three. res = %d.",
                                ch, i, startIndex, 1 + startIndex, Narration.s(1 + startIndex), i, startIndex, res)
                        .var("i", i).var("lastSeen", Arrays.toString(lastSeen)).var("res", res)
                        .arrayState(WindowCells.of(s, startIndex, i)).step();
            } else {
                emit.at("see").say("'%c' at %d. Not all of a, b and c have appeared yet, so no substring ending here counts.",
                                ch, i)
                        .var("i", i).var("lastSeen", Arrays.toString(lastSeen)).var("res", res)
                        .arrayState(WindowCells.of(s, i, i)).step();
            }
        }
        emit.at("done").say("%d substring%s contain a, b and c.", res, Narration.s(res))
                .var("res", res).var("answer", res).arrayState(WindowCells.of(s, -1, -1)).step();
    }
}
