package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Minimum Platforms — a two-pointer sweep over sorted arrivals and departures. Every
 * arrival that lands before the earliest still-open departure needs a fresh platform;
 * every departure that clears before the next arrival frees one up. The running count's
 * high-water mark is the answer.
 */
@Component
public class MinimumPlatformsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "minimum-platforms";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("arrival", FieldType.INT_ARRAY)
                        .label("Arrival times")
                        .help("Arrival time of each train, one entry per train.")
                        .length(1, 40).values(0, 3000)
                        .defaultValue(List.of(900, 940, 950, 1100, 1500, 1800))
                        .build(),
                InputField.of("departure", FieldType.INT_ARRAY)
                        .label("Departure times")
                        .help("Departure time of each train; departure[i] belongs to arrival[i].")
                        .length(1, 40).values(0, 3000)
                        .defaultValue(List.of(910, 1120, 1130, 1200, 1900, 2000))
                        .build());
    }

    /** No two trains overlap at all — only one platform is ever needed. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "arrival", List.of(100, 300, 500),
                "departure", List.of(200, 400, 600));
    }

    @Override
    public String annotatedCode() {
        return """
               public int findPlatform(int[] arrival, int[] departure) {
                   // @a sort
                   Arrays.sort(arrival);
                   Arrays.sort(departure);
                   // @a init
                   int platforms = 1, maxPlatforms = 1;
                   int i = 1, j = 0;
                   while (i < arrival.length && j < departure.length) {
                       // @a compare
                       if (arrival[i] <= departure[j]) {
                           // @a needMore
                           platforms++;
                           i++;
                       } else {
                           // @a freeOne
                           platforms--;
                           j++;
                       }
                       // @a track
                       maxPlatforms = Math.max(maxPlatforms, platforms);
                   }
                   // @a done
                   return maxPlatforms;
               }""";
    }

    private List<ArrayElement> board(int[] vals, int cursor) {
        List<ArrayElement> state = new ArrayList<>(vals.length);
        for (int k = 0; k < vals.length; k++) {
            state.add(new ArrayElement(k, vals[k], k == cursor ? "current" : "target"));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arrival = in.getIntArray("arrival");
        int[] departure = in.getIntArray("departure");

        if (arrival.length != departure.length) {
            throw new InputValidationException(Map.of("departure",
                    "You gave " + arrival.length + " arrivals but " + departure.length + " departures — one per train."));
        }

        int n = arrival.length;
        Arrays.sort(arrival);
        Arrays.sort(departure);

        emit.at("sort").say("Sort arrivals %s and departures %s independently — only the order of events matters now.",
                        Arrays.toString(arrival), Arrays.toString(departure))
                .var("arrival", Arrays.toString(arrival)).var("departure", Arrays.toString(departure))
                .array(arrival).step();

        if (n == 1) {
            emit.at("done").say("Only one train — one platform is always enough.")
                    .var("maxPlatforms", 1)
                    .array(arrival, 0).step();
            return;
        }

        int platforms = 1, maxPlatforms = 1;
        int i = 1, j = 0;

        emit.at("init").say("Train 0 has already arrived, so we start with 1 platform in use.")
                .var("platforms", platforms).var("maxPlatforms", maxPlatforms)
                .array(arrival, 0).step();

        while (i < n && j < n) {
            emit.at("compare").say("Next arrival is %d (train %d); earliest still-open departure is %d.",
                            arrival[i], i, departure[j])
                    .var("platforms", platforms).var("i", i).var("j", j)
                    .array(arrival, i).step();

            if (arrival[i] <= departure[j]) {
                platforms++;
                emit.at("needMore").say("%d <= %d — that arrival lands before any platform frees up. One more platform is needed: %d.",
                                arrival[i], departure[j], platforms)
                        .var("platforms", platforms).var("i", i + 1).var("j", j)
                        .array(arrival, i).step();
                i++;
            } else {
                platforms--;
                emit.at("freeOne").say("%d > %d — a train left before the next one arrives. A platform frees up: %d in use.",
                                arrival[i], departure[j], platforms)
                        .var("platforms", platforms).var("i", i).var("j", j + 1)
                        .array(arrival, i).step();
                j++;
            }
            maxPlatforms = Math.max(maxPlatforms, platforms);

            emit.at("track").say("Highest platform count seen so far: %d.", maxPlatforms)
                    .var("maxPlatforms", maxPlatforms)
                    .array(arrival, Math.min(i, n - 1)).step();
        }

        emit.at("done").say("Every train accounted for. Peak simultaneous trains: %d — that many platforms are required.", maxPlatforms)
                .var("maxPlatforms", maxPlatforms)
                .array(arrival, -1).step();
    }
}
