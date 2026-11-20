package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class LpsApproachesTest {
    private static final String ID = "longest-palindromic-subsequence";
    private static final String ENDPOINT = "/api/problems/" + ID;
    private static final List<String> FORMS = List.of("recursion", "memoization", "canonical");
    private static final AtomicInteger CLIENTS = new AtomicInteger();
    private final ObjectMapper json = new ObjectMapper();
    @Autowired SolutionApproachRegistry approaches;
    @Autowired TracerRegistry canonical;
    @Autowired ProblemCatalog catalog;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;
    @TempDir Path temporary;

    @Test void registrationPreservesTheCanonicalExecutableAndAddsTwoRealAlternatives() throws Exception {
        assertEquals(Set.of("recursion", "memoization", "canonical"),
                approaches.available(ID).stream().map(SolutionApproach::id).collect(Collectors.toSet()));
        assertSame(canonical.find(ID).orElseThrow(), approaches.resolve(ID, null).tracer());
        assertEquals("Tabulation", approaches.resolve(ID, null).label());
        assertEquals(SolutionSource.read("/solutions/dp/" + ID + ".java"), tracer("canonical").annotatedCode());
        assertEquals(Set.of("init", "fill", "done"), AnnotatedCode.parse(tracer("canonical").annotatedCode()).getAnchors().keySet());
        for (String form : FORMS) {
            var selected = approaches.resolve(ID, form);
            assertEquals(ID, selected.tracer().id());
            assertEquals(form.equals("canonical") ? DsType.DP_TABLE : DsType.RECURSION_TREE, selected.tracer().dsType());
            assertNotNull(selected.teaching());
            assertTrue(selected.teaching().state().contains("prefix"));
            var detail = json.readTree(request(get(ENDPOINT).param("approach", form), 200));
            assertEquals(3, detail.path("approaches").size());
            assertEquals(wire(selected.tracer().inputSpec()), detail.path("inputSpec"));
        }
    }

    @Test void canonicalMetadataDescribesTheActualSquareTable() throws Exception {
        var complexity = catalog.find(ID).orElseThrow().getProblem().getComplexity();
        assertEquals("O(N^2)", complexity.getTimeComplexity());
        assertEquals("O(N^2)", complexity.getSpaceComplexity());
        assertTrue(complexity.getSpaceExplanation().contains("(N+1)"));
        assertTrue(complexity.getSpaceExplanation().contains("O(N)"));
        assertTrue(complexity.getSpaceExplanation().contains("O(1)"));
        assertEquals(wire(complexity), wire(approaches.resolve(ID, null).complexity()));
        for (var query : List.of(get(ENDPOINT), get(ENDPOINT).param("approach", "canonical"))) {
            var detail = json.readTree(request(query, 200));
            assertEquals(wire(complexity), detail.path("complexity"));
            var nested = new ArrayList<JsonNode>();
            detail.path("approaches").forEach(a -> { if ("canonical".equals(a.path("id").asText())) nested.add(a); });
            assertEquals(1, nested.size());
            assertEquals(wire(complexity), nested.get(0).path("complexity"));
        }
    }

    @Test void allSpecsPreserveTheOriginalFieldExamplesAndGlobalBudgets() throws Exception {
        for (String form : FORMS) {
            var actual = tracer(form); var spec = actual.inputSpec();
            int cap = cap(form);
            assertEquals(List.of("text"), spec.getFields().stream().map(InputField::getName).toList());
            var field = spec.field("text");
            assertEquals(FieldType.STRING, field.getType());
            assertEquals("Text", field.getLabel());
            assertEquals(Map.of("minLength", 1, "maxLength", cap, "pattern", "[a-z]+", "patternHint", "Lowercase letters a-z only."), field.getConstraints());
            assertEquals(input("bbbab"), InputValidator.defaults(spec).asMap());
            assertEquals(input("cbbd"), actual.alternateInput());
            assertEquals(InputSpec.DEFAULT_MAX_STEPS, spec.getMaxSteps());
            assertEquals(InputSpec.DEFAULT_MAX_BYTES, spec.getMaxBytes());
            var served = json.readTree(request(get(ENDPOINT + "/input-spec").param("approach", form), 200));
            assertEquals(wire(spec), served);
        }
        var summaries = json.readTree(request(get("/api/problems"), 200));
        int found = 0;
        for (var summary : summaries) if (ID.equals(summary.path("id").asText())) {
            found++;
            assertEquals(wire(tracer("canonical").inputSpec()), summary.path("inputSpec"));
            assertEquals("DpTable", summary.path("dsType").asText());
        }
        assertEquals(1, found);
    }

    @Test void exactDisplayedJavaAndRuntimeAgreeWithIndependentPalindromicSubsequences() throws Exception {
        for (String form : FORMS) {
            var actual = tracer(form);
            var source = AnnotatedCode.parse(actual.annotatedCode()).getDisplayCode();
            Path directory = Files.createTempDirectory(temporary, "lps"), file = directory.resolve("Solution.java");
            Files.writeString(file, source);
            var errors = new ByteArrayOutputStream();
            assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, errors,
                    "--release", "17", "-classpath", directory.toString(), "-d", directory.toString(), file.toString()), errors::toString);
            try (var loader = new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                var type = loader.loadClass("Solution"); var solve = type.getMethod("solve", String.class);
                assertEquals(form.equals("canonical") ? long.class : int.class, solve.getReturnType());
                Object instance = type.getConstructor().newInstance();
                for (String text : probes(form)) {
                    int expected = palindromeOracle(text);
                    assertAnswer(form, input(text));
                    assertEquals(expected, ((Number) solve.invoke(instance, text)).intValue(), form + ": " + text);
                }
                assertEquals(0, ((Number) solve.invoke(instance, "")).intValue(), "Public source supports its empty-string base, though UI remains nonempty");
                for (String text : List.of("bbbab", "cbbd", "abcdefgh", "abbbbbbc", "abcba", "aaaaaaaaaaaa")) {
                    assertEquals(palindromeOracle(text), ((Number) solve.invoke(instance, text)).intValue());
                }
            }
        }
    }

    @Test void realCallsUseReversedPrefixesActualChildrenReturnsStackAndKnownMemoZeros() {
        for (String form : List.of("recursion", "memoization")) {
            var reached = new HashSet<Integer>();
            for (String text : List.of("aaaaa", "abcde", "bbbab", "cbbd", "abcba")) {
                var trace = checkCalls(form, input(text));
                trace.getSteps().forEach(step -> reached.add(step.getActiveLine()));
            }
            assertEquals(new HashSet<>(AnnotatedCode.parse(tracer(form).annotatedCode()).getAnchors().values()), reached);
        }
        var zeroHits = checkCalls("memoization", input("abbccdde"));
        assertTrue(zeroHits.getSteps().stream().anyMatch(step -> "cache-hit".equals(step.getVariables().get("event"))
                && "0".equals(step.getVariables().get("value"))));
        var diagonal = checkCalls("memoization", input("aaaaaaaa"));
        assertEquals("9", last(diagonal).get("computedStates"));
        var cell = diagonal.getSteps().get(diagonal.getStepCount() - 1).getDpTable().cells().get(0).get(1);
        assertEquals("·", cell.value()); assertEquals("void", cell.state(), "Unvisited base cells remain unknown");
        var dense = checkCalls("memoization", input("abbbbbbc"));
        assertEquals("76", last(dense).get("computedStates"));
    }

    @Test void exactReverseCorrelatedMaximaCompleteUnderBothTransportAndRendererBudgets() throws Exception {
        for (String form : List.of("recursion", "memoization")) {
            boolean memo = form.equals("memoization");
            var supplied = input(memo ? "abbccdde" : "abcde");
            var trace = checkCalls(form, supplied);
            assertEquals(memo ? "107" : "155", last(trace).get("calls"));
            assertEquals(memo ? 383 : 552, trace.getStepCount());
            assertTrue(Integer.parseInt(last(trace).get("calls")) < 220, "All nodes fit existing renderer");
            assertTrue(trace.getSteps().stream().mapToLong(StepEmitter::estimateBytes).sum() < InputSpec.DEFAULT_MAX_BYTES);
            byte[] fullBytes = execute(form, "full", supplied), deltaBytes = execute(form, "delta", supplied);
            assertTrue(fullBytes.length < InputSpec.DEFAULT_MAX_BYTES, "full bytes=" + fullBytes.length);
            assertTrue(deltaBytes.length < InputSpec.DEFAULT_MAX_BYTES, "delta bytes=" + deltaBytes.length);
            var full = json.readTree(fullBytes); var delta = json.readTree(deltaBytes);
            for (var body : List.of(full, delta)) assertWire(body, form, supplied, trace.getStepCount());
            assertEquals(full.path("steps"), decode(delta.path("steps"), full.path("steps")));
        }
    }

    @Test void detailDefaultCustomNestedAndBothEncodingsKeepTheSameExecutableMetadata() throws Exception {
        for (String form : FORMS) {
            var detail = json.readTree(request(get(ENDPOINT).param("approach", form), 200));
            var selected = approaches.resolve(ID, form);
            assertEquals(wire(selected.complexity()), detail.path("complexity"));
            for (var nested : detail.path("approaches")) {
                var definition = approaches.resolve(ID, nested.path("id").asText());
                assertEquals(wire(definition.complexity()), nested.path("complexity"));
                assertEquals(wire(definition.tracer().inputSpec()), nested.path("inputSpec"));
            }
            for (String encoding : List.of("full", "delta")) {
                for (var supplied : List.of(input("bbbab"), input("cbbd"), input("a".repeat(cap(form))))) {
                    var full = runner.run(tracer(form), supplied);
                    var body = json.readTree(execute(form, encoding, supplied));
                    assertWire(body, form, supplied, full.getStepCount());
                    assertEquals(detail.path("complexity"), body.path("complexity"));
                }
                var defaultBody = json.readTree(request(get(ENDPOINT + "/execute").param("approach", form).param("encoding", encoding), 200));
                assertWire(defaultBody, form, input("bbbab"), runner.runDefaults(tracer(form)).getStepCount());
                assertEquals(detail.path("complexity"), defaultBody.path("complexity"));
            }
        }
    }

    @Test void fieldRefusalsAndUnknownApproachesNeverPublishSteps() throws Exception {
        for (String form : FORMS) {
            for (String bad : List.of("", "A", "a1", "a ", "a".repeat(cap(form) + 1))) {
                var supplied = input(bad);
                assertTrue(assertThrows(InputValidationException.class, () -> runner.run(tracer(form), supplied)).getFieldErrors().containsKey("text"));
                for (String encoding : List.of("full", "delta")) {
                    var body = json.readTree(request(post(ENDPOINT + "/execute").param("approach", form).param("encoding", encoding)
                            .contentType("application/json").content(json.writeValueAsString(supplied)), 400));
                    assertTrue(body.path("fieldErrors").path("text").isTextual()); assertFalse(body.has("steps"));
                }
            }
            var extra = new HashMap<>(input("abc")); extra.put("invented", 1);
            assertThrows(InputValidationException.class, () -> runner.run(tracer(form), extra));
            var body = json.readTree(request(post(ENDPOINT + "/execute").param("approach", form)
                    .contentType("application/json").content(json.writeValueAsString(extra)), 400));
            assertFalse(body.has("steps"));
        }
        for (var request : List.of(get(ENDPOINT), get(ENDPOINT + "/execute"), get(ENDPOINT + "/input-spec"), post(ENDPOINT + "/execute").contentType("application/json").content("{}"))) {
            var body = json.readTree(request(request.param("approach", "invented"), 400));
            assertEquals("unavailable_approach", body.path("error").asText()); assertFalse(body.has("steps"));
        }
    }

    @Test void eitherEmitterCeilingTruncatesHonestlyBeforeAnyDoneOrAnswer() {
        for (String form : FORMS) {
            var actual = tracer(form);
            for (var spec : List.of(actual.inputSpec().withMaxSteps(3), actual.inputSpec().withMaxBytes(1))) {
                AlgorithmTracer tiny = new AlgorithmTracer() {
                    public String id() { return actual.id(); }
                    public DsType dsType() { return actual.dsType(); }
                    public InputSpec inputSpec() { return spec; }
                    public Map<String,Object> alternateInput() { return actual.alternateInput(); }
                    public String annotatedCode() { return actual.annotatedCode(); }
                    public void run(Inputs in, StepEmitter emit) { actual.run(in, emit); }
                };
                var trace = runner.runDefaults(tiny);
                assertTrue(trace.isTruncated());
                assertEquals(spec.getMaxBytes() == 1 ? 0 : 3, trace.getStepCount());
                for (var step : trace.getSteps()) {
                    assertFalse(step.getVariables().containsKey("answer"));
                    assertNotEquals(trace.getAnchors().get("done").intValue(), step.getActiveLine());
                }
            }
        }
    }

    private static final class Call {
        final int i, j;
        boolean hit;
        final List<Integer> children = new ArrayList<>();
        Call(int i, int j) { this.i = i; this.j = j; }
        String key() { return i + "," + j; }
    }

    private ExecutionTrace checkCalls(String form, Map<String,Object> supplied) {
        var trace = assertAnswer(form, supplied); boolean memo = form.equals("memoization");
        String a = text(supplied), b = new StringBuilder(a).reverse().toString();
        var code = AnnotatedCode.parse(tracer(form).annotatedCode());
        var active = new ArrayList<Integer>(); var calls = new HashMap<Integer,Call>();
        var known = new HashMap<String,Integer>(); var expectedValues = new HashMap<String,Integer>();
        for (int i=0;i<=a.length();i++) for (int j=0;j<=b.length();j++) {
            expectedValues.put(i+","+j, prefixOracle(a.substring(0,i), b.substring(0,j)));
        }
        int entered=0,hits=0,evaluations=0; var computed = new HashSet<String>();
        for (var step : trace.getSteps()) {
            assertTrue(step.getDescription().length()<=256);
            assertTrue(step.getDescription().chars().allMatch(ch -> ch>=32 && ch<127));
            var vars = step.getVariables(); String event = vars.get("event");
            var state = java.util.regex.Pattern.compile("i=(\\d+),j=(\\d+)").matcher(vars.get("state"));
            assertTrue(state.matches());
            int i=Integer.parseInt(state.group(1)),j=Integer.parseInt(state.group(2));
            int id=vars.containsKey("callId")?Integer.parseInt(vars.get("callId")):-1;
            if (event.equals("enter")) {
                assertEquals(++entered,id); var call=new Call(i,j); calls.put(id,call);
                if(!active.isEmpty())calls.get(active.get(active.size()-1)).children.add(id);
                active.add(id);
            } else if(event.equals("cache-hit")) {
                assertTrue(memo); assertEquals(known.get(i+","+j),Integer.valueOf(vars.get("value")));
                calls.get(id).hit=true; hits++;
            } else if(Set.of("base","store","combine").contains(event)) {
                int expected=expectedValues.get(i+","+j); assertEquals(String.valueOf(expected),vars.get("value"));
                if(memo)assertNull(known.putIfAbsent(i+","+j,expected),"Each memo state computes once");
                computed.add(i+","+j); evaluations++;
            } else if(event.equals("return")) {
                assertEquals(Integer.valueOf(id),active.remove(active.size()-1));
                assertEquals(String.valueOf(expectedValues.get(i+","+j)),vars.get("value"));
            }
            assertEquals(active.size(),step.getCallStack().size(),event);
            for(int depth=0;depth<active.size();depth++) {
                int callId=active.get(depth);var call=calls.get(callId);
                assertEquals("lps(i="+call.i+",j="+call.j+") #"+callId,step.getCallStack().get(depth));
            }
            assertEquals(String.valueOf(entered),vars.get("calls")); assertEquals(String.valueOf(hits),vars.get("cacheHits"));
            assertEquals(String.valueOf(evaluations),vars.get("stateEvaluations")); assertEquals(String.valueOf(computed.size()),vars.get("computedStates"));
            String anchor=switch(event) {
                case "store","combine" -> "combine";
                case "cache-hit" -> "hit";
                case "return" -> calls.get(id).hit?"hit":i==0||j==0?"base":"combine";
                default -> event;
            };
            assertEquals(code.resolve(anchor),step.getActiveLine());
            if(memo) {
                var table=step.getDpTable(); assertNotNull(table);
                assertEquals(RecursiveDpExecution.labels("i=",a.length()+1),table.rowLabels());
                assertEquals(RecursiveDpExecution.labels("j=",b.length()+1),table.colLabels());
                for(int r=0;r<=a.length();r++)for(int c=0;c<=b.length();c++) {
                    var cell=table.cells().get(r).get(c);Integer value=known.get(r+","+c);
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
                else { expected.add((call.i-1)+","+call.j);expected.add(call.i+","+(call.j-1)); }
            }
            assertEquals(expected,call.children.stream().map(child->calls.get(child).key()).toList());
        }
        return trace;
    }

    private void assertWire(JsonNode body,String form,Map<String,Object> supplied,int steps) throws Exception {
        var actual=tracer(form);var code=AnnotatedCode.parse(actual.annotatedCode());
        assertEquals(ID,body.path("problemId").asText()); assertEquals(form,body.path("approachId").asText());
        assertEquals(actual.dsType().wireValue(),body.path("dsType").asText());
        assertEquals(code.getDisplayCode(),body.path("code").asText()); assertEquals(wire(code.getAnchors()),body.path("anchors"));
        assertEquals(wire(approaches.resolve(ID,form).complexity()),body.path("complexity"));
        assertEquals(wire(supplied),body.path("resolvedInput"));assertFalse(body.path("truncated").asBoolean());
        assertEquals(steps,body.path("stepCount").asInt());
        assertEquals(InputSpec.DEFAULT_MAX_STEPS,body.path("maxSteps").asInt()); assertEquals(InputSpec.DEFAULT_MAX_BYTES,body.path("maxBytes").asLong());
    }

    private ExecutionTrace assertAnswer(String form,Map<String,Object> supplied) {
        var trace=runner.run(tracer(form),supplied);assertFalse(trace.isTruncated());assertEquals(supplied,trace.getResolvedInput());
        assertEquals(String.valueOf(palindromeOracle(text(supplied))),last(trace).get("answer"));
        assertTrue(trace.getSteps().get(trace.getStepCount()-1).getCallStack().isEmpty());return trace;
    }
    private AlgorithmTracer tracer(String form) { return approaches.resolve(ID,form).tracer(); }
    private static Map<String,String> last(ExecutionTrace trace) { return trace.getSteps().get(trace.getStepCount()-1).getVariables(); }
    private static String text(Map<String,Object> input) { return (String)input.get("text"); }
    private static Map<String,Object> input(String text) { return Map.of("text",text); }
    private static int cap(String form) { return form.equals("canonical")?12:form.equals("memoization")?8:5; }

    /** Direct subsequence palindrome test, independent of LCS or interval DP. */
    private static int palindromeOracle(String text) {
        int best=0;
        for(int mask=0;mask<(1<<text.length());mask++) {
            if(Integer.bitCount(mask)<=best)continue;
            var word=new StringBuilder();for(int i=0;i<text.length();i++)if((mask&(1<<i))!=0)word.append(text.charAt(i));
            boolean palindrome=true;
            for(int i=0,j=word.length()-1;i<j;i++,j--)if(word.charAt(i)!=word.charAt(j)){palindrome=false;break;}
            if(palindrome)best=word.length();
        }
        return best;
    }
    /** General prefix-pair oracle for internal states: subsequence membership, not the traced recurrence. */
    private static int prefixOracle(String a,String b) {
        int best=0;
        for(int mask=0;mask<(1<<a.length());mask++) {
            if(Integer.bitCount(mask)<=best)continue;
            int cursor=0;boolean fits=true;
            for(int i=0;i<a.length();i++)if((mask&(1<<i))!=0) {
                cursor=b.indexOf(a.charAt(i),cursor);if(cursor<0){fits=false;break;}cursor++;
            }
            if(fits)best=Integer.bitCount(mask);
        }
        return best;
    }
    private static List<String> probes(String form) {
        var result=new LinkedHashSet<String>(List.of("bbbab","cbbd","abcde","aaaaa","abcba","a","ab","abab"));
        for(int n=1;n<=5;n++)for(int mask=0;mask<(1<<n);mask++) {
            var word=new StringBuilder();for(int i=0;i<n;i++)word.append((mask&(1<<i))==0?'a':'b');result.add(word.toString());
        }
        // Restricted-growth strings cover every length-five equality partition (Bell number52).
        var patterns=new ArrayList<String>(); equalityPatterns(new int[5],1,0,patterns);assertEquals(52,patterns.size());result.addAll(patterns);
        var random=new Random(51619);int cap=cap(form);
        for(int sample=0;sample<36;sample++) {
            var word=new StringBuilder();int n=1+random.nextInt(cap);
            for(int i=0;i<n;i++)word.append((char)('a'+random.nextInt(5)));result.add(word.toString());
        }
        if(!form.equals("recursion"))result.addAll(List.of("abbccdde","abbbbbbc","abcdefgh","aaaaaaaa"));
        if(form.equals("canonical"))result.addAll(List.of("abcdefghijkl","aaaaaaaaaaaa","abbaabbaabba"));
        return new ArrayList<>(result);
    }
    private static void equalityPatterns(int[] values,int index,int maximum,List<String> result) {
        if(index==values.length) {
            var text=new StringBuilder();for(int value:values)text.append((char)('a'+value));result.add(text.toString());return;
        }
        for(int value=0;value<=maximum+1;value++){values[index]=value;equalityPatterns(values,index+1,Math.max(maximum,value),result);}
    }
    private JsonNode wire(Object value) throws Exception { return json.readTree(json.writeValueAsBytes(value)); }
    private byte[] execute(String form,String encoding,Map<String,Object> input) throws Exception {
        return request(post(ENDPOINT+"/execute").param("approach",form).param("encoding",encoding)
                .contentType("application/json").content(json.writeValueAsString(input)),200);
    }
    private byte[] request(MockHttpServletRequestBuilder request,int expectedStatus) throws Exception {
        String client="lps-approach-"+CLIENTS.incrementAndGet();
        var result=http.perform(request.with(r->{r.setRemoteAddr(client);return r;})).andReturn();
        assertEquals(client,result.getRequest().getRemoteAddr());
        assertEquals(expectedStatus,result.getResponse().getStatus(),result.getResponse().getContentAsString());
        return result.getResponse().getContentAsByteArray();
    }
    private JsonNode decode(JsonNode deltas,JsonNode full) {
        var result=json.createArrayNode();ObjectNode snapshot=json.createObjectNode();var fields=new HashSet<String>();
        for(var step:full)step.fieldNames().forEachRemaining(fields::add);
        for(var delta:deltas) {
            if(delta.path("keyframe").asBoolean()){snapshot=json.createObjectNode();for(String field:fields)snapshot.set(field,NullNode.instance);}
            var entries=delta.fields();while(entries.hasNext()){var entry=entries.next();if(!entry.getKey().equals("keyframe"))snapshot.set(entry.getKey(),entry.getValue());}
            result.add(snapshot.deepCopy());
        }
        return result;
    }
}
