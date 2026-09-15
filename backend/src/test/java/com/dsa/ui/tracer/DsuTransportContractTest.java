package com.dsa.ui.tracer;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ExecutionStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DSU is the one structure with no payload field of its own.
 *
 * <p>Its state is smuggled through {@code variables} as human-readable strings and re-parsed
 * with a regex in the browser: {@code DsuCanvas} splits {@code parent[]} and {@code rank[]}
 * on commas to rebuild the tables. That makes the string FORMAT part of the wire contract
 * as surely as any field name, and nothing was checking it — a tracer could emit
 * {@code "parent: 0 1 2"} and the canvas would draw a garbage structure with no test
 * failing.
 *
 * <p>This is the compensating guard for the transport recorded as open in RCA-031. It does
 * not make the transport good; it makes drift in it visible.
 */
@SpringBootTest
class DsuTransportContractTest {

    /** Exactly what DsuCanvas strips brackets from and splits on commas. */
    private static final Pattern INT_LIST = Pattern.compile("\\[\\s*-?\\d+\\s*(,\\s*-?\\d+\\s*)*]");

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    static Stream<String> dsuIds() {
        return Stream.of("disjoint-set-dsu", "accounts-merge", "most-stones-removed");
    }

    @ParameterizedTest(name = "{0} emits parent[] and rank[] in the shape the canvas parses")
    @MethodSource("dsuIds")
    @DisplayName("DSU tables arrive as bracketed integer lists of equal length")
    void dsuTablesParse(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        assertEquals(DsType.DSU, tracer.dsType(), id + " is not a DSU tracer any more");

        List<ExecutionStep> steps = runner.runDefaults(tracer).getSteps();
        int checked = 0;

        for (ExecutionStep step : steps) {
            Map<String, String> vars = step.getVariables();
            if (vars == null || !vars.containsKey("parent[]")) {
                continue;
            }
            checked++;
            String parent = vars.get("parent[]");
            String rank = vars.get("rank[]");

            assertNotNull(rank, id + " emitted parent[] without rank[]; DsuCanvas needs both"
                    + " and renders its unavailable panel when either is missing");
            assertTrue(INT_LIST.matcher(parent.trim()).matches(),
                    id + " parent[] is not a bracketed integer list, so the canvas's split on"
                            + " commas produces nonsense: " + parent);
            assertTrue(INT_LIST.matcher(rank.trim()).matches(),
                    id + " rank[] is not a bracketed integer list: " + rank);
            assertEquals(count(parent), count(rank),
                    id + " parent[] and rank[] are different lengths, so the canvas draws rows"
                            + " that do not line up: " + parent + " vs " + rank);
        }

        assertTrue(checked > 0, id + " never emitted a DSU table at all");
    }

    @ParameterizedTest(name = "{0} names the operation each step performs")
    @MethodSource("dsuIds")
    @DisplayName("The Operation key the canvas reads is populated, not left to its default")
    void operationIsNamed(String id) {
        List<ExecutionStep> steps = runner.runDefaults(registry.find(id).orElseThrow()).getSteps();
        boolean named = steps.stream()
                .map(ExecutionStep::getVariables)
                .anyMatch(v -> v != null && v.get("Operation") != null
                        && !v.get("Operation").isBlank());
        assertTrue(named, id + " never sets Operation, so the canvas's banner stays empty for"
                + " the whole run and no step says which union or find is happening");
    }

    private static int count(String list) {
        return list.replaceAll("[\\[\\]\\s]", "").split(",").length;
    }
}
