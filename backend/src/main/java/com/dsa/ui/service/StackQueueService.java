package com.dsa.ui.service;

import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StackQueueService implements ProblemProvider {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public StackQueueService() {
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
            case "balanced-parentheses": return generateBalancedParenthesesSteps();
            case "next-greater-element-1": return generateNextGreaterElementSteps();
            case "trapping-rainwater": return generateTrappingRainwaterSteps();
            case "largest-rectangle-histogram": return generateHistogramSteps();
            case "lru-cache": return generateLruCacheSteps();
            default: return generateBalancedParenthesesSteps();
        }
    }

    private void initProblems() {
        // 1. Balanced Parentheses
        problems.put("balanced-parentheses", new ProblemDetail(
            "balanced-parentheses", "Balanced Parentheses", "Stack & Queue - Learning", "Stack & Queue", "Easy",
            "Given a string s containing '(', ')', '{', '}', '[' and ']', determine if input string is valid using Stack.",
            """
            // Java Balanced Parentheses (LeetCode 20)
            public boolean isValid(String s) {
                Stack<Character> stack = new Stack<>();
                for (char c : s.toCharArray()) {
                    if (c == '(' || c == '{' || c == '[') stack.push(c);
                    else {
                        if (stack.isEmpty()) return false;
                        char top = stack.pop();
                        if ((c == ')' && top != '(') || (c == '}' && top != '{') || (c == ']' && top != '[')) return false;
                    }
                }
                return stack.isEmpty();
            }
            """,
            null, null, null, createArrayState(new int[]{1, 2, 2, 1}, -1, -1), null, null, null,
            new ComplexityDetail("O(N)", "Time Complexity: Single pass string traversal.", "Stack Operations", "O(N)", "Space Complexity: Stack memory depth bounded by string length N.", "Stack Memory", "Auxiliary Space: O(N)", "Memory"), "Stack"
        ));

        // 2. Next Greater Element I
        problems.put("next-greater-element-1", new ProblemDetail(
            "next-greater-element-1", "Next Greater Element I", "Stack & Queue - Monotonic", "Stack & Queue", "Easy",
            "Find the next greater element for each array element using Monotonic Decreasing Stack.",
            """
            // Java Next Greater Element (LeetCode 496)
            public int[] nextGreaterElement(int[] nums) {
                int n = nums.length, nge[] = new int[n];
                Stack<Integer> st = new Stack<>();
                for (int i = n - 1; i >= 0; i--) {
                    while (!st.isEmpty() && st.peek() <= nums[i]) st.pop();
                    nge[i] = st.isEmpty() ? -1 : st.peek();
                    st.push(nums[i]);
                }
                return nge;
            }
            """,
            null, null, null, createArrayState(new int[]{4, 5, 2, 10, 8}, -1, -1), null, null, null,
            new ComplexityDetail("O(N)", "Time Complexity: Each element pushed and popped at most once.", "Monotonic Stack", "O(N)", "Space Complexity: Monotonic stack space.", "Monotonic Stack", "Auxiliary Space: O(N)", "Memory"), "Stack"
        ));

        // Bulk register remaining 28 Stack & Queue problems
        populateRemainingStackQueueProblems();
    }

    private void populateRemainingStackQueueProblems() {
        String[][] list = new String[][]{
            {"stack-array-impl", "Implement Stack Using Arrays", "Stack & Queue - Learning", "Easy", "Fixed size array stack implementation with top pointer."},
            {"queue-array-impl", "Implement Queue Using Arrays", "Stack & Queue - Learning", "Easy", "Circular array queue implementation with front and rear pointers."},
            {"stack-queue-impl", "Implement Stack Using Queue", "Stack & Queue - Learning", "Easy", "Implement LIFO stack using single/double FIFO queue."},
            {"queue-stack-impl", "Implement Queue Using Stack", "Stack & Queue - Learning", "Easy", "Implement FIFO queue using input and output stacks."},
            {"stack-ll-impl", "Implement Stack Using LinkedList", "Stack & Queue - Learning", "Easy", "Dynamic stack using singly linked list top node."},
            {"queue-ll-impl", "Implement Queue Using LinkedList", "Stack & Queue - Learning", "Easy", "Dynamic queue using head (front) and tail (rear) pointers."},
            {"min-stack", "Implement Min Stack", "Stack & Queue - Learning", "Medium", "Stack supporting push, pop, top, and getMin in O(1) time."},
            {"infix-to-postfix", "Infix to Postfix Conversion", "Stack & Queue - Conversions", "Medium", "Convert infix expression to postfix notation using operator stack."},
            {"prefix-to-infix", "Prefix to Infix Conversion", "Stack & Queue - Conversions", "Medium", "Convert prefix expression to infix notation using operand stack."},
            {"prefix-to-postfix", "Prefix to Postfix Conversion", "Stack & Queue - Conversions", "Medium", "Convert prefix expression to postfix notation using operand stack."},
            {"postfix-to-prefix", "Postfix to Prefix Conversion", "Stack & Queue - Conversions", "Medium", "Convert postfix expression to prefix notation using operand stack."},
            {"postfix-to-infix", "Postfix to Infix Conversion", "Stack & Queue - Conversions", "Medium", "Convert postfix expression to infix notation using operand stack."},
            {"infix-to-prefix", "Infix to Prefix Conversion", "Stack & Queue - Conversions", "Medium", "Convert infix expression to prefix notation using operator stack."},
            {"next-greater-element-2", "Next Greater Element II", "Stack & Queue - Monotonic", "Medium", "Next greater element in circular array using 2*N loop monotonic stack."},
            {"next-smaller-element", "Next Smaller Element", "Stack & Queue - Monotonic", "Easy", "Find next smaller element to the right using monotonic stack."},
            {"number-greater-elements-right", "Number of Greater Elements to Right", "Stack & Queue - Monotonic", "Medium", "Count greater elements to the right for Q queries."},
            {"trapping-rainwater", "Trapping Rainwater", "Stack & Queue - Monotonic", "Hard", "Calculate trapped rainwater using Two Pointers or Prefix Max arrays."},
            {"sum-subarray-minimums", "Sum of Subarray Minimums", "Stack & Queue - Monotonic", "Medium", "Sum of min(b) for all subarrays using Next/Previous Smaller Element."},
            {"asteroid-collision", "Asteroid Collision", "Stack & Queue - Monotonic", "Medium", "Simulate asteroid collisions using stack."},
            {"sum-subarray-ranges", "Sum of Subarray Ranges", "Stack & Queue - Monotonic", "Medium", "Sum of (max - min) over all subarrays using monotonic stack."},
            {"remove-k-digits", "Remove K Digits", "Stack & Queue - Monotonic", "Medium", "Remove K digits to build smallest possible integer string."},
            {"largest-rectangle-histogram", "Largest Rectangle in Histogram", "Stack & Queue - Monotonic", "Hard", "Find max area histogram rectangle using monotonic stack."},
            {"maximum-rectangles-binary-matrix", "Maximum Rectangles in Binary Matrix", "Stack & Queue - Monotonic", "Hard", "Find max 1s rectangle in 2D binary matrix using Histogram DP."},
            {"sliding-window-maximum", "Sliding Window Maximum", "Stack & Queue - Monotonic", "Hard", "Find max element in every sliding window of size K using Deque."},
            {"stock-span-problem", "Stock Span Problem", "Stack & Queue - Implementation", "Medium", "Calculate consecutive days stock price <= current using monotonic stack."},
            {"celebrity-problem", "Celebrity Problem", "Stack & Queue - Implementation", "Medium", "Find celebrity who knows nobody and everybody knows using Stack/Pointers."},
            {"lru-cache", "LRU Cache", "Stack & Queue - Implementation", "Hard", "Least Recently Used Cache using Doubly LL + HashMap in O(1) time."},
            {"lfu-cache", "LFU Cache", "Stack & Queue - Implementation", "Hard", "Least Frequently Used Cache using Frequency Map + Doubly LL in O(1) time."}
        };

        for (String[] p : list) {
            String id = p[0]; String title = p[1]; String cat = p[2]; String diff = p[3]; String desc = p[4];
            problems.put(id, new ProblemDetail(
                id, title, cat, "Stack & Queue", diff, desc,
                String.format("// Java Implementation for %s\npublic void solve() {\n    // Stack & Queue Striver A2Z Implementation\n}", title),
                null, null, null, createArrayState(new int[]{4, 2, 7, 5}, -1, -1), null, null, null,
                new ComplexityDetail("O(N)", "Time Complexity: Linear O(N) stack / queue operations.", "Stack Processing", "O(N)", "Space Complexity: Stack memory O(N).", "Memory", "Auxiliary Space: O(N)", "Memory"), "Stack"
            ));
        }
    }

    // Step Generators
    private ExecutionStep createStackStep(int stepNum, int line, String desc, List<String> stackState, List<ArrayElement> arrayState, Map<String, String> vars) {
        return new ExecutionStep(
            stepNum, line, desc,
            stackState, Map.of(), List.of(), vars,
            "Stack", null, arrayState, null, null
        );
    }

    private List<ExecutionStep> generateBalancedParenthesesSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] vals = new int[]{1, 2, 2, 1};
        int stepNum = 1;
        steps.add(createStackStep(stepNum++, 4, "Balanced Parentheses: Input string s = \"()[]{}\". Initialize empty stack.", List.of(), createArrayState(vals, -1, -1), Map.of("stack", "[]")));
        steps.add(createStackStep(stepNum++, 6, "Process '(': Push '(' onto stack. Stack: ['('].", List.of("("), createArrayState(vals, 0, -1), Map.of("top", "(")));
        steps.add(createStackStep(stepNum++, 8, "Process ')': Match top '('! Pop '('. Stack: [].", List.of(), createArrayState(vals, 1, -1), Map.of("popped", "(")));
        steps.add(createStackStep(stepNum++, 12, "Balanced Parentheses Complete! Stack is empty -> Return TRUE.", List.of(), createArrayState(vals, -1, -1), Map.of("Result", "TRUE")));
        return steps;
    }

    private List<ExecutionStep> generateNextGreaterElementSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{4, 5, 2, 10, 8};
        int n = nums.length;
        List<String> stack = new ArrayList<>();
        int stepNum = 1;
        steps.add(createStackStep(stepNum++, 4, "Next Greater Element: Scan right to left on [4, 5, 2, 10, 8]. Initialize Monotonic Decreasing Stack.", stack, createArrayState(nums, -1, -1), Map.of("nums", Arrays.toString(nums))));

        for (int i = n - 1; i >= 0; i--) {
            steps.add(createStackStep(stepNum++, 6, "i=" + i + " (nums[i]=" + nums[i] + "): Compare with stack top " + (stack.isEmpty() ? "EMPTY" : stack.get(stack.size() - 1)), new ArrayList<>(stack), createArrayState(nums, i, -1), Map.of("i", String.valueOf(i), "num", String.valueOf(nums[i]))));
            while (!stack.isEmpty() && Integer.parseInt(stack.get(stack.size() - 1)) <= nums[i]) {
                String popped = stack.remove(stack.size() - 1);
                steps.add(createStackStep(stepNum++, 7, "Pop " + popped + " <= " + nums[i] + " from stack.", new ArrayList<>(stack), createArrayState(nums, i, -1), Map.of("popped", popped)));
            }
            String ngeVal = stack.isEmpty() ? "-1" : stack.get(stack.size() - 1);
            steps.add(createStackStep(stepNum++, 8, "Next Greater Element for " + nums[i] + " = " + ngeVal, new ArrayList<>(stack), createArrayState(nums, i, -1), Map.of("NGE", ngeVal)));
            stack.add(String.valueOf(nums[i]));
            steps.add(createStackStep(stepNum++, 9, "Push " + nums[i] + " onto stack. Stack state: " + stack, new ArrayList<>(stack), createArrayState(nums, i, -1), Map.of("pushed", String.valueOf(nums[i]))));
        }
        steps.add(createStackStep(stepNum++, 12, "Next Greater Element Complete! Resulting NGE array: [5, 10, 10, -1, -1]", new ArrayList<>(stack), createArrayState(nums, -1, -1), Map.of("result", "[5, 10, 10, -1, -1]")));
        return steps;
    }

    private List<ExecutionStep> generateTrappingRainwaterSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] height = new int[]{0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1};
        int stepNum = 1;
        steps.add(createStackStep(stepNum++, 3, "Trapping Rainwater: Heights [0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1]. Compute trapped water.", List.of(), createArrayState(height, -1, -1), Map.of("water", "0")));
        steps.add(createStackStep(stepNum++, 7, "Trapped water calculation complete! Total trapped water = 6 units.", List.of(), createArrayState(height, -1, -1), Map.of("trappedWater", "6")));
        return steps;
    }

    private List<ExecutionStep> generateHistogramSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] heights = new int[]{2, 1, 5, 6, 2, 3};
        int stepNum = 1;
        steps.add(createStackStep(stepNum++, 3, "Largest Rectangle in Histogram: Heights [2, 1, 5, 6, 2, 3]. Use Monotonic Stack.", List.of(), createArrayState(heights, -1, -1), Map.of("maxArea", "0")));
        steps.add(createStackStep(stepNum++, 7, "Found maximum area rectangle at indices [2..3] (Heights 5 & 6) -> Max Area = 10 units sq.", List.of(), createArrayState(heights, 2, 3), Map.of("maxArea", "10")));
        return steps;
    }

    private List<ExecutionStep> generateLruCacheSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] keys = new int[]{1, 2, 3, 4};
        int stepNum = 1;
        steps.add(createStackStep(stepNum++, 3, "LRU Cache (Capacity = 2): Put(1,1), Put(2,2). Cache: [2=2, 1=1].", List.of("2", "1"), createArrayState(keys, 0, 1), Map.of("cache", "{2=2, 1=1}")));
        steps.add(createStackStep(stepNum++, 6, "Get(1): Cache Hit! Move key 1 to MRU head. Cache: [1=1, 2=2].", List.of("1", "2"), createArrayState(keys, 0, 1), Map.of("cache", "{1=1, 2=2}")));
        steps.add(createStackStep(stepNum++, 9, "Put(3,3): Capacity full! Evict LRU key 2. Insert 3. Cache: [3=3, 1=1].", List.of("3", "1"), createArrayState(keys, 2, 0), Map.of("evicted", "2", "cache", "{3=3, 1=1}")));
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
