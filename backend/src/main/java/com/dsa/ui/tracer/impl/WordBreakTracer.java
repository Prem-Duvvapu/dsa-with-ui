package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Every recursive call is keyed only by its start index, and two calls that reach the same
 * start index will always explore the exact same remaining problem — so once an index's
 * answer is known, {@code memo} makes every later call to it instant instead of re-running
 * the whole search. Without that memo, overlapping segmentations of a long string would
 * revisit the same suffix exponentially many times.
 */
@Component
public class WordBreakTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "word-break";
    }

    @Override
    public DsType dsType() {
        return DsType.STRING;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("s", FieldType.STRING)
                        .label("String")
                        .length(1, 16)
                        .constraint("pattern", "[a-z]+")
                        .constraint("patternHint", "Lowercase letters only.")
                        .defaultValue("leetcode")
                        .build(),
                InputField.of("wordDict", FieldType.STRING)
                        .label("Dictionary (comma-separated)")
                        .length(1, 60)
                        .constraint("pattern", "[a-z]+(,[a-z]+)*")
                        .constraint("patternHint", "Lowercase words separated by commas, e.g. leet,code.")
                        .defaultValue("leet,code")
                        .build());
    }

    /** No valid segmentation exists at all - the recursion runs to exhaustion at every branch. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "catsandog", "wordDict", "cats,dog,sand,and,cat");
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean wordBreak(String s, Set<String> dict) {
                   return canBreak(0, s, dict, new HashMap<>());
               }

               private boolean canBreak(int start, String s, Set<String> dict, Map<Integer, Boolean> memo) {
                   if (start == s.length()) {
                       // @a fullyBroken
                       return true;
                   }
                   if (memo.containsKey(start)) {
                       // @a memoHit
                       return memo.get(start);
                   }
                   for (int end = start + 1; end <= s.length(); end++) {
                       String word = s.substring(start, end);
                       if (!dict.contains(word)) {
                           // @a notAWord
                           continue;
                       }
                       if (canBreak(end, s, dict, memo)) {
                           // @a matchAndRecurse
                           memo.put(start, true);
                           return true;
                       }
                   }
                   // @a noSegmentation
                   memo.put(start, false);
                   return false;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        Set<String> dict = new HashSet<>();
        for (String w : in.getString("wordDict").split(",")) {
            dict.add(w);
        }
        Map<Integer, Boolean> memo = new HashMap<>();

        boolean result = canBreak(0, s, dict, memo, emit);

        emit.at(result ? "fullyBroken" : "noSegmentation")
                .say("Overall result: \"%s\" %s be segmented using %s.", s, result ? "CAN" : "cannot", dict)
                .var("result", result).chars(s).step();
    }

    private boolean canBreak(int start, String s, Set<String> dict, Map<Integer, Boolean> memo, StepEmitter emit) {
        emit.push("canBreak(start=" + start + ")");

        if (start == s.length()) {
            emit.at("fullyBroken")
                    .say("Reached the end of \"%s\" - every character up to here was consumed by dictionary words.", s)
                    .var("start", start).chars(s, s.length() - 1, -1).step();
            emit.pop();
            return true;
        }

        if (memo.containsKey(start)) {
            boolean cached = memo.get(start);
            emit.at("memoHit")
                    .say("Index %d was already resolved to %s by an earlier call - reuse it instead of searching again.",
                            start, cached)
                    .var("start", start).var("cached", cached).chars(s, start, -1).step();
            emit.pop();
            return cached;
        }

        for (int end = start + 1; end <= s.length(); end++) {
            String word = s.substring(start, end);
            if (!dict.contains(word)) {
                emit.at("notAWord")
                        .say("\"%s\" (%d..%d) is not in the dictionary - try a longer or shorter cut.", word, start, end)
                        .var("candidate", word).chars(s, start, end - 1).step();
                continue;
            }

            emit.at("matchAndRecurse")
                    .say("\"%s\" (%d..%d) is a dictionary word - check whether the rest, starting at index %d, can also break.",
                            word, start, end, end)
                    .var("word", word).chars(s, start, end - 1).step();
            if (canBreak(end, s, dict, memo, emit)) {
                memo.put(start, true);
                emit.pop();
                return true;
            }
        }

        memo.put(start, false);
        emit.at("noSegmentation")
                .say("No dictionary word starting at index %d leads to a full segmentation.", start)
                .var("start", start).chars(s, start, -1).step();
        emit.pop();
        return false;
    }
}
