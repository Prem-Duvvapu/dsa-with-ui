package com.dsa.ui.tracer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Strict resource loading for complete learner-facing Java. Callers retain the immutable string. */
public final class SolutionSource {
    private SolutionSource() {}
    public static String read(String path) {
        if (!path.startsWith("/solutions/")) throw new IllegalArgumentException("Not a solution resource: " + path);
        try (var stream = SolutionSource.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing complete Java solution: " + path);
            String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            if (source.isBlank()) throw new IllegalStateException("Empty Java solution: " + path);
            return source;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read Java solution: " + path, error);
        }
    }
}
