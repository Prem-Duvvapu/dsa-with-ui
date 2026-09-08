package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Two directional passes satisfy each child's left and right rating constraints. */
@Component
public class CandyTracer implements AlgorithmTracer {
    @Override public String id() { return "candy"; }
    @Override public DsType dsType() { return DsType.ARRAY; }
    @Override public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("ratings", FieldType.INT_ARRAY).label("Ratings")
                .length(1, 60).values(0, 10_000).defaultValue(List.of(1, 0, 2, 2, 3, 1)).build());
    }
    @Override public Map<String, Object> alternateInput() { return Map.of("ratings", List.of(1, 2, 2)); }
    @Override public String annotatedCode() {
        return """
               public int candy(int[] ratings) {
                   // @a init
                   int[] candies = new int[ratings.length];
                   Arrays.fill(candies, 1);
                   for (int i = 1; i < ratings.length; i++) {
                       if (ratings[i] > ratings[i - 1]) {
                           // @a leftRise
                           candies[i] = candies[i - 1] + 1;
                       } else {
                           // @a leftKeep
                           candies[i] = 1;
                       }
                   }
                   for (int i = ratings.length - 2; i >= 0; i--) {
                       if (ratings[i] > ratings[i + 1]) {
                           // @a rightRise
                           candies[i] = Math.max(candies[i], candies[i + 1] + 1);
                       } else {
                           // @a rightKeep
                           candies[i] = candies[i];
                       }
                   }
                   // @a done
                   return Arrays.stream(candies).sum();
               }""";
    }
    @Override public void run(Inputs in, StepEmitter emit) {
        int[] ratings = in.getIntArray("ratings");
        int[] candies = new int[ratings.length];
        Arrays.fill(candies, 1);
        emit.at("init").say("Give every child one candy before enforcing neighbour constraints.")
                .var("candies", Arrays.toString(candies)).arrayState(board(ratings, candies, -1)).step();
        for (int i = 1; i < ratings.length; i++) {
            String anchor;
            if (ratings[i] > ratings[i - 1]) { candies[i] = candies[i - 1] + 1; anchor = "leftRise"; }
            else { candies[i] = 1; anchor = "leftKeep"; }
            emit.at(anchor).say("Left pass at %d: ratings %d vs %d, candies now %s.",
                            i, ratings[i - 1], ratings[i], Arrays.toString(candies))
                    .var("phase", "left").var("candies", Arrays.toString(candies))
                    .arrayState(board(ratings, candies, i)).step();
        }
        for (int i = ratings.length - 2; i >= 0; i--) {
            String anchor;
            if (ratings[i] > ratings[i + 1]) {
                candies[i] = Math.max(candies[i], candies[i + 1] + 1); anchor = "rightRise";
            } else anchor = "rightKeep";
            emit.at(anchor).say("Right pass at %d: ratings %d vs %d, candies now %s.",
                            i, ratings[i], ratings[i + 1], Arrays.toString(candies))
                    .var("phase", "right").var("candies", Arrays.toString(candies))
                    .arrayState(board(ratings, candies, i)).step();
        }
        int total = Arrays.stream(candies).sum();
        emit.at("done").say("Both neighbour rules hold. Minimum candy total: %d.", total)
                .var("total", total).var("candies", Arrays.toString(candies))
                .arrayState(board(ratings, candies, -1)).step();
    }
    private static List<ArrayElement> board(int[] ratings, int[] candies, int current) {
        List<ArrayElement> out = new ArrayList<>();
        for (int i = 0; i < ratings.length; i++) out.add(new ArrayElement(i, ratings[i],
                i == current ? "current" : "default", ratings[i] + "→" + candies[i]));
        return out;
    }
}
