package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Every cut point is tried: for each candidate end index, the substring from the current
 * start is tested, and only a palindromic one is worth recursing past. A leaf is reached
 * only when the whole string has been consumed by palindromic pieces — a single bad cut
 * prunes an entire branch before it ever grows further.
 */
@Component
public class PalindromePartitioningTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "palindrome-partitioning";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("s", FieldType.STRING)
                        .label("String")
                        .help("Letters only.")
                        .length(1, 10)
                        .constraint("pattern", "[A-Za-z]+")
                        .constraint("patternHint", "Letters only.")
                        .defaultValue("aab")
                        .build());
    }

    /** A single character — trivially its own one-piece partition, no branching at all. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "a");
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<String>> partition(String s) {
                   List<List<String>> res = new ArrayList<>();
                   backtrack(0, s, new ArrayList<>(), res);
                   // @a done
                   return res;
               }

               private void backtrack(int start, String s, List<String> path, List<List<String>> res) {
                   if (start == s.length()) {
                       // @a capture
                       res.add(new ArrayList<>(path));
                       return;
                   }
                   for (int end = start + 1; end <= s.length(); end++) {
                       String segment = s.substring(start, end);
                       if (!isPalindrome(segment)) {
                           // @a reject
                           continue;
                       }
                       path.add(segment);
                       // @a accept
                       backtrack(end, s, path, res);
                       path.remove(path.size() - 1);
                       // @a undo
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        List<List<String>> res = new ArrayList<>();
        backtrack(0, s, new ArrayList<>(), res, emit);

        emit.at("done")
                .say("Every start position exhausted. %d valid palindromic partition%s found.",
                        res.size(), res.size() == 1 ? "" : "s")
                .var("partitions", res.size()).stack(List.of()).step();
    }

    private void backtrack(int start, String s, List<String> path, List<List<String>> res, StepEmitter emit) {
        emit.push("backtrack(start=" + start + ")");

        if (start == s.length()) {
            res.add(new ArrayList<>(path));
            emit.at("capture")
                    .say("Reached the end of \"%s\" with every piece a palindrome - capture %s as partition #%d.",
                            s, path, res.size())
                    .var("partition", path.toString()).var("count", res.size()).stack(path).step();
            emit.pop();
            return;
        }

        for (int end = start + 1; end <= s.length(); end++) {
            String segment = s.substring(start, end);
            if (!isPalindrome(segment)) {
                emit.at("reject")
                        .say("\"%s\" (%d..%d) is not a palindrome - skip this cut.", segment, start, end)
                        .var("segment", segment).stack(path).step();
                continue;
            }

            path.add(segment);
            emit.at("accept")
                    .say("\"%s\" (%d..%d) is a palindrome - add it to the path and recurse from index %d.",
                            segment, start, end, end)
                    .var("segment", segment).stack(path).step();
            backtrack(end, s, path, res, emit);

            path.remove(path.size() - 1);
            emit.at("undo")
                    .say("Undo \"%s\" - try a longer cut starting at index %d.", segment, start)
                    .var("removed", segment).stack(path).step();
        }

        emit.pop();
    }

    private boolean isPalindrome(String s) {
        int i = 0, j = s.length() - 1;
        while (i < j) {
            if (s.charAt(i) != s.charAt(j)) {
                return false;
            }
            i++;
            j--;
        }
        return true;
    }
}
