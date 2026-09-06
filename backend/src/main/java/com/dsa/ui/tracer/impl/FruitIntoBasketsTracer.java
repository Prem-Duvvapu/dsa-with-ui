package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Two baskets means the window may hold at most two distinct fruit types at once - the
 * classic "at most K distinct" sliding window with K fixed at 2. The window only ever
 * grows on the right and shrinks from the left, so every tree is added and removed from
 * the frequency map at most once each: O(N) total, not one scan per starting tree.
 */
@Component
public class FruitIntoBasketsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "fruit-into-baskets";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("fruits", FieldType.INT_ARRAY)
                        .label("Fruit types (encoded as small ids)")
                        .help("Each number is a tree's fruit type; repeats mean the same type again.")
                        .length(1, 30).values(0, 9)
                        .defaultValue(List.of(1, 2, 3, 2, 2))
                        .build());
    }

    /** Every tree the same type - the window never has to shrink, a different code path. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("fruits", List.of(1, 1, 1, 1));
    }

    @Override
    public String annotatedCode() {
        return """
               public int totalFruit(int[] fruits) {
                   Map<Integer, Integer> basket = new LinkedHashMap<>();
                   int left = 0, maxLen = 0;

                   for (int right = 0; right < fruits.length; right++) {
                       // @a expand
                       basket.merge(fruits[right], 1, Integer::sum);

                       while (basket.size() > 2) {
                           // @a shrink
                           int leftType = fruits[left];
                           int remaining = basket.merge(leftType, -1, Integer::sum);
                           if (remaining == 0) basket.remove(leftType);
                           left++;
                       }

                       // @a windowComplete
                       maxLen = Math.max(maxLen, right - left + 1);
                   }
                   // @a done
                   return maxLen;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] fruits = in.getIntArray("fruits");
        Map<Integer, Integer> basket = new LinkedHashMap<>();
        int left = 0;
        int maxLen = 0;

        for (int right = 0; right < fruits.length; right++) {
            basket.merge(fruits[right], 1, Integer::sum);
            emit.at("expand")
                    .say("Add tree %d (type %d). Basket now holds %d distinct type%s.",
                            right, fruits[right], basket.size(), basket.size() == 1 ? "" : "s")
                    .var("right", right).var("distinctTypes", basket.size())
                    .arrayState(windowState(fruits, left, right)).step();

            while (basket.size() > 2) {
                int leftType = fruits[left];
                int remaining = basket.merge(leftType, -1, Integer::sum);
                if (remaining == 0) basket.remove(leftType);
                left++;
                emit.at("shrink")
                        .say("Three distinct types is one too many - drop tree %d (type %d) from "
                                        + "the left. Window now starts at %d.",
                                left - 1, leftType, left)
                        .var("left", left).var("distinctTypes", basket.size())
                        .arrayState(windowState(fruits, left, right)).step();
            }

            maxLen = Math.max(maxLen, right - left + 1);
            emit.at("windowComplete")
                    .say("Window [%d,%d] holds %d trees with at most 2 types. Best so far: %d.",
                            left, right, right - left + 1, maxLen)
                    .var("windowLen", right - left + 1).var("maxLen", maxLen)
                    .arrayState(windowState(fruits, left, right)).step();
        }

        emit.at("done")
                .say("Every tree processed. Most fruit collectible with 2 basket types: %d.", maxLen)
                .var("answer", maxLen)
                .arrayState(windowState(fruits, -1, -1)).step();
    }

    private static List<ArrayElement> windowState(int[] fruits, int left, int right) {
        List<ArrayElement> state = new ArrayList<>(fruits.length);
        for (int i = 0; i < fruits.length; i++) {
            String s = i == right ? "current" : i == left ? "target" : (i > left && i < right) ? "active" : "default";
            state.add(new ArrayElement(i, fruits[i], s));
        }
        return state;
    }
}
