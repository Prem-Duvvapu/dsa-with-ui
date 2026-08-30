package com.dsa.ui.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DsTypeContractTest {

    @Test
    void everyBackendTypeMatchesTheSharedCanvasContract() throws IOException {
        Path fixture = findFixture();
        List<Map<String, String>> expected = new ObjectMapper().readValue(
                fixture.toFile(), new TypeReference<>() {});

        Map<String, String> expectedTypes = new LinkedHashMap<>();
        expected.forEach(row -> expectedTypes.put(row.get("enum"), row.get("wire")));

        Map<String, String> backendTypes = new LinkedHashMap<>();
        for (DsType type : DsType.values()) {
            backendTypes.put(type.name(), type.wireValue());
        }

        assertEquals(expectedTypes, backendTypes,
                "Backend DsType changed without a matching entry in contracts/ds-types.json; "
                        + "the frontend canvas registry would drift");
    }

    private static Path findFixture() {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null) {
            Path candidate = directory.resolve("contracts/ds-types.json");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }
        throw new AssertionError("Could not find contracts/ds-types.json from the test working directory");
    }
}
