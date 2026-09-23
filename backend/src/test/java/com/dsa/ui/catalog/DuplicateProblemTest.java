package com.dsa.ui.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two services catalogued the same problems and nothing stopped them.
 *
 * <p>Where the ids happened to match exactly, {@code stats.duplicateIds} caught it - seven
 * of them. Where they differed only in word order or tense, nothing did:
 * {@code rotten-oranges} and {@code rotting-oranges} are one problem, as are
 * {@code cycle-undirected-bfs} and {@code undirected-cycle-bfs}. Eleven duplicated problems
 * in total, all from AdvancedGraphService and GraphBfsDfsService overlapping.
 *
 * <p>The root cause is fixed - both now live in one {@code Graphs} topic - but nothing about
 * that prevents a nineteenth service doing it again. This is the check that does.
 */
@SpringBootTest
class DuplicateProblemTest {

    @Autowired
    private ProblemCatalog catalog;

    /**
     * Pairs that normalise alike and are kept apart on purpose, each with the reason.
     * Adding to this list is a decision; an unlisted collision is an accident.
     */
    private static final Map<String, String> DELIBERATE = Map.of(
            // The same problem taught two standard ways. Their titles say which is which,
            // and their traces were deliberately made to teach different things - CLRS
            // edge classification against the two-boolean recursion-path formulation, and
            // reconstructing the cycle against merely answering whether one exists.
            "cycle-directed-dfs", "directed-cycle-dfs",
            "cycle-undirected-dfs", "undirected-cycle-dfs",
            // Word order carries the meaning: ascending versus descending, and the two
            // inverse conversions. Genuinely different problems.
            "print-1-to-n", "print-n-to-1",
            "infix-to-postfix", "postfix-to-infix",
            "infix-to-prefix", "prefix-to-infix",
            "postfix-to-prefix", "prefix-to-postfix",
            // "queue using stacks" and "stack using queues" - different implementations.
            "queue-stack-impl", "stack-queue-impl"
    );

    @Test
    @DisplayName("No problem is catalogued twice")
    void noIdIsClaimedByTwoProviders() {
        assertTrue(catalog.getDuplicateIds().isEmpty(),
                "these ids are registered by more than one provider, so one registration is"
                        + " silently discarded along with whatever metadata it carried: "
                        + catalog.getDuplicateIds());
    }

    @Test
    @DisplayName("No two ids are the same words in a different order")
    void noIdCollidesOnItsWordsAlone() {
        Map<String, List<String>> byShape = new LinkedHashMap<>();
        for (CatalogEntry entry : catalog.all()) {
            byShape.computeIfAbsent(shapeOf(entry.getProblem().getId()), k -> new ArrayList<>())
                    .add(entry.getProblem().getId());
        }

        List<String> accidental = new ArrayList<>();
        for (List<String> ids : byShape.values()) {
            if (ids.size() < 2) {
                continue;
            }
            Set<String> sorted = new TreeSet<>(ids);
            boolean allowed = sorted.size() == 2
                    && isDeliberatePair(sorted.toArray(new String[0]));
            if (!allowed) {
                accidental.add(ids.toString());
            }
        }

        assertTrue(accidental.isEmpty(),
                "these ids differ only in word order or tense, which is how"
                        + " rotten-oranges and rotting-oranges both sat in the catalogue as"
                        + " separate problems: " + accidental + ". Either they are one problem"
                        + " and one should go, or they are two and DELIBERATE should say why.");
    }

    private static boolean isDeliberatePair(String[] pair) {
        return pair[1].equals(DELIBERATE.get(pair[0])) || pair[0].equals(DELIBERATE.get(pair[1]));
    }

    /** An id reduced to its bag of stems, so word order and tense stop distinguishing it. */
    private static String shapeOf(String id) {
        return new TreeSet<>(Arrays.stream(id.split("-"))
                .map(DuplicateProblemTest::stem)
                .toList()).toString();
    }

    private static String stem(String word) {
        String w = word.endsWith("s") ? word.substring(0, word.length() - 1) : word;
        return w.replace("ing", "").replace("en", "");
    }
}
