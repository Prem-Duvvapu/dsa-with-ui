package com.dsa.ui.model;

import java.util.Objects;
import java.util.Set;

/** One labelled DP-table value and its pedagogical Bench state. */
public record DpCell(String value, String state) {

    private static final Set<String> VALID_STATES =
            Set.of("probe", "read", "known", "resolved", "void");

    public DpCell {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(state, "state");
        if (!VALID_STATES.contains(state)) {
            throw new IllegalArgumentException(
                    "Unknown DP cell state '" + state + "'. Expected one of " + VALID_STATES);
        }
    }
}
