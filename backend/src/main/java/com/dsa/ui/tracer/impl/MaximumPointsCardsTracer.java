package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maximum Points You Can Obtain from Cards (LeetCode 1423), traced on the owner's own accepted
 * submission: start with the first k cards, then k times give back the last front card and take
 * one more from the back - {@code i} wraps around to the end with (i + n) % n. Every split of k
 * cards between the two ends is tried once. O(k).
 *
 * <p>The cards taken are not one contiguous run - they wrap around the end - so the canvas marks
 * each taken card, the one just taken in brightest.
 */
@Component
public class MaximumPointsCardsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "maximum-points-cards";
    }

    @Override
    public DsType dsType() {
        return DsType.WINDOW;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("cardPoints", FieldType.INT_ARRAY)
                        .label("Card Points")
                        .help("Points for each card in row.")
                        .length(1, 30).values(1, 1000)
                        .defaultValue(List.of(1, 2, 3, 4, 5, 6, 1))
                        .workScalesWith("k")
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("K (cards to take)")
                        .help("Number of cards to pick from either end.")
                        .range(1, 30)
                        .defaultValue(3)
                        .build());
    }

    /** Alternate input with uniform cards and k=2. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("cardPoints", List.of(9, 7, 7, 9, 7, 7, 9), "k", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public int maxScore(int[] cardPoints, int k) {
                       // @a init
                       int n = cardPoints.length;
                       int maxSum = 0;
                       int currSum = 0;

                       for (int i=0;i<k;i++) {
                           currSum += cardPoints[i];
                       }

                       maxSum = currSum;

                       int i = 0;
                       int j = k-1;

                       while (j>=0) {
                           // @a swap
                           currSum -= cardPoints[j];
                           j--;
                           i--;

                           i = (i+n)%n;
                           currSum += cardPoints[i];
                           maxSum = Math.max(currSum, maxSum);
                       }

                       // @a done
                       return maxSum;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] cardPoints = in.getIntArray("cardPoints");
        int k = in.getInt("k");
        int n = cardPoints.length;
        if (k > n) {
            throw new InputValidationException(Map.of("k", "can take at most all " + n + " cards"));
        }
        int currSum = 0;
        for (int i = 0; i < k; i++) {
            currSum += cardPoints[i];
        }
        int maxSum = currSum;
        int i = 0;
        int j = k - 1;
        boolean[] taken = new boolean[n];
        for (int t = 0; t < k; t++) taken[t] = true;

        emit.at("init").say("Take the first %d card%s from the front: currSum = %d. That is one way to split the "
                        + "%d cards between the two ends.", k, Narration.s(k), currSum, k)
                .var("currSum", currSum).var("maxSum", maxSum).arrayState(cards(cardPoints, taken, -1)).step();

        while (j >= 0) {
            currSum -= cardPoints[j];
            taken[j] = false;
            int gaveBack = j;
            j--;
            i--;
            i = (i + n) % n;
            currSum += cardPoints[i];
            taken[i] = true;
            maxSum = Math.max(currSum, maxSum);
            emit.at("swap").say("Give back front card %d (%d) and take back card %d (%d): currSum = %d. maxSum = %d.",
                            gaveBack, cardPoints[gaveBack], i, cardPoints[i], currSum, maxSum)
                    .var("i", i).var("j", j).var("currSum", currSum).var("maxSum", maxSum)
                    .arrayState(cards(cardPoints, taken, i)).step();
        }

        emit.at("done").say("Every split has been tried. The best total is %d.", maxSum)
                .var("maxSum", maxSum).var("answer", maxSum).arrayState(cards(cardPoints, taken, -1)).step();
    }

    /** Taken cards are "active", the one just taken "current", the rest "default". */
    private static List<ArrayElement> cards(int[] a, boolean[] taken, int justTaken) {
        List<ArrayElement> out = new ArrayList<>(a.length);
        for (int t = 0; t < a.length; t++) {
            out.add(new ArrayElement(t, a[t], t == justTaken ? "current" : taken[t] ? "active" : "default"));
        }
        return out;
    }
}
