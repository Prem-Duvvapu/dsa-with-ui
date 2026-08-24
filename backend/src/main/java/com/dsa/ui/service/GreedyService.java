package com.dsa.ui.service;

import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import com.dsa.ui.trace.ListTraceRecorder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GreedyService implements ProblemProvider {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public GreedyService() {
        initProblems();
    }

    public List<ProblemDetail> getAllProblems() {
        return new ArrayList<>(problems.values());
    }

    public ProblemDetail getProblemById(String id) {
        return problems.get(id);
    }

    public List<ExecutionStep> generateSteps(String problemId) {
        switch (problemId) {
            // n-meetings-in-one-room has a real tracer (tracer/impl). Refuse rather than
            // let default: serve another meeting set's steps under this id. The default:
            // stays until PROMPT D; other ids in this service still rely on it.
            case "n-meetings-in-one-room":
                throw new LegacyTraceRetiredException(problemId);
            case "jump-game-i":
            case "jump-game-1": return generateJumpGameSteps();
            case "job-sequencing": return generateJobSequencingSteps();
            case "assign-cookies": return generateJumpGameSteps();
            case "fractional-knapsack": return generateJumpGameSteps();
            case "lemonade-change": return generateJumpGameSteps();
            case "valid-parentheses-checker": return generateJumpGameSteps();
            case "jump-game-2": return generateJumpGameSteps();
            case "minimum-platforms": return generateJumpGameSteps();
            case "candy": return generateJumpGameSteps();
            case "shortest-job-first": return generateJumpGameSteps();
            case "lru-page-replacement": return generateJumpGameSteps();
            case "insert-interval": return generateJumpGameSteps();
            case "merge-intervals": return generateJumpGameSteps();
            case "non-overlapping-intervals": return generateJumpGameSteps();
            default: return generateMeetingsSteps();
        }
    }

    private void initProblems() {
        // 1. N Meetings in One Room
        problems.put("n-meetings-in-one-room", new ProblemDetail(
            "n-meetings-in-one-room", "N Meetings in One Room", "Greedy - Activity Selection", "Greedy Algorithms", "Medium",
            "Find maximum number of meetings accommodated in a single meeting room.",
            """
            // Java N Meetings in One Room (Striver A2Z Sheet)
            public int maxMeetings(int start[], int end[], int n) {
                ArrayList<Meeting> meet = new ArrayList<>();
                for (int i = 0; i < n; i++) meet.add(new Meeting(start[i], end[i], i + 1));
                Collections.sort(meet, (a, b) -> a.end - b.end);
                int count = 1, limit = meet.get(0).end;
                for (int i = 1; i < n; i++) {
                    if (meet.get(i).start > limit) {
                        limit = meet.get(i).end; count++;
                    }
                }
                return count;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 3, 0, 5, 8, 5}, -1, -1), null, null, null,
            new ComplexityDetail("O(N log N)", "Time Complexity: Sorting meetings by end time.", "Activity Selection", "O(N)", "Space Complexity: Meeting list space.", "Memory", "Auxiliary Space: O(N)", "Memory"), "Array"
        ));

        // 2. Jump Game I
        problems.put("jump-game-1", new ProblemDetail(
            "jump-game-1", "Jump Game I", "Greedy - Array Jumps", "Greedy Algorithms", "Medium",
            "Determine if you can reach the last index from index 0.",
            """
            // Java Jump Game I (LeetCode 55)
            public boolean canJump(int[] nums) {
                int maxReach = 0;
                for (int i = 0; i < nums.length; i++) {
                    if (i > maxReach) return false;
                    maxReach = Math.max(maxReach, i + nums[i]);
                }
                return true;
            }
            """,
            null, null, null, createArrayState(new int[]{2, 3, 1, 1, 4}, -1, -1), null, null, null,
            new ComplexityDetail("O(N)", "Time Complexity: Single pass maxReach tracking.", "Greedy Max Reach", "O(1)", "Space Complexity: Single primitive variable maxReach.", "Memory", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 3. Job Sequencing
        problems.put("job-sequencing", new ProblemDetail(
            "job-sequencing", "Job Sequencing Problem", "Greedy - Scheduling", "Greedy Algorithms", "Medium",
            "Find maximum profit and count of jobs done given deadlines and profits.",
            """
            // Java Job Sequencing (Striver A2Z Sheet)
            public int[] JobScheduling(Job arr[], int n) {
                Arrays.sort(arr, (a, b) -> (b.profit - a.profit));
                int result[] = new int[maxDeadline + 1];
                Arrays.fill(result, -1);
                int countJobs = 0, jobProfit = 0;
                for (int i = 0; i < n; i++) {
                    for (int j = arr[i].deadline; j > 0; j--) {
                        if (result[j] == -1) {
                            result[j] = arr[i].id; countJobs++; jobProfit += arr[i].profit; break;
                        }
                    }
                }
                return new int[]{countJobs, jobProfit};
            }
            """,
            null, null, null, createArrayState(new int[]{100, 50, 40, 20}, -1, -1), null, null, null,
            new ComplexityDetail("O(N log N + N x maxDeadline)", "Time Complexity: Profit sort + deadline slot assignment.", "Job Scheduling", "O(maxDeadline)", "Space Complexity: Slot array.", "Memory", "Auxiliary Space: O(maxDeadline)", "Memory"), "Array"
        ));

        // Bulk register remaining 12 Greedy problems
        populateRemainingGreedyProblems();
    }

    private void populateRemainingGreedyProblems() {
        String[][] list = new String[][]{
            {"assign-cookies", "Assign Cookies", "Greedy - Easy", "Easy", "Maximize satisfied children with greed factor g and cookie size s."},
            {"fractional-knapsack", "Fractional Knapsack Problem", "Greedy - Easy", "Medium", "Maximize total knapsack value by picking items fractionally by value/weight ratio."},
            {"lemonade-change", "Lemonade Change", "Greedy - Easy", "Easy", "Provide $5/$10 change for $5/$10/$20 bills using greedy bill count."},
            {"valid-parentheses-checker", "Valid Parenthesis String", "Greedy - Easy", "Medium", "Check valid string containing '(', ')', and '*' using min/max open count range."},
            {"jump-game-2", "Jump Game II", "Greedy - Array Jumps", "Medium", "Find minimum number of jumps to reach last index."},
            {"minimum-platforms", "Minimum Platforms Required for Railway", "Greedy - Scheduling", "Medium", "Find minimum railway platforms needed using arrival/departure sorting."},
            {"candy", "Candy Distribution", "Greedy - Array Jumps", "Hard", "Distribute minimum candies to children such that higher ratings get more than neighbors."},
            {"shortest-job-first", "Shortest Job First (SJF) Scheduling", "Greedy - Scheduling", "Medium", "Calculate average waiting time for CPU tasks using SJF scheduling."},
            {"lru-page-replacement", "LRU Page Replacement Algorithm", "Greedy - Cache", "Easy", "Calculate total page faults using LRU page replacement."},
            {"insert-interval", "Insert Interval", "Greedy - Intervals", "Medium", "Insert newInterval into sorted non-overlapping intervals array."},
            {"merge-intervals", "Merge Overlapping Intervals", "Greedy - Intervals", "Medium", "Merge all overlapping intervals into non-overlapping range list."},
            {"non-overlapping-intervals", "Non-overlapping Intervals", "Greedy - Intervals", "Medium", "Find minimum number of intervals to remove to make remaining non-overlapping."}
        };

        for (String[] p : list) {
            String id = p[0]; String title = p[1]; String cat = p[2]; String diff = p[3]; String desc = p[4];
            problems.put(id, new ProblemDetail(
                id, title, cat, "Greedy Algorithms", diff, desc,
                String.format("// Java Implementation for %s\npublic int solve() {\n    // Greedy Striver A2Z Implementation\n    return 0;\n}", title),
                null, null, null, createArrayState(new int[]{1, 2, 3, 4}, -1, -1), null, null, null,
                new ComplexityDetail("O(N log N)", "Time Complexity: Greedy sorting or linear pass.", "Greedy Strategy", "O(1)", "Space Complexity: Constant memory.", "Memory", "Auxiliary Space: O(1)", "Memory"), "Array"
            ));
        }
    }

    // Step Generators
    private ExecutionStep createStep(int stepNum, int line, String desc, List<ArrayElement> arrayState, Map<String, String> vars) {
        return new ExecutionStep(
            stepNum, line, desc,
            List.of(), Map.of(), List.of(), vars,
            "Array", null, arrayState, null, null
        );
    }

    private List<ExecutionStep> generateMeetingsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] start = new int[]{1, 3, 0, 5, 8, 5};
        int[] end = new int[]{2, 4, 6, 7, 9, 9};
        int stepNum = 1;

        steps.add(createStep(stepNum++, 4, "N Meetings: Start = [1,3,0,5,8,5], End = [2,4,6,7,9,9]. Sort by end time.", createArrayState(end, -1, -1), Map.of("count", "0")));
        steps.add(createStep(stepNum++, 8, "Select Meeting 1: [1..2]. End time limit = 2. Total meetings = 1.", createArrayState(end, 0, -1), Map.of("count", "1", "limit", "2")));
        steps.add(createStep(stepNum++, 11, "Select Meeting 2: [3..4] (3 > 2). End time limit = 4. Total meetings = 2.", createArrayState(end, 1, -1), Map.of("count", "2", "limit", "4")));
        steps.add(createStep(stepNum++, 11, "Select Meeting 4: [5..7] (5 > 4). End time limit = 7. Total meetings = 3.", createArrayState(end, 3, -1), Map.of("count", "3", "limit", "7")));
        steps.add(createStep(stepNum++, 11, "Select Meeting 5: [8..9] (8 > 7). End time limit = 9. Total meetings = 4.", createArrayState(end, 4, -1), Map.of("count", "4", "limit", "9")));
        steps.add(createStep(stepNum++, 15, "N Meetings Complete! Maximum non-overlapping meetings = 4.", createArrayState(end, -1, -1), Map.of("maxMeetings", "4")));
        return steps;
    }

    private List<ExecutionStep> generateJumpGameSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{2, 3, 1, 1, 4};
        int maxReach = 0;
        int stepNum = 1;

        steps.add(createStep(stepNum++, 3, "Jump Game I: nums = [2, 3, 1, 1, 4]. Initialize maxReach = 0.", createArrayState(nums, -1, -1), Map.of("maxReach", "0")));

        for (int i = 0; i < nums.length; i++) {
            maxReach = Math.max(maxReach, i + nums[i]);
            steps.add(createStep(stepNum++, 6, "i=" + i + " (val " + nums[i] + "): Update maxReach = max(" + maxReach + ", " + i + "+" + nums[i] + ") = " + maxReach, createArrayState(nums, i, -1), Map.of("i", String.valueOf(i), "maxReach", String.valueOf(maxReach))));
            if (maxReach >= nums.length - 1) {
                steps.add(createStep(stepNum++, 8, "maxReach (" + maxReach + ") >= last index (" + (nums.length - 1) + ")! Can reach target!", createArrayState(nums, i, nums.length - 1), Map.of("canJump", "true")));
                return steps;
            }
        }
        steps.add(createStep(stepNum++, 10, "Jump Game Complete! Return TRUE.", createArrayState(nums, -1, -1), Map.of("canJump", "true")));
        return steps;
    }

    private List<ExecutionStep> generateJobSequencingSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] profit = new int[]{100, 50, 40, 20};
        int stepNum = 1;

        steps.add(createStep(stepNum++, 4, "Job Sequencing: Profits = [100, 50, 40, 20]. Sort jobs by profit descending.", createArrayState(profit, -1, -1), Map.of("totalProfit", "0")));
        steps.add(createStep(stepNum++, 8, "Schedule Job 1 (Profit 100) at deadline slot 2 -> Total Profit = 100", createArrayState(profit, 0, -1), Map.of("totalProfit", "100")));
        steps.add(createStep(stepNum++, 11, "Schedule Job 2 (Profit 50) at deadline slot 1 -> Total Profit = 150", createArrayState(profit, 1, -1), Map.of("totalProfit", "150")));
        steps.add(createStep(stepNum++, 14, "Job Sequencing Complete! Max Profit = 150 across 2 scheduled jobs.", createArrayState(profit, -1, -1), Map.of("maxProfit", "150")));
        return steps;
    }

    private List<ArrayElement> createArrayState(int[] vals, int idx1, int idx2) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            String st = (i == idx1 || i == idx2) ? "active" : "default";
            list.add(new ArrayElement(i, vals[i], st));
        }
        return list;
    }
}
