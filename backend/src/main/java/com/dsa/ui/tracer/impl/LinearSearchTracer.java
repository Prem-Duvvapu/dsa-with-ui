package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Linear search: iterate through the array from left to right, comparing each
 * element to the target value until a match is found or the array is exhausted.
 */
@Component
public class LinearSearchTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "linear-search";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("The numbers to search.")
                        .length(1, 40).values(-999, 999)
                        .defaultValue(List.of(1, 2, 3, 4, 5))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target")
                        .range(-999, 999)
                        .defaultValue(3)
                        .build());
    }

    /** Target 9 is not in the array, so this is the input that reaches the not-found branch. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 3, 4, 5), "target", 9);
    }

    @Override
    public String annotatedCode() {
        return """
               public int search(int[] arr, int num) {
                   // @a init
                   for (int i = 0; i < arr.length; i++) {
                       // @a check
                       if (arr[i] == num) {
                           // @a found
                           return i;
                       }
                   }
                   // @a not-found
                   return -1;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("nums");
        int target = in.getInt("target");

        emit.at("init")
                .say("Start linear search for target %d in array of length %d.", target, arr.length)
                .var("target", target).array(arr).step();

        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == target) {
                emit.at("check")
                        .say("i=%d: arr[%d]=%d matches target %d!", i, i, arr[i], target)
                        .var("i", i).var("target", target).array(arr, i).step();
                emit.at("found")
                        .say("Target %d found at index %d.", target, i)
                        .var("i", i).var("result", i).array(arr, i).step();
                return;
            }
            emit.at("check")
                    .say("i=%d: arr[%d]=%d != target %d, continue scanning.", i, i, arr[i], target)
                    .var("i", i).var("target", target).array(arr, i).step();
        }

        emit.at("not-found")
                .say("Target %d not found after scanning all %d elements. Return -1.", target, arr.length)
                .var("target", target).var("result", -1).array(arr).step();
    }
}
