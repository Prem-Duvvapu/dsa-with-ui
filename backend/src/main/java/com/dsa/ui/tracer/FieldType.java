package com.dsa.ui.tracer;

/**
 * The kinds of input an algorithm can declare.
 *
 * <p>The frontend renders one editor per kind, so adding a problem never requires new
 * form code — only a new {@link InputSpec}. Wire representations:
 *
 * <ul>
 *   <li>{@code INT} — a JSON number</li>
 *   <li>{@code INT_ARRAY} — {@code [2, 7, 11, 15]}</li>
 *   <li>{@code STRING} — a JSON string</li>
 *   <li>{@code INT_GRID} — {@code [[1,0],[0,1]]}, rectangular</li>
 *   <li>{@code GRAPH} — {@code {"vertices": 5, "edges": [[0,1],[1,2]]}}, edges carry a
 *       third element when the spec is weighted</li>
 *   <li>{@code LINKED_LIST} — node values in order, {@code [1,2,3,4]}</li>
 *   <li>{@code BINARY_TREE} — level order with {@code null} for absent children,
 *       {@code [3,9,20,null,null,15,7]}</li>
 * </ul>
 */
public enum FieldType {
    INT,
    INT_ARRAY,
    STRING,
    INT_GRID,
    GRAPH,
    LINKED_LIST,
    BINARY_TREE
}
