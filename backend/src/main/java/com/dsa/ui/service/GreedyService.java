package com.dsa.ui.service;

import com.dsa.ui.algorithm.greedy.*;
import com.dsa.ui.model.*;
import com.dsa.ui.trace.ListTraceRecorder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GreedyService {

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
            case "n-meetings-in-one-room": return generateMeetingsSteps();
            case "jump-game-i": return generateJumpGameSteps();
            case "job-sequencing": return generateJobSequencingSteps();
            default: return generateMeetingsSteps();
        }
    }

    private void initProblems() {
        // 1. N Meetings in One Room
        problems.put("n-meetings-in-one-room", new ProblemDetail(
            "n-meetings-in-one-room", "N Meetings in One Room", "Greedy - Activity Selection", "Greedy Algorithms", "Medium",
            "Find maximum number of meetings that can be accommodated in a single meeting room.",
            """
            // Java N Meetings in One Room (Striver A2Z Sheet)
            class Meeting {
                int start, end, pos;
                Meeting(int start, int end, int pos) {
                    this.start = start; this.end = end; this.pos = pos;
                }
            }

            public int maxMeetings(int start[], int end[], int n) {
                ArrayList<Meeting> meet = new ArrayList<>();
                for (int i = 0; i < n; i++) meet.add(new Meeting(start[i], end[i], i + 1));

                // Sort meetings by end time
                Collections.sort(meet, (a, b) -> a.end - b.end);

                int count = 1;
                int limit = meet.get(0).end;

                for (int i = 1; i < n; i++) {
                    if (meet.get(i).start > limit) {
                        limit = meet.get(i).end;
                        count++;
                    }
                }
                return count;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 3, 0, 5, 8, 5}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N log N)",
                "Time Complexity: Sorting N meetings by end time takes O(N log N) time + O(N) linear pass.",
                "Why sort by end time? Finishing meetings as early as possible frees up maximum remaining time for subsequent meetings.",
                "O(N)",
                "Space Complexity: O(N) to store meeting objects.",
                "Why O(N)? Holds start time, end time, and original meeting index.",
                "Auxiliary Space: O(N)",
                "Return Count: O(1)"
            ),
            "Array"
        ));

        // 2. Jump Game I
        problems.put("jump-game-i", new ProblemDetail(
            "jump-game-i", "Jump Game I", "Greedy - Array Jumps", "Greedy Algorithms", "Medium",
            "You are given an integer array nums. You are initially positioned at the array's first index. Determine if you can reach last index.",
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
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single pass loop through array of size N.",
                "Why Greedy maxReach works? Maintains furthest reachable index seen so far. If current index i > maxReach, stuck!",
                "O(1)",
                "Space Complexity: Constant O(1) space.",
                "Why O(1)? Tracks only primitive variable `maxReach`.",
                "Auxiliary Space: O(1)",
                "Return Boolean: O(1)"
            ),
            "Array"
        ));

        // 3. Job Sequencing Problem
        problems.put("job-sequencing", new ProblemDetail(
            "job-sequencing", "Job Sequencing Problem", "Greedy - Scheduling", "Greedy Algorithms", "Medium",
            "Given a set of N jobs where each job has a deadline and profit, find maximum profit and count of jobs done.",
            """
            // Java Job Sequencing Problem (Striver A2Z Sheet)
            public int[] JobScheduling(Job arr[], int n) {
                Arrays.sort(arr, (a, b) -> (b.profit - a.profit)); // Sort by profit descending

                int maxDeadline = 0;
                for (int i = 0; i < n; i++) maxDeadline = Math.max(maxDeadline, arr[i].deadline);

                int result[] = new int[maxDeadline + 1];
                Arrays.fill(result, -1);

                int countJobs = 0, jobProfit = 0;
                for (int i = 0; i < n; i++) {
                    for (int j = arr[i].deadline; j > 0; j--) {
                        if (result[j] == -1) {
                            result[j] = arr[i].id;
                            countJobs++;
                            jobProfit += arr[i].profit;
                            break;
                        }
                    }
                }
                return new int[]{countJobs, jobProfit};
            }
            """,
            null, null, null, createArrayState(new int[]{100, 50, 40, 20}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N log N + N x maxDeadline)",
                "Time Complexity: Sorting by profit O(N log N) + inner loop matching available deadline slots.",
                "Why pick max deadline slot? Scheduling high-profit job on its latest available slot leaves earlier slots open for other jobs.",
                "O(maxDeadline)",
                "Space Complexity: Time slot array result[maxDeadline + 1].",
                "Why O(maxDeadline)? Tracks occupied time slots from day 1 to maxDeadline.",
                "Auxiliary Space: O(maxDeadline)",
                "Profit Output: O(1)"
            ),
            "Array"
        ));
    }

    // Step Generators
    private List<ExecutionStep> generateMeetingsSteps() {
        int[] start = {1, 3, 0, 5, 8, 5};
        int[] end = {2, 4, 6, 7, 9, 9};
        ListTraceRecorder recorder = new ListTraceRecorder();
        new NMeetingsInOneRoom().solve(start, end, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generateJumpGameSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{2, 3, 1, 1, 4};
        int maxReach = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Jump Game I: nums = [2, 3, 1, 1, 4]. Target: Reach last index 4. Initialize maxReach = 0.",
            List.of(), Map.of(), List.of(), Map.of("maxReach", "0"),
            "Array", null, createArrayState(nums, 0, -1), null, null
        ));

        for (int i = 0; i < nums.length; i++) {
            maxReach = Math.max(maxReach, i + nums[i]);
            steps.add(new ExecutionStep(
                stepNum++, 7,
                String.format("Loop i = %d (jump val %d): i (%d) <= maxReach. Update maxReach = max(%d, %d + %d) = %d.", i, nums[i], i, maxReach, i, nums[i], maxReach),
                List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "maxReach", String.valueOf(maxReach)),
                "Array", null, createArrayState(nums, i, maxReach < nums.length ? maxReach : nums.length - 1), null, null
            ));
        }

        steps.add(new ExecutionStep(
            stepNum++, 9,
            "Jump Game Complete! maxReach (6) >= last index (4). Return TRUE!",
            List.of(), Map.of(), List.of(), Map.of("Result", "TRUE"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateJobSequencingSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] profits = new int[]{100, 50, 40, 20};
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Job Sequencing: Sort jobs by profit descending -> [Job1(p:100, d:2), Job2(p:50, d:1), Job3(p:40, d:2), Job4(p:20, d:1)].",
            List.of(), Map.of(), List.of(), Map.of("maxDeadline", "2"),
            "Array", null, createArrayState(profits, -1, -1), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 14,
            "Schedule Job1 (profit 100, deadline 2): Occupy slot 2. Total profit = 100, Jobs = 1.",
            List.of(), Map.of(), List.of(), Map.of("slot 2", "Job1", "profit", "100"),
            "Array", null, createArrayState(profits, 0, -1), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 14,
            "Schedule Job2 (profit 50, deadline 1): Occupy slot 1. Total profit = 150, Jobs = 2.",
            List.of(), Map.of(), List.of(), Map.of("slot 1", "Job2", "profit", "150"),
            "Array", null, createArrayState(profits, 1, -1), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 21,
            "Job Sequencing Complete! Max Profit = 150 across 2 jobs.",
            List.of(), Map.of(), List.of(), Map.of("Max Profit", "150", "Count Jobs", "2"),
            "Array", null, createArrayState(profits, -1, -1), null, null
        ));

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
