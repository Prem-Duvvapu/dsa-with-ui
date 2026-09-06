package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Course Schedule I asks only whether every course is finishable - exactly whether the
 * prerequisite graph is a DAG. Kahn's BFS answers this for free: it can only process a
 * course once every prerequisite is processed, so processed == numCourses iff no cycle
 * blocks progress forever.
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
               public boolean canFinish(int numCourses, int[][] prerequisites) {
                   // @a indegree
                   int[] indegree = new int[numCourses];
                   for (int[] p : prerequisites) indegree[p[1]]++;

                   // @a seed
                   Queue<Integer> queue = new LinkedList<>();
                   for (int i = 0; i < numCourses; i++) {
                       if (indegree[i] == 0) queue.add(i);
                   }

                   int processed = 0;
                   while (!queue.isEmpty()) {
                       // @a poll
                       int course = queue.poll();
                       processed++;
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
                   // @a verdict
                   return processed == numCourses;
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
                .say("%d courses, %d prerequisite links. Indegree (unmet prerequisites) of each: %s.",
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
                .say("Every course with 0 unmet prerequisites can be scheduled now: %s.", queue)
                .var("queue", queue.toString())
                .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();

        int processed = 0;
        while (!queue.isEmpty()) {
            int course = queue.poll();
            processed++;
            states.put(course, "visited");
            emit.at("poll")
                    .say("Schedule course %d (%d of %d courses scheduled so far).", course, processed, v)
                    .var("course", course).var("processed", processed)
                    .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();

            for (int next : adj.get(course)) {
                indegree[next]--;
                emit.at("decrement")
                        .say("%d -> %d: course %d has %d unmet prerequisite(s) left.", course, next, next, indegree[next])
                        .var("course", course).var("unlocked", next).var("indegreeLeft", indegree[next])
                        .graph(layout.nodes(), layout.edges()).nodes(states)
                        .edges(List.of(course + "-" + next)).queue(queue).step();

                if (indegree[next] == 0) {
                    queue.add(next);
                    states.put(next, "queued");
                    emit.at("enqueue")
                            .say("Course %d has every prerequisite met - it can be scheduled.", next)
                            .var("unlocked", next)
                            .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();
                }
            }
        }

        boolean canFinish = processed == v;
        emit.at("verdict")
                .say(canFinish
                        ? "All %d of %d courses were scheduled. canFinish = true."
                        : "Only %d of %d courses were ever schedulable - a prerequisite cycle blocks the rest. canFinish = false.",
                        processed, v)
                .var("processed", processed).var("answer", canFinish)
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
    }
}
