package com.dsa.ui.catalog;

import com.dsa.ui.model.ProblemDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The source problem's constraints are shown to the learner as fact, so the rules here are
 * about honesty rather than coverage: an id may have none, but what it has must be real.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProblemConstraintsTest {

    @Autowired
    private ProblemCatalog catalog;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Constraints survive the wire, not just the catalogue object")
    void constraintsReachTheApiResponse() throws Exception {
        // /api/problems/{id} hand-builds its response map, so a field present on
        // ProblemDetail is not automatically served. This asserts the JSON a browser
        // actually receives - the first version of this feature populated the catalogue
        // correctly and shipped an endpoint that dropped the field.
        mockMvc.perform(get("/api/problems/kadane-algo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.constraints", hasItem("1 <= nums.length <= 10^5")));
    }

    @Test
    @DisplayName("An id with no recorded constraints serves null rather than a guess")
    void unrecordedIdServesNothingOverTheWire() throws Exception {
        mockMvc.perform(get("/api/problems/graph-intro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.constraints").doesNotExist());
    }

    @Test
    @DisplayName("Recorded constraints reach the catalogue entry the API serves")
    void recordedConstraintsAreApplied() {
        ProblemDetail kadane = catalog.find("kadane-algo").orElseThrow().getProblem();
        assertNotNull(kadane.getConstraints(), "kadane-algo has recorded constraints");
        assertTrue(kadane.getConstraints().contains("1 <= nums.length <= 10^5"),
                "expected the source constraint, got: " + kadane.getConstraints());
    }

    @Test
    @DisplayName("An id with no recorded constraints stays empty rather than inventing one")
    void unrecordedIdsStayEmpty() {
        // The UI omits the section entirely for these. A bound invented from memory is
        // indistinguishable from a real one on screen, and strictly worse than silence.
        List<String> none = ProblemConstraints.forId("definitely-not-a-real-problem-id");
        assertTrue(none.isEmpty(), "an unknown id must yield no constraints, got: " + none);
    }

    @Test
    @DisplayName("No recorded constraint is blank or a placeholder")
    void everyRecordedConstraintIsSubstantive() {
        List<String> bad = new ArrayList<>();
        for (CatalogEntry entry : catalog.all()) {
            List<String> constraints = entry.getProblem().getConstraints();
            if (constraints == null) {
                continue;
            }
            for (String c : constraints) {
                if (c == null || c.isBlank() || c.strip().equals("-") || c.toLowerCase().contains("tbd")) {
                    bad.add(entry.getProblem().getId() + ": " + c);
                }
            }
        }
        assertTrue(bad.isEmpty(), "blank or placeholder constraints: " + bad);
    }

    @Test
    @DisplayName("Every recorded id actually exists in the catalogue")
    void recordedIdsAreRealProblems() {
        // A typo'd key here would silently record constraints nothing ever shows.
        long shown = catalog.all().stream()
                .filter(e -> e.getProblem().getConstraints() != null
                        && !e.getProblem().getConstraints().isEmpty())
                .count();
        assertEquals(ProblemConstraints.recordedCount(), shown,
                "every recorded id must match a catalogued problem - a key that matches nothing"
                        + " records constraints the UI can never display");
    }
}
