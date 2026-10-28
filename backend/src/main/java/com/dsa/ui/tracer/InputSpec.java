package com.dsa.ui.tracer;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
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
    private final List<List<String>> equalLengthGroups;

    private InputSpec(List<InputField> fields, int maxSteps, long maxBytes, List<List<String>> equalLengthGroups) {
        this.fields = List.copyOf(fields);
        this.maxSteps = maxSteps;
        this.maxBytes = maxBytes;
        this.equalLengthGroups = equalLengthGroups.stream().map(List::copyOf).toList();
    }

    public static InputSpec of(InputField... fields) {
        return new InputSpec(List.of(fields), DEFAULT_MAX_STEPS, DEFAULT_MAX_BYTES, List.of());
    }

    /** For problems whose traces are inherently large (or dangerously so). */
    public InputSpec withMaxSteps(int maxSteps) {
        return new InputSpec(fields, maxSteps, maxBytes, equalLengthGroups);
    }

    /** For problems whose individual steps carry an unusually large payload. */
    public InputSpec withMaxBytes(long maxBytes) {
        return new InputSpec(fields, maxSteps, maxBytes, equalLengthGroups);
    }

    /** Indexed arrays describing the same items; later members are checked against the first. */
    public InputSpec withEqualLengthGroup(String... names) {
        if (names == null || names.length < 2) {
            throw new IllegalArgumentException("An equal-length group needs at least two INT_ARRAY fields.");
        }
        var used = new HashSet<String>();
        equalLengthGroups.forEach(used::addAll);
        var group = new ArrayList<String>();
        int minimum = 0;
        int maximum = Integer.MAX_VALUE;
        for (String name : names) {
            if (name == null || name.isBlank() || !used.add(name)) {
                throw new IllegalArgumentException("Equal-length groups need disjoint, unique field names.");
            }
            if (fields.stream().filter(candidate -> candidate.getName().equals(name)).count() != 1) {
                throw new IllegalArgumentException("Equal-length fields must be declared exactly once: " + name);
            }
            InputField member = field(name);
            if (member.getType() != FieldType.INT_ARRAY) {
                throw new IllegalArgumentException("Equal-length groups support INT_ARRAY fields only: " + name);
            }
            minimum = Math.max(minimum, lengthBound(member, "minLength", 0));
            maximum = Math.min(maximum, lengthBound(member, "maxLength", Integer.MAX_VALUE));
            group.add(name);
        }
        if (minimum > maximum) {
            throw new IllegalArgumentException("Equal-length fields have no common permitted length.");
        }
        var groups = new ArrayList<>(equalLengthGroups);
        groups.add(group);
        return new InputSpec(fields, maxSteps, maxBytes, groups);
    }

    private static int lengthBound(InputField member, String key, int fallback) {
        if (!member.getConstraints().containsKey(key)) return fallback;
        Object raw = member.getConstraints().get(key);
        String message = member.getName() + "." + key + " must be a nonnegative 32-bit integer.";
        if (!(raw instanceof Number)) throw new IllegalArgumentException(message);
        final int bound;
        try {
            bound = new BigDecimal(raw.toString()).intValueExact();
        } catch (NumberFormatException | ArithmeticException invalid) {
            throw new IllegalArgumentException(message, invalid);
        }
        if (bound < 0) throw new IllegalArgumentException(message);
        return bound;
    }

    public List<InputField> getFields() { return fields; }
    public int getMaxSteps() { return maxSteps; }
    public long getMaxBytes() { return maxBytes; }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<List<String>> getEqualLengthGroups() { return equalLengthGroups; }

    public InputField field(String name) {
        return fields.stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such input field: " + name));
    }
}
