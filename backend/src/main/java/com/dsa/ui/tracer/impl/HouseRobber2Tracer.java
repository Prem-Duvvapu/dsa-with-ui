package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * House Robber II: the same non-adjacent recurrence as
 * {@link MaxSumNonAdjacentTracer}, but the houses sit in a circle, so the first and last are
 * neighbours too.
 *
 * <p>The trick is not a new recurrence — it is breaking the circle. Running the linear solver
 * twice, once forbidding the last house and once forbidding the first, guarantees the two
 * ends are never taken together, and the better run is the answer. So the table carries
 * <em>both</em> passes as their own rows with the forbidden house voided out, and the default
 * input is chosen so the two passes disagree (11 against 10). A trace showing one pass, or
 * two passes that happen to agree, would not show why the second pass exists.
 */
@Component
public class HouseRobber2Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "house-robber-2";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("House values")
                        .help("Arranged in a circle, so house 0 and the last house are "
                                + "adjacent and cannot both be robbed.")
                        .length(2, 16).values(0, 999)
                        .defaultValue(List.of(2, 7, 9, 3, 1))
                        .build());
    }

    /** Shorter, and the other pass wins — proof that neither pass may be dropped. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 1, 1));
    }

    @Override
    public String annotatedCode() {
        return """
               public int rob(int[] nums) {
                   int n = nums.length;
                   // @a init
                   int[] skipLast = new int[n];    // rob houses 0 .. n-2
                   int[] skipFirst = new int[n];   // rob houses 1 .. n-1
                   // @a passOne
                   fill(skipLast, nums, 0, n - 2);
                   // @a passTwo
                   fill(skipFirst, nums, 1, n - 1);
                   // @a done
                   return Math.max(skipLast[n - 2], skipFirst[n - 1]);
               }

               private void fill(int[] best, int[] nums, int lo, int hi) {
                   // @a seed
                   best[lo] = nums[lo];
                   for (int i = lo + 1; i <= hi; i++) {
                       int take = nums[i] + (i - 2 >= lo ? best[i - 2] : 0);
                       int skip = best[i - 1];
                       // @a decide
                       best[i] = Math.max(take, skip);
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        Pass skipLast = new Pass("skip last", new int[n], new boolean[n], n - 1, 0, n - 2);
        Pass skipFirst = new Pass("skip first", new int[n], new boolean[n], 0, 1, n - 1);

        emit.at("init")
                .say("House 0 and house %d are neighbours, so the linear recurrence cannot be "
                        + "applied once — it would happily rob both ends. Instead the circle is "
                        + "broken two ways, and each break gets its own row.", n - 1)
                .var("n", n)
                .dpTable(table(nums, skipLast, skipFirst, null, -1, "?", Set.of(), false))
                .step();

        emit.at("passOne")
                .say("First break: forbid house %d outright. Every selection this pass finds is "
                        + "legal on the circle, because the one conflict the linear recurrence "
                        + "cannot see — house 0 sitting next to house %d — is now impossible.",
                        n - 1, n - 1)
                .var("pass", "skip-last")
                .var("forbidden", n - 1)
                .dpTable(table(nums, skipLast, skipFirst, skipLast, -1, "?", Set.of(), false))
                .step();
        int first = fillPass(emit, nums, skipLast, skipFirst, skipLast, "skip-last");

        emit.at("passTwo")
                .say("Second break: forbid house 0 instead, freeing house %d. No legal selection "
                        + "can contain both ends, so every one of them survives at least one of "
                        + "these two passes — which is what makes taking the better result "
                        + "correct.", n - 1)
                .var("pass", "skip-first")
                .var("forbidden", 0)
                .dpTable(table(nums, skipLast, skipFirst, skipFirst, -1, "?", Set.of(), false))
                .step();
        int second = fillPass(emit, nums, skipLast, skipFirst, skipFirst, "skip-first");

        int answer = Math.max(first, second);
        emit.at("done")
                .say("Forbidding the last house yields %d; forbidding the first yields %d. The "
                        + "answer is the better break, %d. %s", first, second, answer,
                        first == second
                                ? "Here they agree, but they need not — and only running both "
                                        + "guarantees the ends are never taken together."
                                : "They disagree, which is exactly why one pass is not enough.")
                .var("skipLast", first)
                .var("skipFirst", second)
                .var("answer", answer)
                .dpTable(table(nums, skipLast, skipFirst, null, -1, "?", Set.of(), true))
                .step();
    }

    /** Fills one pass over {@code [lo, hi]}, emitting a step per decision. */
    private static int fillPass(StepEmitter emit, int[] nums, Pass a, Pass b, Pass active,
                                String passVar) {
        int[] best = active.dp();
        int lo = active.lo();
        int hi = active.hi();

        best[lo] = nums[lo];
        active.settled()[lo] = true;
        emit.at("seed")
                .say("House %d is the earliest one this pass may rob, so with nothing before it "
                        + "the best is its own value, %d.", lo, nums[lo])
                .var("pass", passVar)
                .var("i", lo)
                .var("best[i]", best[lo])
                .dpTable(table(nums, a, b, active, -1, "?", Set.of(), false)).step();

        for (int i = lo + 1; i <= hi; i++) {
            boolean hasGap = i - 2 >= lo;
            int carried = hasGap ? best[i - 2] : 0;
            int take = nums[i] + carried;
            int skip = best[i - 1];
            int chosen = Math.max(take, skip);

            String robClause = hasGap
                    ? "rob it and add best[%d]: %d + %d = %d".formatted(i - 2, nums[i],
                            carried, take)
                    : "rob it, with no earlier house to add, for %d".formatted(take);
            String verdict;
            if (take > skip) {
                verdict = "robbing house %d wins".formatted(i);
            } else if (skip > take) {
                verdict = ("leaving house %d alone wins: its %d does not cover what house %d "
                        + "already secured").formatted(i, nums[i], i - 1);
            } else {
                verdict = "the two tie and house %d is optional".formatted(i);
            }

            emit.at("decide")
                    .say("House %d holds %d: %s, or leave it and inherit best[%d] = %d. "
                            + "best[%d] = %d, so %s.",
                            i, nums[i], robClause, i - 1, skip, i, chosen, verdict)
                    .var("pass", passVar)
                    .var("i", i)
                    .var("take", take)
                    .var("skip", skip)
                    .var("best[i]", chosen)
                    .dpTable(table(nums, a, b, active, i, String.valueOf(chosen),
                            hasGap ? Set.of(i - 1, i - 2) : Set.of(i - 1), false)).step();

            best[i] = chosen;
            active.settled()[i] = true;
        }

        return best[hi];
    }

    private static DpTable table(int[] nums, Pass a, Pass b, Pass active,
                                 int probe, String probeValue, Set<Integer> reads,
                                 boolean done) {
        int n = nums.length;
        List<String> colLabels = new ArrayList<>(n);
        List<DpCell> valueRow = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            colLabels.add(String.valueOf(i));
            valueRow.add(new DpCell(String.valueOf(nums[i]), "known"));
        }

        return new DpTable(
                List.of("value", a.label(), b.label()),
                colLabels,
                List.of(valueRow,
                        dpRow(a, a == active ? probe : -1, probeValue,
                                a == active ? reads : Set.of(), done),
                        dpRow(b, b == active ? probe : -1, probeValue,
                                b == active ? reads : Set.of(), done)));
    }

    private static List<DpCell> dpRow(Pass pass, int probe, String probeValue,
                                      Set<Integer> reads, boolean done) {
        int n = pass.dp().length;
        List<DpCell> row = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            if (i == pass.excluded()) {
                // The forbidden house never holds a value in this pass, closing step included.
                row.add(new DpCell("—", "void"));
            } else if (i == probe) {
                row.add(new DpCell(probeValue, "probe"));
            } else if (reads.contains(i)) {
                row.add(new DpCell(String.valueOf(pass.dp()[i]), "read"));
            } else if (pass.settled()[i]) {
                row.add(new DpCell(String.valueOf(pass.dp()[i]), done ? "resolved" : "known"));
            } else {
                row.add(new DpCell("·", "void"));
            }
        }
        return row;
    }

    /** One break of the circle: its DP row, which houses it has settled, and which it forbids. */
    private record Pass(String label, int[] dp, boolean[] settled, int excluded, int lo, int hi) {}
}
