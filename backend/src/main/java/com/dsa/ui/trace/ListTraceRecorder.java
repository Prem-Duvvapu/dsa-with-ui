package com.dsa.ui.trace;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ListNode;
import com.dsa.ui.model.TrieNodeModel;

import java.util.ArrayList;
import java.util.List;

public class ListTraceRecorder implements TraceRecorder {
    private final List<TraceEvent> events = new ArrayList<>();

    @Override
    public void record(TraceEvent event) {
        events.add(event);
    }

    public List<TraceEvent> getEvents() {
        return events;
    }

    @SuppressWarnings("unchecked")
    public List<ExecutionStep> toExecutionSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int stepNum = 1;
        for (TraceEvent ev : events) {
            int[][] gridState = null;
            List<ArrayElement> arrayState = null;
            List<ListNode> listState = null;
            List<TrieNodeModel> trieState = null;

            if (ev.getSnapshot() instanceof int[][]) {
                gridState = (int[][]) ev.getSnapshot();
            } else if (ev.getSnapshot() instanceof List<?>) {
                List<?> rawList = (List<?>) ev.getSnapshot();
                if (!rawList.isEmpty()) {
                    Object first = rawList.get(0);
                    if (first instanceof ArrayElement) {
                        arrayState = (List<ArrayElement>) rawList;
                    } else if (first instanceof ListNode) {
                        listState = (List<ListNode>) rawList;
                    } else if (first instanceof TrieNodeModel) {
                        trieState = (List<TrieNodeModel>) rawList;
                    }
                }
            }

            steps.add(new ExecutionStep(
                stepNum++,
                ev.getCodeLine(),
                ev.getDescription(),
                ev.getCallStack(),
                ev.getNodeStates(),
                ev.getActiveEdges(),
                ev.getVariables(),
                ev.getDsType(),
                gridState,
                arrayState,
                listState,
                trieState,
                ev.getTreeNodes()
            ));
        }
        return steps;
    }
}
