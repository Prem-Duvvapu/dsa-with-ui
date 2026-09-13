package com.dsa.ui.tracer;

import com.dsa.ui.model.ExecutionStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a trace says about itself, checked mechanically.
 *
 * <p>Written after reading all 431 golden files end to end. Reading found real defects that
 * no test could see — a swap naming both cells' pre-swap values beside a picture already
 * showing the result, three traces ending without stating their answer — and it also found
 * two classes that are pure repetition and belong in a test instead of in someone's eyes:
 *
 * <ul>
 *   <li>"1 <plural>": 33 steps across 14 problems said "at most 1 replacements", "only 1
 *       nodes remain", "1 transactions are allowed". Every trace that counts something
 *       eventually prints 1, and a hardcoded "s" is wrong on exactly that step.</li>
 *   <li>"%d-th": 12 steps across 7 problems said "the 2-th largest", "the 3-th root". It
 *       reads like a format placeholder nobody finished.</li>
 * </ul>
 *
 * <p>{@code Narration.s} / {@code Narration.ordinal} exist so the fix is one call. This test
 * exists so the next tracer cannot quietly skip them.
 *
 * <p>The noun list is a whitelist on purpose. An earlier version of this check matched any
 * word ending in "s" and reported 190 hits, of which every single one was a verb — "1
 * beats", "1 costs", "1 joins". No rule separates a plural noun from a third-person verb in
 * these sentences, so the ones that matter are named.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NarrationContractTest {

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    /** Not after a digit, a minus or an '=': "-1 entries" is right, and "a[0]=1 pairs" is a verb. */
    private static final Pattern ONE_NOUN = Pattern.compile("(?<![-\\d.=])1 ([a-z]+)\\b");

    private static final Pattern ORDINAL = Pattern.compile("\\b\\d+-th\\b");

    /** The dodge: "1 step(s)", "6 vertex/vertices" - correct for no value of the count. */
    private static final Pattern DODGE = Pattern.compile("\\(s\\)|\\(es\\)|vertex/vertices");

    private static final Set<String> COUNTABLE = Set.of(
            "steps", "nodes", "elements", "values", "items", "characters", "chars", "digits",
            "bits", "times", "ways", "combinations", "permutations", "subsets", "subarrays",
            "substrings", "subsequences", "replacements", "transactions", "stops", "platforms",
            "queens", "moves", "swaps", "comparisons", "edges", "vertices", "words", "letters",
            "cells", "rows", "columns", "pairs", "intervals", "meetings", "frames", "heads",
            "candidates", "occurrences", "operations", "passes", "rounds", "levels",
            "partitions", "islands", "colours", "colors", "coins", "sticks", "cards", "tasks",
            "books", "cows", "bouquets", "divisors", "factors", "primes", "solutions",
            "arrangements", "paths", "flips", "deletions", "insertions", "edits", "splits",
            "cuts", "instances", "candies", "entries", "buckets", "slots", "lists", "trees",
            "points", "ones", "zeroes", "zeros", "groups", "components", "provinces",
            "oranges", "stones", "jumps", "reversals", "rotations", "multiplications",
            "additions", "blocks", "jobs", "keys", "tweets", "pages", "trains", "cities",
            "flights", "courses", "accounts", "quadruplets", "triplets", "segments", "drops",
            "inversions", "houses", "stairs", "windows", "brackets", "chains", "bars",
            "rectangles", "squares", "denominations", "matrices");

    Stream<String> tracerIds() {
        return registry.tracedIds().stream().sorted();
    }

    private List<ExecutionStep> defaults(String id) {
        return runner.runDefaults(registry.find(id).orElseThrow()).getSteps();
    }

    @ParameterizedTest(name = "{0} agrees in number with what it counts")
    @MethodSource("tracerIds")
    @DisplayName("No trace says \"1\" and then a plural noun")
    void countsAgreeWithTheirNouns(String id) {
        for (ExecutionStep step : defaults(id)) {
            String said = step.getDescription();
            if (said == null) {
                continue;
            }
            Matcher m = ONE_NOUN.matcher(said);
            while (m.find()) {
                assertTrue(!COUNTABLE.contains(m.group(1)),
                        id + " step " + step.getStepNumber() + " says \"" + m.group()
                                + "\": \"" + said + "\". Use Narration.s(n) — every trace that"
                                + " counts something eventually prints 1.");
            }
        }
    }

    @ParameterizedTest(name = "{0} commits to singular or plural")
    @MethodSource("tracerIds")
    @DisplayName("No trace hedges with \"(s)\"")
    void nothingHedgesItsPlurals(String id) {
        for (ExecutionStep step : defaults(id)) {
            String said = step.getDescription();
            if (said == null) {
                continue;
            }
            Matcher m = DODGE.matcher(said);
            assertTrue(!m.find(),
                    id + " step " + step.getStepNumber() + " says \"" + (m.hitEnd() ? "" : m.group())
                            + "\" in: \"" + said + "\". The count is known when the step is"
                            + " emitted, so the sentence can say \"1 step\" or \"4 steps\" rather"
                            + " than hedging. Narration.s / Narration.plural do it in one call —"
                            + " and check the VERB too: \"1 subarray sums\", not \"1 subarray sum\".");
        }
    }

    @ParameterizedTest(name = "{0} writes ordinals as ordinals")
    @MethodSource("tracerIds")
    @DisplayName("No trace writes \"2-th\" where it means \"2nd\"")
    void ordinalsAreSpelledOut(String id) {
        for (ExecutionStep step : defaults(id)) {
            String said = step.getDescription();
            if (said == null) {
                continue;
            }
            Matcher m = ORDINAL.matcher(said);
            assertTrue(!m.find(),
                    id + " step " + step.getStepNumber() + " writes \"" + (m.hitEnd() ? "" : m.group())
                            + "\" in: \"" + said + "\". Use Narration.ordinal(n) — \"the 2-th"
                            + " largest\" reads like an unfinished format string.");
        }
    }
}
