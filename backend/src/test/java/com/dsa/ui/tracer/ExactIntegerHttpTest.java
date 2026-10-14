package com.dsa.ui.tracer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class ExactIntegerHttpTest {
    @Autowired MockMvc http;
    private final ObjectMapper json = new ObjectMapper();

    @ParameterizedTest @ValueSource(strings = {"full", "delta"})
    void numericFieldsRejectPositiveNegativeAndBigIntegerAliasesWithoutATrace(String encoding) throws Exception {
        for (Number value : List.<Number>of(4294967297L, -4294967295L,
                new BigInteger("340282366920938463463374607431768211457"),
                new BigInteger("-340282366920938463463374607431768211455"))) {
            for (var sample : List.of(
                    new Sample("count-good-numbers", "n", value),
                    new Sample("largest-element", "nums", List.of(value)),
                    new Sample("matrix-median", "matrix", List.of(List.of(value))),
                    new Sample("tree-preorder", "tree", List.of(value)),
                    new Sample("reverse-linked-list", "values", List.of(value)),
                    new Sample("bfs-traversal", "start", value))) {
                reject(sample, encoding);
            }
        }
        reject(new Sample("bfs-traversal", "graph", Map.of("vertices", 4294967297L, "edges", List.of())), encoding);
    }

    private void reject(Sample sample, String encoding) throws Exception {
        http.perform(post("/api/problems/{id}/execute", sample.id()).param("encoding", encoding)
                        .contentType("application/json")
                        .content(json.writeValueAsString(Map.of(sample.field(), sample.value())))
                        .with(r -> { r.setRemoteAddr("exact-number-" + encoding + "-" + sample.id()); return r; }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_input"))
                .andExpect(jsonPath("$.fieldErrors." + sample.field()).isString())
                .andExpect(jsonPath("$.steps").doesNotExist());
    }

    private record Sample(String id, String field, Object value) {}
}
