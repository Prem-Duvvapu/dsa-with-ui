package com.dsa.ui.algorithm.greedy;

import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.*;

/**
 * Problem: N Meetings in One Room (Greedy Activity Selection)
 *
 * Find maximum number of meetings that can be performed in one room by sorting by end time.
 */
public class NMeetingsInOneRoom {

    public static class Meeting {
        public int start;
        public int end;
        public int id;
        public Meeting(int id, int start, int end) {
            this.id = id;
            this.start = start;
            this.end = end;
        }
    }

    public List<Integer> solve(int[] start, int[] end, TraceRecorder recorder) {
        int n = start.length;
        List<Meeting> meetings = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            meetings.add(new Meeting(i + 1, start[i], end[i]));
        }

        recorder.record(new TraceEvent(
            "start", 12,
            String.format("N Meetings in One Room: Input %d meetings. Sort meetings in ascending order of END TIME.", n),
            Map.of("meetingsCount", String.valueOf(n)),
            "Array", null
        ));

        // Sort greedily by end time
        meetings.sort(Comparator.comparingInt(m -> m.end));

        List<Integer> selected = new ArrayList<>();
        int lastEndTime = -1;

        for (Meeting m : meetings) {
            if (m.start > lastEndTime) {
                selected.add(m.id);
                lastEndTime = m.end;

                recorder.record(new TraceEvent(
                    "select_meeting", 22,
                    String.format("Meeting #%d (start=%d, end=%d): Start %d > lastEndTime %d -> GREEDY SELECT! Total selected = %d.",
                        m.id, m.start, m.end, m.start, lastEndTime, selected.size()),
                    Map.of("selectedMeeting", "M#" + m.id, "lastEndTime", String.valueOf(lastEndTime)),
                    "Array", null
                ));
            } else {
                recorder.record(new TraceEvent(
                    "reject_meeting", 28,
                    String.format("Meeting #%d (start=%d, end=%d): Start %d <= lastEndTime %d -> CONFLICT! Reject meeting.",
                        m.id, m.start, m.end, m.start, lastEndTime),
                    Map.of("rejectedMeeting", "M#" + m.id, "conflictWith", String.valueOf(lastEndTime)),
                    "Array", null
                ));
            }
        }

        recorder.record(new TraceEvent(
            "complete", 35,
            String.format("N Meetings Complete! Maximum meetings possible = %d. Selected Meeting IDs: %s", selected.size(), selected.toString()),
            Map.of("MaxMeetings", String.valueOf(selected.size()), "SelectedIDs", selected.toString()),
            "Array", null
        ));

        return selected;
    }
}
