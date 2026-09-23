package com.dsa.ui.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The sidebar renders its category buttons from a hardcoded list, so a category the backend
 * serves and the frontend has never heard of is simply unreachable - its problems exist,
 * answer over the API, and cannot be browsed to.
 *
 * <p>That had already happened twice. The backend calls a category {@code BST}; the sidebar
 * called it {@code Binary Search Trees}, so sixteen problems were invisible in the category
 * grid. And merging the two graph topics into {@code Graphs} would have made fifty-eight
 * more invisible the same way.
 *
 * <p>{@code contracts/categories.json} is the shared truth. This asserts the backend matches
 * it; {@code Sidebar.test.jsx} asserts the sidebar does. Neither side can drift alone.
 */
@SpringBootTest
class CategoryContractTest {

    @Autowired
    private ProblemCatalog catalog;

    @Test
    @DisplayName("Every catalogue category is declared in the shared contract")
    void categoriesMatchTheSharedContract() throws IOException {
        List<Map<String, String>> declared = new ObjectMapper()
                .readValue(findFixture().toFile(), new TypeReference<>() {});

        TreeSet<String> expected = new TreeSet<>(declared.stream()
                .map(row -> row.get("category")).toList());
        TreeSet<String> actual = new TreeSet<>(catalog.all().stream()
                .map(entry -> entry.getProblem().getCategory()).toList());

        assertEquals(expected, actual,
                "The catalogue's categories and contracts/categories.json disagree. A category"
                        + " the backend serves but the contract omits is unreachable from the"
                        + " sidebar, and its problems can only be found by search.");
    }

    private static Path findFixture() {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null) {
            Path candidate = directory.resolve("contracts/categories.json");
            if (candidate.toFile().isFile()) {
                return candidate;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("contracts/categories.json not found");
    }
}
