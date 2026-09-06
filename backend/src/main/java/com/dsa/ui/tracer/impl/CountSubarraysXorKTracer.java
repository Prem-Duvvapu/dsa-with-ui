package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Count subarrays with bitwise XOR equal to K.
 *
 * <p>Uses cumulative prefix XOR: if {@code xr[0..i] ^ xr[0..j] = K}, then
 * {@code xr[0..j] = xr[0..i] ^ K}. The HashMap counts how many earlier
 * prefixes have the required XOR value, just like the sum-based version
 * counts prefix sums.
 */
@Component
public class CountSubarraysXorKTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "count-subarrays-xor-k";
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
                        .help("Non-negative integers. Count subarrays whose XOR equals K.")
                        .length(1, 30).values(0, 50)
                        .defaultValue(List.of(4, 2, 2, 6, 4))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Target XOR K")
                        .range(0, 100)
                        .defaultValue(6)
                        .build());
    }

    /** Different array and target where XOR matches occur at different positions. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "nums", List.of(5, 6, 7, 8, 9),
                "k", 5);
    }

    @Override
    public String annotatedCode() {
        return """
               public int subarraysWithXorK(int[] a, int k) {
                   // @a init
                   int xr = 0, cnt = 0;
                   Map<Integer, Integer> map = new HashMap<>();
                   map.put(0, 1);
               
                   for (int i = 0; i < a.length; i++) {
                       // @a xorElement
                       xr = xr ^ a[i];
                       int x = xr ^ k;
                       if (map.containsKey(x)) {
                           // @a countMatch
                           cnt += map.getOrDefault(x, 0);
                       }
                       // @a updateMap
                       map.put(xr, map.getOrDefault(xr, 0) + 1);
                   }
                   // @a done
                   return cnt;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] a = in.getIntArray("nums");
        int k = in.getInt("k");
        int xr = 0, cnt = 0;
        Map<Integer, Integer> map = new HashMap<>();
        map.put(0, 1);

        emit.at("init")
                .say("Count subarrays with XOR = %d in %s. Prefix XOR starts at 0, map = {0: 1}.",
                        k, java.util.Arrays.toString(a))
                .var("k", k).var("xr", 0).var("cnt", 0)
                .array(a)
                .step();

        for (int i = 0; i < a.length; i++) {
            xr = xr ^ a[i];
            int x = xr ^ k;

            emit.at("xorElement")
                    .say("i=%d: XOR with %d → prefix XOR = %d. Need xr^K = %d^%d = %d in map.",
                            i, a[i], xr, xr, k, x)
                    .var("i", i).var("xr", xr).var("x", x)
                    .array(a, i)
                    .step();

            if (map.containsKey(x)) {
                int freq = map.get(x);
                cnt += freq;
                emit.at("countMatch")
                        .say("Map contains %d with frequency %d → %d subarray(s) ending at index %d have XOR = %d. Total = %d.",
                                x, freq, freq, i, k, cnt)
                        .var("cnt", cnt).var("freq", freq)
                        .array(a, i)
                        .step();
            }

            map.put(xr, map.getOrDefault(xr, 0) + 1);
            emit.at("updateMap")
                    .say("Store prefix XOR %d in map (now seen %d time(s)).", xr, map.get(xr))
                    .var("map", map.toString()).var("xr", xr)
                    .array(a, i)
                    .step();
        }

        emit.at("done")
                .say("Total subarrays with XOR %d: %d.", k, cnt)
                .var("cnt", cnt)
                .array(a)
                .step();
    }
}
