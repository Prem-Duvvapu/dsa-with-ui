package com.dsa.ui.tracer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Java source with named line anchors, and the mapping from anchor name to line number.
 *
 * <p>Before this, {@code activeLine} was a bare integer written by hand next to each
 * step. Nothing tied it to the code it was supposed to highlight, and it drifted:
 * {@code LinkedListService.generateReverseSteps()} emitted lines 51 and 56 into a
 * nine-line snippet, and twenty-nine problems delegated to it.
 *
 * <p>An anchor is a comment on its own line naming the statement below it:
 *
 * <pre>
 * // &#64;a loop.compare
 * if (sum &gt; maxi) {
 * </pre>
 *
 * The marker is stripped from the displayed source, so {@code loop.compare} resolves to
 * the line the {@code if} actually occupies. A tracer names the anchor; it never counts
 * lines. {@link #resolve} throws on an unknown name, so a typo fails a test rather than
 * silently highlighting the wrong line.
 */
public final class AnnotatedCode {

    private static final String MARKER = "//@a";

    private final String displayCode;
    private final Map<String, Integer> anchors;

    private AnnotatedCode(String displayCode, Map<String, Integer> anchors) {
        this.displayCode = displayCode;
        this.anchors = Map.copyOf(anchors);
    }

    public static AnnotatedCode parse(String annotatedSource) {
        List<String> output = new ArrayList<>();
        Map<String, Integer> found = new LinkedHashMap<>();
        List<String> pending = new ArrayList<>();

        for (String line : annotatedSource.split("\n", -1)) {
            String compact = line.replace(" ", "").replace("\t", "");
            if (compact.startsWith(MARKER)) {
                String name = line.substring(line.indexOf("@a") + 2).trim();
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Anchor marker with no name: " + line);
                }
                if (found.containsKey(name) || pending.contains(name)) {
                    throw new IllegalArgumentException("Duplicate anchor: " + name);
                }
                // Attaches to the next real line, whose number we do not know yet.
                pending.add(name);
                continue;
            }
            output.add(line);
            if (!pending.isEmpty()) {
                for (String name : pending) {
                    found.put(name, output.size()); // 1-based
                }
                pending.clear();
            }
        }

        if (!pending.isEmpty()) {
            throw new IllegalArgumentException(
                    "Anchor(s) " + pending + " have no statement following them.");
        }
        return new AnnotatedCode(String.join("\n", output), found);
    }

    /** The source with anchor markers removed — what the code viewer renders. */
    public String getDisplayCode() {
        return displayCode;
    }

    public Map<String, Integer> getAnchors() {
        return anchors;
    }

    public boolean has(String anchor) {
        return anchors.containsKey(anchor);
    }

    public int resolve(String anchor) {
        Integer line = anchors.get(anchor);
        if (line == null) {
            throw new IllegalArgumentException(
                    "Unknown code anchor '" + anchor + "'. Declared: " + anchors.keySet());
        }
        return line;
    }

    public int lineCount() {
        return displayCode.isEmpty() ? 0 : displayCode.split("\n", -1).length;
    }
}
