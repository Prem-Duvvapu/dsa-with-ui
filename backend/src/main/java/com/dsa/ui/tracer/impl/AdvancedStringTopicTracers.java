package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
class BracketReversalsTracer extends StringTracerSupport {
    public String id() { return "bracket-reversals"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("brackets", "Curly brackets", "}{{}}{{{", 2, 30,
                "[{}]+", "Use only '{' and '}'. An odd-length string cannot be balanced."));
    }

    public Map<String, Object> alternateInput() { return Map.of("brackets", "{{{{"); }

    public String annotatedCode() {
        return """
               public int minimumReversals(String brackets) {
                   int open = 0, close = 0;
                   for (char bracket : brackets.toCharArray()) {
                       // @a scan
                       if (bracket == '{') open++;
                       else if (open > 0) open--;
                       else close++;
                   }
                   // @a done
                   return (open + 1) / 2 + (close + 1) / 2;
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String value = in.getString("brackets");
        int open = 0;
        int close = 0;
        for (int i = 0; i < value.length(); i++) {
            char bracket = value.charAt(i);
            String decision;
            if (bracket == '{') {
                open++;
                decision = "save an unmatched opening bracket";
            } else if (open > 0) {
                open--;
                decision = "pair this closing bracket with the latest unmatched opening bracket";
            } else {
                close++;
                decision = "save an unmatched closing bracket";
            }
            emit.at("scan").say("Index %d is '%c': %s. Unmatched open=%d, close=%d.",
                            i, bracket, decision, open, close)
                    .var("index", i).var("open", open).var("close", close)
                    .chars(value, i).step();
        }
        int answer = value.length() % 2 == 0 ? (open + 1) / 2 + (close + 1) / 2 : -1;
        emit.at("done").say(answer < 0
                        ? "Odd length cannot be balanced; return -1."
                        : "Reverse ceil(%d/2) opening and ceil(%d/2) closing brackets: %d reversals.",
                        open, close, answer)
                .var("open", open).var("close", close).var("answer", answer)
                .chars(value).step();
    }
}

@Component
class CountAndSayTracer extends StringTracerSupport {
    public String id() { return "count-and-say"; }

    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("n", FieldType.INT).label("Term number")
                .help("Generate the nth look-and-say term.").range(1, 8).defaultValue(5).build());
    }

    public Map<String, Object> alternateInput() { return Map.of("n", 4); }

    public String annotatedCode() {
        return """
               public String countAndSay(int n) {
                   String term = "1";
                   for (int round = 2; round <= n; round++) {
                       StringBuilder next = new StringBuilder();
                       for (int i = 0; i < term.length();) {
                           int end = i + 1;
                           while (end < term.length() && term.charAt(end) == term.charAt(i)) end++;
                           // @a group
                           next.append(end - i).append(term.charAt(i));
                           i = end;
                       }
                       // @a term
                       term = next.toString();
                   }
                   // @a done
                   return term;
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        String term = "1";
        if (n == 1) {
            emit.at("term").say("The first term is the seed \"1\".")
                    .var("round", 1).var("term", term).chars(term).step();
        }
        for (int round = 2; round <= n; round++) {
            StringBuilder next = new StringBuilder();
            for (int i = 0; i < term.length();) {
                int end = i + 1;
                while (end < term.length() && term.charAt(end) == term.charAt(i)) end++;
                int count = end - i;
                next.append(count).append(term.charAt(i));
                emit.at("group").say("Round %d reads %d consecutive '%c' character(s), appending \"%d%c\".",
                                round, count, term.charAt(i), count, term.charAt(i))
                        .var("round", round).var("count", count).var("next", next)
                        .chars(term, i, end - 1).step();
                i = end;
            }
            term = next.toString();
            emit.at("term").say("Round %d is complete: \"%s\".", round, term)
                    .var("round", round).var("term", term).chars(term).step();
        }
        emit.at("done").say("The count-and-say term at n=%d is \"%s\".", n, term)
                .var("n", n).var("answer", term).chars(term).step();
    }
}

@Component
class StringHashingTheoryTracer extends StringTracerSupport {
    public String id() { return "string-hashing-theory"; }

    public InputSpec inputSpec() {
        return InputSpec.of(
                patternedText("text", "Text", "hash", 1, 20, "[a-z]+", "Use lowercase letters a-z."),
                InputField.of("base", FieldType.INT).label("Polynomial base").range(2, 101)
                        .defaultValue(31).build(),
                InputField.of("modulus", FieldType.INT).label("Modulus").range(2, 1_000_000_007)
                        .defaultValue(1_000_000_007).build());
    }

    public Map<String, Object> alternateInput() {
        return Map.of("text", "rolling", "base", 37, "modulus", 100_003);
    }

    public String annotatedCode() {
        return """
               public long polynomialHash(String text, int base, int modulus) {
                   long hash = 0;
                   for (char ch : text.toCharArray()) {
                       // @a update
                       hash = (hash * base + (ch - 'a' + 1)) % modulus;
                   }
                   // @a done
                   return hash;
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String text = in.getString("text");
        int base = in.getInt("base");
        int modulus = in.getInt("modulus");
        long hash = 0;
        for (int i = 0; i < text.length(); i++) {
            long previous = hash;
            int value = text.charAt(i) - 'a' + 1;
            hash = (hash * base + value) % modulus;
            emit.at("update").say("Index %d maps '%c' to %d: (%d × %d + %d) mod %d = %d.",
                            i, text.charAt(i), value, previous, base, value, modulus, hash)
                    .var("index", i).var("characterValue", value).var("hash", hash)
                    .chars(text, i).step();
        }
        emit.at("done").say("Polynomial rolling hash of \"%s\" is %d.", text, hash)
                .var("answer", hash).var("base", base).var("modulus", modulus)
                .chars(text).step();
    }
}

@Component
class RabinKarpTracer extends StringTracerSupport {
    private static final int BASE = 256;
    private static final int MOD = 101;

    public String id() { return "rabin-karp-algo"; }

    public InputSpec inputSpec() {
        return InputSpec.of(
                patternedText("text", "Text", "abracadabra", 1, 30, "[a-z]+", "Use lowercase letters a-z."),
                patternedText("pattern", "Pattern", "abra", 1, 12, "[a-z]+", "Use lowercase letters a-z."));
    }

    public Map<String, Object> alternateInput() { return Map.of("text", "aaaaa", "pattern", "aa"); }

    public String annotatedCode() {
        return """
               public List<Integer> rabinKarp(String text, String pattern) {
                   List<Integer> matches = new ArrayList<>();
                   long patternHash = hash(pattern), windowHash = hash(text.substring(0, pattern.length()));
                   for (int start = 0; start + pattern.length() <= text.length(); start++) {
                       // @a window
                       if (patternHash == windowHash && text.startsWith(pattern, start)) matches.add(start);
                       windowHash = roll(windowHash, text, start, pattern.length());
                   }
                   // @a done
                   return matches;
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String text = in.getString("text");
        String pattern = in.getString("pattern");
        List<Integer> matches = new ArrayList<>();
        if (pattern.length() <= text.length()) {
            long high = 1;
            for (int i = 1; i < pattern.length(); i++) high = high * BASE % MOD;
            long patternHash = hash(pattern);
            long windowHash = hash(text.substring(0, pattern.length()));
            for (int start = 0; start + pattern.length() <= text.length(); start++) {
                boolean hashesMatch = patternHash == windowHash;
                boolean exact = hashesMatch && text.startsWith(pattern, start);
                if (exact) matches.add(start);
                emit.at("window").say("Window [%d,%d] \"%s\" has hash %d; pattern hash is %d. %s",
                                start, start + pattern.length() - 1,
                                text.substring(start, start + pattern.length()), windowHash, patternHash,
                                exact ? "Exact match recorded." : hashesMatch ? "Hash collision rejected by comparison." : "Move on.")
                        .var("start", start).var("windowHash", windowHash)
                        .var("patternHash", patternHash).var("matches", matches)
                        .chars(text, start, start + pattern.length() - 1).step();
                if (start + pattern.length() < text.length()) {
                    windowHash = (windowHash - text.charAt(start) * high) % MOD;
                    if (windowHash < 0) windowHash += MOD;
                    windowHash = (windowHash * BASE + text.charAt(start + pattern.length())) % MOD;
                }
            }
        } else {
            emit.at("window").say("Pattern length %d exceeds text length %d, so no window can match.",
                            pattern.length(), text.length())
                    .var("textLength", text.length()).var("patternLength", pattern.length())
                    .chars(text).step();
        }
        emit.at("done").say("Rabin–Karp found pattern \"%s\" at indices %s.", pattern, matches)
                .var("answer", matches).chars(text).step();
    }

    private static long hash(String value) {
        long hash = 0;
        for (int i = 0; i < value.length(); i++) hash = (hash * BASE + value.charAt(i)) % MOD;
        return hash;
    }
}

@Component
class CountPalindromicSubsequencesTracer extends StringTracerSupport {
    public String id() { return "count-palindromic-subsequences"; }

    @Override
    public DsType dsType() { return DsType.DP_TABLE; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("s", "String", "bccb", 1, 10,
                "[a-z]+", "Use lowercase letters a-z."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "aaa"); }

    public String annotatedCode() {
        return """
               public long countPalindromicSubsequences(String s) {
                   int n = s.length();
                   long[][] dp = new long[n][n];
                   // @a base
                   for (int i = 0; i < n; i++) dp[i][i] = 1;
                   for (int length = 2; length <= n; length++) {
                       for (int left = 0; left + length <= n; left++) {
                           int right = left + length - 1;
                           // @a transition
                           if (s.charAt(left) == s.charAt(right))
                               dp[left][right] = dp[left + 1][right] + dp[left][right - 1] + 1;
                           else
                               dp[left][right] = dp[left + 1][right] + dp[left][right - 1] - dp[left + 1][right - 1];
                       }
                   }
                   // @a done
                   return dp[0][n - 1];
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int n = s.length();
        long[][] dp = new long[n][n];
        boolean[][] settled = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            dp[i][i] = 1;
            settled[i][i] = true;
            emit.at("base").say("A single character '%c' at [%d,%d] is one palindromic subsequence.",
                            s.charAt(i), i, i)
                    .var("left", i).var("right", i).var("count", 1)
                    .dpTable(intervalTable(s, dp, settled, i, i, false)).step();
        }
        for (int length = 2; length <= n; length++) {
            for (int left = 0; left + length <= n; left++) {
                int right = left + length - 1;
                if (s.charAt(left) == s.charAt(right)) {
                    dp[left][right] = dp[left + 1][right] + dp[left][right - 1] + 1;
                } else {
                    long overlap = length == 2 ? 0 : dp[left + 1][right - 1];
                    dp[left][right] = dp[left + 1][right] + dp[left][right - 1] - overlap;
                }
                settled[left][right] = true;
                emit.at("transition").say("Interval [%d,%d] \"%s\": ends '%c' and '%c' %s; count=%d.",
                                left, right, s.substring(left, right + 1), s.charAt(left), s.charAt(right),
                                s.charAt(left) == s.charAt(right) ? "match, so include the new paired subsequence"
                                        : "differ, so subtract the double-counted overlap",
                                dp[left][right])
                        .var("left", left).var("right", right).var("count", dp[left][right])
                        .dpTable(intervalTable(s, dp, settled, left, right, false)).step();
            }
        }
        emit.at("done").say("The string \"%s\" has %d non-empty palindromic subsequences, counting occurrences.",
                        s, dp[0][n - 1])
                .var("answer", dp[0][n - 1])
                .dpTable(intervalTable(s, dp, settled, -1, -1, true)).step();
    }
}
