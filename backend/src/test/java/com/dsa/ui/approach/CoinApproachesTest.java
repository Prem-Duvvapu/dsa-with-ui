package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class CoinApproachesTest {
    @Autowired SolutionApproachRegistry approaches;
    @Autowired TracerRegistry canonical;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;
    @TempDir Path temporary;
    private final ObjectMapper json=new ObjectMapper();
    private static final List<String> FORMS=List.of("recursion","memoization","canonical");

    @ParameterizedTest @ValueSource(strings={"minimum-coins-dp","coin-change-2"})
    void discoveryAndBothHttpEncodingsKeepTheSelectedIdentity(String id) throws Exception {
        assertEquals(new HashSet<>(FORMS),new HashSet<>(approaches.available(id).stream().map(SolutionApproach::id).toList()));
        assertSame(canonical.find(id).orElseThrow(),approaches.resolve(id,null).tracer());
        assertEquals("Tabulation",approaches.resolve(id,null).label());
        for(String form:FORMS) {
            var tracer=approaches.resolve(id,form).tracer();
            assertEquals(form.equals("canonical")?DsType.DP_TABLE:DsType.RECURSION_TREE,tracer.dsType());
            var detail=json.readTree(http.perform(get("/api/problems/{id}",id).param("approach",form)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            assertEquals(3,detail.path("approaches").size());
            for(String encoding:List.of("full","delta")) {
                String client="coin-identity-"+id+form+encoding;
                var body=json.readTree(http.perform(get("/api/problems/{id}/execute",id).param("approach",form).param("encoding",encoding)
                        .with(r->{r.setRemoteAddr(client);return r;})).andExpect(status().isOk())
                        .andDo(r->assertEquals(client,r.getRequest().getRemoteAddr())).andReturn().getResponse().getContentAsByteArray());
                assertEquals(id,body.path("problemId").asText());assertEquals(form,body.path("approachId").asText());
                assertEquals(detail.path("javaCode"),body.path("code"));assertEquals(detail.path("dsType"),body.path("dsType"));
                assertEquals(detail.path("complexity"),body.path("complexity"));assertFalse(body.path("truncated").asBoolean());
            }
        }
    }

    @ParameterizedTest @ValueSource(strings={"minimum-coins-dp","coin-change-2"})
    void exactDisplayedJavaAndTracersAgreeWithIndependentReferences(String id) throws Exception {
        for(String form:FORMS) {
            var tracer=approaches.resolve(id,form).tracer();
            String source=AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode();
            Path directory=Files.createTempDirectory(temporary,"coin"),file=directory.resolve("Solution.java");
            Files.writeString(file,form.equals("canonical")?"public class Solution {\n"+source+"\n}":source);
            var errors=new ByteArrayOutputStream();
            assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,errors,"--release","17","-classpath",directory.toString(),"-d",directory.toString(),file.toString()),errors::toString);
            try(var loader=new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
                var type=loader.loadClass("Solution");var method=type.getMethod(minimum(id)?"coinChange":"change",int[].class,int.class);
                assertEquals(int.class,method.getReturnType());
                var probes=new ArrayList<>(cases(id));probes.add(InputValidator.defaults(tracer.inputSpec()).asMap());probes.add(tracer.alternateInput());probes.add(maximum(id,form));
                for(var input:probes) {
                    assertAnswer(tracer,input);
                    assertEquals(oracle(id,coins(input),amount(input)),method.invoke(type.getConstructor().newInstance(),coins(input),amount(input)),id+"/"+form+input);
                }
            }
        }
    }

    @ParameterizedTest @ValueSource(strings={"minimum-coins-dp","coin-change-2"})
    void realCallsReachAllAnchorsAndReuseKnownImpossibleResultsWithoutChildren(String id) {
        for(String form:List.of("recursion","memoization")) {
            var tracer=approaches.resolve(id,form).tracer();var lines=new HashSet<Integer>();boolean impossibleHit=false,zeroBase=false;
            for(var input:List.of(InputValidator.defaults(tracer.inputSpec()).asMap(),impossible(id),maximum(id,form))) {
                var trace=assertAnswer(tracer,input);int roots=0;var computed=new HashSet<String>();
                for(int i=0;i<trace.getStepCount();i++) {
                    var step=trace.getSteps().get(i);lines.add(step.getActiveLine());var vars=step.getVariables();String event=vars.get("event");
                    if("enter".equals(event)&&step.getCallStack().size()==1) roots++;
                    if(form.equals("recursion"))assertNull(step.getDpTable());
                    else {
                        assertNotNull(step.getDpTable());
                        if(Set.of("base","store").contains(event))assertTrue(computed.add(vars.get("state")),"Cache must evaluate a complete state only once");
                        if("cache-hit".equals(event)) {
                            impossibleHit|=(minimum(id)?"1000000":"0").equals(vars.get("value"));
                            assertEquals("return",trace.getSteps().get(i+1).getVariables().get("event"));
                            assertEquals(vars.get("callId"),trace.getSteps().get(i+1).getVariables().get("callId"));
                        }
                    }
                    zeroBase|="base".equals(event)&&"0".equals(vars.get("value"));
                }
                assertEquals(1,roots);assertTrue(Integer.parseInt(last(trace).get("calls"))<=220);
                if(form.equals("recursion")) {
                    assertEquals("0",last(trace).get("cacheHits"));
                    assertEquals(calls(coins(input),coins(input).length,amount(input)),Integer.parseInt(last(trace).get("calls")));
                }
            }
            assertEquals(new HashSet<>(AnnotatedCode.parse(tracer.annotatedCode()).getAnchors().values()),lines);
            assertTrue(zeroBase);
            if(form.equals("memoization"))assertTrue(impossibleHit,"Known unreachable/zero is not an uncomputed state");
        }
    }

    @ParameterizedTest @ValueSource(strings={"minimum-coins-dp","coin-change-2"})
    void everyMemoCellHasItsIndependentValueOnlyAfterARealComputation(String id) {
        var tracer=approaches.resolve(id,"memoization").tracer();
        for(var input:List.of(InputValidator.defaults(tracer.inputSpec()).asMap(),impossible(id))) {
            var trace=assertAnswer(tracer,input);var known=new HashMap<String,Integer>();int n=coins(input).length,a=amount(input);
            for(var step:trace.getSteps()) {
                var vars=step.getVariables();String event=vars.get("event");
                var coordinates=java.util.regex.Pattern.compile("n=(\\d+),a=(\\d+)").matcher(vars.get("state"));assertTrue(coordinates.matches());
                int r=Integer.parseInt(coordinates.group(1)),c=Integer.parseInt(coordinates.group(2));String key=r+","+c;
                if(Set.of("base","store").contains(event)) {
                    int expected=oracle(id,Arrays.copyOf(coins(input),r),c);if(minimum(id)&&expected<0)expected=1_000_000;
                    assertEquals(String.valueOf(expected),vars.get("value"));assertNull(known.putIfAbsent(key,expected));
                }else if("cache-hit".equals(event))assertEquals(String.valueOf(known.get(key)),vars.get("value"));
                var table=step.getDpTable();assertNotNull(table);
                assertEquals(java.util.stream.IntStream.rangeClosed(0,n).mapToObj(i->"n="+i).toList(),table.rowLabels());
                assertEquals(java.util.stream.IntStream.rangeClosed(0,a).mapToObj(i->"a="+i).toList(),table.colLabels());
                if(minimum(id)) {
                    assertTrue(vars.get("memoDomain").contains("unreachable"));assertTrue(vars.get("memoDomain").contains("1000000"));
                    assertTrue(vars.get("memoDomain").contains("uncomputed"));
                }
                for(int row=0;row<=n;row++)for(int col=0;col<=a;col++) {
                    var cell=table.cells().get(row).get(col);Integer value=known.get(row+","+col);
                    assertEquals(value==null?"·":minimum(id)&&value>=1_000_000?"∞":String.valueOf(value),cell.value(),id+" "+event+" cell "+row+","+col);
                    if(value==null)assertEquals("void",cell.state());else assertNotEquals("void",cell.state());
                }
            }
        }
    }

    @ParameterizedTest @ValueSource(strings={"minimum-coins-dp","coin-change-2"})
    void realDomainReuseDuplicatesAndInvalidInputsArePreserved(String id) throws Exception {
        for(String form:FORMS) {
            var tracer=approaches.resolve(id,form).tracer();
            for(var input:List.of(input(List.of(0),3),input(List.of(26),3),input(List.of(),3),input(Collections.nCopies(9,25),1),input(List.of(1),0),input(List.of(1),41),Map.<String,Object>of("coins",List.of(4294967297L),"amount",3)))
                assertThrows(InputValidationException.class,()->runner.run(tracer,input));
            var extra=new HashMap<>(tracer.alternateInput());extra.put("invented",1);assertThrows(InputValidationException.class,()->runner.run(tracer,extra));
            var duplicate=input(List.of(2,2,2),3);
            if(minimum(id))assertAnswer(tracer,duplicate);
            else {
                assertTrue(assertThrows(InputValidationException.class,()->runner.run(tracer,duplicate)).getFieldErrors().containsKey("coins"));
                String client="coin-duplicate-"+form;
                http.perform(post("/api/problems/{id}/execute",id).param("approach",form).contentType("application/json").content(json.writeValueAsString(duplicate))
                        .with(r->{r.setRemoteAddr(client);return r;})).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.coins").isString())
                        .andExpect(jsonPath("$.steps").doesNotExist()).andDo(r->assertEquals(client,r.getRequest().getRemoteAddr()));
            }
            // Reuse is real; counts describe combinations, not ordered sequences.
            assertAnswer(tracer,input(List.of(2),4));assertAnswer(tracer,input(List.of(2,1),3));
        }
        assertEquals(2,oracle(id,new int[]{1,2},3));
    }

    @ParameterizedTest @ValueSource(strings={"minimum-coins-dp","coin-change-2"})
    void measuredMaximaAndDeepChainsCompleteAndOversizeFailsBeforeEmission(String id) throws Exception {
        for(String form:FORMS) {
            var tracer=approaches.resolve(id,form).tracer();assertAnswer(tracer,maximum(id,form));
            for(String encoding:List.of("full","delta")) {
                String client="coin-max-"+id+form+encoding;
                var bytes=http.perform(post("/api/problems/{id}/execute",id).param("approach",form).param("encoding",encoding)
                        .contentType("application/json").content(json.writeValueAsString(maximum(id,form))).with(r->{r.setRemoteAddr(client);return r;}))
                        .andExpect(status().isOk()).andDo(r->assertEquals(client,r.getRequest().getRemoteAddr())).andReturn().getResponse().getContentAsByteArray();
                assertTrue(bytes.length<2_000_000,id+"/"+form+"/"+encoding+" bytes="+bytes.length);
                assertFalse(json.readTree(bytes).path("truncated").asBoolean());
            }
            var over=form.equals("recursion")?input(List.of(7,23,25,6,5,20),31):form.equals("memoization")?input(List.of(1,2,3,4,5,6,7,8),8):input(List.of(1,2,3,4,5,6,7,8),25);
            if(form.equals("recursion"))assertEquals(221,calls(coins(over),coins(over).length,amount(over)));
            assertThrows(InputValidationException.class,()->runner.run(tracer,over));
            String client="coin-over-"+id+form;
            http.perform(post("/api/problems/{id}/execute",id).param("approach",form).contentType("application/json").content(json.writeValueAsString(over))
                    .with(r->{r.setRemoteAddr(client);return r;})).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors."+(form.equals("canonical")?"amount":"coins")).isString()).andExpect(jsonPath("$.steps").doesNotExist());
        }
        assertAnswer(approaches.resolve(id,"recursion").tracer(),input(List.of(1),40));
        assertThrows(InputValidationException.class,()->runner.run(approaches.resolve(id,"recursion").tracer(),input(List.of(1,2,3,4),12)));
        assertAnswer(approaches.resolve(id,"memoization").tracer(),input(List.of(1,2,3,4),12));
        assertAnswer(approaches.resolve(id,"memoization").tracer(),input(List.of(1),39));
        assertThrows(InputValidationException.class,()->runner.run(approaches.resolve(id,"memoization").tracer(),input(List.of(1),40)));
        assertAnswer(approaches.resolve(id,"recursion").tracer(),input(List.of(18,19,20,21,22,23,24,25),40));
        for(int n=1;n<=8;n++)assertAnswer(approaches.resolve(id,"memoization").tracer(),input(minimum(id)?Collections.nCopies(n,1):java.util.stream.IntStream.iterate(n,i->i-1).limit(n).boxed().toList(),80/(n+1)-1));
    }

    @ParameterizedTest @ValueSource(strings={"minimum-coins-dp","coin-change-2"})
    void aTinyBudgetCannotClaimACompletedAnswer(String id) {
        for(String form:List.of("recursion","memoization")) {
            var actual=approaches.resolve(id,form).tracer();
            var capped=new AlgorithmTracer(){public String id(){return actual.id();}public DsType dsType(){return actual.dsType();}public String annotatedCode(){return actual.annotatedCode();}public InputSpec inputSpec(){return actual.inputSpec().withMaxSteps(3);}public Map<String,Object> alternateInput(){return actual.alternateInput();}public void run(Inputs in,StepEmitter emit){actual.run(in,emit);}};
            var trace=runner.run(capped,maximum(id,form));assertTrue(trace.isTruncated());assertEquals(3,trace.getStepCount());
            assertTrue(trace.getSteps().stream().noneMatch(s->s.getVariables().containsKey("answer")));
        }
    }

    private ExecutionTrace assertAnswer(AlgorithmTracer tracer,Map<String,Object> input) {
        var trace=runner.run(tracer,input);assertFalse(trace.isTruncated(),tracer.id()+input);
        assertEquals(String.valueOf(oracle(tracer.id(),coins(input),amount(input))),last(trace).get("answer"));
        assertEquals(input,trace.getResolvedInput());assertTrue(trace.getSteps().get(trace.getStepCount()-1).getCallStack().isEmpty());return trace;
    }
    private static Map<String,String> last(ExecutionTrace trace){return trace.getSteps().get(trace.getStepCount()-1).getVariables();}
    private static boolean minimum(String id){return id.equals("minimum-coins-dp");}
    private static int[] coins(Map<String,Object> in){return ((List<Integer>)in.get("coins")).stream().mapToInt(Integer::intValue).toArray();}
    private static int amount(Map<String,Object> in){return (int)in.get("amount");}
    static Map<String,Object> input(List<Integer> coins,int amount){return Map.of("coins",coins,"amount",amount);}
    static Map<String,Object> maximum(String id,String form){
        if(form.equals("recursion"))return input(List.of(23,6,7,15,3,11),23); // exactly 220 real calls
        if(form.equals("memoization"))return minimum(id)?input(Collections.nCopies(7,1),9):input(List.of(4,3,2,1),15); // 80 allocated cells, dense traversals
        return input(List.of(1,2,3,4,5,6,7,8),24); // 225 allocated cells
    }
    // Minimum: breadth-first traversal of reachable amounts. Count: enumerate denomination quantities.
    static int oracle(String id,int[] coins,int amount){
        if(!minimum(id))return quantities(coins,0,amount);
        int[] distance=new int[amount+1];Arrays.fill(distance,-1);distance[0]=0;
        var queue=new ArrayDeque<Integer>();queue.add(0);
        while(!queue.isEmpty()){int sum=queue.remove();for(int coin:coins)if(sum+coin<=amount&&distance[sum+coin]<0){distance[sum+coin]=distance[sum]+1;queue.add(sum+coin);}}
        return distance[amount];
    }
    private static int quantities(int[] coins,int index,int amount){if(index==coins.length)return amount==0?1:0;int count=0;for(int k=0;k*coins[index]<=amount;k++)count+=quantities(coins,index+1,amount-k*coins[index]);return count;}
    private static int calls(int[] coins,int n,int a){if(n==0||a==0)return 1;return 1+calls(coins,n-1,a)+(coins[n-1]<=a?calls(coins,n,a-coins[n-1]):0);}
    private static List<Map<String,Object>> cases(String id){
        var result=new ArrayList<Map<String,Object>>();
        for(int n=1;n<=4;n++)for(int code=0;code<(int)Math.pow(minimum(id)?3:5,n);code++) {
            int rest=code;var coins=new ArrayList<Integer>();for(int i=0;i<n;i++,rest/=minimum(id)?3:5)coins.add(1+rest%(minimum(id)?3:5));
            if(!minimum(id)&&new HashSet<>(coins).size()!=n)continue;
            for(int a=1;a<=6;a++)if(calls(coins.stream().mapToInt(Integer::intValue).toArray(),n,a)<=220)result.add(input(coins,a));
        }
        var random=new Random(23223);
        int seededTotal=result.size()+40;
        while(result.size()<seededTotal){int n=1+random.nextInt(4),a=1+random.nextInt(40);var coins=new ArrayList<Integer>();for(int i=0;i<n;i++)coins.add(1+random.nextInt(25));
            if((!minimum(id)&&new HashSet<>(coins).size()!=n)||(n+1)*(a+1)>80||calls(coins.stream().mapToInt(Integer::intValue).toArray(),n,a)>220)continue;
            result.add(input(coins,a));}
        return result;
    }
    private static Map<String,Object> impossible(String id){return minimum(id)?input(List.of(2,2,2),3):input(List.of(2,4,6),7);}
}
