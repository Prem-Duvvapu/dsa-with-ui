package com.dsa.ui.tracer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns an untrusted JSON body into an {@link Inputs}, or explains why it cannot.
 *
 * <p>This is the only trust boundary in the tracer pipeline. Everything downstream —
 * every tracer — assumes its declared fields are present, correctly typed, and within
 * the bounds the {@link InputSpec} declared. Size limits are enforced here rather than
 * inside tracers because an unbounded input on a factorial-time algorithm is a denial of
 * service, not a bad user experience.
 *
 * <p>Unknown fields are rejected rather than ignored: silently dropping a misspelled key
 * would run the algorithm on a default the caller did not ask for.
 */
public final class InputValidator {

    private InputValidator() {}

    public static Inputs validate(InputSpec spec, Map<String, Object> supplied) {
        Map<String, Object> raw = supplied == null ? Map.of() : supplied;
        Map<String, String> errors = new LinkedHashMap<>();
        Map<String, Object> resolved = new LinkedHashMap<>();

        List<String> declared = spec.getFields().stream().map(InputField::getName).toList();
        for (String key : raw.keySet()) {
            if (!declared.contains(key)) {
                errors.put(key, "Unknown input field. Expected one of: " + declared);
            }
        }

        for (InputField field : spec.getFields()) {
            Object value = raw.containsKey(field.getName())
                    ? raw.get(field.getName())
                    : field.getDefaultValue();

            if (value == null) {
                errors.put(field.getName(), "This input is required.");
                continue;
            }
            try {
                resolved.put(field.getName(), coerceAndCheck(field, value));
            } catch (IllegalArgumentException e) {
                errors.put(field.getName(), e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new InputValidationException(errors);
        }
        return new Inputs(resolved);
    }

    /** Runs the spec's own defaults through validation, so a bad default fails loudly. */
    public static Inputs defaults(InputSpec spec) {
        return validate(spec, Map.of());
    }

    private static Object coerceAndCheck(InputField f, Object value) {
        return switch (f.getType()) {
            case INT -> checkInt(f, value);
            case INT_ARRAY -> checkIntArray(f, value, false);
            case LINKED_LIST -> checkIntArray(f, value, false);
            case BINARY_TREE -> checkIntArray(f, value, true);
            case STRING -> checkString(f, value);
            case INT_GRID -> checkGrid(f, value);
            case GRAPH -> checkGraph(f, value);
        };
    }

    private static int checkInt(InputField f, Object value) {
        if (!(value instanceof Number n) || value instanceof Double || value instanceof Float) {
            throw new IllegalArgumentException("Expected a whole number.");
        }
        int v = n.intValue();
        Integer min = f.intConstraint("min");
        Integer max = f.intConstraint("max");
        if (min != null && v < min) {
            throw new IllegalArgumentException("Must be at least " + min + ".");
        }
        if (max != null && v > max) {
            throw new IllegalArgumentException("Must be at most " + max + ".");
        }
        return v;
    }

    /** @param allowNulls level-order trees use null to mean "no node here" */
    private static List<Number> checkIntArray(InputField f, Object value, boolean allowNulls) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("Expected a list of whole numbers.");
        }
        Integer minLen = f.intConstraint("minLength");
        Integer maxLen = f.intConstraint("maxLength");
        if (minLen != null && list.size() < minLen) {
            throw new IllegalArgumentException("Needs at least " + minLen + " values.");
        }
        if (maxLen != null && list.size() > maxLen) {
            throw new IllegalArgumentException(
                    "Limited to " + maxLen + " values so the trace stays viewable.");
        }

        Integer minVal = f.intConstraint("minValue");
        Integer maxVal = f.intConstraint("maxValue");
        List<Number> out = new ArrayList<>(list.size());
        Integer previous = null;
        List<Integer> seen = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            Object raw = list.get(i);
            if (raw == null) {
                if (!allowNulls) {
                    throw new IllegalArgumentException("Position " + i + " is empty.");
                }
                out.add(null);
                continue;
            }
            if (!(raw instanceof Number n) || raw instanceof Double || raw instanceof Float) {
                throw new IllegalArgumentException("Position " + i + " is not a whole number.");
            }
            int v = n.intValue();
            if (minVal != null && v < minVal) {
                throw new IllegalArgumentException("Values must be at least " + minVal + ".");
            }
            if (maxVal != null && v > maxVal) {
                throw new IllegalArgumentException("Values must be at most " + maxVal + ".");
            }
            if (f.flag("requireSorted")) {
                if (previous != null && v < previous) {
                    throw new IllegalArgumentException(
                            "This algorithm needs a sorted list; " + v + " comes after " + previous + ".");
                }
                previous = v;
            }
            if (f.flag("requireDistinct")) {
                if (seen.contains(v)) {
                    throw new IllegalArgumentException("Values must be distinct; " + v + " repeats.");
                }
                seen.add(v);
            }
            out.add(v);
        }
        return out;
    }

    private static String checkString(InputField f, Object value) {
        if (!(value instanceof String s)) {
            throw new IllegalArgumentException("Expected text.");
        }
        Integer minLen = f.intConstraint("minLength");
        Integer maxLen = f.intConstraint("maxLength");
        if (minLen != null && s.length() < minLen) {
            throw new IllegalArgumentException("Needs at least " + minLen + " characters.");
        }
        if (maxLen != null && s.length() > maxLen) {
            throw new IllegalArgumentException(
                    "Limited to " + maxLen + " characters so the trace stays viewable.");
        }
        Object pattern = f.getConstraints().get("pattern");
        if (pattern instanceof String p && !s.matches(p)) {
            Object hint = f.getConstraints().get("patternHint");
            throw new IllegalArgumentException(
                    hint instanceof String h ? h : "Contains unsupported characters.");
        }
        return s;
    }

    private static List<List<Number>> checkGrid(InputField f, Object value) {
        if (!(value instanceof List<?> rows)) {
            throw new IllegalArgumentException("Expected a grid of whole numbers.");
        }
        Integer minRows = f.intConstraint("minRows");
        Integer maxRows = f.intConstraint("maxRows");
        if (minRows != null && rows.size() < minRows) {
            throw new IllegalArgumentException("Needs at least " + minRows + " rows.");
        }
        if (maxRows != null && rows.size() > maxRows) {
            throw new IllegalArgumentException("Limited to " + maxRows + " rows.");
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("The grid cannot be empty.");
        }

        Integer maxCols = f.intConstraint("maxCols");
        Integer minVal = f.intConstraint("minValue");
        Integer maxVal = f.intConstraint("maxValue");
        List<List<Number>> out = new ArrayList<>();
        int width = -1;

        for (int r = 0; r < rows.size(); r++) {
            if (!(rows.get(r) instanceof List<?> row)) {
                throw new IllegalArgumentException("Row " + r + " is not a list.");
            }
            if (width == -1) {
                width = row.size();
                if (maxCols != null && width > maxCols) {
                    throw new IllegalArgumentException("Limited to " + maxCols + " columns.");
                }
                if (width == 0) {
                    throw new IllegalArgumentException("Rows cannot be empty.");
                }
            } else if (row.size() != width) {
                throw new IllegalArgumentException(
                        "Every row must be the same width; row " + r + " has "
                                + row.size() + ", expected " + width + ".");
            }
            List<Number> parsed = new ArrayList<>(row.size());
            for (int c = 0; c < row.size(); c++) {
                Object cell = row.get(c);
                if (!(cell instanceof Number n) || cell instanceof Double || cell instanceof Float) {
                    throw new IllegalArgumentException(
                            "Cell (" + r + "," + c + ") is not a whole number.");
                }
                int v = n.intValue();
                if (minVal != null && v < minVal) {
                    throw new IllegalArgumentException("Cells must be at least " + minVal + ".");
                }
                if (maxVal != null && v > maxVal) {
                    throw new IllegalArgumentException("Cells must be at most " + maxVal + ".");
                }
                parsed.add(v);
            }
            out.add(parsed);
        }
        return out;
    }

    private static Map<String, Object> checkGraph(InputField f, Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(
                    "Expected a graph as {\"vertices\": n, \"edges\": [[from, to]]}.");
        }
        Object rawVertices = map.get("vertices");
        if (rawVertices == null) {
            throw new IllegalArgumentException("Missing the vertex count.");
        }
        int vertices = graphInteger(rawVertices,
                "The vertex count must be a whole number in the supported range.");
        if (vertices < 1) {
            throw new IllegalArgumentException("A graph needs at least one vertex.");
        }
        Integer maxVertices = f.intConstraint("maxVertices");
        if (maxVertices != null && vertices > maxVertices) {
            throw new IllegalArgumentException("Limited to " + maxVertices + " vertices.");
        }

        Object rawEdges = map.get("edges");
        if (!(rawEdges instanceof List<?> edges)) {
            throw new IllegalArgumentException("Missing the edge list.");
        }
        Integer maxEdges = f.intConstraint("maxEdges");
        if (maxEdges != null && edges.size() > maxEdges) {
            throw new IllegalArgumentException("Limited to " + maxEdges + " edges.");
        }

        boolean weighted = f.flag("weighted");
        Integer minWeight = f.intConstraint("minWeight");
        Integer maxWeight = f.intConstraint("maxWeight");
        List<List<Number>> parsedEdges = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            if (!(edges.get(i) instanceof List<?> e)) {
                throw new IllegalArgumentException("Edge " + i + " is not a list.");
            }
            int expected = weighted ? 3 : 2;
            if (e.size() != expected) {
                throw new IllegalArgumentException("Edge " + i + " needs exactly " + expected
                        + " values" + (weighted ? " [from, to, weight]." : " [from, to]."));
            }
            List<Number> parsed = new ArrayList<>(e.size());
            for (int j = 0; j < e.size(); j++) {
                parsed.add(graphInteger(e.get(j),
                        "Edge " + i + " must contain whole numbers in the supported range."));
            }
            for (int endpoint = 0; endpoint < 2; endpoint++) {
                int v = parsed.get(endpoint).intValue();
                if (v < 0 || v >= vertices) {
                    throw new IllegalArgumentException("Edge " + i + " refers to vertex " + v
                            + ", outside 0.." + (vertices - 1) + ".");
                }
            }
            if (weighted) {
                int weight = parsed.get(2).intValue();
                if (minWeight != null && weight < minWeight) {
                    throw new IllegalArgumentException(
                            "Edge " + i + " weight must be at least " + minWeight + ".");
                }
                if (maxWeight != null && weight > maxWeight) {
                    throw new IllegalArgumentException(
                            "Edge " + i + " weight must be at most " + maxWeight + ".");
                }
            }
            parsedEdges.add(parsed);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("vertices", vertices);
        out.put("edges", parsedEdges);
        return out;
    }

    private static int graphInteger(Object value, String errorMessage) {
        if (!(value instanceof Number number)
                || value instanceof Double
                || value instanceof Float
                || value instanceof BigDecimal) {
            throw new IllegalArgumentException(errorMessage);
        }

        if (number instanceof BigInteger bigInteger) {
            try {
                return bigInteger.intValueExact();
            } catch (ArithmeticException outOfRange) {
                throw new IllegalArgumentException(errorMessage);
            }
        }

        long candidate = number.longValue();
        if (candidate < Integer.MIN_VALUE || candidate > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(errorMessage);
        }
        return (int) candidate;
    }
}
