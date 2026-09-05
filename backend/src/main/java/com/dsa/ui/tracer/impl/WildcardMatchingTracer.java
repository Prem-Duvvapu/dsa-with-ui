package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Does pattern p (with '?' matching any single character and '*' matching any run of
 * characters, including none) match the whole of s? A '*' cell reads two predecessors at
 * once - one where the star absorbs the current character of s and stays put in the
 * pattern, one where the star gives up and lets the next pattern character take over - and
 * is true if either path already was. Everything else is a single diagonal copy or a dead
 * cell, no OR involved.
 */
@Component
public class WildcardMatchingTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "wildcard-matching";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("s", FieldType.STRING)
                        .label("Text")
                        .help("Lowercase letters only.")
                        .length(1, 15)
                        .constraint("pattern", "[a-z]+")
                        .constraint("patternHint", "Lowercase letters a-z only.")
                        .defaultValue("adceb")
                        .build(),
                InputField.of("p", FieldType.STRING)
                        .label("Pattern")
                        .help("Lowercase letters plus '?' (any one character) and '*' (any run, including none).")
                        .length(1, 15)
                        .constraint("pattern", "[a-z?*]+")
                        .constraint("patternHint", "Lowercase letters, '?' and '*' only.")
                        .defaultValue("*a*b")
                        .build());
    }

    /** A different text/pattern pair, and a different answer - false instead of true. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "acdcb", "p", "a*c?b");
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isMatch(String s, String p) {
                   int m = s.length(), n = p.length();
                   boolean[][] dp = new boolean[m + 1][n + 1];
                   dp[0][0] = true;
                   // @a init
                   for (int j = 1; j <= n; j++) {
                       if (p.charAt(j - 1) == '*') dp[0][j] = dp[0][j - 1];
                   }

                   for (int i = 1; i <= m; i++) {
                       for (int j = 1; j <= n; j++) {
                           char pc = p.charAt(j - 1);
                           if (pc == '*') {
                               // @a starMatch
                               dp[i][j] = dp[i - 1][j] || dp[i][j - 1];
                           } else if (pc == '?' || pc == s.charAt(i - 1)) {
                               // @a directMatch
                               dp[i][j] = dp[i - 1][j - 1];
                           } else {
                               // @a mismatch
                               dp[i][j] = false;
                           }
                       }
                   }
                   // @a done
                   return dp[m][n];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        String p = in.getString("p");
        int m = s.length();
        int n = p.length();

        boolean[][] dp = new boolean[m + 1][n + 1];
        boolean[][] settled = new boolean[m + 1][n + 1];
        dp[0][0] = true;
        settled[0][0] = true;
        for (int j = 1; j <= n; j++) {
            if (p.charAt(j - 1) == '*') {
                dp[0][j] = dp[0][j - 1];
            }
            settled[0][j] = true;
        }
        for (int i = 1; i <= m; i++) {
            settled[i][0] = true;
        }

        emit.at("init").say(
                        "dp[0][0] = true (empty pattern matches empty text). A leading run of "
                                + "'*' can also match the empty text; anything else in row 0 stays false.")
                .dpTable(table(dp, settled, s, p, null, null, Set.of(), false)).step();

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                StringDpTable.Coord here = new StringDpTable.Coord(i, j);
                char pc = p.charAt(j - 1);
                if (pc == '*') {
                    StringDpTable.Coord up = new StringDpTable.Coord(i - 1, j);
                    StringDpTable.Coord left = new StringDpTable.Coord(i, j - 1);
                    boolean consume = dp[i - 1][j];
                    boolean skip = dp[i][j - 1];
                    dp[i][j] = consume || skip;
                    settled[i][j] = true;
                    emit.at("starMatch").say(
                                    "p[%d]='*' can absorb '%c' (dp[%d][%d]=%b) or match nothing more "
                                            + "(dp[%d][%d]=%b) - either wins: dp[%d][%d]=%b.",
                                    j - 1, s.charAt(i - 1), i - 1, j, consume, i, j - 1, skip,
                                    i, j, dp[i][j])
                            .var("i", i).var("j", j).var("value", dp[i][j])
                            .dpTable(table(dp, settled, s, p, here, String.valueOf(dp[i][j]),
                                    Set.of(up, left), false)).step();
                } else if (pc == '?' || pc == s.charAt(i - 1)) {
                    StringDpTable.Coord diag = new StringDpTable.Coord(i - 1, j - 1);
                    dp[i][j] = dp[i - 1][j - 1];
                    settled[i][j] = true;
                    emit.at("directMatch").say(
                                    "p[%d]='%c' matches s[%d]='%c' directly. Copy dp[%d][%d]=%b.",
                                    j - 1, pc, i - 1, s.charAt(i - 1), i - 1, j - 1, dp[i][j])
                            .var("i", i).var("j", j).var("value", dp[i][j])
                            .dpTable(table(dp, settled, s, p, here, String.valueOf(dp[i][j]),
                                    Set.of(diag), false)).step();
                } else {
                    dp[i][j] = false;
                    settled[i][j] = true;
                    emit.at("mismatch").say(
                                    "p[%d]='%c' cannot match s[%d]='%c' - neither '?' nor '*' nor "
                                            + "the same character. dp[%d][%d]=false.",
                                    j - 1, pc, i - 1, s.charAt(i - 1), i, j)
                            .var("i", i).var("j", j).var("value", false)
                            .dpTable(table(dp, settled, s, p, here, "false", Set.of(), false)).step();
                }
            }
        }

        emit.at("done").say(
                        "Every cell filled. Does \"%s\" match pattern \"%s\"? %b.", s, p, dp[m][n])
                .var("answer", dp[m][n])
                .dpTable(table(dp, settled, s, p, null, null, Set.of(), true)).step();
    }

    private static com.dsa.ui.model.DpTable table(boolean[][] dp, boolean[][] settled, String s, String p,
                                                   StringDpTable.Coord probe, String probeValue,
                                                   Set<StringDpTable.Coord> reads, boolean done) {
        String[][] values = new String[dp.length][dp[0].length];
        for (int i = 0; i < dp.length; i++) {
            for (int j = 0; j < dp[0].length; j++) {
                values[i][j] = String.valueOf(dp[i][j]);
            }
        }
        return StringDpTable.of(values, settled, s, p, probe, probeValue, reads, done);
    }
}
