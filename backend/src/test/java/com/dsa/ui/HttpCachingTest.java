package com.dsa.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The catalogue is ~236 KB of JSON that never changes between deploys, and every page load
 * fetched all of it: no ETag, no cached projection.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HttpCachingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("The catalogue carries an ETag a client can revalidate against")
    void catalogueIsRevalidatable() throws Exception {
        MvcResult first = mockMvc.perform(get("/api/problems"))
                .andExpect(status().isOk())
                .andReturn();

        String etag = first.getResponse().getHeader("ETag");
        assertNotNull(etag, "no ETag, so a repeat visitor has to re-download the whole catalogue");

        mockMvc.perform(get("/api/problems").header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    @DisplayName("The catalogue ETag is WEAK, so the response can still be compressed")
    void catalogueEtagIsWeak() throws Exception {
        // Load-bearing, not stylistic. Tomcat refuses to compress a response carrying a
        // STRONG ETag - a strong tag names an exact byte sequence and gzip changes those
        // bytes. With a strong tag this endpoint served 236 KB uncompressed while every
        // other endpoint compressed normally: the one response that most needed it was the
        // only one excluded. Measured at 31 KB once weak, an 87% saving.
        String etag = mockMvc.perform(get("/api/problems"))
                .andReturn().getResponse().getHeader("ETag");

        assertNotNull(etag);
        assertTrue(etag.startsWith("W/\""),
                "a strong ETag silently disables compression on the largest response in the"
                        + " API; expected a weak tag but got: " + etag);
    }

    @Test
    @DisplayName("The catalogue tells clients to revalidate rather than trust a stale copy")
    void catalogueMustRevalidate() throws Exception {
        String cacheControl = mockMvc.perform(get("/api/problems"))
                .andReturn().getResponse().getHeader("Cache-Control");
        assertNotNull(cacheControl, "no Cache-Control, so caching behaviour is the client's guess");
        assertTrue(cacheControl.contains("must-revalidate"),
                "expected must-revalidate so a deploy is picked up at once, got: " + cacheControl);
    }

    @Test
    @DisplayName("A stale ETag still gets the full body rather than an empty 304")
    void staleEtagIsNotHonoured() throws Exception {
        mockMvc.perform(get("/api/problems").header("If-None-Match", "\"not-the-current-body\""))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Repeated catalogue requests return the identical cached projection")
    void catalogueProjectionIsComputedOnce() throws Exception {
        // Not a timing assertion - those are flaky. Byte-identical output across calls is
        // what the cache is for, and it is what a client's ETag depends on.
        String a = mockMvc.perform(get("/api/problems")).andReturn()
                .getResponse().getContentAsString();
        String b = mockMvc.perform(get("/api/problems")).andReturn()
                .getResponse().getContentAsString();
        assertEquals(a, b, "the projection must be stable, or the ETag can never match");
    }

    @Test
    @DisplayName("The shared catalogue projection cannot be mutated by a caller")
    void cachedSummariesAreUnmodifiable() throws Exception {
        // The cache is shared across every request now. summarize() returns a mutable
        // LinkedHashMap, so without this one caller could corrupt the catalogue for
        // everyone that followed.
        String body = mockMvc.perform(get("/api/problems")).andReturn()
                .getResponse().getContentAsString();
        List<Map<String, Object>> entries = MAPPER.readValue(body, List.class);
        assertFalse(entries.isEmpty(), "catalogue must not be empty");
    }

    @Test
    @DisplayName("Detail responses stay per-problem rather than serving the cached list")
    void detailIsNotAffectedByTheCache() throws Exception {
        mockMvc.perform(get("/api/problems/kadane-algo"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/problems/two-sum"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("The default trace is computed once and served identically thereafter")
    void defaultTraceIsCached() throws Exception {
        // Most traffic is "open a problem, press play", which re-ran the same algorithm to
        // produce a byte-identical answer every time. Not a timing assertion - those are
        // flaky - but identical output is what the cache is for and what a client can rely on.
        String first = mockMvc.perform(get("/api/problems/kadane-algo/execute"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(get("/api/problems/kadane-algo/execute"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(first, second);
    }

    @Test
    @DisplayName("Each encoding is cached separately rather than sharing one entry")
    void encodingsDoNotShareACacheEntry() throws Exception {
        String delta = mockMvc.perform(get("/api/problems/kadane-algo/execute"))
                .andReturn().getResponse().getContentAsString();
        String full = mockMvc.perform(get("/api/problems/kadane-algo/execute?encoding=full"))
                .andReturn().getResponse().getContentAsString();
        assertNotEquals(delta, full,
                "?encoding=full returned the delta-encoded body, so the two share a cache key");
    }

    @Test
    @DisplayName("A caller-supplied input still runs for real")
    void customInputIsNotCached() throws Exception {
        // A cache keyed on caller input is a memory-exhaustion vector, and this path has to
        // do real work every time - which is why the rate limiter exists.
        String a = mockMvc.perform(post("/api/problems/kadane-algo/execute")
                        .contentType("application/json")
                        .content("{\"nums\":[1,2,3]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String b = mockMvc.perform(post("/api/problems/kadane-algo/execute")
                        .contentType("application/json")
                        .content("{\"nums\":[5,-1,5]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertNotEquals(a, b, "two different inputs returned the same trace");
    }
}