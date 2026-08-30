package com.dsa.ui.tracer.wire;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.ExecutionStep;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DpTableWireTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void deltaEncodingCarriesAndReconstructsDpTable() throws Exception {
        DpTable first = table("probe");
        DpTable second = table("resolved");
        List<ExecutionStep> original = List.of(
                step(1, first),
                step(2, second),
                step(3, table("resolved")));

        List<DeltaStep> encoded = TraceEncoder.encode(original);
        String wireJson = json.writeValueAsString(encoded);

        assertTrue(wireJson.contains("\"dpTable\""),
                "the delta wire dropped dpTable, so a canvas can never receive it");
        assertNotNull(encoded.get(0).getDpTable(), "a keyframe must stand alone");
        assertNotNull(encoded.get(1).getDpTable(), "a changed table must be transmitted");
        assertNull(encoded.get(2).getDpTable(), "an unchanged table should be carried");
        assertEquals(List.of(first, second, second), decode(encoded));
    }

    private static ExecutionStep step(int number, DpTable table) {
        ExecutionStep step = new ExecutionStep();
        step.setStepNumber(number);
        step.setDpTable(table);
        return step;
    }

    /** Literal reference decoder, matching TraceEncoderTest and the frontend decoder. */
    private static List<DpTable> decode(List<DeltaStep> deltas) {
        java.util.ArrayList<DpTable> decoded = new java.util.ArrayList<>();
        DpTable carried = null;
        for (DeltaStep delta : deltas) {
            DpTable previous = Boolean.TRUE.equals(delta.getKeyframe()) ? null : carried;
            DpTable table = delta.getDpTable() != null ? delta.getDpTable() : previous;
            decoded.add(table);
            carried = table;
        }
        return decoded;
    }

    private static DpTable table(String state) {
        return new DpTable(
                List.of("length"),
                List.of("0", "1"),
                List.of(List.of(new DpCell("1", "known"), new DpCell("2", state))));
    }
}
