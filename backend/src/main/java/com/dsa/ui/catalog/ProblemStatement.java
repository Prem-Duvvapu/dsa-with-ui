package com.dsa.ui.catalog;

import java.util.List;
import java.util.Map;

/**
 * A problem's full statement, written in this project's own words (never copied from the
 * source site), with examples whose outputs are PROVEN by StatementContractTest: each
 * example's input is run through the real tracer and {@code output} must equal the final
 * step's {@code answerVariable}. Constraints are the original problem's bounds - facts,
 * kept apart from the visualizer's own input caps.
 *
 * <p>Loaded from {@code resources/statements/<id>.json} so editing copy never means a
 * recompile of a provider class (REVIEW.md gate 5).
 */
public record ProblemStatement(
        String id,
        List<String> statement,
        List<Example> examples,
        List<String> constraints,
        List<Source> sources) {

    public record Example(Map<String, Object> input, String output, String answerVariable, String explanation) { }

    public record Source(String label, String url) { }
}
