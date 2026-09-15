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


    /**
     * The two sorted arrays as ONE timeline, which is what the algorithm actually walks.
     *
     * <p>This tracer used to emit only {@code arrival}, with the departure pointer living
     * in a variable string - so a two-pointer sweep was drawn with one pointer, and the
     * comparison the whole algorithm turns on ("is the next arrival before the earliest
     * still-open departure?") had one of its two operands off screen.
     *
     * <p>Cells are labelled A or D because {@code ArrayCanvas} draws the value as the bar
     * height and the label underneath: the height is the clock time, the label is what
     * happens at it.
     *
     * @param i the next unconsumed arrival, or {@code arrival.length} when they are done
     * @param j the earliest still-open departure
     */
    private static List<ArrayElement> timeline(int[] arrival, int[] departure, int i, int j) {
        record Event(int time, boolean isArrival, int index) {}
        List<Event> events = new ArrayList<>(arrival.length + departure.length);
        for (int k = 0; k < arrival.length; k++) {
            events.add(new Event(arrival[k], true, k));
        }
        for (int k = 0; k < departure.length; k++) {
            events.add(new Event(departure[k], false, k));
        }
        // Ties put the arrival first, matching the algorithm's own `arrival[i] <= departure[j]`:
        // a train that arrives exactly as another leaves still needs its own platform.
        events.sort((a, b) -> a.time() != b.time()
                ? Integer.compare(a.time(), b.time())
                : Boolean.compare(!a.isArrival(), !b.isArrival()));

        List<ArrayElement> cells = new ArrayList<>(events.size());
        for (int k = 0; k < events.size(); k++) {
            Event e = events.get(k);
            boolean isCursor = e.isArrival() ? e.index() == i : e.index() == j;
            boolean consumed = e.isArrival() ? e.index() < i : e.index() < j;
            String state = isCursor
                    ? (e.isArrival() ? "current" : "target")
                    : consumed ? "sorted" : "default";
            cells.add(new ArrayElement(k, e.time(), state, e.isArrival() ? "A" : "D"));
        }
        return cells;
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

        emit.at("sort").say("Sort arrivals %s and departures %s independently, then read them as ONE "
                        + "timeline: every A is a train arriving, every D is one leaving. Which train "
                        + "each event belongs to stops mattering from here - only the order does.",
                        Arrays.toString(arrival), Arrays.toString(departure))
                .var("arrival", Arrays.toString(arrival)).var("departure", Arrays.toString(departure))
                .arrayState(timeline(arrival, departure, 0, 0)).step();

        if (n == 1) {
            emit.at("done").say("Only one train — one platform is always enough.")
                    .var("maxPlatforms", 1)
                    .arrayState(timeline(arrival, departure, 1, 0)).step();
            return;
        }

        int platforms = 1, maxPlatforms = 1;
        int i = 1, j = 0;

        emit.at("init").say("The first event on the timeline is always an arrival, so start with "
                        + "1 platform in use. A marks the next arrival, D the earliest departure "
                        + "not yet passed - those two cells are the only ones ever compared.")
                .var("platforms", platforms).var("maxPlatforms", maxPlatforms)
                .arrayState(timeline(arrival, departure, i, j)).step();

        while (i < n && j < n) {
            emit.at("compare").say("Which comes first on the timeline: the next arrival at %d, or the "
                            + "earliest still-open departure at %d?", arrival[i], departure[j])
                    .var("platforms", platforms).var("nextArrival", arrival[i])
                    .var("nextDeparture", departure[j])
                    .arrayState(timeline(arrival, departure, i, j)).step();

            if (arrival[i] <= departure[j]) {
                platforms++;
                emit.at("needMore").say("The arrival wins: %d <= %d, so that train pulls in before any "
                                + "platform frees up. One more platform is needed: %d. Step A forward.",
                                arrival[i], departure[j], platforms)
                        .var("platforms", platforms).var("nextArrival", arrival[i])
                        .var("nextDeparture", departure[j])
                        .arrayState(timeline(arrival, departure, i + 1, j)).step();
                i++;
            } else {
                platforms--;
                emit.at("freeOne").say("The departure wins: %d > %d, so a train leaves before the next "
                                + "one arrives. A platform frees up: %d in use. Step D forward.",
                                arrival[i], departure[j], platforms)
                        .var("platforms", platforms).var("nextArrival", arrival[i])
                        .var("nextDeparture", departure[j])
                        .arrayState(timeline(arrival, departure, i, j + 1)).step();
                j++;
            }
            maxPlatforms = Math.max(maxPlatforms, platforms);

            emit.at("track").say("Highest platform count seen so far: %d.", maxPlatforms)
                    .var("maxPlatforms", maxPlatforms)
                    .arrayState(timeline(arrival, departure, i, j)).step();
        }

        emit.at("done").say("One pointer ran off its end, so no overlap can grow any further. Peak "
                        + "simultaneous trains: %d — that many platforms are required.", maxPlatforms)
                .var("maxPlatforms", maxPlatforms)
                .arrayState(timeline(arrival, departure, i, j)).step();
    }
}
