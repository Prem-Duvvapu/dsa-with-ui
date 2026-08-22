package com.dsa.ui.tracer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins what each tracer actually says, step by step.
 *
 * <p>The rest of the suite constrains a trace's SHAPE — that it responds to its input, that
 * it grows, that its anchors are reachable, that no two tracers agree. A trace can satisfy
 * all of that and still be wrong: swap two descriptions, mislabel a variable, highlight the
 * sibling branch, and every existing test stays green. These files are the only thing that
 * notices.
 *
 * <h2>Regenerating</h2>
 * <pre>
 * cd backend &amp;&amp; mvn test -Dtest=GoldenTraceTest -Dgolden.regenerate=true
 * </pre>
 * Then <b>read the diff</b>. A golden file regenerated without reading it records the bug as
 * expected behaviour, which is worse than having no golden file at all. Regenerate only when
 * you meant to change the trace, and say so in the commit message.
 *
 * <p>Pinned against the FULL step shape, never the delta encoding. These record what the
 * algorithm produced; how it is packed for transport is a separate concern that changes on
 * its own schedule (see HANDOFF 9c).
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoldenTraceTest {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden");
    private static final boolean REGENERATE = Boolean.getBoolean("golden.regenerate");

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    private final ObjectMapper json = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    Stream<String> tracerIds() {
        return registry.tracedIds().stream().sorted();
    }

    @ParameterizedTest(name = "{0} matches its golden file")
    @MethodSource("tracerIds")
    @DisplayName("The trace content is exactly what was last approved")
    void traceMatchesItsGoldenFile(String id) throws IOException {
        String actual = render(runner.runDefaults(registry.find(id).orElseThrow()));
        Path file = GOLDEN_DIR.resolve(id + ".json");

        if (REGENERATE) {
            Files.createDirectories(GOLDEN_DIR);
            Files.writeString(file, actual, StandardCharsets.UTF_8);
            return;
        }

        assertTrue(Files.exists(file), id + " has no golden file. If this tracer is new, run:"
                + " mvn test -Dtest=GoldenTraceTest -Dgolden.regenerate=true, then read the"
                + " generated file before committing it.");

        assertEquals(normalise(Files.readString(file, StandardCharsets.UTF_8)), actual,
                id + " produces a different trace than the one last approved. If the change was"
                        + " deliberate, regenerate and explain it in the commit message; if it"
                        + " was not, this is the regression the file exists to catch.");
    }

    @Test
    @DisplayName("No golden file outlives the tracer it pins")
    void everyGoldenFileHasATracer() throws IOException {
        if (!Files.exists(GOLDEN_DIR)) {
            return;
        }
        List<String> orphaned = new ArrayList<>();
        try (Stream<Path> files = Files.list(GOLDEN_DIR)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".json")) {
                    continue;
                }
                String id = name.substring(0, name.length() - ".json".length());
                if (registry.find(id).isEmpty()) {
                    orphaned.add(name);
                }
            }
        }
        assertEquals(List.of(), orphaned,
                "These golden files pin tracers that no longer exist, so they assert nothing"
                        + " while looking like coverage. Delete them.");
    }

    /**
     * What gets pinned: the input actually used, the source the viewer reads, where each
     * anchor resolved, and every step in full.
     *
     * <p>Deliberately excludes maxSteps and maxBytes. Those are budget configuration, not
     * trace content, and tuning a ceiling should not invalidate eight files.
     */
    private String render(ExecutionTrace trace) throws IOException {
        Map<String, Object> pinned = new LinkedHashMap<>();
        pinned.put("problemId", trace.getProblemId());
        pinned.put("resolvedInput", trace.getResolvedInput());
        pinned.put("truncated", trace.isTruncated());
        pinned.put("code", trace.getCode());
        pinned.put("anchors", new TreeMap<>(trace.getAnchors()));
        pinned.put("stepCount", trace.getStepCount());
        pinned.put("steps", trace.getSteps());
        return normalise(json.writeValueAsString(pinned));
    }

    /** Git may hand back CRLF on Windows; the comparison is about content, not line endings. */
    private static String normalise(String text) {
        return text.replace("\r\n", "\n").stripTrailing() + "\n";
    }
}
