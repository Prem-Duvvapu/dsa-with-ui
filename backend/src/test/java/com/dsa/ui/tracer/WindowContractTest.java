package com.dsa.ui.tracer;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ExecutionStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A window has to be smaller than the array at least once, or it is not a window.
 *
 * <p>{@code WindowCanvas} derives the window from cell states rather than from variable
 * names — the tracers disagree about whether the bounds are called left/right, start/end or
 * just i, but all twelve agree that cells inside the window carry a non-default state and
 * everything outside stays {@code "default"}. That marking is load-bearing, and it is the
 * kind of contract that lives in a comment until something breaks it.
 *
 * <p>{@code maximum-points-cards} broke it by marking the cards outside its window
 * {@code "sorted"} to mean "already picked" — a reasonable-looking choice that is simply not
 * {@code "default"}. Every cell was therefore inside the window on all nine of its steps,
 * so the canvas drew a window spanning the whole array, motionless, while the narration read
 * "Slide window: drop cardPoints[0]=1, add cardPoints[4]=5". The words described a window
 * moving and the picture showed one that never did, in the topic whose entire subject is the
 * window.
 *
 * <p>A window legitimately covers everything on individual steps — several of these problems
 * end with the whole array inside — so the assertion is that it is a proper subset at least
 * once, not on every step.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WindowContractTest {

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    Stream<String> windowIds() {
        return registry.tracedIds().stream()
                .filter(id -> registry.find(id).orElseThrow().dsType() == DsType.WINDOW)
                .sorted();
    }

    @ParameterizedTest(name = "{0} draws a window narrower than its array at some point")
    @MethodSource("windowIds")
    @DisplayName("A WINDOW tracer leaves something outside the window on some step")
    void windowIsAProperSubsetAtSomePoint(String id) {
        List<ExecutionStep> steps = runner.runDefaults(registry.find(id).orElseThrow()).getSteps();

        boolean everNarrower = steps.stream()
                .map(ExecutionStep::getArrayState)
                .filter(cells -> cells != null && !cells.isEmpty())
                .anyMatch(cells -> cells.stream()
                        .map(ArrayElement::getState)
                        .anyMatch(state -> "default".equals(state)));

        assertTrue(everNarrower, id + " marks every cell as inside the window on every step of"
                + " its default trace, so WindowCanvas draws a window spanning the whole array"
                + " and it never appears to move. The canvas reads the window off the CELL"
                + " STATES, not the variables: cells inside carry a non-default state and"
                + " everything outside must stay \"default\". A state like \"sorted\" for"
                + " \"already consumed\" reads as inside.");
    }
}
