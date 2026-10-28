package com.dsa.ui.tracer;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

/** Two bounded visualization preconditions, independent of algorithm answers and events. */
public record InputWorkLimit(Kind kind, String arrayField, String scalarField, int limit,
                             boolean reuse, String errorField, String message) {
    public enum Kind { CAPACITY_TABLE_CELLS, SKIP_TAKE_CALLS }

    public InputWorkLimit {
        if (kind == null || blank(arrayField) || blank(scalarField) || blank(errorField)
                || blank(message) || limit <= 0) {
            throw new IllegalArgumentException("A work limit needs a supported kind, fields, positive limit and message.");
        }
        if (kind == Kind.CAPACITY_TABLE_CELLS && reuse) {
            throw new IllegalArgumentException("A table-cell work limit has no reuse dependency.");
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private record Call(int items, int capacity) {}

    /** Exact table size, or exact uncached calls saturated at limit+1. */
    long measure(List<? extends Number> entries, int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("Work limits require nonnegative capacity.");
        if (kind == Kind.CAPACITY_TABLE_CELLS) {
            return (entries.size() + 1L) * (capacity + 1L);
        }
        var pending = new ArrayDeque<Call>();
        pending.push(new Call(entries.size(), capacity));
        long calls = 0;
        while (!pending.isEmpty()) {
            Call call = pending.pop();
            if (++calls > limit) return (long) limit + 1;
            if (call.items == 0 || call.capacity == 0) continue;
            int weight = entries.get(call.items - 1).intValue();
            if (weight <= 0) throw new IllegalArgumentException("Skip/take weights must be positive.");
            // Push take first so the stack visits skip first, as the real recurrence does.
            if (weight <= call.capacity) {
                pending.push(new Call(reuse ? call.items : call.items - 1, call.capacity - weight));
            }
            pending.push(new Call(call.items - 1, call.capacity));
        }
        return calls;
    }

    String refusalMessage(List<? extends Number> entries, int capacity) {
        if (kind == Kind.CAPACITY_TABLE_CELLS) {
            return String.format(Locale.ROOT,
                    "This input needs a %d × %d table (%d cells); a step-by-step trace can show at most %d. %s",
                    entries.size() + 1L, capacity + 1L, measure(entries, capacity), limit, message);
        }
        return message;
    }
}
