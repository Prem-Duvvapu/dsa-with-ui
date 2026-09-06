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
            // trapping-rainwater, largest-rectangle-histogram, next-greater-element-2,
            // and asteroid-collision have real tracers (tracer/impl). Refuse rather than
            // let default: serve the placeholder steps under these ids.
            case "trapping-rainwater":
            case "largest-rectangle-histogram":
            case "next-greater-element-2":
            case "asteroid-collision":
                throw new LegacyTraceRetiredException(problemId);
            // sliding-window-maximum, min-stack, and sum-subarray-minimums have real
            // tracers (tracer/impl) now. They never had their own case at all - just
            // default: serving balanced-parentheses's steps. Refuse rather than keep
            // that silent.
            case "sliding-window-maximum":
            case "min-stack":
            case "sum-subarray-minimums":
                throw new LegacyTraceRetiredException(problemId);
            // lru-cache has a real tracer (tracer/impl) now, tracing the whole put/get
            // sequence as one FieldType.STRING mini-language input. Refuse rather than
            // let default: serve the placeholder steps under this id.
            case "lru-cache":
                throw new LegacyTraceRetiredException(problemId);
            // The Learning cluster is traced (tracer/impl). balanced-parentheses and
            // next-greater-element-1 are the only two that ever had real generators here;
            // both are deleted, and the other six were on default: all along.
            case "balanced-parentheses":
            case "next-greater-element-1":
            case "stack-array-impl":
            case "queue-array-impl":
            case "stack-queue-impl":
            case "queue-stack-impl":
            case "stack-ll-impl":
            case "queue-ll-impl":
                throw new LegacyTraceRetiredException(problemId);
            // All six Conversions ids are traced (tracer/impl). None of them ever had a
            // generator here either - every one was on default:.
            case "infix-to-postfix":
            case "infix-to-prefix":
            case "prefix-to-infix":
            case "prefix-to-postfix":
            case "postfix-to-prefix":
            case "postfix-to-infix":
                throw new LegacyTraceRetiredException(problemId);
            default: return generatePlaceholderStackSteps();
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
                new ComplexityDetail("O(N)", "Time Complexity: Linear O(N) stack / queue operations.", "Stack Processing", "O(N)", "Space Complexity: Stack memory O(N).", "Memory", "Auxiliary Space: O(N)", "Memory"), bulkDsType(id).wireValue()
            ));
        }
    }

    /**
     * Most bulk-registered problems are genuinely Stack-shaped, but several are not, and the
     * canvas has to follow the structure the algorithm actually operates on:
     *
     * <ul>
     *   <li>lru-cache's eviction order IS a doubly linked list (most- to least-recently-used),
     *       not a stack.</li>
     *   <li>queue-array-impl is a circular buffer read front-to-back — a queue.</li>
     *   <li>stack-queue-impl builds a stack out of ONE queue, and the rotation that keeps the
     *       queue's front equal to the stack's top is the thing worth watching.</li>
     *   <li>queue-stack-impl is the mirror: two stacks whose combined contents are one
     *       logical queue, emitted front-to-back with each element labelled by its home stack.</li>
     *   <li>stack-ll-impl and queue-ll-impl are chains of nodes; the pointer work is the
     *       lesson, and the array versions already cover the pile-of-slots picture.</li>
     * </ul>
     *
     * <p>This must agree with each tracer's own {@code dsType()} —
     * {@code CatalogTracerMetadataTest} fails application-wide if the catalogue routes a
     * traced problem to a canvas its tracer never emits.
     */
    private static DsType bulkDsType(String id) {
        return switch (id) {
            case "lru-cache", "stack-ll-impl", "queue-ll-impl" -> DsType.LINKED_LIST;
            case "queue-array-impl", "stack-queue-impl", "queue-stack-impl" -> DsType.QUEUE;
            default -> DsType.STACK;
        };
    }

    // Step Generators
    private ExecutionStep createStackStep(int stepNum, int line, String desc, List<String> stackState, List<ArrayElement> arrayState, Map<String, String> vars) {
        return new ExecutionStep(
            stepNum, line, desc,
            stackState, Map.of(), List.of(), vars,
            "Stack", null, arrayState, null, null
        );
    }

    /**
     * The legacy {@code default:} branch, for the Stack &amp; Queue ids that still have no
     * tracer. It used to be {@code generateBalancedParenthesesSteps()}, which is exactly the
     * substitution this migration exists to remove: every untraced id in this service served
     * balanced-parentheses's animation under its own name. balanced-parentheses now has a
     * real tracer, so the content stays only as an unattributed placeholder until the
     * remaining ids are migrated and this method, with {@code default:}, is deleted.
     */
    private List<ExecutionStep> generatePlaceholderStackSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] vals = new int[]{1, 2, 2, 1};
        int stepNum = 1;
        steps.add(createStackStep(stepNum++, 4, "Not yet traced. Placeholder stack narration; see /api/problems for the migrated animations.", List.of(), createArrayState(vals, -1, -1), Map.of("stack", "[]")));
        steps.add(createStackStep(stepNum++, 6, "Push onto the stack. Stack: ['('].", List.of("("), createArrayState(vals, 0, -1), Map.of("top", "(")));
        steps.add(createStackStep(stepNum++, 8, "Pop the top. Stack: [].", List.of(), createArrayState(vals, 1, -1), Map.of("popped", "(")));
        steps.add(createStackStep(stepNum++, 12, "Placeholder complete.", List.of(), createArrayState(vals, -1, -1), Map.of("Result", "TRUE")));
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
