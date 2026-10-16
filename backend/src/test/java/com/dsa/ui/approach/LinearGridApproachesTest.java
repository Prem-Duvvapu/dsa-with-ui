package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class LinearGridApproachesTest {
    @Autowired SolutionApproachRegistry approaches;
    @Autowired TracerRegistry canonical;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;
    @TempDir Path temporary;
    private final ObjectMapper json = new ObjectMapper();
    private static final List<String> FORMS = List.of("recursion", "memoization", "canonical");

    @ParameterizedTest @ValueSource(strings={"frog-jump-k-distance", "max-sum-non-adjacent", "grid-unique-paths"})
    void allThreeAreServedAndKeepTheExactCanonicalExecutable(String problem) throws Exception {
        assertEquals(new HashSet<>(FORMS), new HashSet<>(approaches.available(problem).stream().map(SolutionApproach::id).toList()));
        assertSame(canonical.find(problem).orElseThrow(), approaches.resolve(problem, null).tracer());
        assertEquals("canonical", approaches.resolve(problem, null).id());
        assertEquals("Tabulation", approaches.resolve(problem, null).label());
        for (String form : FORMS) {
            var approach = approaches.resolve(problem, form);
            assertNotNull(approach.teaching());
            assertEquals(form.equals("canonical") ? DsType.DP_TABLE : DsType.RECURSION_TREE, approach.tracer().dsType());
            var detail = json.readTree(http.perform(get("/api/problems/{id}", problem).param("approach", form))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            assertEquals(3, detail.path("approaches").size());
            assertFalse(detail.path("teaching").path("state").asText().isBlank());
            for (String encoding : List.of("full", "delta")) {
                var response = json.readTree(http.perform(get("/api/problems/{id}/execute", problem)
                                .param("approach", form).param("encoding", encoding)
                                .with(r -> { r.setRemoteAddr("linear-grid-" + problem + form); return r; }))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
                assertEquals(problem, response.path("problemId").asText());
                assertEquals(form, response.path("approachId").asText());
                assertEquals(detail.path("javaCode"), response.path("code"));
                assertEquals(detail.path("dsType"), response.path("dsType"));
                assertEquals(detail.path("complexity"), response.path("complexity"));
                assertFalse(response.path("truncated").asBoolean());
            }
        }
    }

    @ParameterizedTest @ValueSource(strings={"frog-jump-k-distance", "max-sum-non-adjacent", "grid-unique-paths"})
    void everyFormAndExactDisplayedSourceMatchesIndependentEnumeration(String problem) throws Exception {
        for (String form : FORMS) {
            var tracer = approaches.resolve(problem, form).tracer();
            String code = AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode();
            // The unchanged canonical listing is a complete method, with no missing helpers.
            String unit = form.equals("canonical") ? "public class Solution {\n" + code + "\n}" : code;
            Path directory = Files.createTempDirectory(temporary, "solution");
            Path file = directory.resolve("Solution.java");
            Files.writeString(file, unit);
            var errors = new ByteArrayOutputStream();
            assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, errors, "--release", "17",
                    "-classpath", directory.toString(), "-d", directory.toString(), file.toString()), errors::toString);
            try (var loader = new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                Class<?> solution = loader.loadClass("Solution");
                var method = Arrays.stream(solution.getDeclaredMethods()).filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                        .filter(m -> !m.isSynthetic()).findFirst().orElseThrow();
                assertEquals(int.class, method.getReturnType());
                for (var input : cases(problem)) {
                    assertAnswer(tracer, input);
                    Object[] args = switch (problem) {
                        case "frog-jump-k-distance" -> form.equals("canonical")
                                ? new Object[]{((List<?>) input.get("heights")).size(), input.get("k"), array(input, "heights")}
                                : new Object[]{array(input, "heights"), input.get("k")};
                        case "max-sum-non-adjacent" -> new Object[]{array(input, "nums")};
                        default -> new Object[]{input.get("rows"), input.get("cols")};
                    };
                    assertEquals(oracle(problem, input), method.invoke(solution.getConstructor().newInstance(), args), problem + " " + input);
                }
            }
        }
    }

    @ParameterizedTest @ValueSource(strings={"frog-jump-k-distance", "max-sum-non-adjacent", "grid-unique-paths"})
    void largestDeclaredInputsCompleteAndOneOverIsRefusedWithoutTrace(String problem) throws Exception {
        for (String form : FORMS) {
            var tracer = approaches.resolve(problem, form).tracer();
            Map<String,Object> maximum = maximum(problem, form);
            var trace = assertAnswer(tracer, maximum);
            if (!form.equals("canonical")) {
                int expectedCalls = problem.equals("frog-jump-k-distance") ? form.equals("recursion") ? 128 : 93
                        : problem.equals("max-sum-non-adjacent") ? form.equals("recursion") ? 177 : 39
                        : form.equals("recursion") ? 39 : 99;
                assertEquals(expectedCalls, Integer.parseInt(last(trace).get("calls")), "Do not replace the advertised recursive work with a shortcut");
                assertTrue(expectedCalls <= 220, "Every call must fit the renderer");
            }
            Map<String,Object> over = new LinkedHashMap<>(maximum);
            String field = problem.equals("grid-unique-paths") ? "rows" : problem.equals("frog-jump-k-distance") ? "heights" : "nums";
            if (problem.equals("grid-unique-paths")) over.put(field, (int) maximum.get(field) + 1);
            else { var values = new ArrayList<>((List<Integer>) maximum.get(field)); values.add(0); over.put(field, values); }
            assertThrows(InputValidationException.class, () -> runner.run(tracer, over));
            for (var invalid : invalidInputs(problem, maximum)) {
                assertThrows(InputValidationException.class, () -> runner.run(tracer, invalid), invalid.toString());
            }
            http.perform(post("/api/problems/{id}/execute", problem).param("approach", form)
                            .contentType("application/json").content(json.writeValueAsString(over)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors." + field).isString())
                    .andExpect(jsonPath("$.steps").doesNotExist());
        }
    }

    @ParameterizedTest @ValueSource(strings={"frog-jump-k-distance", "max-sum-non-adjacent", "grid-unique-paths"})
    void memoActuallyReusesStatesIncludingZeroAndEveryAnchorIsReachable(String problem) {
        for (String form : FORMS) {
            var tracer = approaches.resolve(problem, form).tracer();
            var input = problem.equals("grid-unique-paths") ? Map.<String,Object>of("rows", 4, "cols", 4)
                    : problem.equals("frog-jump-k-distance") ? Map.<String,Object>of("heights", Collections.nCopies(7, 0), "k", 3)
                    : Map.<String,Object>of("nums", Collections.nCopies(7, 0));
            var run = assertAnswer(tracer, input);
            var code = AnnotatedCode.parse(tracer.annotatedCode());
            assertEquals(new HashSet<>(code.getAnchors().values()), new HashSet<>(run.getSteps().stream().map(s -> s.getActiveLine()).toList()));
            if (form.equals("canonical")) continue;
            var enters = new HashSet<String>();
            var computed = new HashSet<String>();
            int hits = 0;
            for (int i = 0; i < run.getSteps().size(); i++) {
                var step = run.getSteps().get(i);
                var vars = step.getVariables();
                String event = vars.getOrDefault("event", "");
                if (event.equals("enter")) {
                    assertTrue(enters.add(vars.get("callId")));
                    String function = problem.equals("frog-jump-k-distance") ? "energy" : problem.equals("grid-unique-paths") ? "paths" : "best";
                    assertTrue(step.getCallStack().get(step.getCallStack().size()-1).startsWith(function + "("), "Show the actual source function name");
                }
                if (form.equals("recursion")) assertNull(step.getDpTable());
                else {
                    assertNotNull(step.getDpTable());
                    if (Set.of("base", "store").contains(event)) assertTrue(computed.add(vars.get("state")), "Compute each memo state once");
                    if (event.equals("cache-hit")) {
                        hits++;
                        assertEquals("return", run.getSteps().get(i + 1).getVariables().get("event"));
                        assertEquals(vars.get("callId"), run.getSteps().get(i + 1).getVariables().get("callId"));
                        if (!problem.equals("grid-unique-paths")) assertEquals("0", vars.get("value"));
                    }
                }
            }
            assertEquals(enters.size(), Integer.parseInt(last(run).get("calls")));
            if (form.equals("memoization")) { assertTrue(hits > 0); assertEquals(hits, Integer.parseInt(last(run).get("cacheHits"))); }
            else assertEquals("0", last(run).get("cacheHits"));
        }
    }

    @ParameterizedTest @ValueSource(strings={"frog-jump-k-distance", "max-sum-non-adjacent", "grid-unique-paths"})
    void tinyBudgetStopsActualExpansionWithoutClaimingAnAnswer(String problem) {
        for (String form : List.of("recursion", "memoization")) {
            var actual = approaches.resolve(problem, form).tracer();
            var capped = new AlgorithmTracer() {
                public String id() { return actual.id(); }
                public DsType dsType() { return actual.dsType(); }
                public String annotatedCode() { return actual.annotatedCode(); }
                public InputSpec inputSpec() { return actual.inputSpec().withMaxSteps(3); }
                public Map<String,Object> alternateInput() { return actual.alternateInput(); }
                public void run(Inputs in, StepEmitter out) { actual.run(in, out); }
            };
            var trace = runner.run(capped, maximum(problem, form));
            assertTrue(trace.isTruncated());
            assertEquals(3, trace.getStepCount());
            assertTrue(trace.getSteps().stream().noneMatch(s -> s.getVariables().containsKey("answer")));
        }
    }

    private ExecutionTrace assertAnswer(AlgorithmTracer tracer, Map<String,Object> input) {
        var trace = runner.run(tracer, input);
        assertFalse(trace.isTruncated(), tracer.id() + input);
        assertEquals(String.valueOf(oracle(tracer.id(), input)), last(trace).get("answer"), input.toString());
        assertTrue(trace.getSteps().get(trace.getStepCount()-1).getCallStack().isEmpty());
        return trace;
    }
    private static Map<String,String> last(ExecutionTrace t) { return t.getSteps().get(t.getStepCount()-1).getVariables(); }
    private static List<Map<String,Object>> invalidInputs(String problem, Map<String,Object> maximum) {
        if (problem.equals("grid-unique-paths")) return List.of(Map.of("rows",0,"cols",1),
                Map.of("rows",1,"cols",0), Map.of("rows",1,"cols",(int)maximum.get("cols")+1));
        if (problem.equals("frog-jump-k-distance")) return List.of(Map.of("heights",List.of(0,1),"k",0),
                Map.of("heights",List.of(0,1),"k",9), Map.of("heights",List.of(0),"k",1),
                Map.of("heights",List.of(-1,0),"k",1), Map.of("heights",List.of(0,1000),"k",1));
        return List.of(Map.of("nums",List.of()),Map.of("nums",List.of(-1)),Map.of("nums",List.of(1000)));
    }
    private static int[] array(Map<String,Object> in, String field) { return ((List<Integer>)in.get(field)).stream().mapToInt(Integer::intValue).toArray(); }
    static Map<String,Object> maximum(String problem, String form) {
        if (problem.equals("grid-unique-paths")) return Map.of("rows", form.equals("recursion") ? 4 : 8, "cols", form.equals("recursion") ? 4 : 8);
        int length = problem.equals("frog-jump-k-distance") ? form.equals("recursion") ? 8 : 16 : form.equals("recursion") ? 10 : 20;
        var nums = new ArrayList<Integer>();
        for (int i=0; i<length; i++) nums.add(i%2==0 ? 0 : 999);
        return problem.equals("frog-jump-k-distance") ? Map.of("heights", nums, "k", 8) : Map.of("nums", nums);
    }
    private static List<Map<String,Object>> cases(String problem) {
        var result = new ArrayList<Map<String,Object>>();
        if (problem.equals("grid-unique-paths")) {
            for(int rows=1;rows<=4;rows++) for(int cols=1;cols<=4;cols++) result.add(Map.of("rows",rows,"cols",cols));
        } else {
            for(int n=problem.equals("frog-jump-k-distance")?2:1;n<=5;n++) {
                int count=(int)Math.pow(3,n);
                for(int mask=0;mask<count;mask++) {
                    var nums=new ArrayList<Integer>(); int x=mask;
                    for(int i=0;i<n;i++,x/=3) nums.add(new int[]{0,1,3}[x%3]);
                    if(problem.equals("frog-jump-k-distance")) for(int k=1;k<=4;k++) result.add(Map.of("heights",nums,"k",k));
                    else result.add(Map.of("nums",nums));
                }
            }
        }
        return result;
    }
    // Independent forward path enumeration / index-subset enumeration / binomial formula.
    private static int oracle(String problem, Map<String,Object> in) {
        if(problem.equals("grid-unique-paths")) {
            int r=(int)in.get("rows")-1,c=(int)in.get("cols")-1; long answer=1;
            for(int i=1;i<=r;i++) answer=answer*(c+i)/i;
            return (int)answer;
        }
        if(problem.equals("frog-jump-k-distance")) return jump(array(in,"heights"),(int)in.get("k"),0,0);
        int[] nums=array(in,"nums"); int best=0;
        for(int mask=0;mask<(1<<nums.length);mask++) {
            if((mask & (mask<<1))!=0) continue;
            int sum=0; for(int i=0;i<nums.length;i++) if((mask & (1<<i))!=0) sum+=nums[i];
            best=Math.max(best,sum);
        }
        return best;
    }
    private static int jump(int[] heights,int k,int at,int spent) {
        if(at==heights.length-1) return spent;
        int best=Integer.MAX_VALUE;
        for(int d=1;d<=k && at+d<heights.length;d++) best=Math.min(best,jump(heights,k,at+d,spent+Math.abs(heights[at+d]-heights[at])));
        return best;
    }
}
