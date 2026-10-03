package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Hand of Straights (LeetCode 846), traced on the owner's own accepted submission. Walking the
 * distinct cards in order (a TreeMap of counts), every copy of a card either continues one of the
 * groups still open or starts a new one; groupStartQ remembers how many groups started at each of
 * the last groupSize cards, so the oldest batch closes exactly groupSize cards later. A gap in the
 * values, or too few copies to continue the open groups, makes it impossible. O(N log N).
 *
 * <p>No heap is involved, so the canvas shows the distinct cards in order and the queue of group
 * starts beside them.
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
               class Solution {
                   public boolean isNStraightHand(int[] hand, int groupSize) {
                       // @a count
                       int n=hand.length;

                       Map<Integer,Integer> map=new TreeMap<>();

                       for (int card: hand)
                           map.put(card,map.getOrDefault(card,0)+1);

                       Queue<Integer> groupStartQ=new LinkedList<>();
                       int lastCard=-1;
                       int currOpenGroups=0;

                       for (Map.Entry<Integer,Integer> m: map.entrySet()) {
                           int currCard=m.getKey();
                           int currCardFreq=m.getValue();

                           // @a check
                           if (currOpenGroups>0 && currCard>lastCard+1)
                               return false;

                           if (currCardFreq<currOpenGroups)
                               return false;

                           // @a open
                           groupStartQ.add(currCardFreq-currOpenGroups);

                           lastCard=currCard;
                           currOpenGroups=currCardFreq;

                           if (groupStartQ.size()==groupSize)
                               // @a close
                               currOpenGroups-=groupStartQ.poll();
                       }

                       // @a done
                       return currOpenGroups==0;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] hand = in.getIntArray("hand");
        int groupSize = in.getInt("groupSize");
        java.util.TreeMap<Integer, Integer> map = new java.util.TreeMap<>();
        for (int card : hand) map.put(card, map.getOrDefault(card, 0) + 1);
        int[] cards = map.keySet().stream().mapToInt(Integer::intValue).toArray();
        java.util.ArrayDeque<Integer> groupStartQ = new java.util.ArrayDeque<>();
        int lastCard = -1;
        int currOpenGroups = 0;
        emit.at("count").say("Count the cards in value order: %s.", map)
                .var("map", map.toString()).array(cards).queue(groupStartQ).step();
        int idx = 0;
        for (Map.Entry<Integer, Integer> m : map.entrySet()) {
            int currCard = m.getKey();
            int currCardFreq = m.getValue();
            if (currOpenGroups > 0 && currCard > lastCard + 1) {
                emit.at("check").say("%d group%s still need %d, but the next card is %d: the run breaks. Return false.",
                                currOpenGroups, currOpenGroups == 1 ? "" : "s", lastCard + 1, currCard)
                        .var("answer", false).array(cards, idx).queue(groupStartQ).step();
                return;
            }
            if (currCardFreq < currOpenGroups) {
                emit.at("check").say("%d open group%s each need a %d, but there %s only %d. Return false.",
                                currOpenGroups, currOpenGroups == 1 ? "" : "s", currCard, Narration.is(currCardFreq), currCardFreq)
                        .var("answer", false).array(cards, idx).queue(groupStartQ).step();
                return;
            }
            emit.at("check").say(currOpenGroups == 0
                            ? String.format("Card %d (x%d): no group is open, so nothing needs it yet.", currCard, currCardFreq)
                            : String.format("Card %d (x%d) is enough to extend %s %d open group%s.", currCard, currCardFreq,
                                    currOpenGroups == 1 ? "the" : "all", currOpenGroups, currOpenGroups == 1 ? "" : "s"))
                    .var("currOpenGroups", currOpenGroups).array(cards, idx).queue(groupStartQ).step();
            groupStartQ.add(currCardFreq - currOpenGroups);
            lastCard = currCard;
            int started = currCardFreq - currOpenGroups;
            emit.at("open").say("%s at %d; %d group%s now run%s through it.",
                            started == 0 ? "No new group starts" : started + " new group" + (started == 1 ? " starts" : "s start"),
                            currCard, currCardFreq, currCardFreq == 1 ? "" : "s", currCardFreq == 1 ? "s" : "")
                    .var("currOpenGroups", currCardFreq).array(cards, idx).queue(groupStartQ).step();
            currOpenGroups = currCardFreq;
            if (groupStartQ.size() == groupSize) {
                int closed = groupStartQ.poll();
                int startCard = cards[idx - groupSize + 1]; // the distinct card whose entry was just polled
                currOpenGroups -= closed;
                emit.at("close").say(closed == 0
                                ? String.format("No group started at %d, so none closes here. %d still open.",
                                        startCard, currOpenGroups)
                                : String.format("The %d group%s that started at %d now ha%s %d cards - complete. %d still open.",
                                        closed, closed == 1 ? "" : "s", startCard, closed == 1 ? "s" : "ve",
                                        groupSize, currOpenGroups))
                        .var("currOpenGroups", currOpenGroups).array(cards, idx).queue(groupStartQ).step();
            }
            idx++;
        }
        boolean answer = currOpenGroups == 0;
        emit.at("done").say(answer
                        ? "Every group closed with exactly groupSize cards. Return true."
                        : currOpenGroups + " group" + (currOpenGroups == 1 ? " is" : "s are") + " still short of "
                                + groupSize + " cards. Return false.")
                .var("answer", answer).array(cards).queue(groupStartQ).step();
    }
}
