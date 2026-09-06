package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maximum Points You Can Obtain from Cards — O(N) sliding window.
 * Picking K cards from either end leaves a contiguous subarray of size (N - K) in the middle.
 * Maximizing the sum of the K picked cards is mathematically equivalent to minimizing
 * the sum of the unpicked (N - K) contiguous cards:
 * {@code maxScore = totalSum - minSubarraySum(size N - K)}.
 */
@Component
public class MaximumPointsCardsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "maximum-points-cards";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("cardPoints", FieldType.INT_ARRAY)
                        .label("Card Points")
                        .help("Points for each card in row.")
                        .length(1, 30).values(1, 1000)
                        .defaultValue(List.of(1, 2, 3, 4, 5, 6, 1))
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
               public int maxScore(int[] cardPoints, int k) {
                   int n = cardPoints.length;
                   int totalSum = 0;
                   for (int pt : cardPoints) totalSum += pt;

                   int windowSize = n - k;
                   int windowSum = 0;
                   for (int i = 0; i < windowSize; i++) {
                       // @a buildWindow
                       windowSum += cardPoints[i];
                   }

                   int minWindowSum = windowSum;
                   // @a initialWindow

                   for (int right = windowSize; right < n; right++) {
                       // @a slideWindow
                       int left = right - windowSize;
                       windowSum += cardPoints[right] - cardPoints[left];
                       minWindowSum = Math.min(minWindowSum, windowSum);
                   }
                   // @a done
                   return totalSum - minWindowSum;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] cardPoints = in.getIntArray("cardPoints");
        int k = in.getInt("k");
        int n = cardPoints.length;

        // Clamp k to n if larger
        k = Math.min(k, n);
        int totalSum = 0;
        for (int pt : cardPoints) totalSum += pt;

        int windowSize = n - k;
        int windowSum = 0;

        for (int i = 0; i < windowSize; i++) {
            windowSum += cardPoints[i];
            emit.at("buildWindow")
                    .say("Initial unpicked window (size %d = N - K): add cardPoints[%d]=%d → sum is %d.",
                            windowSize, i, cardPoints[i], windowSum)
                    .var("i", i).var("windowSum", windowSum).var("windowSize", windowSize)
                    .arrayState(windowState(cardPoints, 0, i))
                    .step();
        }

        int minWindowSum = windowSum;
        emit.at("initialWindow")
                .say("Initial unpicked window [0,%d] has sum %d. Picked card score: %d - %d = %d.",
                        Math.max(0, windowSize - 1), minWindowSum, totalSum, minWindowSum, totalSum - minWindowSum)
                .var("minWindowSum", minWindowSum).var("currentScore", totalSum - minWindowSum)
                .arrayState(windowState(cardPoints, 0, Math.max(0, windowSize - 1)))
                .step();

        for (int right = windowSize; right < n; right++) {
            int left = right - windowSize;
            windowSum += cardPoints[right] - cardPoints[left];
            minWindowSum = Math.min(minWindowSum, windowSum);
            int score = totalSum - minWindowSum;

            emit.at("slideWindow")
                    .say("Slide window: drop cardPoints[%d]=%d, add cardPoints[%d]=%d. Window sum: %d (min: %d, maxScore: %d).",
                            left, cardPoints[left], right, cardPoints[right], windowSum, minWindowSum, score)
                    .var("left", left + 1).var("right", right).var("windowSum", windowSum)
                    .var("minWindowSum", minWindowSum).var("maxScore", score)
                    .arrayState(windowState(cardPoints, left + 1, right))
                    .step();
        }

        int answer = totalSum - minWindowSum;
        emit.at("done")
                .say("All unpicked window positions checked. Maximum points from taking %d cards: %d.", k, answer)
                .var("answer", answer)
                .arrayState(windowState(cardPoints, -1, -1))
                .step();
    }

    private static List<ArrayElement> windowState(int[] cardPoints, int left, int right) {
        List<ArrayElement> state = new ArrayList<>(cardPoints.length);
        for (int i = 0; i < cardPoints.length; i++) {
            // Cards inside [left, right] are unpicked (active/target/current), cards outside are picked
            String st = (left >= 0 && right >= 0 && i >= left && i <= right)
                    ? (i == right ? "current" : i == left ? "target" : "active")
                    : "sorted";
            state.add(new ArrayElement(i, cardPoints[i], st, (left >= 0 && i >= left && i <= right) ? "unpicked" : "picked"));
        }
        return state;
    }
}
