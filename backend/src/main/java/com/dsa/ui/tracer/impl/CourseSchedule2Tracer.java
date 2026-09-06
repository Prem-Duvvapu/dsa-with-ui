package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Course Schedule II wants the actual order, not just a yes/no - the same Kahn's BFS as
 * Course Schedule I, but recording every dequeued course instead of only counting it. A
 * cycle still shows up the same way: fewer than numCourses entries end up in the order, so
 * the result is the empty array rather than a partial, misleading schedule.
 */
@Component
public class CourseSchedule2Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "course-schedule-2";
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
                                "edges", List.of(List.of(0, 1), List.of(0, 2), List.of(1, 3), List.of(2, 3))))
                        .build());
    }

    /** A direct mutual dependency - two courses that each require the other. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 3,
                "edges", List.of(List.of(0, 1), List.of(1, 0))));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] findOrder(int numCourses, int[][] prerequisites) {
                   // @a indegree
                   int[] indegree = new int[numCourses];
                   for (int[] p : prerequisites) indegree[p[1]]++;

                   // @a seed
                   Queue<Integer> queue = new LinkedList<>();
                   for (int i = 0; i < numCourses; i++) {
                       if (indegree[i] == 0) queue.add(i);
                   }

                   int[] order = new int[numCourses];
                   int idx = 0;
                   while (!queue.isEmpty()) {
                       // @a poll
                       int course = queue.poll();
                       order[idx++] = course;
                       for (int[] p : prerequisites) {
                           if (p[0] == course) {
                               // @a decrement
                               indegree[p[1]]--;
                               if (indegree[p[1]] == 0) {
                                   // @a enqueue
                                   queue.add(p[1]);
                               }
                           }
                       }
                   }
                   // @a done
                   return idx == numCourses ? order : new int[0];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency(true);
        int v = graph.vertices();
        int[] indegree = new int[v];
        for (int node = 0; node < v; node++) {
            for (int next : adj.get(node)) indegree[next]++;
        }

        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) states.put(i, "unvisited");

        emit.at("indegree")
                .say("%d courses, %d prerequisite links. Indegree of each: %s.",
                        v, graph.edges().length, Arrays.toString(indegree))
                .var("indegree", Arrays.toString(indegree))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < v; i++) {
            if (indegree[i] == 0) {
                queue.add(i);
                states.put(i, "queued");
            }
        }
        emit.at("seed")
                .say("Courses with no prerequisites seed the queue: %s.", queue)
                .var("queue", queue.toString())
                .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();

        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int course = queue.poll();
            order.add(course);
            states.put(course, "visited");
            emit.at("poll")
                    .say("Dequeue course %d and place it next in the schedule: %s.", course, order)
                    .var("course", course).var("order", order.toString())
                    .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();

            for (int next : adj.get(course)) {
                indegree[next]--;
                emit.at("decrement")
                        .say("%d -> %d: course %d's remaining prerequisite count drops to %d.", course, next, next, indegree[next])
                        .var("course", course).var("unlocked", next).var("indegreeLeft", indegree[next])
                        .graph(layout.nodes(), layout.edges()).nodes(states)
                        .edges(List.of(course + "-" + next)).queue(queue).step();

                if (indegree[next] == 0) {
                    queue.add(next);
                    states.put(next, "queued");
                    emit.at("enqueue")
                            .say("Course %d has no prerequisite left - enqueue it.", next)
                            .var("unlocked", next)
                            .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();
                }
            }
        }

        boolean complete = order.size() == v;
        String result = complete ? order.toString() : "[] (cycle - no valid order)";
        emit.at("done")
                .say(complete
                        ? String.format("Schedule complete: %s.", order)
                        : String.format("Only %d of %d courses were ever schedulable - a cycle blocks the rest. No valid order.",
                                order.size(), v))
                .var("order", result)
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
    }
}
