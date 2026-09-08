package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Schedules unit-time jobs by descending profit into their latest free deadline slot. */
@Component
public class JobSequencingTracer implements AlgorithmTracer {
    @Override public String id() { return "job-sequencing"; }
    @Override public DsType dsType() { return DsType.ARRAY; }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("jobs", FieldType.INT_GRID)
                .label("Jobs [deadline, profit]")
                .help("Each row is one unit-time job. Deadlines must be 1..20.")
                .constraint("maxRows", 20).constraint("maxCols", 2).values(0, 10_000)
                .defaultValue(List.of(List.of(2, 100), List.of(1, 19), List.of(2, 27),
                        List.of(1, 25), List.of(3, 15)))
                .build());
    }

    @Override public Map<String, Object> alternateInput() {
        return Map.of("jobs", List.of(List.of(1, 35), List.of(3, 30), List.of(2, 25),
                List.of(1, 40), List.of(3, 20), List.of(2, 10)));
    }

    @Override public String annotatedCode() {
        return """
               public int[] schedule(int[][] jobs) {
                   // @a sort
                   Arrays.sort(jobs, (a, b) -> b[1] - a[1]);
                   int[] slots = new int[maxDeadline + 1];
                   Arrays.fill(slots, -1);
                   int count = 0, profit = 0;
                   for (int job = 0; job < jobs.length; job++) {
                       // @a consider
                       for (int slot = jobs[job][0]; slot >= 1; slot--) {
                           // @a probe
                           if (slots[slot] == -1) {
                               // @a place
                               slots[slot] = job;
                               count++;
                               profit += jobs[job][1];
                               break;
                           }
                       }
                   }
                   // @a done
                   return new int[]{count, profit};
               }""";
    }

    @Override public void run(Inputs in, StepEmitter emit) {
        int[][] raw = in.getGrid("jobs");
        Job[] jobs = new Job[raw.length];
        int maxDeadline = 0;
        for (int i = 0; i < raw.length; i++) {
            if (raw[i].length != 2 || raw[i][0] < 1 || raw[i][0] > 20) {
                throw new InputValidationException(Map.of("jobs",
                        "Row " + i + " must be [deadline, profit] with deadline 1..20."));
            }
            jobs[i] = new Job(i + 1, raw[i][0], raw[i][1]);
            maxDeadline = Math.max(maxDeadline, raw[i][0]);
        }
        Arrays.sort(jobs, Comparator.comparingInt(Job::profit).reversed());
        int[] slots = new int[maxDeadline + 1];
        Arrays.fill(slots, -1);
        emit.at("sort").say("Sort jobs by profit descending: %s.", Arrays.toString(jobs))
                .var("slots", slots(slots)).arrayState(board(jobs, -1)).step();

        int count = 0, profit = 0;
        for (int i = 0; i < jobs.length; i++) {
            Job job = jobs[i];
            emit.at("consider").say("Consider J%d: profit %d, deadline %d.", job.id(), job.profit(), job.deadline())
                    .var("profit", profit).var("slots", slots(slots)).arrayState(board(jobs, i)).step();
            boolean placed = false;
            for (int slot = job.deadline(); slot >= 1; slot--) {
                emit.at("probe").say("Try latest legal slot %d: %s.", slot,
                                slots[slot] < 0 ? "free" : "occupied by J" + slots[slot])
                        .var("slot", slot).var("slots", slots(slots)).arrayState(board(jobs, i)).step();
                if (slots[slot] < 0) {
                    slots[slot] = job.id();
                    count++;
                    profit += job.profit();
                    placed = true;
                    emit.at("place").say("Place J%d in slot %d. Total profit is now %d.", job.id(), slot, profit)
                            .var("count", count).var("profit", profit).var("slots", slots(slots))
                            .arrayState(board(jobs, i)).step();
                    break;
                }
            }
            if (!placed) {
                emit.at("consider").say("No free slot before deadline %d; reject J%d.", job.deadline(), job.id())
                        .var("count", count).var("profit", profit).arrayState(board(jobs, i)).step();
            }
        }
        emit.at("done").say("Scheduled %d job(s) for maximum profit %d. Slots: %s.", count, profit, slots(slots))
                .var("count", count).var("profit", profit).var("slots", slots(slots))
                .arrayState(board(jobs, -1)).step();
    }

    private static List<ArrayElement> board(Job[] jobs, int current) {
        List<ArrayElement> out = new ArrayList<>();
        for (int i = 0; i < jobs.length; i++) {
            Job j = jobs[i];
            out.add(new ArrayElement(i, j.profit(), i == current ? "current" : "default",
                    "J" + j.id() + " d" + j.deadline() + " p" + j.profit()));
        }
        return out;
    }
    private static String slots(int[] slots) { return Arrays.toString(Arrays.copyOfRange(slots, 1, slots.length)); }
    private record Job(int id, int deadline, int profit) {
        @Override public String toString() { return "J" + id + "(d" + deadline + ",p" + profit + ")"; }
    }
}
