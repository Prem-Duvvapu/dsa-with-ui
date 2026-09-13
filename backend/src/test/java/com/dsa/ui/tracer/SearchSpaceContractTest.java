package com.dsa.ui.tracer;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ExecutionStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A {@code SEARCH_SPACE} tracer has to show a range, and that range has to move.
 *
 * <p>{@code SearchSpaceCanvas} draws one thing: the interval {@code [low, high]} shrinking
 * against the interval it started at. It reads both bounds off the <em>same</em> step -
 * deliberately, because taking {@code low} from one step and {@code high} from another is
 * how {@code GraphCanvas} once drew a carried set of nodes with the current step's edges
 * (RCA-034/035). So a tracer that narrates only the bound it just changed - {@code .var(
 * "high", mid - 1)} and nothing else, which reads like good delta hygiene - never gives the
 * canvas a pair, and every step of the trace renders "No search range for this step."
 *
 * <p>That was live for three problems at once: {@code count-occurrences},
 * {@code first-last-occurrence} and {@code floor-ceil-sorted-array} each ran a correct
 * binary search beside a permanently blank canvas. {@code DsTypePayloadContractTest} could
 * not see it, because they do emit {@code arrayState}; the missing payload was in
 * {@code variables}, which no contract looked at.
 *
 * <p>The second assertion is the pedagogical one. A binary search whose default input
 * resolves on the first probe animates a single static interval, which is the one thing the
 * topic exists to disprove. {@code median-2-sorted-arrays} and
 * {@code kth-element-2-sorted-arrays} both shipped that way: a valid partition on the very
 * first guess, four steps, no halving to watch.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchSpaceContractTest {

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    /** The aliases SearchSpaceCanvas accepts, in its own precedence order. */
    private static final List<String> LOW = List.of("low", "lo", "left", "l", "start");
    private static final List<String> HIGH = List.of("high", "hi", "right", "r", "end");

    Stream<String> searchSpaceIds() {
        return registry.tracedIds().stream()
                .filter(id -> registry.find(id).orElseThrow().dsType() == DsType.SEARCH_SPACE)
                .sorted();
    }

    private static String pick(Map<String, String> vars, List<String> names) {
        if (vars == null) {
            return null;
        }
        for (String name : names) {
            String value = vars.get(name);
            if (value != null && isNumeric(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isNumeric(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** The ranges the canvas would actually draw, one per change, in order. */
    private List<String> rangesDrawn(String id) {
        List<String> ranges = new ArrayList<>();
        for (ExecutionStep step : runner.runDefaults(registry.find(id).orElseThrow()).getSteps()) {
            String low = pick(step.getVariables(), LOW);
            String high = pick(step.getVariables(), HIGH);
            if (low == null || high == null) {
                continue;
            }
            String range = "[" + low + ", " + high + "]";
            if (ranges.isEmpty() || !ranges.get(ranges.size() - 1).equals(range)) {
                ranges.add(range);
            }
        }
        return ranges;
    }

    @ParameterizedTest(name = "{0} states a search range the canvas can draw")
    @MethodSource("searchSpaceIds")
    @DisplayName("Every SEARCH_SPACE tracer names both bounds on one step")
    void statesBothBoundsOnSomeStep(String id) {
        assertFalse(rangesDrawn(id).isEmpty(), id + " declares SEARCH_SPACE but no step of its"
                + " default trace carries a numeric low AND high together, so SearchSpaceCanvas"
                + " renders \"No search range for this step.\" for the whole run. Naming only the"
                + " bound that changed is not enough: the canvas reads the pair off one step on"
                + " purpose, so that the two halves of an interval can never come from different"
                + " moments in time.");
    }

    @ParameterizedTest(name = "{0} narrows its range on its own default input")
    @MethodSource("searchSpaceIds")
    @DisplayName("Every SEARCH_SPACE tracer's default input makes the range move")
    void rangeNarrowsOnDefaults(String id) {
        List<String> ranges = rangesDrawn(id);
        assertTrue(ranges.size() >= 2, id + " draws only the range " + ranges + " on its default"
                + " input, so the search space never visibly halves. The default is the input"
                + " every visitor sees first, and for this topic the halving IS the lesson -"
                + " pick one that takes at least two probes.");
    }
}
