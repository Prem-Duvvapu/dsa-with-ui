package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Minimum edits (insert, delete, replace) to turn one string into another. Matching
 * characters cost nothing and copy the diagonal predecessor; a mismatch costs one edit plus
 * whichever of the three neighbours (replace = diagonal, delete = above, insert = left) was
 * already cheapest - the operation the narration names is not a guess, it is literally the
 * arithmetic minimum just computed.
 */
@Component
public class EditDistanceTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "edit-distance";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("word1", FieldType.STRING)
                        .label("Word 1 (source)")
                        .help("Lowercase letters only.")
                        .length(1, 15)
                        .constraint("pattern", "[a-z]+")
                        .constraint("patternHint", "Lowercase letters a-z only.")
                        .defaultValue("horse")
                        .build(),
                InputField.of("word2", FieldType.STRING)
                        .label("Word 2 (target)")
                        .help("Lowercase letters only.")
                        .length(1, 15)
                        .constraint("pattern", "[a-z]+")
                        .constraint("patternHint", "Lowercase letters a-z only.")
                        .defaultValue("ros")
                        .build());
    }

    /** LeetCode's own second worked example - a larger table and a different answer, 5 instead of 3. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("word1", "intention", "word2", "execution");
    }

    @Override
    public String annotatedCode() {
        return """
               public int minDistance(String word1, String word2) {
                   int m = word1.length(), n = word2.length();
                   int[][] dp = new int[m + 1][n + 1];
                   // @a init
                   for (int i = 0; i <= m; i++) dp[i][0] = i;
                   for (int j = 0; j <= n; j++) dp[0][j] = j;

                   for (int i = 1; i <= m; i++) {
                       for (int j = 1; j <= n; j++) {
                           if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                               // @a match
                               dp[i][j] = dp[i - 1][j - 1];
                           } else {
                               int replace = dp[i - 1][j - 1];
                               int delete = dp[i - 1][j];
                               int insert = dp[i][j - 1];
                               if (replace <= delete && replace <= insert) {
                                   // @a replace
                                   dp[i][j] = 1 + replace;
                               } else if (delete <= insert) {
                                   // @a delete
                                   dp[i][j] = 1 + delete;
                               } else {
                                   // @a insert
                                   dp[i][j] = 1 + insert;
                               }
                           }
                       }
                   }
                   // @a done
                   return dp[m][n];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String word1 = in.getString("word1");
        String word2 = in.getString("word2");
        int m = word1.length();
        int n = word2.length();

        int[][] dp = new int[m + 1][n + 1];
        boolean[][] settled = new boolean[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
            settled[i][0] = true;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
            settled[0][j] = true;
        }

        emit.at("init").say(
                        "Base cases: turning any prefix of \"%s\" into the empty string costs one "
                                + "delete per character, and vice versa for \"%s\".", word1, word2)
                .dpTable(table(dp, settled, word1, word2, null, null, Set.of(), false)).step();

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                StringDpTable.Coord here = new StringDpTable.Coord(i, j);
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    StringDpTable.Coord diag = new StringDpTable.Coord(i - 1, j - 1);
                    dp[i][j] = dp[i - 1][j - 1];
                    settled[i][j] = true;
                    emit.at("match").say(
                                    "'%c' matches '%c' - no edit needed here. Copy dp[%d][%d] = %d.",
                                    word1.charAt(i - 1), word2.charAt(j - 1), i - 1, j - 1, dp[i][j])
                            .var("i", i).var("j", j).var("value", dp[i][j])
                            .dpTable(table(dp, settled, word1, word2, here, String.valueOf(dp[i][j]),
                                    Set.of(diag), false)).step();
                } else {
                    int replace = dp[i - 1][j - 1];
                    int delete = dp[i - 1][j];
                    int insert = dp[i][j - 1];
                    StringDpTable.Coord diag = new StringDpTable.Coord(i - 1, j - 1);
                    StringDpTable.Coord up = new StringDpTable.Coord(i - 1, j);
                    StringDpTable.Coord left = new StringDpTable.Coord(i, j - 1);
                    Set<StringDpTable.Coord> reads = Set.of(diag, up, left);

                    String anchor;
                    int best;
                    if (replace <= delete && replace <= insert) {
                        anchor = "replace";
                        best = replace;
                    } else if (delete <= insert) {
                        anchor = "delete";
                        best = delete;
                    } else {
                        anchor = "insert";
                        best = insert;
                    }
                    dp[i][j] = 1 + best;
                    settled[i][j] = true;

                    emit.at(anchor).say(
                                    "'%c' != '%c'. replace=%d, delete=%d, insert=%d - cheapest is %s "
                                            + "(%d). dp[%d][%d] = 1 + %d = %d.",
                                    word1.charAt(i - 1), word2.charAt(j - 1), replace, delete, insert,
                                    anchor, best, i, j, best, dp[i][j])
                            .var("i", i).var("j", j).var("value", dp[i][j])
                            .dpTable(table(dp, settled, word1, word2, here, String.valueOf(dp[i][j]),
                                    reads, false)).step();
                }
            }
        }

        emit.at("done").say(
                        "Every cell filled. Minimum edits to turn \"%s\" into \"%s\": %d.",
                        word1, word2, dp[m][n])
                .var("answer", dp[m][n])
                .dpTable(table(dp, settled, word1, word2, null, null, Set.of(), true)).step();
    }

    private static com.dsa.ui.model.DpTable table(int[][] dp, boolean[][] settled, String word1, String word2,
                                                   StringDpTable.Coord probe, String probeValue,
                                                   Set<StringDpTable.Coord> reads, boolean done) {
        String[][] values = new String[dp.length][dp[0].length];
        for (int i = 0; i < dp.length; i++) {
            for (int j = 0; j < dp[0].length; j++) {
                values[i][j] = String.valueOf(dp[i][j]);
            }
        }
        return StringDpTable.of(values, settled, word1, word2, probe, probeValue, reads, done);
    }
}
