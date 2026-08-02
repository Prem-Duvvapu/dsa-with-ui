package com.dsa.ui;

import com.dsa.ui.controller.GraphBfsDfsController;
import com.dsa.ui.service.GraphBfsDfsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GraphBfsDfsController.class)
class GraphBfsDfsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private GraphBfsDfsService graphBfsDfsService;

    @Test
    @DisplayName("GET /api/graphs/bfs-dfs/problems should return HTTP 200 with list of 11 problems")
    void testGetAllProblemsEndpoint() throws Exception {
        mockMvc.perform(get("/api/graphs/bfs-dfs/problems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(11)))
                .andExpect(jsonPath("$[0].id", is("bfs-traversal")))
                .andExpect(jsonPath("$[0].title", is("BFS Traversal of Graph")))
                .andExpect(jsonPath("$[0].difficulty", is("Easy")));
    }

    @Test
    @DisplayName("GET /api/graphs/bfs-dfs/problems/{id} should return HTTP 200 for valid ID and 404 for invalid ID")
    void testGetProblemByIdEndpoint() throws Exception {
        mockMvc.perform(get("/api/graphs/bfs-dfs/problems/bfs-traversal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("bfs-traversal")))
                .andExpect(jsonPath("$.complexity.timeComplexity", is("O(V + 2E)")));

        mockMvc.perform(get("/api/graphs/bfs-dfs/problems/invalid-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/graphs/bfs-dfs/execute/{id} should return execution steps list")
    void testGetExecutionStepsEndpoint() throws Exception {
        mockMvc.perform(get("/api/graphs/bfs-dfs/execute/bfs-traversal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].stepNumber", is(1)))
                .andExpect(jsonPath("$[0].dsType", is("Queue")));
    }
}
