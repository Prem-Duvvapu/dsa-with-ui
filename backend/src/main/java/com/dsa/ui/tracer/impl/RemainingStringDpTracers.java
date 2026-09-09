package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.InputSpec;
import com.dsa.ui.tracer.Inputs;
import com.dsa.ui.tracer.StepEmitter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

abstract class LcsFamilyRemainingTracer extends RemainingDpTracer {
    protected String firstField() { return "first"; }
    protected String secondField() { return "second"; }
    protected String firstDefault() { return "abcde"; }
    protected String secondDefault() { return "ace"; }
    protected String[] strings(Inputs in) { return new String[]{in.getString(firstField()), in.getString(secondField())}; }

    @Override public InputSpec inputSpec() {
        return InputSpec.of(DpTraceSupport.text(firstField(), "First string", firstDefault(), 12),
                DpTraceSupport.text(secondField(), "Second string", secondDefault(), 12));
    }

    @Override public Map<String, Object> alternateInput() {
        return Map.of(firstField(), "abac", secondField(), "cab");
    }

    @Override public void run(Inputs in, StepEmitter emit) {
        String[] pair = strings(in); String a = pair[0], b = pair[1];
        long[][] dp = new long[a.length() + 1][b.length() + 1]; boolean[][] known = new boolean[dp.length][dp[0].length]; bases(dp, known);
        emit.at("init").say("dp[i][j] compares prefixes of lengths i and j; empty prefixes have LCS length 0.")
                .dpTable(table(dp, known, a, b, null, Set.of(), false, "base = 0")).step();
        for (int i = 1; i <= a.length(); i++) for (int j = 1; j <= b.length(); j++) {
            boolean equal = a.charAt(i - 1) == b.charAt(j - 1);
            dp[i][j] = equal ? 1 + dp[i - 1][j - 1] : Math.max(dp[i - 1][j], dp[i][j - 1]); known[i][j] = true;
            Set<DpTraceSupport.Coord> reads = equal ? Set.of(new DpTraceSupport.Coord(i - 1, j - 1))
                    : Set.of(new DpTraceSupport.Coord(i - 1, j), new DpTraceSupport.Coord(i, j - 1));
            emit.at("fill").say("Compare '%c' with '%c': prefix LCS length becomes %d.", a.charAt(i - 1), b.charAt(j - 1), dp[i][j])
                    .var("i", i).var("j", j).var("lcs", dp[i][j])
                    .dpTable(table(dp, known, a, b, new DpTraceSupport.Coord(i, j), reads, false,
                            equal ? "1 + diagonal = " + dp[i][j] : "max(up,left) = " + dp[i][j])).step();
        }
        finish(a, b, dp, known, emit);
    }

    protected abstract void finish(String a, String b, long[][] dp, boolean[][] known, StepEmitter emit);

    protected static com.dsa.ui.model.DpTable table(long[][] dp, boolean[][] known, String a, String b,
                                                     DpTraceSupport.Coord probe, Set<DpTraceSupport.Coord> reads,
                                                     boolean done, String substitution) {
        return DpTraceSupport.table(dp, known, probe, reads, DpTraceSupport.chars(a), DpTraceSupport.chars(b), done,
                "equal ? 1 + diagonal : max(up, left)", substitution);
    }

    protected static String lcs(String a, String b, long[][] dp, boolean[][] known, StepEmitter emit) {
        StringBuilder reversed = new StringBuilder(); int i = a.length(), j = b.length();
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                reversed.append(a.charAt(i - 1));
                emit.at("reconstruct").say("Characters match at (%d,%d); prepend '%c'.", i, j, a.charAt(i - 1))
                        .var("partial", reversed.reverse().toString()).dpTable(table(dp, known, a, b,
                                new DpTraceSupport.Coord(i, j), Set.of(), false, "take matching character")).step();
                reversed.reverse(); i--; j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                emit.at("reconstruct").say("Move up from (%d,%d) toward the larger predecessor.", i, j)
                        .dpTable(table(dp, known, a, b, new DpTraceSupport.Coord(i, j),
                                Set.of(new DpTraceSupport.Coord(i - 1, j)), false, "move up")).step(); i--;
            } else {
                emit.at("reconstruct").say("Move left from (%d,%d) toward the larger predecessor.", i, j)
                        .dpTable(table(dp, known, a, b, new DpTraceSupport.Coord(i, j),
                                Set.of(new DpTraceSupport.Coord(i, j - 1)), false, "move left")).step(); j--;
            }
        }
        return reversed.reverse().toString();
    }
}

@Component
class PrintLongestCommonSubsequenceRemainingTracer extends LcsFamilyRemainingTracer {
    @Override public String id() { return "print-longest-common-subsequence"; }
    @Override protected String firstDefault() { return "abcde"; }
    @Override protected String secondDefault() { return "bdgek"; }
    @Override protected void finish(String a, String b, long[][] dp, boolean[][] known, StepEmitter emit) {
        String answer = lcs(a, b, dp, known, emit);
        emit.at("done").say("One longest common subsequence is \"%s\".", answer).var("answer", answer)
                .dpTable(table(dp, known, a, b, null, Set.of(), true, "reconstructed = " + answer)).step();
    }
}

@Component
class LongestPalindromicSubsequenceRemainingTracer extends LcsFamilyRemainingTracer {
    @Override public String id() { return "longest-palindromic-subsequence"; }
    @Override public InputSpec inputSpec() { return InputSpec.of(DpTraceSupport.text("text", "Text", "bbbab", 12)); }
    @Override public Map<String, Object> alternateInput() { return Map.of("text", "cbbd"); }
    @Override protected String[] strings(Inputs in) {
        String value = in.getString("text"); return new String[]{value, new StringBuilder(value).reverse().toString()};
    }
    @Override protected void finish(String a, String b, long[][] dp, boolean[][] known, StepEmitter emit) {
        long answer = dp[a.length()][b.length()];
        emit.at("done").say("LCS with the reversed string gives palindromic subsequence length %d.", answer).var("answer", answer)
                .dpTable(table(dp, known, a, b, null, Set.of(), true, "answer = " + answer)).step();
    }
}

@Component
class MinInsertionsPalindromeRemainingTracer extends LongestPalindromicSubsequenceRemainingTracer {
    @Override public String id() { return "min-insertions-palindrome"; }
    @Override public InputSpec inputSpec() { return InputSpec.of(DpTraceSupport.text("text", "Text", "mbadm", 12)); }
    @Override public Map<String, Object> alternateInput() { return Map.of("text", "leetcode"); }
    @Override protected void finish(String a, String b, long[][] dp, boolean[][] known, StepEmitter emit) {
        long lps = dp[a.length()][b.length()], answer = a.length() - lps;
        emit.at("done").say("Length %d minus LPS %d gives %d required insertions.", a.length(), lps, answer)
                .var("lps", lps).var("answer", answer).dpTable(table(dp, known, a, b, null, Set.of(), true,
                        a.length() + " - " + lps + " = " + answer)).step();
    }
}

@Component
class MinInsertionsDeletionsRemainingTracer extends LcsFamilyRemainingTracer {
    @Override public String id() { return "min-insertions-deletions-a-b"; }
    @Override protected String firstDefault() { return "heap"; }
    @Override protected String secondDefault() { return "pea"; }
    @Override protected void finish(String a, String b, long[][] dp, boolean[][] known, StepEmitter emit) {
        long lcs = dp[a.length()][b.length()], deletions = a.length() - lcs, insertions = b.length() - lcs;
        emit.at("done").say("Keep an LCS of %d: delete %d and insert %d, %d operations total.", lcs, deletions, insertions, deletions + insertions)
                .var("deletions", deletions).var("insertions", insertions).var("answer", deletions + insertions)
                .dpTable(table(dp, known, a, b, null, Set.of(), true, "delete + insert = " + (deletions + insertions))).step();
    }
}

@Component
class ShortestCommonSupersequenceRemainingTracer extends LcsFamilyRemainingTracer {
    @Override public String id() { return "shortest-common-supersequence"; }
    @Override protected String firstDefault() { return "abac"; }
    @Override protected String secondDefault() { return "cab"; }
    @Override protected void finish(String a, String b, long[][] dp, boolean[][] known, StepEmitter emit) {
        StringBuilder reverse = new StringBuilder(); int i = a.length(), j = b.length();
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) { reverse.append(a.charAt(i - 1)); i--; j--; }
            else if (dp[i - 1][j] >= dp[i][j - 1]) { reverse.append(a.charAt(--i)); }
            else { reverse.append(b.charAt(--j)); }
            emit.at("reconstruct").say("Backtrack at (%d,%d); reversed supersequence prefix is \"%s\".", i, j, reverse)
                    .var("i", i).var("j", j).dpTable(table(dp, known, a, b,
                            new DpTraceSupport.Coord(i, j), Set.of(), false, "append omitted character")).step();
        }
        while (i > 0) reverse.append(a.charAt(--i)); while (j > 0) reverse.append(b.charAt(--j));
        String answer = reverse.reverse().toString();
        emit.at("done").say("A shortest common supersequence is \"%s\" (length %d).", answer, answer.length())
                .var("answer", answer).var("length", answer.length()).dpTable(table(dp, known, a, b, null, Set.of(), true,
                        "reconstructed = " + answer)).step();
    }
}

@Component
class LongestCommonSubstringRemainingTracer extends RemainingDpTracer {
    @Override public String id() { return "longest-common-substring"; }
    @Override public InputSpec inputSpec() { return InputSpec.of(DpTraceSupport.text("first", "First string", "abcd", 12),
            DpTraceSupport.text("second", "Second string", "abzd", 12)); }
    @Override public Map<String, Object> alternateInput() { return Map.of("first", "zxabcdezy", "second", "yzabcdezx"); }
    @Override public void run(Inputs in, StepEmitter emit) {
        String a = in.getString("first"), b = in.getString("second"); long best = 0;
        long[][] dp = new long[a.length() + 1][b.length() + 1]; boolean[][] known = new boolean[dp.length][dp[0].length]; bases(dp, known);
        emit.at("init").say("dp[i][j] is the common suffix length ending at characters i and j; empty prefixes are 0.")
                .dpTable(LcsFamilyRemainingTracer.table(dp, known, a, b, null, Set.of(), false, "base = 0")).step();
        for (int i = 1; i <= a.length(); i++) for (int j = 1; j <= b.length(); j++) {
            boolean equal = a.charAt(i - 1) == b.charAt(j - 1); dp[i][j] = equal ? 1 + dp[i - 1][j - 1] : 0; best = Math.max(best, dp[i][j]); known[i][j] = true;
            emit.at("fill").say("Ending at '%c' and '%c': suffix length %d; global best %d.", a.charAt(i - 1), b.charAt(j - 1), dp[i][j], best)
                    .var("i", i).var("j", j).var("best", best).dpTable(LcsFamilyRemainingTracer.table(dp, known, a, b,
                            new DpTraceSupport.Coord(i, j), equal ? Set.of(new DpTraceSupport.Coord(i - 1, j - 1)) : Set.of(), false,
                            equal ? "1 + diagonal = " + dp[i][j] : "mismatch = 0")).step();
        }
        emit.at("done").say("Longest common substring length: %d.", best).var("answer", best)
                .dpTable(LcsFamilyRemainingTracer.table(dp, known, a, b, null, Set.of(), true, "answer = " + best)).step();
    }
}

@Component
class DistinctSubsequencesRemainingTracer extends RemainingDpTracer {
    @Override public String id() { return "distinct-subsequences"; }
    @Override public InputSpec inputSpec() { return InputSpec.of(DpTraceSupport.text("source", "Source", "rabbbit", 12),
            DpTraceSupport.text("target", "Target", "rabbit", 10)); }
    @Override public Map<String, Object> alternateInput() { return Map.of("source", "babgbag", "target", "bag"); }
    @Override public void run(Inputs in, StepEmitter emit) {
        String source = in.getString("source"), target = in.getString("target");
        long[][] dp = new long[source.length() + 1][target.length() + 1]; boolean[][] known = new boolean[dp.length][dp[0].length];
        for (int i = 0; i <= source.length(); i++) { dp[i][0] = 1; known[i][0] = true; }
        for (int j = 1; j <= target.length(); j++) known[0][j] = true;
        emit.at("init").say("There is one way to form the empty target from any source prefix.")
                .dpTable(DpTraceSupport.table(dp, known, null, Set.of(), DpTraceSupport.chars(source), DpTraceSupport.chars(target), false,
                        "match ? skip + take : skip", "dp[i][0] = 1")).step();
        for (int i = 1; i <= source.length(); i++) for (int j = 1; j <= target.length(); j++) {
            boolean equal = source.charAt(i - 1) == target.charAt(j - 1);
            dp[i][j] = dp[i - 1][j] + (equal ? dp[i - 1][j - 1] : 0); known[i][j] = true;
            Set<DpTraceSupport.Coord> reads = equal ? Set.of(new DpTraceSupport.Coord(i - 1, j), new DpTraceSupport.Coord(i - 1, j - 1))
                    : Set.of(new DpTraceSupport.Coord(i - 1, j));
            emit.at("fill").say("Source '%c', target '%c': %d ways for these prefixes.", source.charAt(i - 1), target.charAt(j - 1), dp[i][j])
                    .var("i", i).var("j", j).var("ways", dp[i][j]).dpTable(DpTraceSupport.table(dp, known,
                            new DpTraceSupport.Coord(i, j), reads, DpTraceSupport.chars(source), DpTraceSupport.chars(target), false,
                            "match ? skip + take : skip", equal ? "skip + take = " + dp[i][j] : "skip = " + dp[i][j])).step();
        }
        long answer = dp[source.length()][target.length()];
        emit.at("done").say("The target occurs as a subsequence %d times.", answer).var("answer", answer)
                .dpTable(DpTraceSupport.table(dp, known, null, Set.of(), DpTraceSupport.chars(source), DpTraceSupport.chars(target), true,
                        "answer = dp[m][n]", "answer = " + answer)).step();
    }
}
