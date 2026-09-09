package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DFS with an in-place visited mark: a cell is only a valid next step when it matches the
 * word's next character AND is not already part of the path being built. The mark is undone
 * on the way back out of a failed branch, so a cell ruled out for one path is free again for
 * the next starting position's attempt.
 */
@Component
public class WordSearchTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "word-search";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("board", FieldType.STRING)
                        .label("Board (rows separated by commas)")
                        .help("Letters only, e.g. \"ABCE,SFCS,ADEE\" for a 3x4 board.")
                        .length(1, 40)
                        .constraint("pattern", "[A-Za-z]+(,[A-Za-z]+)*")
                        .constraint("patternHint", "Comma-separated rows of letters, e.g. ABCE,SFCS,ADEE.")
                        .defaultValue("ABCE,SFCS,ADEE")
                        .build(),
                InputField.of("word", FieldType.STRING)
                        .label("Word to find")
                        .length(1, 12)
                        .constraint("pattern", "[A-Za-z]+")
                        .constraint("patternHint", "Letters only.")
                        .defaultValue("ABCCED")
                        .build());
    }

    /** Absent from the board — every starting cell's search eventually dead-ends. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("board", "ABCE,SFCS,ADEE", "word", "ABCB");
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean exist(char[][] board, String word) {
                   for (int r = 0; r < board.length; r++) {
                       for (int c = 0; c < board[0].length; c++) {
                           if (dfs(board, r, c, word, 0)) {
                               // @a foundStart
                               return true;
                           }
                       }
                   }
                   // @a exhausted
                   return false;
               }

               private boolean dfs(char[][] board, int r, int c, String word, int i) {
                   if (i == word.length()) {
                       // @a matched
                       return true;
                   }
                   if (r < 0 || r >= board.length || c < 0 || c >= board[0].length
                           || visited[r][c] || board[r][c] != word.charAt(i)) {
                       // @a mismatch
                       return false;
                   }
                   visited[r][c] = true;
                   // @a mark
                   boolean found = dfs(board, r + 1, c, word, i + 1)
                           || dfs(board, r - 1, c, word, i + 1)
                           || dfs(board, r, c + 1, word, i + 1)
                           || dfs(board, r, c - 1, word, i + 1);
                   visited[r][c] = false;
                   // @a unmark
                   return found;
               }""";
    }

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private char[][] parseBoard(String raw) {
        String[] rowStrings = raw.split(",");
        int cols = rowStrings[0].length();
        char[][] board = new char[rowStrings.length][cols];
        for (int r = 0; r < rowStrings.length; r++) {
            if (rowStrings[r].length() != cols) {
                throw new InputValidationException(Map.of("board", "Every row must have the same length."));
            }
            board[r] = rowStrings[r].toUpperCase().toCharArray();
        }
        return board;
    }

    private int[][] toCodeGrid(char[][] board) {
        int[][] grid = new int[board.length][board[0].length];
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[0].length; c++) {
                grid[r][c] = board[r][c];
            }
        }
        return grid;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        char[][] board = parseBoard(in.getString("board"));
        String word = in.getString("word").toUpperCase();
        int rows = board.length, cols = board[0].length;
        boolean[][] visited = new boolean[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (dfs(board, r, c, word, 0, visited, emit)) {
                    emit.at("foundStart")
                            .say("Starting search from (%d,%d) succeeded - \"%s\" is present on the board.",
                                    r, c, word)
                            .var("result", true).var("start", "(" + r + "," + c + ")")
                            .grid(toCodeGrid(board)).step();
                    return;
                }
            }
        }

        emit.at("exhausted")
                .say("Every starting cell tried, every branch dead-ended. \"%s\" is not on the board.", word)
                .var("result", false).grid(toCodeGrid(board)).step();
    }

    private boolean dfs(char[][] board, int r, int c, String word, int i, boolean[][] visited, StepEmitter emit) {
        emit.push("dfs(" + r + "," + c + ",i=" + i + ")");

        if (i == word.length()) {
            emit.at("matched")
                    .say("Every character of \"%s\" matched in sequence - path complete.", word)
                    .var("i", i).grid(toCodeGrid(board)).step();
            emit.pop();
            return true;
        }

        if (r < 0 || r >= board.length || c < 0 || c >= board[0].length
                || visited[r][c] || board[r][c] != word.charAt(i)) {
            String reason = (r < 0 || r >= board.length || c < 0 || c >= board[0].length)
                    ? "out of bounds"
                    : visited[r][c] ? "already used on this path" : "letter does not match '" + word.charAt(i) + "'";
            emit.at("mismatch")
                    .say("(%d,%d) at word index %d: %s.", r, c, i, reason)
                    .var("i", i).grid(toCodeGrid(board)).step();
            emit.pop();
            return false;
        }

        visited[r][c] = true;
        emit.at("mark")
                .say("(%d,%d) = '%c' matches word[%d]. Mark it and try all four neighbours for index %d.",
                        r, c, board[r][c], i, i + 1)
                .var("i", i).var("cell", "(" + r + "," + c + ")").grid(toCodeGrid(board)).step();

        boolean found = false;
        for (int[] d : DIRS) {
            if (dfs(board, r + d[0], c + d[1], word, i + 1, visited, emit)) {
                found = true;
                break;
            }
        }

        visited[r][c] = false;
        emit.at("unmark")
                .say("Unmark (%d,%d) - a different path may still need to pass through it.", r, c)
                .var("cell", "(" + r + "," + c + ")").grid(toCodeGrid(board)).step();

        emit.pop();
        return found;
    }
}
