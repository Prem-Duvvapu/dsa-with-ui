package com.dsa.ui.tracer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The trust boundary. Everything downstream assumes these rules held.
 */
class InputValidatorTest {

    private static InputValidationException reject(InputSpec spec, Map<String, Object> input) {
        return assertThrows(InputValidationException.class, () -> InputValidator.validate(spec, input));
    }

    @Nested
    @DisplayName("defaults")
    class Defaults {

        @Test
        @DisplayName("Absent fields fall back to the declared default")
        void defaultsFillIn() {
            InputSpec spec = InputSpec.of(
                    InputField.of("n", FieldType.INT).defaultValue(7).build());
            assertEquals(7, InputValidator.defaults(spec).getInt("n"));
        }

        @Test
        @DisplayName("A field with no default and no value is an error, not a null")
        void missingRequiredField() {
            InputSpec spec = InputSpec.of(InputField.of("n", FieldType.INT).build());
            assertTrue(reject(spec, Map.of()).getFieldErrors().containsKey("n"));
        }

        @Test
        @DisplayName("Declared defaults are themselves validated")
        void badDefaultIsCaught() {
            InputSpec spec = InputSpec.of(
                    InputField.of("n", FieldType.INT).range(1, 10).defaultValue(99).build());
            assertThrows(InputValidationException.class, () -> InputValidator.defaults(spec));
        }
    }

    @Nested
    @DisplayName("unknown fields")
    class UnknownFields {

        @Test
        @DisplayName("A misspelled key is rejected rather than silently ignored")
        void unknownFieldRejected() {
            InputSpec spec = InputSpec.of(
                    InputField.of("nums", FieldType.INT_ARRAY).defaultValue(List.of(1)).build());
            // Ignoring it would run the algorithm on a default the caller never asked for.
            assertTrue(reject(spec, Map.of("num", List.of(9))).getFieldErrors().containsKey("num"));
        }
    }

    @Nested
    @DisplayName("INT")
    class Ints {

        private final InputSpec spec = InputSpec.of(
                InputField.of("n", FieldType.INT).range(1, 10).defaultValue(5).build());

        @Test
        void acceptsInRange() {
            assertEquals(10, InputValidator.validate(spec, Map.of("n", 10)).getInt("n"));
        }

        @Test
        void rejectsBelowMin() {
            assertTrue(reject(spec, Map.of("n", 0)).getFieldErrors().get("n").contains("at least 1"));
        }

        @Test
        void rejectsAboveMax() {
            assertTrue(reject(spec, Map.of("n", 11)).getFieldErrors().get("n").contains("at most 10"));
        }

        @Test
        @DisplayName("Fractional values are not silently truncated")
        void rejectsNonInteger() {
            assertTrue(reject(spec, Map.of("n", 4.5)).getFieldErrors().containsKey("n"));
        }

        @Test
        void rejectsWrongType() {
            assertTrue(reject(spec, Map.of("n", "five")).getFieldErrors().containsKey("n"));
        }
    }

    @Nested
    @DisplayName("INT_ARRAY")
    class IntArrays {

        private InputSpec spec(InputField.Builder b) {
            return InputSpec.of(b.defaultValue(List.of(1, 2, 3)).build());
        }

        @Test
        void acceptsAndConverts() {
            InputSpec s = spec(InputField.of("a", FieldType.INT_ARRAY));
            assertArrayEquals(new int[]{4, 5},
                    InputValidator.validate(s, Map.of("a", List.of(4, 5))).getIntArray("a"));
        }

        @Test
        @DisplayName("Length caps keep a trace viewable and the server alive")
        void rejectsTooLong() {
            InputSpec s = spec(InputField.of("a", FieldType.INT_ARRAY).length(1, 3));
            assertTrue(reject(s, Map.of("a", List.of(1, 2, 3, 4))).getFieldErrors()
                    .get("a").contains("3"));
        }

        @Test
        void rejectsTooShort() {
            InputSpec s = spec(InputField.of("a", FieldType.INT_ARRAY).length(2, 9));
            assertTrue(reject(s, Map.of("a", List.of(1))).getFieldErrors().containsKey("a"));
        }

        @Test
        void enforcesValueBounds() {
            InputSpec s = spec(InputField.of("a", FieldType.INT_ARRAY).values(0, 5));
            assertTrue(reject(s, Map.of("a", List.of(1, 99))).getFieldErrors().containsKey("a"));
        }

        @Test
        @DisplayName("A sorted-only algorithm refuses unsorted input with a readable reason")
        void enforcesSorted() {
            InputSpec s = spec(InputField.of("a", FieldType.INT_ARRAY).sorted());
            String msg = reject(s, Map.of("a", List.of(1, 9, 4))).getFieldErrors().get("a");
            assertTrue(msg.contains("sorted"), msg);
        }

        @Test
        void allowsSortedWithDuplicates() {
            InputSpec s = spec(InputField.of("a", FieldType.INT_ARRAY).sorted());
            assertDoesNotThrow(() -> InputValidator.validate(s, Map.of("a", List.of(1, 1, 2))));
        }

        @Test
        void enforcesDistinct() {
            InputSpec s = spec(InputField.of("a", FieldType.INT_ARRAY).distinct());
            assertTrue(reject(s, Map.of("a", List.of(1, 1))).getFieldErrors().containsKey("a"));
        }

        @Test
        @DisplayName("A null element is an error for a plain array")
        void rejectsNullElement() {
            InputSpec s = spec(InputField.of("a", FieldType.INT_ARRAY));
            Map<String, Object> input = new HashMap<>();
            input.put("a", Arrays.asList(1, null, 3));
            assertTrue(reject(s, input).getFieldErrors().containsKey("a"));
        }
    }

    @Nested
    @DisplayName("BINARY_TREE")
    class Trees {

        @Test
        @DisplayName("null means 'no node here' and is preserved")
        void nullsAllowedAndPreserved() {
            InputSpec s = InputSpec.of(InputField.of("t", FieldType.BINARY_TREE)
                    .defaultValue(Arrays.asList(1, null, 2)).build());
            Integer[] tree = InputValidator.defaults(s).getBinaryTree("t");
            assertArrayEquals(new Integer[]{1, null, 2}, tree);
        }
    }

    @Nested
    @DisplayName("STRING")
    class Strings {

        private final InputSpec spec = InputSpec.of(InputField.of("s", FieldType.STRING)
                .length(1, 8).constraint("pattern", "[a-z]+")
                .constraint("patternHint", "Lowercase letters only.")
                .defaultValue("abc").build());

        @Test
        void acceptsValid() {
            assertEquals("hello", InputValidator.validate(spec, Map.of("s", "hello")).getString("s"));
        }

        @Test
        void enforcesMaxLength() {
            assertTrue(reject(spec, Map.of("s", "abcdefghij")).getFieldErrors().containsKey("s"));
        }

        @Test
        @DisplayName("A pattern failure reports the author's hint, not the regex")
        void patternUsesHint() {
            assertEquals("Lowercase letters only.",
                    reject(spec, Map.of("s", "ABC")).getFieldErrors().get("s"));
        }
    }

    @Nested
    @DisplayName("INT_GRID")
    class Grids {

        private final InputSpec spec = InputSpec.of(InputField.of("g", FieldType.INT_GRID)
                .constraint("maxRows", 3).constraint("maxCols", 3).values(0, 1)
                .defaultValue(List.of(List.of(0, 1), List.of(1, 0))).build());

        @Test
        void acceptsRectangular() {
            int[][] g = InputValidator.defaults(spec).getGrid("g");
            assertArrayEquals(new int[]{0, 1}, g[0]);
            assertArrayEquals(new int[]{1, 0}, g[1]);
        }

        @Test
        @DisplayName("A ragged grid is rejected with the offending row")
        void rejectsRagged() {
            String msg = reject(spec, Map.of("g", List.of(List.of(0, 1), List.of(1))))
                    .getFieldErrors().get("g");
            assertTrue(msg.contains("row 1"), msg);
        }

        @Test
        void rejectsOversizeAndOutOfRange() {
            assertTrue(reject(spec, Map.of("g", List.of(List.of(0, 9)))).getFieldErrors().containsKey("g"));
            assertTrue(reject(spec, Map.of("g", List.of(
                    List.of(0), List.of(0), List.of(0), List.of(0)))).getFieldErrors().containsKey("g"));
        }

        @Test
        void rejectsEmpty() {
            assertTrue(reject(spec, Map.of("g", List.of())).getFieldErrors().containsKey("g"));
        }
    }

    @Nested
    @DisplayName("GRAPH")
    class Graphs {

        private final InputSpec spec = InputSpec.of(InputField.of("g", FieldType.GRAPH)
                .constraint("maxVertices", 5).constraint("maxEdges", 6)
                .defaultValue(Map.of("vertices", 3, "edges", List.of(List.of(0, 1), List.of(1, 2))))
                .build());

        @Test
        void buildsAdjacency() {
            Inputs.GraphInput g = InputValidator.defaults(spec).getGraph("g");
            assertEquals(3, g.vertices());
            assertEquals(List.of(1), g.adjacency().get(0));
            assertEquals(List.of(0, 2), g.adjacency().get(1));
        }

        @Test
        @DisplayName("An edge naming a vertex outside the graph is rejected")
        void rejectsOutOfRangeEndpoint() {
            String msg = reject(spec, Map.of("g", Map.of(
                    "vertices", 3, "edges", List.of(List.of(0, 7))))).getFieldErrors().get("g");
            assertTrue(msg.contains("vertex 7"), msg);
        }

        @Test
        void rejectsTooManyVertices() {
            assertTrue(reject(spec, Map.of("g", Map.of("vertices", 99, "edges", List.of())))
                    .getFieldErrors().containsKey("g"));
        }

        @Test
        @DisplayName("An unweighted spec refuses a weight it would silently ignore")
        void rejectsWrongEdgeArity() {
            assertTrue(reject(spec, Map.of("g", Map.of(
                    "vertices", 3, "edges", List.of(List.of(0, 1, 5))))).getFieldErrors().containsKey("g"));
        }
    }

    @Test
    @DisplayName("Several bad fields are all reported together, not one at a time")
    void reportsEveryFieldError() {
        InputSpec spec = InputSpec.of(
                InputField.of("a", FieldType.INT).range(0, 5).defaultValue(1).build(),
                InputField.of("b", FieldType.INT).range(0, 5).defaultValue(1).build());
        Map<String, String> errors = reject(spec, Map.of("a", 99, "b", -3)).getFieldErrors();
        assertEquals(2, errors.size());
        assertTrue(errors.containsKey("a"));
        assertTrue(errors.containsKey("b"));
    }
}
