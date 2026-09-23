package com.dsa.ui;

import com.dsa.ui.model.ProblemDetail;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code /api/problems/{id}} hand-builds its response map field by field, so a field added
 * to {@link ProblemDetail} is not served until someone remembers to name it there.
 *
 * <p>That has already shipped once. {@code constraints} was added to the model, populated
 * correctly, covered by a test asserting the in-memory catalogue - and served nothing,
 * because the endpoint dropped it. The test passed the whole time.
 *
 * <p>This walks the model by reflection and fails on any field the wire does not carry, so
 * the next addition cannot repeat it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DetailResponseContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Fields the endpoint deliberately does not serve, each for a stated reason. Adding to
     * this set is a decision; forgetting a field is not.
     */
    private static final Set<String> INTENTIONALLY_ABSENT = Set.of();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Every ProblemDetail field reaches the detail response")
    void everyModelFieldIsServed() throws Exception {
        // kadane-algo is chosen because it populates the widest set: description,
        // constraints, complexity, javaCode and an array default.
        JsonNode body = MAPPER.readTree(
                mockMvc.perform(get("/api/problems/kadane-algo"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray());

        List<String> missing = new ArrayList<>();
        for (Field f : ProblemDetail.class.getDeclaredFields()) {
            String name = f.getName();
            if (f.isSynthetic() || INTENTIONALLY_ABSENT.contains(name)) {
                continue;
            }
            if (!body.has(name)) {
                missing.add(name);
            }
        }

        assertTrue(missing.isEmpty(),
                "/api/problems/{id} builds its response map by hand, and these ProblemDetail"
                        + " fields never reach a client: " + missing + ". Add them to"
                        + " ProblemsController.detail(), or list them in INTENTIONALLY_ABSENT"
                        + " with a reason.");
    }

    @Test
    @DisplayName("A field that is populated in the catalogue is populated on the wire")
    void populatedFieldsArriveWithContent() throws Exception {
        // Presence is not enough: a field can be served as null while the catalogue holds a
        // value, which is the same outage with a different shape.
        JsonNode body = MAPPER.readTree(
                mockMvc.perform(get("/api/problems/kadane-algo"))
                        .andReturn().getResponse().getContentAsByteArray());

        assertFalse(body.path("description").asText("").isBlank(), "description arrived empty");
        assertTrue(body.path("constraints").isArray() && body.path("constraints").size() > 0,
                "constraints arrived empty for a problem that has them recorded");
        assertFalse(body.path("complexity").isMissingNode() || body.path("complexity").isNull(),
                "complexity arrived null");
        assertFalse(body.path("javaCode").asText("").isBlank(), "javaCode arrived empty");
    }

    @Test
    @DisplayName("The summary in the list carries the fields the sidebar renders from")
    void listSummaryCarriesWhatTheUiNeeds() throws Exception {
        JsonNode list = MAPPER.readTree(
                mockMvc.perform(get("/api/problems"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray());

        assertTrue(list.isArray() && list.size() > 0, "catalogue must not be empty");
        JsonNode first = list.get(0);
        for (String required : List.of("id", "title", "category", "difficulty", "dsType", "traced")) {
            assertTrue(first.has(required),
                    "the catalogue summary dropped '" + required + "', which the sidebar reads");
        }
    }
}
