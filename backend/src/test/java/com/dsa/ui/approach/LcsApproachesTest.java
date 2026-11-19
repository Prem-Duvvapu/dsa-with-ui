package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest @AutoConfigureMockMvc
class LcsApproachesTest {
    private static final String ID = "longest-common-subsequence", ENDPOINT = "/api/problems/" + ID;
    private static final List<String> FORMS = List.of("recursion", "memoization", "canonical");
    private static final AtomicInteger CLIENTS = new AtomicInteger();
    private final ObjectMapper json = new ObjectMapper();
    @Autowired SolutionApproachRegistry approaches;
    @Autowired TracerRegistry canonical;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;
    @TempDir Path temporary;

    @Test void registrationPreservesTheCanonicalExecutableAndAddsTwoRealAlternatives() throws Exception {
        assertEquals(new HashSet<>(FORMS), new HashSet<>(approaches.available(ID).stream().map(SolutionApproach::id).toList()));
        assertSame(canonical.find(ID).orElseThrow(), approaches.resolve(ID, null).tracer());
        assertEquals("Tabulation", approaches.resolve(ID, null).label());
        assertEquals(SolutionSource.read("/solutions/dp/" + ID + ".java"), tracer("canonical").annotatedCode());
        for (String form : FORMS) {
            var tracer = tracer(form);
            assertEquals(form.equals("canonical") ? DsType.DP_TABLE : DsType.RECURSION_TREE, tracer.dsType());
            var detail = json.readTree(request(get(ENDPOINT).param("approach", form), 200));
            assertEquals(3, detail.path("approaches").size());
            for (String encoding : List.of("full", "delta")) {
                var body = json.readTree(request(get(ENDPOINT + "/execute").param("approach", form).param("encoding", encoding), 200));
                assertEquals(ID, body.path("problemId").asText());
                assertEquals(form, body.path("approachId").asText());
                assertEquals(detail.path("javaCode"), body.path("code"));
                assertEquals(detail.path("complexity"), body.path("complexity"));
                assertEquals(detail.path("dsType"), body.path("dsType"));
                assertFalse(body.path("truncated").asBoolean());
            }
        }
    }

    @Test void specsHaveExplicitOwnDefaultsAndPreserveTheOriginalCanonicalDomain() {
        for (String form : FORMS) {
            var spec = tracer(form).inputSpec();
            int cap = form.equals("canonical") ? 12 : form.equals("memoization") ? 8 : 4;
            assertEquals(List.of("first", "second"), spec.getFields().stream().map(InputField::getName).toList());
            for (var field : spec.getFields()) {
                assertEquals(FieldType.STRING, field.getType());
                assertEquals(Map.of("minLength", 1, "maxLength", cap, "pattern", "[a-z]+", "patternHint", "Lowercase letters a-z only."), field.getConstraints());
            }
            assertEquals(form.equals("recursion") ? input("abcd", "acd") : input("abcde", "ace"), InputValidator.defaults(spec).asMap());
            assertEquals(form.equals("recursion") ? input("abc", "xyz") : input("aggtab", "gxtxayb"), tracer(form).alternateInput());
            assertEquals(InputSpec.DEFAULT_MAX_STEPS, spec.getMaxSteps());
            assertEquals(InputSpec.DEFAULT_MAX_BYTES, spec.getMaxBytes());
        }
    }

    @Test void exactDisplayedJavaAndTracersAgreeWithAnIndependentSubsequenceOracle() throws Exception {
        for (String form : FORMS) {
            var tracer = tracer(form);
            String source = AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode();
            Path dir = Files.createTempDirectory(temporary, "lcs"), file = dir.resolve("Solution.java");
            Files.writeString(file, source);
            var errors = new ByteArrayOutputStream();
            assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, errors, "--release", "17", "-classpath", dir.toString(), "-d", dir.toString(), file.toString()), errors::toString);
            try (var loader = new URLClassLoader(new java.net.URL[]{dir.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                var type = loader.loadClass("Solution"); var solve = type.getMethod("solve", String.class, String.class);
                assertEquals(form.equals("canonical") ? long.class : int.class, solve.getReturnType());
                Object instance = type.getConstructor().newInstance();
                var probes = probes(form);
                probes.add(InputValidator.defaults(tracer.inputSpec()).asMap()); probes.add(tracer.alternateInput());
                for (var supplied : probes) {
                    int expected = oracle(first(supplied), second(supplied));
                    assertAnswer(form, supplied);
                    assertEquals(expected, ((Number) solve.invoke(instance, first(supplied), second(supplied))).intValue());
                }
                for (var supplied : List.of(input("", ""), input("", "abc"), input("abc", ""))) {
                    assertEquals(0, ((Number) solve.invoke(instance, first(supplied), second(supplied))).intValue());
                }
                assertEquals(oracle("abababab", "babababa"), ((Number) solve.invoke(instance, "abababab", "babababa")).intValue());
            }
        }
    }

    @Test void everyRealCallHasTheRightChildrenReturnValueAndStackAndMemoZeroIsKnown() {
        for (String form : List.of("recursion", "memoization")) {
            var visited = new HashSet<Integer>();
            for (var supplied : List.of(input("aaaa", "aaaa"), input("aaaa", "bbbb"), input("abab", "baba"))) {
                var trace = checkCalls(form, supplied);
                for (var step : trace.getSteps()) visited.add(step.getActiveLine());
            }
            assertEquals(new HashSet<>(AnnotatedCode.parse(tracer(form).annotatedCode()).getAnchors().values()), visited);
            if (form.equals("memoization")) {
                var trace = checkCalls(form, input("aaaaaaaa", "bbbbbbbb"));
                assertTrue(trace.getSteps().stream().anyMatch(step -> "cache-hit".equals(step.getVariables().get("event")) && "0".equals(step.getVariables().get("value"))));
                var equal = checkCalls(form, input("aaaa", "aaaa"));
                assertEquals("5", last(equal).get("computedStates"));
                assertEquals("·", equal.getSteps().get(equal.getStepCount() - 1).getDpTable().cells().get(0).get(1).value(), "Unvisited base cells stay unknown");
            }
        }
    }

    @Test void measuredRecursionAndMemoMaximaCompleteWithinBothOriginalTransportBudgets() throws Exception {
        for (String form : List.of("recursion", "memoization")) {
            boolean memo = form.equals("memoization");
            var supplied = input("a".repeat(memo ? 8 : 4), "b".repeat(memo ? 8 : 4));
            var trace = checkCalls(form, supplied);
            assertEquals(memo ? 129 : 139, Integer.parseInt(last(trace).get("calls")));
            assertEquals(memo ? 453 : 488, trace.getStepCount());
            byte[] fullBytes = execute(form, "full", supplied), deltaBytes = execute(form, "delta", supplied);
            assertTrue(fullBytes.length < InputSpec.DEFAULT_MAX_BYTES, form + " full bytes=" + fullBytes.length);
            assertTrue(deltaBytes.length < InputSpec.DEFAULT_MAX_BYTES, form + " delta bytes=" + deltaBytes.length);
            var full = json.readTree(fullBytes); var delta = json.readTree(deltaBytes);
            for (var body : List.of(full, delta)) {
                assertFalse(body.path("truncated").asBoolean());
                assertEquals(trace.getStepCount(), body.path("stepCount").asInt());
                assertEquals(json.valueToTree(supplied), body.path("resolvedInput"));
                assertEquals(InputSpec.DEFAULT_MAX_STEPS, body.path("maxSteps").asInt());
                assertEquals(InputSpec.DEFAULT_MAX_BYTES, body.path("maxBytes").asLong());
            }
            assertEquals(full.path("steps"), decode(delta.path("steps"), full.path("steps")));
        }
    }

    @Test void invalidAndOversizedDraftsAreRefusedWithoutStepsAndEmitterTruncationHasNoAnswer() throws Exception {
        for (String form : List.of("recursion", "memoization")) {
            var actual = tracer(form); int cap = form.equals("recursion") ? 4 : 8;
            for (String bad : List.of("", "A", "a1", "a ", "a".repeat(cap + 1))) {
                var supplied = input(bad, "a");
                assertTrue(assertThrows(InputValidationException.class, () -> runner.run(actual, supplied)).getFieldErrors().containsKey("first"));
                var body = json.readTree(request(post(ENDPOINT + "/execute").param("approach", form).contentType("application/json").content(json.writeValueAsString(supplied)), 400));
                assertTrue(body.path("fieldErrors").path("first").isTextual()); assertFalse(body.has("steps"));
            }
            var extra = new HashMap<>(actual.alternateInput()); extra.put("invented", 1);
            assertThrows(InputValidationException.class, () -> runner.run(actual, extra));
            AlgorithmTracer tiny = new AlgorithmTracer() {
                public String id() { return actual.id(); } public DsType dsType() { return actual.dsType(); }
                public InputSpec inputSpec() { return actual.inputSpec().withMaxSteps(3); }
                public Map<String,Object> alternateInput() { return actual.alternateInput(); }
                public String annotatedCode() { return actual.annotatedCode(); }
                public void run(Inputs in, StepEmitter emit) { actual.run(in, emit); }
            };
            var trace = runner.runDefaults(tiny); assertTrue(trace.isTruncated()); assertEquals(3, trace.getStepCount());
            for (var step : trace.getSteps()) { assertFalse(step.getVariables().containsKey("answer")); assertNotEquals(trace.getAnchors().get("done").intValue(), step.getActiveLine()); }
        }
    }

    private static final class Call {
        int i, j; boolean hit; final List<Integer> children = new ArrayList<>();
        Call(int i, int j) { this.i = i; this.j = j; }
        String key() { return i + "," + j; }
    }
    private ExecutionTrace checkCalls(String form, Map<String,Object> supplied) {
        var trace = assertAnswer(form, supplied); boolean memo = form.equals("memoization");
        String a = first(supplied), b = second(supplied); var code = AnnotatedCode.parse(tracer(form).annotatedCode());
        var active = new ArrayList<Integer>(); var calls = new HashMap<Integer,Call>(); var known = new HashMap<String,Integer>();
        int entered = 0, hits = 0, evaluations = 0; var computed = new HashSet<String>();
        for (var step : trace.getSteps()) {
            assertTrue(step.getDescription().length() <= 256); assertTrue(step.getDescription().chars().allMatch(ch -> ch < 128));
            var vars = step.getVariables(); String event = vars.get("event");
            var state = java.util.regex.Pattern.compile("i=(\\d+),j=(\\d+)").matcher(vars.get("state")); assertTrue(state.matches());
            int i = Integer.parseInt(state.group(1)), j = Integer.parseInt(state.group(2));
            int id = vars.containsKey("callId") ? Integer.parseInt(vars.get("callId")) : -1;
            if (event.equals("enter")) {
                assertEquals(++entered, id); var call = new Call(i,j); calls.put(id, call);
                if (!active.isEmpty()) calls.get(active.get(active.size()-1)).children.add(id);
                active.add(id);
            } else if (event.equals("cache-hit")) {
                assertTrue(memo); assertEquals(known.get(i+","+j), Integer.valueOf(vars.get("value"))); calls.get(id).hit = true; hits++;
            } else if (Set.of("base","store","combine").contains(event)) {
                int expected = oracle(a.substring(0,i), b.substring(0,j)); assertEquals(String.valueOf(expected), vars.get("value"));
                if (memo) assertNull(known.putIfAbsent(i+","+j,expected), "Compute each memo state once");
                computed.add(i+","+j); evaluations++;
            } else if (event.equals("return")) {
                assertEquals(Integer.valueOf(id), active.remove(active.size()-1));
                assertEquals(String.valueOf(oracle(a.substring(0,i),b.substring(0,j))),vars.get("value"));
            }
            assertEquals(active.size(),step.getCallStack().size(),event);
            for (int depth=0;depth<active.size();depth++) {
                int callId=active.get(depth); var call=calls.get(callId);
                assertEquals("lcs(i="+call.i+",j="+call.j+") #"+callId,step.getCallStack().get(depth));
            }
            assertEquals(String.valueOf(entered),vars.get("calls")); assertEquals(String.valueOf(hits),vars.get("cacheHits"));
            assertEquals(String.valueOf(evaluations),vars.get("stateEvaluations")); assertEquals(String.valueOf(computed.size()),vars.get("computedStates"));
            String anchor=switch(event) { case "store","combine" -> "combine"; case "cache-hit" -> "hit"; case "return" -> calls.get(id).hit?"hit":i==0||j==0?"base":"combine"; default -> event; };
            assertEquals(code.resolve(anchor),step.getActiveLine());
            if (memo) {
                var table=step.getDpTable(); assertNotNull(table);
                assertEquals(RecursiveDpExecution.labels("i=",a.length()+1),table.rowLabels()); assertEquals(RecursiveDpExecution.labels("j=",b.length()+1),table.colLabels());
                for(int r=0;r<=a.length();r++) for(int c=0;c<=b.length();c++) {
                    var cell=table.cells().get(r).get(c); Integer value=known.get(r+","+c);
                    assertEquals(value==null?"·":String.valueOf(value),cell.value());
                    if(value==null)assertEquals("void",cell.state());
                }
            } else assertNull(step.getDpTable());
        }
        assertTrue(active.isEmpty());
        for(var call:calls.values()) {
            var expected=new ArrayList<String>();
            if(!call.hit&&call.i>0&&call.j>0) {
                if(a.charAt(call.i-1)==b.charAt(call.j-1))expected.add((call.i-1)+","+(call.j-1));
                else { expected.add((call.i-1)+","+call.j); expected.add(call.i+","+(call.j-1)); }
            }
            assertEquals(expected,call.children.stream().map(child->calls.get(child).key()).toList());
        }
        return trace;
    }
    private ExecutionTrace assertAnswer(String form, Map<String,Object> supplied) {
        var trace=runner.run(tracer(form),supplied); assertFalse(trace.isTruncated()); assertEquals(supplied,trace.getResolvedInput());
        assertEquals(String.valueOf(oracle(first(supplied),second(supplied))),last(trace).get("answer")); return trace;
    }
    private AlgorithmTracer tracer(String form) { return approaches.resolve(ID,form).tracer(); }
    private static Map<String,String> last(ExecutionTrace trace) { return trace.getSteps().get(trace.getStepCount()-1).getVariables(); }
    private static String first(Map<String,Object> input) { return (String)input.get("first"); }
    private static String second(Map<String,Object> input) { return (String)input.get("second"); }
    private static Map<String,Object> input(String first,String second) { return Map.of("first",first,"second",second); }
    private static int oracle(String a,String b) {
        Set<String> all=subsequences(a); int best=0;
        for(String word:subsequences(b))if(all.contains(word))best=Math.max(best,word.length()); return best;
    }
    private static Set<String> subsequences(String s) {
        var result=new HashSet<String>();
        for(int mask=0;mask<(1<<s.length());mask++){var word=new StringBuilder();for(int i=0;i<s.length();i++)if((mask&(1<<i))!=0)word.append(s.charAt(i));result.add(word.toString());}return result;
    }
    private static List<Map<String,Object>> probes(String form) {
        var words=new ArrayList<String>();for(int n=1;n<=3;n++)for(int mask=0;mask<(1<<n);mask++){var w=new StringBuilder();for(int i=0;i<n;i++)w.append((mask&(1<<i))==0?'a':'b');words.add(w.toString());}
        var result=new ArrayList<Map<String,Object>>();for(String a:words)for(String b:words)result.add(input(a,b));
        var random=new Random(114319);int cap=form.equals("recursion")?4:8;
        for(int sample=0;sample<30;sample++){var a=new StringBuilder();var b=new StringBuilder();int m=1+random.nextInt(cap),n=1+random.nextInt(cap);for(int i=0;i<m;i++)a.append((char)('a'+random.nextInt(4)));for(int j=0;j<n;j++)b.append((char)('a'+random.nextInt(4)));result.add(input(a.toString(),b.toString()));}return result;
    }
    private byte[] execute(String form,String encoding,Map<String,Object> input) throws Exception { return request(post(ENDPOINT+"/execute").param("approach",form).param("encoding",encoding).contentType("application/json").content(json.writeValueAsString(input)),200); }
    private byte[] request(MockHttpServletRequestBuilder request,int expectedStatus) throws Exception {
        String client="lcs-approach-"+CLIENTS.incrementAndGet();var result=http.perform(request.with(r->{r.setRemoteAddr(client);return r;})).andReturn();
        assertEquals(client,result.getRequest().getRemoteAddr());assertEquals(expectedStatus,result.getResponse().getStatus(),result.getResponse().getContentAsString());return result.getResponse().getContentAsByteArray();
    }
    private JsonNode decode(JsonNode deltas,JsonNode full) {
        var result=json.createArrayNode();ObjectNode snapshot=json.createObjectNode();var fields=new HashSet<String>();for(var step:full)step.fieldNames().forEachRemaining(fields::add);
        for(var delta:deltas){if(delta.path("keyframe").asBoolean()){snapshot=json.createObjectNode();for(String field:fields)snapshot.set(field,NullNode.instance);}var entries=delta.fields();while(entries.hasNext()){var e=entries.next();if(!e.getKey().equals("keyframe"))snapshot.set(e.getKey(),e.getValue());}result.add(snapshot.deepCopy());}return result;
    }
}
