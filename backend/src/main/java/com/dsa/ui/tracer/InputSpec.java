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

    /**
     * Default response ceiling, in bytes.
     *
     * <p>The step budget bounds CPU and does not bound the response. Every step carries a
     * snapshot of the data structure, so the payload grows as steps x n: kadane-algo costs
     * 687 bytes/step on its 9-element default and 2,268 on its 40-element ceiling, which
     * puts a full 5000-step trace at roughly 11 MB. The server survives that; the browser
     * does not. 2 MB leaves generous headroom over every trace this catalogue currently
     * produces while capping the pathological case.
     */
    public static final long DEFAULT_MAX_BYTES = 2_000_000L;

    private final List<InputField> fields;
    private final int maxSteps;
    private final long maxBytes;

    private InputSpec(List<InputField> fields, int maxSteps, long maxBytes) {
        this.fields = List.copyOf(fields);
        this.maxSteps = maxSteps;
        this.maxBytes = maxBytes;
    }

    public static InputSpec of(InputField... fields) {
        return new InputSpec(List.of(fields), DEFAULT_MAX_STEPS, DEFAULT_MAX_BYTES);
    }

    /** For problems whose traces are inherently large (or dangerously so). */
    public InputSpec withMaxSteps(int maxSteps) {
        return new InputSpec(fields, maxSteps, maxBytes);
    }

    /** For problems whose individual steps carry an unusually large payload. */
    public InputSpec withMaxBytes(long maxBytes) {
        return new InputSpec(fields, maxSteps, maxBytes);
    }

    public List<InputField> getFields() { return fields; }
    public int getMaxSteps() { return maxSteps; }
    public long getMaxBytes() { return maxBytes; }

    public InputField field(String name) {
        return fields.stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such input field: " + name));
    }
}
