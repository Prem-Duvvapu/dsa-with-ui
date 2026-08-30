package com.dsa.ui.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Closed vocabulary for every visualization mode the backend may put on the wire.
 *
 * <p>The wire values deliberately retain the legacy spellings used by existing clients
 * and golden traces. The enum names are the canonical taxonomy used inside Java.
 */
public enum DsType {
    ARRAY("Array"),
    WINDOW("Window"),
    SEARCH_SPACE("SearchSpace"),
    MATRIX("Matrix"),
    DP_TABLE("DpTable"),
    STRING("String"),
    BITS("Bits"),
    TREE("Tree"),
    GRAPH("Graph"),
    LINKED_LIST("LinkedList"),
    STACK("Stack"),
    QUEUE("Queue"),
    HEAP("PriorityQueue"),
    TRIE("Trie"),
    RECURSION_TREE("RecursionTree"),
    DSU("Dsu");

    private final String wireValue;

    DsType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    @JsonCreator
    public static DsType fromWireValue(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(type -> type.wireValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown dsType '" + value + "'. Expected one of "
                                + Arrays.stream(values()).map(DsType::wireValue).toList()));
    }
}
