package com.dsa.ui.algorithm.backtracking;

import com.dsa.ui.trace.SnapshotUtil;
import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Problem: Word Search in 2D Board (LeetCode 79)
 *
 * Find if a target word exists in a 2D board of characters using 4-directional backtracking.
 *
 * Time Complexity:  O(N * M * 4^L) where L is length of word.
 * Space Complexity: O(L) recursion stack depth.
 */
public class WordSearch {

    public boolean solve(char[][] board, String word, TraceRecorder recorder) {
        int rows = board.length;
        int cols = board[0].length;
        int[][] numGrid = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                numGrid[r][c] = board[r][c]; // Character char values
            }
        }

        List<String> callStack = new ArrayList<>();

        recorder.record(new TraceEvent(
            "start", 15,
            String.format("Word Search: Search for target word \"%s\" in %dx%d character board.", word, rows, cols),
            Map.of("word", word, "boardSize", rows + "x" + cols),
            "Matrix", SnapshotUtil.clone2DGrid(numGrid),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c] == word.charAt(0)) {
                    if (search(r, c, 0, board, numGrid, word, recorder, callStack)) {
                        recorder.record(new TraceEvent(
                            "complete", 45,
                            String.format("Word Search Complete! Word \"%s\" FOUND in board!", word),
                            Map.of("Result", "FOUND", "word", word),
                            "Matrix", SnapshotUtil.clone2DGrid(numGrid),
                            List.of(), Map.of(), List.of()
                        ));
                        return true;
                    }
                }
            }
        }

        recorder.record(new TraceEvent(
            "complete", 48,
            String.format("Word Search Complete! Word \"%s\" NOT FOUND in board.", word),
            Map.of("Result", "NOT_FOUND", "word", word),
            "Matrix", SnapshotUtil.clone2DGrid(numGrid),
            List.of(), Map.of(), List.of()
        ));

        return false;
    }

    private boolean search(int r, int c, int idx, char[][] board, int[][] numGrid, String word,
                           TraceRecorder recorder, List<String> callStack) {
        String callFrame = String.format("search(%d, %d, idx=%d)", r, c, idx);
        callStack.add(callFrame);

        if (idx == word.length()) {
            callStack.remove(callStack.size() - 1);
            return true;
        }

        if (r < 0 || r >= board.length || c < 0 || c >= board[0].length || board[r][c] != word.charAt(idx)) {
            callStack.remove(callStack.size() - 1);
            return false;
        }

        char temp = board[r][c];
        board[r][c] = '#'; // Mark visited
        numGrid[r][c] = 99; // Visited visual code

        recorder.record(new TraceEvent(
            "match_char", 32,
            String.format("At (%d, %d): Matched char '%c' (idx=%d of \"%s\"). Mark visited ('#'). Recurse 4-directionally...", r, c, temp, idx, word),
            Map.of("char", String.valueOf(temp), "idx", String.valueOf(idx), "pos", "(" + r + "," + c + ")"),
            "Matrix", SnapshotUtil.clone2DGrid(numGrid),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        if (idx == word.length() - 1) {
            recorder.record(new TraceEvent(
                "word_complete", 35,
                String.format("ALL CHARACTERS MATCHED! Word \"%s\" fully completed at path end (%d, %d).", word, r, c),
                Map.of("Word", word, "Status", "MATCHED"),
                "Matrix", SnapshotUtil.clone2DGrid(numGrid),
                new ArrayList<>(callStack), Map.of(), List.of()
            ));
            board[r][c] = temp;
            numGrid[r][c] = temp;
            callStack.remove(callStack.size() - 1);
            return true;
        }

        int[] dr = {1, 0, 0, -1};
        int[] dc = {0, -1, 1, 0};

        for (int i = 0; i < 4; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];

            if (search(nr, nc, idx + 1, board, numGrid, word, recorder, callStack)) {
                board[r][c] = temp;
                numGrid[r][c] = temp;
                callStack.remove(callStack.size() - 1);
                return true;
            }
        }

        // Backtrack
        board[r][c] = temp;
        numGrid[r][c] = temp;

        recorder.record(new TraceEvent(
            "backtrack_word", 42,
            String.format("BACKTRACK from (%d, %d): Unmark char '%c'. Dead end for remaining substring.", r, c, temp),
            Map.of("unmark", String.valueOf(temp), "pos", "(" + r + "," + c + ")"),
            "Matrix", SnapshotUtil.clone2DGrid(numGrid),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        callStack.remove(callStack.size() - 1);
        return false;
    }
}
