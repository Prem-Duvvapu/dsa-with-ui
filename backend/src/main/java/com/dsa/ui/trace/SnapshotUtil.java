package com.dsa.ui.trace;

import com.dsa.ui.model.ArrayElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SnapshotUtil {

    public static int[][] clone2DGrid(int[][] grid) {
        if (grid == null) return null;
        int[][] clone = new int[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            clone[i] = grid[i].clone();
        }
        return clone;
    }

    public static int[] cloneArray(int[] arr) {
        if (arr == null) return null;
        return arr.clone();
    }

    public static List<ArrayElement> createArrayState(int[] arr, int activeIdx1, int activeIdx2) {
        List<ArrayElement> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length; i++) {
            String state = (i == activeIdx1 || i == activeIdx2) ? "active" : "default";
            list.add(new ArrayElement(i, arr[i], state));
        }
        return list;
    }

    public static List<ArrayElement> createDetailedArrayState(int[] arr, int iIdx, int miniIdx, int jIdx, int sortedLength) {
        List<ArrayElement> list = new ArrayList<>();
        if (arr == null) return list;
        for (int idx = 0; idx < arr.length; idx++) {
            String state = "default";
            if (idx < sortedLength) {
                state = "sorted";
            } else if (idx == miniIdx) {
                state = "target"; // mini / minimum found
            } else if (idx == jIdx) {
                state = "active"; // current comparison candidate
            } else if (idx == iIdx) {
                state = "visiting"; // pass start
            }
            list.add(new ArrayElement(idx, arr[idx], state));
        }
        return list;
    }
}
