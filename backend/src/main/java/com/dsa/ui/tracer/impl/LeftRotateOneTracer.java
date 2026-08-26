package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Left rotate array by one position:
 * store the first element in a temp variable, shift all remaining elements left by 1,
 * and place the temp element at the end of the array.
 */
@Component
public class LeftRotateOneTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "left-rotate-one";
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("Shift all elements left by 1 position and place the first element at the end.")
                        .length(1, 40).values(-999, 999)
                        .defaultValue(List.of(1, 2, 3, 4, 5))
                        .build());
    }

    /** Different length and values to exercise shifting across a different size. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(10, 20, 30));
    }

    @Override
    public String annotatedCode() {
        return """
               public void rotateByOne(int[] arr) {
                   // @a init
                   int temp = arr[0];
                   for (int i = 0; i < arr.length - 1; i++) {
                       // @a shift
                       arr[i] = arr[i + 1];
                   }
                   // @a wrap
                   arr[arr.length - 1] = temp;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("nums");
        int temp = arr[0];

        emit.using("Array");
        emit.at("init")
                .say("Save first element temp = arr[0] = %d before shifting.", temp)
                .var("temp", temp).array(arr, 0).step();

        for (int i = 0; i < arr.length - 1; i++) {
            arr[i] = arr[i + 1];
            emit.at("shift")
                    .say("i=%d: Shift arr[%d]=%d left to arr[%d].", i, i + 1, arr[i], i)
                    .var("i", i).var("temp", temp).array(arr, i, i + 1).step();
        }

        arr[arr.length - 1] = temp;
        emit.at("wrap")
                .say("Place saved temp = %d at the last position arr[%d].", temp, arr.length - 1)
                .var("temp", temp).array(arr, arr.length - 1).step();
    }
}
