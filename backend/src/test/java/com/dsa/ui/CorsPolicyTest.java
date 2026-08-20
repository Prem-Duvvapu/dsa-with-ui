package com.dsa.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS is centrally owned by {@link com.dsa.ui.config.CorsConfig}.
 *
 * <p>Eight controllers previously carried {@code @CrossOrigin(origins = "*")}. A
 * handler-level annotation overrides the central registry, so half the API was wide
 * open while the other half was locked down — and the central config pointed at
 * :5173 and :3000, neither of which this project runs on. These tests pin both halves:
 * the real dev/docker origins are allowed, and an arbitrary origin is refused
 * <em>on the controllers that used to permit everything</em>.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsPolicyTest {

    /** The controllers that previously declared @CrossOrigin(origins = "*"). */
    static Stream<String> formerlyWildcardPaths() {
        return Stream.of(
                "/api/bitmanipulation/problems",
                "/api/greedy/problems",
                "/api/heaps/problems",
                "/api/recursion-backtracking/problems",
                "/api/slidingwindow/problems",
                "/api/stackqueue/problems",
                "/api/strings/problems",
                "/api/tries/problems"
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "origin {0} is allowed")
    @ValueSource(strings = {
            "http://localhost:5180",    // Vite dev server
            "http://127.0.0.1:5180",
            "http://localhost:5174",    // published Docker port
            "http://127.0.0.1:5174"
    })
    @DisplayName("The ports this project actually runs on are allowed")
    void allowsRealFrontendOrigins(String origin) throws Exception {
        mockMvc.perform(get("/api/arrays/problems").header("Origin", origin))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin));
    }

    @ParameterizedTest(name = "stale origin {0} is refused")
    @ValueSource(strings = {
            "http://localhost:5173",    // the old, wrong value in CorsConfig
            "http://localhost:3000"
    })
    @DisplayName("The origins that were configured but never used are no longer allowed")
    void refusesStaleConfiguredOrigins(String origin) throws Exception {
        mockMvc.perform(get("/api/arrays/problems").header("Origin", origin))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} refuses an arbitrary origin")
    @MethodSource("formerlyWildcardPaths")
    @DisplayName("No controller silently permits every origin")
    void formerlyWildcardControllersNoLongerAllowAnyOrigin(String path) throws Exception {
        mockMvc.perform(get(path).header("Origin", "http://evil.example"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} still serves same-origin requests")
    @MethodSource("formerlyWildcardPaths")
    @DisplayName("Tightening CORS did not break ordinary requests")
    void formerlyWildcardControllersStillServeRequests(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk());
    }
}
