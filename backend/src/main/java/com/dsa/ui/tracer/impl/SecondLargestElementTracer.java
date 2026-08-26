package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Single-pass second largest: maintain the largest and second-largest values seen so far.
 * Each element updates either largest (pushing the old largest down to second-largest) or
 * second-largest directly if it falls strictly between them.
 */
@Component
public class SecondLargestElementTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "second-largest-element";
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("Scan the array tracking both largest and second-largest values in a single pass.")
                        .length(2, 40).values(-999, 999)
                        .defaultValue(List.of(12, 35, 1, 10, 34, 1))
                        .build());
    }

    /** Strictly decreasing: the new-largest branch never fires after initialization. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(9, 8, 7, 6));
    }

    @Override
    public String annotatedCode() {
        return """
               public int getSecondLargest(int[] arr) {
                   // @a init
                   int largest = arr[0];
                   int secondLargest = -1;
                   for (int i = 1; i < arr.length; i++) {
                       // @a compare
                       if (arr[i] > largest) {
                           // @a new-largest
                           secondLargest = largest;
                           largest = arr[i];
                       } else if (arr[i] < largest && arr[i] > secondLargest) {
                           // @a new-second
                           secondLargest = arr[i];
                       }
                   }
                   // @a done
                   return secondLargest;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("nums");
        int largest = arr[0];
        int secondLargest = -1;

        emit.using("Array");
        emit.at("init")
                .say("Start with first element: largest = %d, secondLargest = -1.", largest)
                .var("largest", largest).var("secondLargest", secondLargest).array(arr, 0).step();

        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > largest) {
                emit.at("compare")
                        .say("i=%d: arr[%d]=%d > largest=%d.", i, i, arr[i], largest)
                        .var("i", i).var("largest", largest).var("secondLargest", secondLargest)
                        .array(arr, i, 0).step();
                secondLargest = largest;
                largest = arr[i];
                emit.at("new-largest")
                        .say("New largest is %d; previous largest %d becomes secondLargest.", largest, secondLargest)
                        .var("i", i).var("largest", largest).var("secondLargest", secondLargest)
                        .array(arr, i, 0).step();
            } else if (arr[i] < largest && arr[i] > secondLargest) {
                emit.at("compare")
                        .say("i=%d: arr[%d]=%d < largest=%d, but beats secondLargest=%d.", i, i, arr[i], largest, secondLargest)
                        .var("i", i).var("largest", largest).var("secondLargest", secondLargest)
                        .array(arr, i, 0).step();
                secondLargest = arr[i];
                emit.at("new-second")
                        .say("New secondLargest becomes %d (largest stays %d).", secondLargest, largest)
                        .var("i", i).var("largest", largest).var("secondLargest", secondLargest)
                        .array(arr, i, 0).step();
            } else {
                emit.at("compare")
                        .say("i=%d: arr[%d]=%d <= secondLargest=%d (or equals largest), no update.", i, i, arr[i], secondLargest)
                        .var("i", i).var("largest", largest).var("secondLargest", secondLargest)
                        .array(arr, i, 0).step();
            }
        }

        emit.at("done")
                .say("Scan complete: largest is %d, second largest is %d.", largest, secondLargest)
                .var("largest", largest).var("secondLargest", secondLargest).array(arr).step();
    }
}
