package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Cell states for a sliding window, the way WindowCanvas reads them: the window is the run of
 * cells with a non-default state. {@code right} is the cell just taken in ("current"), {@code left}
 * the window's first cell ("target"), the cells between them "active", everything else "default".
 */
final class WindowCells {

    private WindowCells() {
    }

    /** A window over a string, each cell labelled with its character. */
    static List<ArrayElement> of(String s, int left, int right) {
        List<ArrayElement> out = new ArrayList<>(s.length());
        for (int i = 0; i < s.length(); i++) {
            out.add(new ArrayElement(i, s.charAt(i), state(i, left, right), String.valueOf(s.charAt(i))));
        }
        return out;
    }

    /** A window over numbers. */
    static List<ArrayElement> of(int[] a, int left, int right) {
        List<ArrayElement> out = new ArrayList<>(a.length);
        for (int i = 0; i < a.length; i++) {
            out.add(new ArrayElement(i, a[i], state(i, left, right)));
        }
        return out;
    }

    private static String state(int i, int left, int right) {
        if (left < 0 || right < left) return "default";
        if (i == right) return "current";
        if (i == left) return "target";
        return i > left && i < right ? "active" : "default";
    }
}
