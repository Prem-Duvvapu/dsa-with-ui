package com.dsa.ui.approach;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ApproachTeachingTest {
    private ApproachTeaching teaching(String state, String bases, String recurrence, String order,
                                      String key, Map<String, String> events, Map<String, String> anchors) {
        return new ApproachTeaching(state, bases, recurrence, order, key, events, anchors);
    }

    @Test void refusesMissingOrBlankExplanationFields() {
        assertThrows(IllegalArgumentException.class, () -> teaching(null, "Bases", "Recurrence", "Order", null, Map.of(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> teaching(" ", "Bases", "Recurrence", "Order", null, Map.of(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> teaching("State", "", "Recurrence", "Order", null, Map.of(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> teaching("State", "Bases", "\n", "Order", null, Map.of(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> teaching("State", "Bases", "Recurrence", "", null, Map.of(), Map.of()));
    }

    @Test void optionalMemoKeyMayBeAbsentButNotBlank() {
        assertNull(teaching("State", "Bases", "Recurrence", "Order", null, Map.of(), Map.of()).cacheKey());
        assertThrows(IllegalArgumentException.class, () -> teaching("State", "Bases", "Recurrence", "Order", " ", Map.of(), Map.of()));
    }

    @Test void refusesBlankEventAndAnchorKeysOrNotes() {
        assertThrows(IllegalArgumentException.class, () -> teaching("State", "Bases", "Recurrence", "Order", null, Map.of("", "Note"), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> teaching("State", "Bases", "Recurrence", "Order", null, Map.of("hit", " "), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> teaching("State", "Bases", "Recurrence", "Order", null, Map.of(), Map.of(" ", "Note")));
        assertThrows(IllegalArgumentException.class, () -> teaching("State", "Bases", "Recurrence", "Order", null, Map.of(), Map.of("done", "")));
    }

    @Test void snapshotsNotesAndMakesThemImmutable() {
        var notes = new HashMap<>(Map.of("hit", "Original"));
        var data = teaching("State", "Bases", "Recurrence", "Order", null, notes, Map.of());
        notes.put("hit", "Replacement");
        assertEquals("Original", data.eventNotes().get("hit"));
        assertThrows(UnsupportedOperationException.class, () -> data.eventNotes().put("hit", "Changed"));
    }
}
