package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;
import java.util.*;

/** Bounded by registered (problem, approach) pairs, with no substitution on lookup. */
@Component
public final class SolutionApproachRegistry {
    private final Map<String, Map<String, SolutionApproach>> byProblem;
    private final Map<String, SolutionApproach> defaults;

    public SolutionApproachRegistry(TracerRegistry canonical, ProblemCatalog catalog,
                                   List<SolutionApproachProvider> providers) {
        Map<String, Map<String, SolutionApproach>> declared = new LinkedHashMap<>();
        for (var provider : providers) for (var approach : provider.approaches(canonical, catalog)) {
            validate(approach, canonical, catalog);
            var group = declared.computeIfAbsent(approach.problemId(), id -> new LinkedHashMap<>());
            if (group.putIfAbsent(approach.id(), approach) != null) {
                throw new IllegalStateException("Duplicate solution approach: " + approach.problemId() + "/" + approach.id());
            }
        }
        Map<String, SolutionApproach> selectedDefaults = new LinkedHashMap<>();
        for (var tracer : canonical.all()) {
            var group = declared.get(tracer.id());
            if (group == null) {
                var problem = catalog.find(tracer.id()).orElseThrow(() ->
                        new IllegalStateException("Orphan canonical tracer: " + tracer.id())).getProblem();
                var current = new SolutionApproach(tracer.id(), "canonical", "Current solution",
                        "The existing solution for this problem.", true, ApproachComplexity.from(problem.getComplexity()), tracer);
                // This adapter identifies existing code without guessing whether it is DP.
                group = new LinkedHashMap<>();
                group.put(current.id(), current);
                declared.put(tracer.id(), group);
            }
            var chosen = group.values().stream().filter(SolutionApproach::defaultApproach).toList();
            if (chosen.size() != 1) throw new IllegalStateException("Expected one default approach: " + tracer.id());
            if (chosen.get(0).tracer() != tracer) {
                throw new IllegalStateException("Default must preserve canonical executable: " + tracer.id());
            }
            selectedDefaults.put(tracer.id(), chosen.get(0));
        }
        Map<String, Map<String, SolutionApproach>> frozen = new LinkedHashMap<>();
        declared.forEach((id, group) -> frozen.put(id, Collections.unmodifiableMap(new LinkedHashMap<>(group))));
        byProblem = Collections.unmodifiableMap(frozen);
        defaults = Map.copyOf(selectedDefaults);
    }

    private static void validate(SolutionApproach approach, TracerRegistry canonical, ProblemCatalog catalog) {
        if (approach == null || approach.problemId() == null || catalog.find(approach.problemId()).isEmpty()
                || canonical.find(approach.problemId()).isEmpty()) throw new IllegalStateException("Orphan solution approach");
        if (approach.id() == null || !approach.id().matches("[a-z][a-z0-9-]*")
                || approach.label() == null || approach.label().isBlank()
                || approach.summary() == null || approach.summary().isBlank()) {
            throw new IllegalStateException("Invalid approach identity: " + approach.problemId());
        }
        var tracer = approach.tracer();
        if (tracer == null || !approach.problemId().equals(tracer.id()) || tracer.dsType() == null) {
            throw new IllegalStateException("Approach executable identity mismatch: " + approach.problemId());
        }
        if (tracer.annotatedCode() == null || tracer.annotatedCode().isBlank()) {
            throw new IllegalStateException("Missing approach source: " + approach.id());
        }
        var source = AnnotatedCode.parse(tracer.annotatedCode());
        if (approach.teaching() != null && !source.getAnchors().keySet().containsAll(approach.teaching().anchorNotes().keySet())) {
            throw new IllegalStateException("Teaching references an undeclared source anchor: " + approach.id());
        }
        var spec = tracer.inputSpec();
        if (spec == null || spec.getMaxSteps() <= 0 || spec.getMaxSteps() > InputSpec.DEFAULT_MAX_STEPS
                || spec.getMaxBytes() <= 0 || spec.getMaxBytes() > InputSpec.DEFAULT_MAX_BYTES || spec.getFields().isEmpty()) {
            throw new IllegalStateException("Unbounded or missing approach spec: " + approach.id());
        }
        var canonicalFields = canonical.find(approach.problemId()).orElseThrow().inputSpec().getFields().stream()
                .map(f -> f.getName() + ":" + f.getType()).toList();
        var fields = spec.getFields().stream().map(f -> f.getName() + ":" + f.getType()).toList();
        if (!fields.equals(canonicalFields)) throw new IllegalStateException("Approaches must preserve input fields: " + approach.id());
        for (var field : spec.getFields()) {
            var ceilings = switch (field.getType()) {
                case INT -> List.of("max");
                case INT_ARRAY, STRING, LINKED_LIST, BINARY_TREE -> List.of("maxLength");
                case INT_GRID -> List.of("maxRows", "maxCols");
                case GRAPH -> List.of("maxVertices", "maxEdges");
            };
            for (String key : ceilings) {
                Object value = field.getConstraints().get(key);
                if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue())
                        || n.doubleValue() != n.intValue()) {
                    throw new IllegalStateException("Missing input ceiling: " + approach.id() + "/" + field.getName() + "/" + key);
                }
            }
        }
        InputValidator.validate(spec, Map.of());
        if (tracer.alternateInput() == null) throw new IllegalStateException("Missing alternate input: " + approach.id());
        InputValidator.validate(spec, tracer.alternateInput());
    }

    public List<SolutionApproach> available(String problemId) {
        return List.copyOf(byProblem.getOrDefault(problemId, Map.of()).values());
    }

    public SolutionApproach resolve(String problemId, String requested) {
        var chosen = requested == null ? defaults.get(problemId)
                : byProblem.getOrDefault(problemId, Map.of()).get(requested);
        if (chosen == null) throw new UnavailableApproachException(problemId, requested,
                available(problemId).stream().map(SolutionApproach::id).toList());
        return chosen;
    }
}
