package com.dsa.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * InputValidator bounds a single request - per-field caps and a step budget, both
 * mandatory. Nothing bounded many of them, and /execute runs real algorithms, so a loop
 * over an expensive problem was a free way to spend the server's CPU.
 *
 * <p>The limit is set to 3 here rather than the production 60 so the test states its intent
 * in three calls instead of sixty.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "dsa.rate-limit.executes-per-minute=3")
class ExecuteRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("A client that keeps executing is eventually refused")
    void executeIsRateLimited() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/problems/two-sum/execute").with(r -> {
                r.setRemoteAddr("203.0.113.10");
                return r;
            })).andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/problems/two-sum/execute").with(r -> {
            r.setRemoteAddr("203.0.113.10");
            return r;
        })).andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("A refusal tells the caller when to come back")
    void refusalCarriesRetryAfter() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(get("/api/problems/two-sum/execute").with(r -> {
                r.setRemoteAddr("203.0.113.11");
                return r;
            }));
        }

        String retryAfter = mockMvc.perform(get("/api/problems/two-sum/execute").with(r -> {
            r.setRemoteAddr("203.0.113.11");
            return r;
        })).andReturn().getResponse().getHeader("Retry-After");

        assertNotNull(retryAfter, "a 429 with no Retry-After leaves the caller guessing");
    }

    @Test
    @DisplayName("One client's limit is not another client's limit")
    void limitsArePerClient() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(get("/api/problems/two-sum/execute").with(r -> {
                r.setRemoteAddr("203.0.113.20");
                return r;
            }));
        }

        // A different address is unaffected. Sharing one bucket across all callers would
        // let a single abusive client lock everyone else out - a self-inflicted outage.
        mockMvc.perform(get("/api/problems/two-sum/execute").with(r -> {
            r.setRemoteAddr("203.0.113.21");
            return r;
        })).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Clients behind a proxy are told apart by X-Forwarded-For")
    void forwardedClientsAreDistinguished() throws Exception {
        // In production this sits behind a PaaS proxy, so every request shares the proxy's
        // address. Without this the whole internet would share one bucket, which is worse
        // than having no limit at all.
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(get("/api/problems/two-sum/execute")
                    .header("X-Forwarded-For", "198.51.100.7"));
        }

        mockMvc.perform(get("/api/problems/two-sum/execute")
                        .header("X-Forwarded-For", "198.51.100.8"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("The catalogue is never rate limited")
    void catalogueIsNotLimited() throws Exception {
        // Charging for the expensive call and not the cheap one is the point. The catalogue
        // is a cached, compressed projection; limiting it would only punish a real user
        // loading the page.
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/api/problems").with(r -> {
                r.setRemoteAddr("203.0.113.30");
                return r;
            })).andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/problems/stats").with(r -> {
            r.setRemoteAddr("203.0.113.30");
            return r;
        })).andExpect(status().isOk());
    }
}
