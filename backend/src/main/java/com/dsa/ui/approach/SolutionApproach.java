package com.dsa.ui.approach;

import com.dsa.ui.tracer.AlgorithmTracer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** A registered executable, not a label or a source-only alternative. */
public record SolutionApproach(String problemId, String id, String label, String summary,
                               boolean defaultApproach, ApproachComplexity complexity,
                               AlgorithmTracer tracer) {
    public Map<String, Object> summaryView() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", id);
        out.put("label", label);
        out.put("summary", summary);
        out.put("isDefault", defaultApproach);
        out.put("dsType", tracer.dsType());
        out.put("inputSpec", tracer.inputSpec());
        out.put("alternateInput", tracer.alternateInput());
        out.put("complexity", complexity);
        return Collections.unmodifiableMap(out);
    }
}
