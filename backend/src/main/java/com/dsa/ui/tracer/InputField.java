package com.dsa.ui.tracer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One declared input, with everything the UI needs to render an editor and everything
 * the server needs to validate what comes back.
 *
 * <p>Constraints are deliberately a loose map rather than a type per field kind: they
 * are serialized straight to the client, and the set of useful constraints differs per
 * {@link FieldType}. {@link InputValidator} is the single place that interprets them.
 */
public final class InputField {

    private final String name;
    private final String label;
    private final FieldType type;
    private final Object defaultValue;
    private final String help;
    private final Map<String, Object> constraints;

    private InputField(Builder b) {
        this.name = b.name;
        this.label = b.label;
        this.type = b.type;
        this.defaultValue = b.defaultValue;
        this.help = b.help;
        this.constraints = Map.copyOf(b.constraints);
    }

    public static Builder of(String name, FieldType type) {
        return new Builder(name, type);
    }

    public String getName() { return name; }
    public String getLabel() { return label; }
    public FieldType getType() { return type; }
    public Object getDefaultValue() { return defaultValue; }
    public String getHelp() { return help; }
    public Map<String, Object> getConstraints() { return constraints; }

    /** Convenience for {@link InputValidator}; returns null when the constraint is absent. */
    Integer intConstraint(String key) {
        Object v = constraints.get(key);
        return v instanceof Number n ? n.intValue() : null;
    }

    boolean flag(String key) {
        return Boolean.TRUE.equals(constraints.get(key));
    }

    public static final class Builder {
        private final String name;
        private final FieldType type;
        private String label;
        private Object defaultValue;
        private String help = "";
        private final Map<String, Object> constraints = new LinkedHashMap<>();

        private Builder(String name, FieldType type) {
            this.name = name;
            this.type = type;
            this.label = name;
        }

        public Builder label(String label) { this.label = label; return this; }
        public Builder help(String help) { this.help = help; return this; }
        public Builder defaultValue(Object value) { this.defaultValue = value; return this; }

        public Builder constraint(String key, Object value) {
            constraints.put(key, value);
            return this;
        }

        /** Inclusive bounds on a scalar INT. */
        public Builder range(int min, int max) {
            return constraint("min", min).constraint("max", max);
        }

        /** Inclusive bounds on the number of elements in a collection field. */
        public Builder length(int min, int max) {
            return constraint("minLength", min).constraint("maxLength", max);
        }

        /** Inclusive bounds on each element of a collection field. */
        public Builder values(int min, int max) {
            return constraint("minValue", min).constraint("maxValue", max);
        }

        public Builder sorted() { return constraint("requireSorted", true); }
        public Builder distinct() { return constraint("requireDistinct", true); }
        public Builder directed() { return constraint("directed", true); }
        public Builder weighted() { return constraint("weighted", true); }

        /** Inclusive bounds on the third value of every weighted graph edge. */
        public Builder weights(int min, int max) {
            return constraint("minWeight", min).constraint("maxWeight", max);
        }

        public InputField build() { return new InputField(this); }
    }
}
