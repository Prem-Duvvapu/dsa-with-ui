package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Non-preemptive SJF for tasks that are all available at time zero. */
@Component
public class ShortestJobFirstTracer implements AlgorithmTracer {
    @Override public String id() { return "shortest-job-first"; }
    @Override public DsType dsType() { return DsType.ARRAY; }
    @Override public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("bursts", FieldType.INT_ARRAY).label("CPU burst times")
                .help("All tasks arrive at time zero.").length(1, 40).values(1, 10_000)
                .defaultValue(List.of(6, 8, 7, 3)).build());
    }
    @Override public Map<String, Object> alternateInput() { return Map.of("bursts", List.of(9, 2, 5, 1, 4)); }
    @Override public String annotatedCode() {
        return """
               public double averageWaitingTime(int[] bursts) {
                   // @a sort
                   Arrays.sort(bursts);
                   int elapsed = 0, totalWait = 0;
                   for (int burst : bursts) {
                       // @a run
                       totalWait += elapsed;
                       elapsed += burst;
                   }
                   // @a done
                   return (double) totalWait / bursts.length;
               }""";
    }
    @Override public void run(Inputs in, StepEmitter emit) {
        int[] bursts = in.getIntArray("bursts");
        Arrays.sort(bursts);
        emit.at("sort").say("Shortest-job-first order: %s.", Arrays.toString(bursts))
                .var("elapsed", 0).var("totalWait", 0).array(bursts).step();
        int elapsed = 0, totalWait = 0;
        for (int i = 0; i < bursts.length; i++) {
            int wait = elapsed;
            totalWait += wait;
            elapsed += bursts[i];
            emit.at("run").say("Task %d waits %d, then runs for %d. Clock advances to %d.", i, wait, bursts[i], elapsed)
                    .var("wait", wait).var("elapsed", elapsed).var("totalWait", totalWait)
                    .array(bursts, i).step();
        }
        double average = (double) totalWait / bursts.length;
        emit.at("done").say("Total waiting time %d across %d tasks: average %.2f.", totalWait, bursts.length, average)
                .var("totalWait", totalWait).var("average", String.format(Locale.ROOT, "%.2f", average))
                .array(bursts).step();
    }
}
