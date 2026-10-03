package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Distance of the nearest cell having 1, for every cell of a 0/1 grid. Traced on the owner's own
 * multi-source BFS (their 01 Matrix submission with 0 and 1 swapped) - see {@link NearestOneBfs},
 * which this shares with the same problem catalogued under its other id.
 */
@Component
public class DistanceNearest1Tracer implements AlgorithmTracer {


    @Override
    public String id() {
        return "distance-nearest-1";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("grid", FieldType.INT_GRID)
                        .label("Grid")
                        .help("0/1 grid with at least one 1. Each cell gets its distance to the nearest 1; a 0 "
                                + "the BFS has not reached yet is drawn as -1.")
                        .constraint("maxRows", 10)
                        .constraint("maxCols", 10)
                        .values(0, 1)
                        .defaultValue(List.of(
                                List.of(0, 0, 0),
                                List.of(0, 1, 0),
                                List.of(1, 0, 1)))
                        .build());
    }

    /** A single seed in a corner of a larger grid, instead of three seeds in a small one. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(1, 0, 0, 0),
                List.of(0, 0, 0, 0),
                List.of(0, 0, 0, 0),
                List.of(0, 0, 0, 0)));
    }

    @Override
    public String annotatedCode() {
        return NearestOneBfs.annotatedCode();
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        NearestOneBfs.run(in.getGrid("grid"), emit);
    }
}
