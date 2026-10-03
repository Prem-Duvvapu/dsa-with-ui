package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Task Scheduler (LeetCode 621), traced on the owner's own accepted submission - a counting formula,
 * not a simulation. The most frequent task (count maxFreq) needs maxFreq - 1 full rounds of n + 1
 * slots, plus one last slot for each task that ties it (maxFreqCnt). If other tasks overflow those
 * gaps there is no idle time at all, so the answer is the larger of that and the number of tasks.
 * O(N), no heap.
 *
 * <p>Tasks are entered as numbers 0..25 for the letters A..Z; the code reads them as letters.
 */
@Component
public class TaskSchedulerTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "task-scheduler";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("tasks", FieldType.INT_ARRAY)
                        .label("Tasks (encoded as small task-type ids)")
                        .help("Each number is a task type; repeats mean the same type again.")
                        .length(1, 40).values(0, 25)
                        .defaultValue(List.of(0, 0, 0, 1, 1, 1))
                        .build(),
                InputField.of("n", FieldType.INT)
                        .label("Cooldown")
                        .help("Ticks that must pass before the same task type can repeat.")
                        .range(0, 10)
                        .defaultValue(2)
                        .build());
    }

    /** Five task types (one appearing 5 times) instead of two, so idle ticks show up too. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tasks", List.of(0, 0, 0, 0, 0, 1, 1, 1, 4, 4, 5, 5, 6, 6), "n", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public int leastInterval(char[] tasks, int n) {
                       int[] freq=new int[26];

                       for (char ch: tasks)
                           // @a count
                           freq[ch-'A']++;

                       // @a sort
                       Arrays.sort(freq);

                       int maxFreq=freq[25];
                       int maxFreqCnt=1;
                       int i=24;

                       while (i>=0 && freq[i]==freq[i+1]) {
                           // @a tie
                           maxFreqCnt++;
                           i--;
                       }

                       // @a formula
                       int minIntervals=tasks.length; //Min possible result
                       int currIntervals=(n+1)*(maxFreq-1)+maxFreqCnt;
                       int res=Math.max(minIntervals,currIntervals);

                       return res;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] taskIds = in.getIntArray("tasks");
        int n = in.getInt("n");
        int[] freq = new int[26];
        for (int t : taskIds) {
            freq[t]++;
            emit.at("count").say("Task %c: freq['%c'] becomes %d.", (char) ('A' + t), (char) ('A' + t), freq[t])
                    .var("ch", String.valueOf((char) ('A' + t))).var("freq", counts(freq)).arrayState(letters(freq, t)).step();
        }
        Arrays.sort(freq);
        int maxFreq = freq[25];
        emit.at("sort").say("Sort the counts. The most frequent task occurs maxFreq = %d time%s.", maxFreq, Narration.s(maxFreq))
                .var("maxFreq", maxFreq).array(freq, 25).step();
        int maxFreqCnt = 1;
        int i = 24;
        while (i >= 0 && freq[i] == freq[i + 1]) {
            maxFreqCnt++;
            emit.at("tie").say("freq[%d] = %d ties the maximum: maxFreqCnt = %d tasks share it.", i, freq[i], maxFreqCnt)
                    .var("maxFreqCnt", maxFreqCnt).array(freq, i).step();
            i--;
        }
        int minIntervals = taskIds.length;
        int currIntervals = (n + 1) * (maxFreq - 1) + maxFreqCnt;
        int res = Math.max(minIntervals, currIntervals);
        emit.at("formula").say("(n+1)*(maxFreq-1) + maxFreqCnt = %d*%d + %d = %d slots. There are %d tasks, so the "
                        + "answer is max(%d, %d) = %d.", n + 1, maxFreq - 1, maxFreqCnt, currIntervals,
                        minIntervals, minIntervals, currIntervals, res)
                .var("currIntervals", currIntervals).var("minIntervals", minIntervals).var("answer", res)
                .array(freq, 25).step();
    }

    /** The non-zero counts by letter. */
    /** freq before sorting, each cell labelled with its task letter; the task just counted is current. */
    private static List<ArrayElement> letters(int[] freq, int current) {
        List<ArrayElement> out = new ArrayList<>(26);
        for (int c = 0; c < 26; c++) {
            out.add(new ArrayElement(c, freq[c], c == current ? "current" : "default", String.valueOf((char) ('A' + c))));
        }
        return out;
    }

    private static String counts(int[] freq) {
        StringBuilder sb = new StringBuilder("{");
        for (int c = 0; c < 26; c++) {
            if (freq[c] == 0) continue;
            if (sb.length() > 1) sb.append(", ");
            sb.append((char) ('A' + c)).append('=').append(freq[c]);
        }
        return sb.append('}').toString();
    }
}
