package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A greedy min-heap read via a {@link TreeMap} acting as one: always start a new group at
 * the smallest card value still available, since any larger starting point would strand
 * that smaller card in no group at all. Each group consumes {@code groupSize} consecutive
 * values in one pass; the moment one of them is missing, no arrangement can work.
 */
@Component
public class HandOfStraightsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "hand-of-straights";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("hand", FieldType.INT_ARRAY)
                        .label("Hand of cards")
                        .help("Card values, any order.")
                        .length(1, 30).values(1, 1000)
                        .defaultValue(List.of(1, 2, 3, 6, 2, 3, 4, 7, 8))
                        .build(),
                InputField.of("groupSize", FieldType.INT)
                        .label("Group size")
                        .range(1, 30)
                        .defaultValue(3)
                        .build());
    }

    /** A hand that cannot be grouped, so the trace ends on failure instead of success. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("hand", List.of(1, 2, 3, 4, 5), "groupSize", 4);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isNStraightHand(int[] hand, int groupSize) {
                   TreeMap<Integer, Integer> count = new TreeMap<>();
                   for (int card : hand) count.merge(card, 1, Integer::sum);

                   while (!count.isEmpty()) {
                       // @a pickSmallest
                       int start = count.firstKey();
                       for (int card = start; card < start + groupSize; card++) {
                           // @a checkConsecutive
                           if (!count.containsKey(card)) return false;
                           int remaining = count.merge(card, -1, Integer::sum);
                           if (remaining == 0) count.remove(card);
                       }
                   }
                   // @a done
                   return true;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] hand = in.getIntArray("hand");
        int groupSize = in.getInt("groupSize");

        TreeMap<Integer, Integer> count = new TreeMap<>();
        for (int card : hand) {
            count.merge(card, 1, Integer::sum);
        }

        if (hand.length % groupSize != 0) {
            emit.at("done")
                    .say("Hand has %d cards, not a multiple of group size %d - impossible.",
                            hand.length, groupSize)
                    .var("answer", false)
                    .array(sortedValues(hand), -1).step();
            return;
        }

        List<Integer> sorted = new ArrayList<>();
        for (int card : hand) sorted.add(card);
        sorted.sort(Integer::compareTo);

        while (!count.isEmpty()) {
            int start = count.firstKey();
            emit.at("pickSmallest")
                    .say("Smallest remaining card is %d - start a new group of %d there.",
                            start, groupSize)
                    .var("groupStart", start).var("groupSize", groupSize)
                    .array(sortedValues(hand), indexOfValue(sorted, start)).step();

            for (int card = start; card < start + groupSize; card++) {
                if (!count.containsKey(card)) {
                    emit.at("checkConsecutive")
                            .say("Need card %d to continue the group starting at %d - none left. Impossible.",
                                    card, start)
                            .var("missing", card).var("answer", false)
                            .array(sortedValues(hand), -1).step();
                    return;
                }
                int remaining = count.merge(card, -1, Integer::sum);
                if (remaining == 0) count.remove(card);
                emit.at("checkConsecutive")
                        .say("Card %d used for this group (%d left of that value).", card, remaining)
                        .var("card", card).var("remainingOfCard", remaining)
                        .array(sortedValues(hand), indexOfValue(sorted, card)).step();
            }
        }

        emit.at("done")
                .say("Every card consumed into a complete group of %d - the hand can be arranged.", groupSize)
                .var("answer", true)
                .array(sortedValues(hand), -1).step();
    }

    private static int[] sortedValues(int[] hand) {
        int[] copy = hand.clone();
        java.util.Arrays.sort(copy);
        return copy;
    }

    private static int indexOfValue(List<Integer> sorted, int value) {
        return sorted.indexOf(value);
    }
}
