package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Course Schedule (LeetCode 207), traced on the owner's own accepted submission: Kahn's algorithm. Count each course's
 * unmet prerequisites ({@code indegree}), start from the courses with none, and every time a
 * course is taken, lower the count of the courses that waited on it. O(V + E).
 *
 * <p>Input: the app's edge [u, v] means "u must come before v", which is LeetCode's
 * prerequisite pair [v, u]; the tracer hands the code {@code prerequisites} in that form. The answer is whether every course can be taken.
 */
@Component
public class CourseSchedule1Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "course-schedule-1";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Prerequisites")
                        .help("Vertex count plus directed edges [prerequisite, course].")
                        .directed()
                        .constraint("maxVertices", 20)
                        .constraint("maxEdges", 48)
                        .defaultValue(Map.of(
                                "vertices", 4,
                                "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(1, 3), List.of(2, 3))))
                        .build());
    }

    /** A 3-course prerequisite cycle - none of the three can ever be scheduled first. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 3,
                "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(2, 0))));
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public boolean canFinish(int numCourses, int[][] prerequisites) {
                       // @a init
                       int V = numCourses;
                       List<List<Integer>> adjList = new ArrayList<>();
                       int[] indegree = new int[V];
                       Queue<Integer> q = new LinkedList<>();

                       for (int i=0;i<V;i++)
                           adjList.add(new ArrayList<>());

                       for (int[] edge: prerequisites) {
                           int a = edge[0];
                           int b = edge[1];

                           adjList.get(b).add(a);
                           indegree[a]++;
                       }

                       // @a seed
                       for (int i=0;i<V;i++)
                           if (indegree[i] == 0)
                               q.add(i);

                       while (!q.isEmpty()) {
                           // @a poll
                           int curr = q.poll();
                           for (int ngbr: adjList.get(curr)) {
                               // @a decrement
                               indegree[ngbr]--;

                               if (indegree[ngbr] == 0)
                                   // @a enqueue
                                   q.add(ngbr);
                           }
                       }

                       for (int i=0;i<V;i++)
                           if (indegree[i] > 0)
                               // @a blocked
                               return false;

                       // @a done
                       return true;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int V = graph.vertices();
        // The app's edge [u, v] (u before v) is LeetCode's prerequisite pair [v, u].
        int[][] prerequisites = new int[graph.edges().length][];
        for (int e = 0; e < prerequisites.length; e++) {
            prerequisites[e] = new int[]{graph.edges()[e][1], graph.edges()[e][0]};
        }
        List<List<Integer>> adjList = new ArrayList<>();
        int[] indegree = new int[V];
        Deque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < V; i++) {
            adjList.add(new ArrayList<>());
        }
        for (int[] edge : prerequisites) {
            int a = edge[0];
            int b = edge[1];
            adjList.get(b).add(a);
            indegree[a]++;
        }

        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < V; i++) {
            states.put(i, "unvisited");
        }
        emit.at("init").say("%d courses and %d prerequisite pair%s. An arrow b -> a means b must be taken "
                        + "before a. indegree[a] counts a's unmet prerequisites: %s.",
                        V, prerequisites.length, Narration.s(prerequisites.length), Arrays.toString(indegree))
                .var("indegree", Arrays.toString(indegree))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        for (int i = 0; i < V; i++) {
            if (indegree[i] == 0) {
                q.add(i);
                states.put(i, "queued");
            }
        }
        emit.at("seed").say(q.isEmpty()
                        ? "No course has 0 unmet prerequisites, so nothing can be taken first."
                        : "Courses with no unmet prerequisites can be taken now: queue " + q + ".")
                .var("indegree", Arrays.toString(indegree)).var("q", q.toString())
                .graph(layout.nodes(), layout.edges()).nodes(states).queue(q).step();

        while (!q.isEmpty()) {
            int curr = q.poll();
            states.put(curr, "done");
            emit.at("poll").say("Take course %d, then lower the count of each course that waits on it.", curr)
                    .var("curr", curr).var("indegree", Arrays.toString(indegree))
                    .graph(layout.nodes(), layout.edges()).nodes(states).queue(q).step();

            for (int ngbr : adjList.get(curr)) {
                indegree[ngbr]--;
                emit.at("decrement").say("%d -> %d: course %d now has %d unmet prerequisite%s.",
                                curr, ngbr, ngbr, indegree[ngbr], Narration.s(indegree[ngbr]))
                        .var("curr", curr).var("ngbr", ngbr).var("indegree", Arrays.toString(indegree))
                        .graph(layout.nodes(), layout.edges()).nodes(states)
                        .edges(List.of(curr + "-" + ngbr)).queue(q).step();
                if (indegree[ngbr] == 0) {
                    q.add(ngbr);
                    states.put(ngbr, "queued");
                    emit.at("enqueue").say("Course %d has no unmet prerequisites left - queue it.", ngbr)
                            .var("ngbr", ngbr).var("q", q.toString())
                            .graph(layout.nodes(), layout.edges()).nodes(states).queue(q).step();
                }
            }
        }

        for (int i = 0; i < V; i++) {
            if (indegree[i] > 0) {
                for (int j = 0; j < V; j++) {
                    if (indegree[j] > 0) states.put(j, "cycle");
                }
                emit.at("blocked").say("The queue is empty but course %d still has %d unmet prerequisite%s: "
                                + "the outlined courses wait on each other in a cycle, so they can never be "
                                + "taken. Return %s.", i, indegree[i], Narration.s(indegree[i]), "false")
                        .var("indegree", Arrays.toString(indegree)).var("answer", false)
                        .graph(layout.nodes(), layout.edges()).nodes(states).step();
                return;
            }
        }
        emit.at("done").say("Every course reached 0 unmet prerequisites and was taken. Return true.")
                .var("indegree", Arrays.toString(indegree)).var("answer", true)
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
    }
}
