package com.dsa.ui.catalog;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.tracer.AlgorithmTracer;
import com.dsa.ui.tracer.ExecutionTrace;
import com.dsa.ui.tracer.TraceRunner;
import com.dsa.ui.tracer.TracerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every full statement is honest about the code that runs it:
 *
 * <ul>
 *   <li>it belongs to a catalogued, traced problem;</li>
 *   <li>every example's input is accepted by that tracer's own InputSpec, and running it for real
 *       ends with {@code answerVariable} equal to the declared {@code output} - so an example can
 *       never claim an answer the visualizer would not produce;</li>
 *   <li>it carries prose, at least one example and an https link to the original problem;</li>
 *   <li>the detail API actually serves it (REVIEW.md gate 1: assert the JSON, not the object).</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class StatementContractTest {

    @Autowired StatementCatalog statements;
    @Autowired ProblemCatalog catalog;
    @Autowired TracerRegistry tracers;
    @Autowired TraceRunner runner;
    @Autowired MockMvc mvc;

    @Test
    void everyExampleOutputIsWhatTheTracerActuallyComputes() {
        assertThat(statements.all()).as("statement files loaded").isNotEmpty();
        List<String> failures = new ArrayList<>();
        statements.all().forEach((id, st) -> {
            if (catalog.find(id).isEmpty()) failures.add(id + ": not in the catalogue");
            AlgorithmTracer tracer = tracers.find(id).orElse(null);
            if (tracer == null) {
                failures.add(id + ": no tracer");
                return;
            }
            if (st.statement() == null || st.statement().isEmpty()) failures.add(id + ": empty statement");
            if (st.examples() == null || st.examples().isEmpty()) failures.add(id + ": no examples");
            if (st.sources() == null || st.sources().isEmpty()
                    || st.sources().stream().anyMatch(s -> s.url() == null || !s.url().startsWith("https://"))) {
                failures.add(id + ": needs https source links");
            }
            for (int i = 0; st.examples() != null && i < st.examples().size(); i++) {
                ProblemStatement.Example example = st.examples().get(i);
                try {
                    ExecutionTrace trace = runner.run(tracer, example.input());
                    List<ExecutionStep> steps = trace.getSteps();
                    String actual = steps.get(steps.size() - 1).getVariables().get(example.answerVariable());
                    if (!example.output().equals(actual)) {
                        failures.add(id + " example " + (i + 1) + ": declares " + example.output()
                                + " but the tracer's " + example.answerVariable() + " is " + actual);
                    }
                } catch (RuntimeException e) {
                    failures.add(id + " example " + (i + 1) + ": input rejected or failed - " + e.getMessage());
                }
            }
        });
        assertThat(failures).isEmpty();
    }

    @Test
    void theDetailEndpointServesTheStatement() throws Exception {
        mvc.perform(get("/api/problems/num-provinces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statement[0]").value(org.hamcrest.Matchers.startsWith("There are n cities")))
                .andExpect(jsonPath("$.examples[0].output").value("2"))
                .andExpect(jsonPath("$.examples[0].input.isConnected[0][1]").value(1))
                .andExpect(jsonPath("$.constraints[0]").value("1 ≤ n ≤ 200"))
                .andExpect(jsonPath("$.sources[0].label").value("LeetCode 547"));
    }
}
