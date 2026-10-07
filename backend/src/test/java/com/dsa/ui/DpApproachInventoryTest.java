package com.dsa.ui;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.AnnotatedCode;
import com.dsa.ui.tracer.TracerRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** The planning ledger must cover the live candidate set and the code actually audited. */
@SpringBootTest
class DpApproachInventoryTest {
    @Autowired ProblemCatalog catalog;
    @Autowired TracerRegistry tracers;

    @Test void inventoryCoversCurrentCandidatesAndPinsAuditedSources() throws Exception {
        var json = new ObjectMapper();
        var document = json.readTree(Path.of("../docs/ui-revamp/dp-approaches/inventory.json").toFile());
        var expected = catalog.all().stream().filter(entry ->
                "Dynamic Programming".equals(entry.getProblem().getCategory())
                || entry.getProblem().getId().equals("count-palindromic-subsequences"))
                .map(entry -> entry.getProblem().getId()).collect(java.util.stream.Collectors.toSet());
        Set<String> seen = new HashSet<>();
        for (var row : document.path("rows")) {
            String id = row.path("id").asText();
            assertTrue(seen.add(id), "Duplicate ledger entry: " + id);
            var tracer = tracers.find(id).orElseThrow();
            String source = AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode();
            assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8))), row.path("codeDigest").asText(),
                    id + " source changed; re-audit its classification before refreshing");
            assertEquals(json.readTree(json.writeValueAsBytes(tracer.inputSpec())), row.path("canonicalInputSpec"), id + " limits changed");
            for (String required : List.of("state", "transition", "canonicalImplementation", "notes", "alternativeSafetyBounds")) {
                assertFalse(row.path(required).asText().isBlank(), id + " lacks " + required);
            }
        }
        assertEquals(expected, seen);
        assertEquals(seen.size(), document.path("candidates").asInt());
    }
}
