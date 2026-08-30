package com.dsa.ui.tracer;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ExecutionStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DpTableEmitterTest {

    @Test
    void emitterCommitsDpTableAndChargesItsPayload() {
        StepEmitter emitter = new StepEmitter(
                AnnotatedCode.parse("// @a fill\ndp[i] = best;"),
                10,
                100_000,
                DsType.DP_TABLE);
        DpTable table = new DpTable(
                List.of("length"),
                List.of("0", "1"),
                List.of(List.of(new DpCell("1", "read"), new DpCell("2", "probe"))));

        emitter.at("fill").dpTable(table).step();

        ExecutionStep committed = emitter.collected().get(0);
        assertSame(table, committed.getDpTable());
        assertTrue(StepEmitter.estimateBytes(committed) > 300,
                "the byte budget must charge labels and DP cells");
    }

    @Test
    void dpCellRejectsStatesOutsideTheBenchVocabulary() {
        assertThrows(IllegalArgumentException.class, () -> new DpCell("3", "active"));
    }

    @Test
    void dpTableRejectsRowsThatDoNotMatchItsColumnLabels() {
        assertThrows(IllegalArgumentException.class, () -> new DpTable(
                List.of("length"),
                List.of("0", "1"),
                List.of(List.of(new DpCell("1", "known")))));
    }
}
