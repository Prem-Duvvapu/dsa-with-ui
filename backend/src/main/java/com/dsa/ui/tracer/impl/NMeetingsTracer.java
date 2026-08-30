package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * N meetings in one room — the canonical greedy exchange: give up freedom of order to
 * buy the proof. Sorting by END time means every selection is provably safe, and the
 * trace shows each rejection as a meeting that simply does not fit before the room
 * frees.
 */
@Component
public class NMeetingsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "n-meetings-in-one-room";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("start", FieldType.INT_ARRAY)
                        .label("Start times")
                        .help("Start time of each meeting, one entry per meeting.")
                        .length(1, 40).values(0, 1000)
                        .defaultValue(List.of(1, 3, 0, 5, 8, 5))
                        .build(),
                InputField.of("end", FieldType.INT_ARRAY)
                        .label("End times")
                        .help("End time of each meeting; end[i] belongs to start[i].")
                        .length(1, 40).values(0, 1000)
                        .defaultValue(List.of(2, 4, 6, 7, 9, 9))
                        .build());
    }

    /** Back-to-back meetings where only the first fits — a very different answer. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "start", List.of(1, 2, 3, 4, 5),
                "end", List.of(6, 7, 8, 9, 10));
    }

    @Override
    public String annotatedCode() {
        return """
               public int maxMeetings(int[] start, int[] end) {
                   // @a pair
                   int n = start.length;
                   int[][] meetings = new int[n][2];
                   for (int i = 0; i < n; i++) {
                       meetings[i][0] = start[i];
                       meetings[i][1] = end[i];
                   }
                   // @a sort
                   Arrays.sort(meetings, (a, b) -> a[1] - b[1]);
                   // @a init
                   int count = 0, lastEnd = -1;
                   for (int[] m : meetings) {
                       // @a check
                       if (m[0] > lastEnd) {
                           // @a select
                           count++;
                           lastEnd = m[1];
                       } else {
                           // @a reject
                           continue;
                       }
                   }
                   // @a done
                   return count;
               }""";
    }

    /**
     * One bar per meeting in SORTED order — the input order is gone and that is the
     * point. state: what the greedy loop has decided about each slot.
     */
    private List<ArrayElement> board(int[][] sorted, List<Integer> selectedIds,
                                     int cursor, Integer rejectedId) {
        List<ArrayElement> state = new ArrayList<>(sorted.length);
        for (int i = 0; i < sorted.length; i++) {
            String s;
            if (selectedIds.contains(sorted[i][2])) {
                s = "sorted";                 // taken
            } else if (rejectedId != null && rejectedId.equals(sorted[i][2])) {
                s = "visited";                // conflict
            } else if (i == cursor) {
                s = "current";                // under consideration now
            } else {
                s = "target";                 // still ahead
            }
            state.add(new ArrayElement(i, sorted[i][1], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] start = in.getIntArray("start");
        int[] end = in.getIntArray("end");

        if (start.length != end.length) {
            throw new InputValidationException(Map.of("end",
                    "You gave " + start.length + " starts but " + end.length + " ends — one per meeting."));
        }
        for (int i = 0; i < start.length; i++) {
            if (end[i] < start[i]) {
                throw new InputValidationException(Map.of("end",
                        "Meeting " + (i + 1) + " ends at " + end[i] + " but starts at " + start[i] + "."));
            }
        }

        int n = start.length;

        int[][] meetings = new int[n][3];   // start, end, original id (1-based)
        for (int i = 0; i < n; i++) {
            meetings[i][0] = start[i];
            meetings[i][1] = end[i];
            meetings[i][2] = i + 1;
        }

        emit.at("pair").say("%d meetings. Keep each end tied to its own start — sorting must not tear them apart.", n)
                .var("meetings", describe(meetings))
                .arrayState(board(meetings, List.of(), -1, null)).step();

        Arrays.sort(meetings, (a, b) -> a[1] - b[1]);

        emit.at("sort").say("Sort by END time: the meeting that frees the room earliest always goes first. That ordering is the whole algorithm.")
                .var("order", describe(meetings))
                .arrayState(board(meetings, List.of(), -1, null)).step();

        int count = 0;
        int lastEnd = -1;
        List<Integer> selected = new ArrayList<>();

        emit.at("init").say("Start greedy: nothing selected, the room is free from time 0. One pass over the sorted list decides everything.")
                .var("count", count).var("lastEnd", 0)
                .arrayState(board(meetings, selected, -1, null)).step();

        for (int i = 0; i < meetings.length; i++) {
            int[] m = meetings[i];

            int freeAt = Math.max(lastEnd, 0);
            emit.at("check").say("Meeting #%d runs %d→%d. The room is free from %d.",
                            m[2], m[0], m[1], freeAt)
                    .var("meeting", "#" + m[2]).var("freeFrom", freeAt)
                    .arrayState(board(meetings, selected, i, null)).step();

            if (m[0] > lastEnd) {
                count++;
                selected.add(m[2]);
                lastEnd = m[1];
                emit.at("select").say("%d > %d, so it fits. Take it — the meeting that ends earliest can never crowd out a better choice later.",
                                m[0], freeAt)
                        .var("count", count).var("selected", selected).var("lastEnd", lastEnd)
                        .arrayState(board(meetings, selected, -1, null)).step();
            } else {
                emit.at("reject").say("%d ≤ %d: it would still be running when the room is promised. Skip it — a later-ending meeting never beats an earlier one.",
                                m[0], lastEnd)
                        .var("rejected", "#" + m[2]).var("count", count)
                        .arrayState(board(meetings, selected, -1, m[2])).step();
            }
        }

        emit.at("done").say("Room used by %d of %d meetings: #%s.", count, n, join(selected))
                .var("count", count).var("selected", selected)
                .arrayState(board(meetings, selected, -1, null)).step();
    }

    private static String describe(int[][] meetings) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < meetings.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("#").append(meetings[i][2]).append("(").append(meetings[i][0])
              .append("-").append(meetings[i][1]).append(")");
        }
        return sb.append(']').toString();
    }

    private static String join(List<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(ids.get(i));
        }
        return sb.toString();
    }
}
