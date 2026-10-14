package com.dsa.ui.tracer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ExactIntegerValidationTest {
    static Stream<FieldType> numericKinds() {
        return Stream.of(FieldType.INT, FieldType.INT_ARRAY, FieldType.INT_GRID,
                FieldType.BINARY_TREE, FieldType.LINKED_LIST);
    }

    private Object wrap(FieldType kind, Object value) {
        return switch (kind) {
            case INT -> value;
            case INT_GRID -> List.of(List.of(value));
            default -> List.of(value);
        };
    }

    @ParameterizedTest @MethodSource("numericKinds")
    void rejectsOverflowBeforeNarrowingOrCheckingBounds(FieldType kind) {
        var spec = InputSpec.of(InputField.of("value", kind).build());
        for (Number value : List.<Number>of(2147483648L, -2147483649L, 4294967297L,
                -4294967295L, Long.MAX_VALUE, Long.MIN_VALUE,
                new BigInteger("340282366920938463463374607431768211457"),
                new BigInteger("-340282366920938463463374607431768211455"))) {
            var error = assertThrows(InputValidationException.class,
                    () -> InputValidator.validate(spec, Map.of("value", wrap(kind, value))),
                    kind + " must not narrow " + value);
            String message = error.getFieldErrors().get("value");
            assertTrue(message.contains("32-bit"), message);
            if (kind == FieldType.INT_GRID) assertTrue(message.contains("Row 1, column 1"));
            else if (kind != FieldType.INT) assertTrue(message.contains("Position 1"));
        }
    }

    @ParameterizedTest @MethodSource("numericKinds")
    void preservesExactlyRepresentableBoundariesAndIntegralWrappers(FieldType kind) {
        var spec = InputSpec.of(InputField.of("value", kind).build());
        for (Number value : List.<Number>of(Integer.MIN_VALUE, Integer.MAX_VALUE,
                (long) Integer.MIN_VALUE, (long) Integer.MAX_VALUE,
                BigInteger.valueOf(Integer.MIN_VALUE), BigInteger.valueOf(Integer.MAX_VALUE),
                (short) -1, (byte) 1, 0)) {
            Object resolved = InputValidator.validate(spec, Map.of("value", wrap(kind, value)))
                    .asMap().get("value");
            assertEquals(wrap(kind, value.intValue()), resolved);
        }
    }

    @ParameterizedTest @MethodSource("numericKinds")
    void rejectsDecimalAndNonNumericValuesRatherThanTruncating(FieldType kind) {
        var spec = InputSpec.of(InputField.of("value", kind).build());
        for (Object value : List.of(1.0, 1.5, 1.0f, new BigDecimal("1"),
                new BigDecimal("1.5"), "1", true)) {
            assertThrows(InputValidationException.class,
                    () -> InputValidator.validate(spec, Map.of("value", wrap(kind, value))), kind + " " + value);
        }
    }

    @Test void retainsTreeNullSlotsAndReportsActualCollectionPosition() {
        var tree = InputSpec.of(InputField.of("tree", FieldType.BINARY_TREE).build());
        assertEquals(Arrays.asList(1, null, -2), InputValidator.validate(tree,
                Map.of("tree", Arrays.asList(1L, null, BigInteger.valueOf(-2)))).asMap().get("tree"));
        var error = assertThrows(InputValidationException.class, () -> InputValidator.validate(tree,
                Map.of("tree", Arrays.asList(1, null, 4294967297L))));
        assertTrue(error.getFieldErrors().get("tree").contains("Position 3"));
    }

    @Test void stillAggregatesErrorsAndEnforcesRangeOrderDistinctAndGridRules() {
        var spec = InputSpec.of(InputField.of("n", FieldType.INT).range(1, 5).build(),
                InputField.of("a", FieldType.INT_ARRAY).values(1, 5).sorted().distinct().build());
        var error = assertThrows(InputValidationException.class,
                () -> InputValidator.validate(spec, Map.of("n", 4294967297L, "a", List.of(4294967297L), "typo", 1)));
        assertEquals(3, error.getFieldErrors().size());
        for (List<Integer> a : List.of(List.of(2, 1), List.of(1, 1), List.of(0), List.of(6))) {
            assertThrows(InputValidationException.class,
                    () -> InputValidator.validate(spec, Map.of("n", 1, "a", a)));
        }
        assertThrows(InputValidationException.class, () -> InputValidator.validate(spec, Map.of("n", 6, "a", List.of(1))));
        var grid = InputSpec.of(InputField.of("g", FieldType.INT_GRID).squarePermutation().build());
        assertThrows(InputValidationException.class, () -> InputValidator.validate(grid, Map.of("g", List.of(List.of(1)))));
    }
}
