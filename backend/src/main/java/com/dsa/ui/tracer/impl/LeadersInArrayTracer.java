package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Find all leaders in an array (elements greater than all elements to their right)
 * by scanning right-to-left while tracking the running maximum.
 */
@Component
public class LeadersInArrayTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "leaders-in-array";
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
                        .help("The array to inspect for leader elements.")
                        .length(1, 40).values(-999, 999)
                        .defaultValue(List.of(16, 17, 4, 3, 5, 2))
                        .build());
    }

    /** Strictly decreasing array [5, 4, 3, 2, 1]: right-to-left scan makes every element a leader (skip never fires). */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(5, 4, 3, 2, 1));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> findLeaders(int[] arr) {
                   // @a init
                   List<Integer> ans = new ArrayList<>();
                   int n = arr.length, maxi = Integer.MIN_VALUE;
               
                   for (int i = n - 1; i >= 0; i--) {
                       // @a compare
                       if (arr[i] > maxi) {
                           // @a leader
                           ans.add(arr[i]); maxi = arr[i];
                       }
                   }
                   // @a done
                   Collections.reverse(ans);
                   return ans;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("nums");
        int n = arr.length;
        List<Integer> ans = new ArrayList<>();
        int maxi = Integer.MIN_VALUE;

        emit.at("init")
                .say("Start right-to-left scan. maxi = -INF, leaders = [].")
                .var("maxi", "-INF").var("leaders", ans.toString()).array(arr).step();

        for (int i = n - 1; i >= 0; i--) {
            if (arr[i] > maxi) {
                emit.at("compare")
                        .say("i=%d: arr[%d]=%d > maxi (%s), new leader found.",
                                i, i, arr[i], maxi == Integer.MIN_VALUE ? "-INF" : String.valueOf(maxi))
                        .var("i", i).var("maxi", maxi == Integer.MIN_VALUE ? "-INF" : String.valueOf(maxi))
                        .var("leaders", ans.toString())
                        .array(arr, i).step();
                maxi = arr[i];
                ans.add(maxi);
                emit.at("leader")
                        .say("Add %d to leaders list; update maxi = %d. Current leaders: %s.", maxi, maxi, ans.toString())
                        .var("i", i).var("maxi", maxi)
                        .var("leaders", ans.toString())
                        .array(arr, i).step();
            } else {
                emit.at("compare")
                        .say("i=%d: arr[%d]=%d <= maxi (%d), not a leader (skip).", i, i, arr[i], maxi)
                        .var("i", i).var("maxi", maxi)
                        .var("leaders", ans.toString())
                        .array(arr, i).step();
            }
        }

        Collections.reverse(ans);
        emit.at("done")
                .say("Reverse leaders list to restore left-to-right order: %s.", ans.toString())
                .var("leaders", ans.toString()).array(arr).step();
    }
}
