package com.dsa.ui.tracer;

import java.util.List;

/**
 * The complete input contract for one problem: the fields it accepts and the ceiling on
 * how much work a caller may ask for.
 *
 * <p>The size ceiling is not optional. Once input is user-supplied, a factorial-time
 * problem such as permutations or N-Queens will exhaust the server if left unbounded, so
 * every field carries its own limits and {@link #getMaxSteps()} bounds the trace itself.
 */
public final class InputSpec {

    /** Default trace ceiling. Generous for teaching, small enough to stay responsive. */
    public static final int DEFAULT_MAX_STEPS = 5000;

    private final List<InputField> fields;
    private final int maxSteps;

    private InputSpec(List<InputField> fields, int maxSteps) {
        this.fields = List.copyOf(fields);
        this.maxSteps = maxSteps;
    }

    public static InputSpec of(InputField... fields) {
        return new InputSpec(List.of(fields), DEFAULT_MAX_STEPS);
    }

    /** For problems whose traces are inherently large (or dangerously so). */
    public InputSpec withMaxSteps(int maxSteps) {
        return new InputSpec(fields, maxSteps);
    }

    public List<InputField> getFields() { return fields; }
    public int getMaxSteps() { return maxSteps; }

    public InputField field(String name) {
        return fields.stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such input field: " + name));
    }
}
