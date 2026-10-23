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
class CapacityApproachesTest {
    @Autowired SolutionApproachRegistry approaches;
    @Autowired TracerRegistry canonical;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;
    @TempDir Path temporary;
    private final ObjectMapper json = new ObjectMapper();
    private static final List<String> FORMS = List.of("recursion", "memoization", "canonical");

    @ParameterizedTest @ValueSource(strings={"knapsack-01","unbounded-knapsack","rod-cutting-problem"})
    void discoveryAndHttpKeepCanonicalExecutableAndSelectedRunIdentity(String id) throws Exception {
        assertEquals(new HashSet<>(FORMS),new HashSet<>(approaches.available(id).stream().map(SolutionApproach::id).toList()));
        assertSame(canonical.find(id).orElseThrow(),approaches.resolve(id,null).tracer());
        assertEquals("Tabulation",approaches.resolve(id,null).label());
        for (String form:FORMS) {
            var tracer=approaches.resolve(id,form).tracer();
            assertEquals(form.equals("canonical")?DsType.DP_TABLE:DsType.RECURSION_TREE,tracer.dsType());
            var detail=json.readTree(http.perform(get("/api/problems/{id}",id).param("approach",form)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            assertEquals(3,detail.path("approaches").size());
            for (String encoding:List.of("full","delta")) {
                String client="capacity-identity-"+id+form+encoding;
                var response=http.perform(get("/api/problems/{id}/execute",id).param("approach",form).param("encoding",encoding)
                        .with(r->{r.setRemoteAddr(client);return r;})).andExpect(status().isOk())
                        .andDo(r->assertEquals(client,r.getRequest().getRemoteAddr())).andReturn();
                var body=json.readTree(response.getResponse().getContentAsByteArray());
                assertEquals(id,body.path("problemId").asText());assertEquals(form,body.path("approachId").asText());
                assertEquals(detail.path("javaCode"),body.path("code"));assertEquals(detail.path("dsType"),body.path("dsType"));
                assertEquals(detail.path("complexity"),body.path("complexity"));assertFalse(body.path("truncated").asBoolean());
            }
        }
    }

    @ParameterizedTest @ValueSource(strings={"knapsack-01","unbounded-knapsack","rod-cutting-problem"})
    void allTracersAndExactDisplayedJavaAgreeWithIndependentEnumeration(String id) throws Exception {
        for (String form:FORMS) {
            var tracer=approaches.resolve(id,form).tracer();String source=AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode();
            Path directory=Files.createTempDirectory(temporary,"solution"),file=directory.resolve("Solution.java");
            Files.writeString(file,form.equals("canonical")&&!rod(id)?"public class Solution {\n"+source+"\n}":source);
            var errors=new ByteArrayOutputStream();
            assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,errors,"--release","17","-classpath",directory.toString(),"-d",directory.toString(),file.toString()),errors::toString);
            try(var loader=new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
                var type=loader.loadClass("Solution");
                var method=Arrays.stream(type.getDeclaredMethods()).filter(m->java.lang.reflect.Modifier.isPublic(m.getModifiers())).findFirst().orElseThrow();
                assertEquals(rod(id)?long.class:int.class,method.getReturnType());
                var inputs=new ArrayList<>(cases(id));
                inputs.add(InputValidator.defaults(tracer.inputSpec()).asMap());inputs.add(tracer.alternateInput());inputs.add(maximum(id,form));
                for(var input:inputs) {
                    assertAnswer(tracer,input);
                    Object[] args=rod(id)?new Object[]{array(input,"prices")}:new Object[]{array(input,"weights"),array(input,"values"),array(input,"weights").length,input.get("capacity")};
                    assertEquals(oracle(id,input),((Number)method.invoke(type.getConstructor().newInstance(),args)).longValue(),id+"/"+form+input);
                }
            }
        }
    }

    @ParameterizedTest @ValueSource(strings={"knapsack-01","unbounded-knapsack","rod-cutting-problem"})
    void realCallsReachEveryAnchorAndReuseComputedZeroWithoutChildren(String id) {
        for(String form:List.of("recursion","memoization")) {
            var tracer=approaches.resolve(id,form).tracer();var lines=new HashSet<Integer>();boolean zeroHit=false;
            var probes=List.of(InputValidator.defaults(tracer.inputSpec()).asMap(),tracer.alternateInput(),maximum(id,form),
                    rod(id)?Map.<String,Object>of("prices",List.of(0,0,0,0,0)):input(List.of(5,5,5),List.of(1,1,1),6));
            for(var probe:probes) {
                var trace=assertAnswer(tracer,probe);var computed=new HashSet<String>();int roots=0;
                for(int i=0;i<trace.getStepCount();i++) {
                    var step=trace.getSteps().get(i);lines.add(step.getActiveLine());var vars=step.getVariables();String event=vars.get("event");
                    if("enter".equals(event)&&step.getCallStack().size()==1) roots++;
                    if(form.equals("recursion")) assertNull(step.getDpTable());
                    else {
                        assertNotNull(step.getDpTable());
                        if(Set.of("base","store").contains(event)) assertTrue(computed.add(vars.get("state")),"Complete state computed only once");
                        if("cache-hit".equals(event)) {
                            zeroHit|="0".equals(vars.get("value"));
                            assertEquals("return",trace.getSteps().get(i+1).getVariables().get("event"));
                            assertEquals(vars.get("callId"),trace.getSteps().get(i+1).getVariables().get("callId"));
                        }
                    }
                }
                assertEquals(1,roots);assertTrue(Integer.parseInt(last(trace).get("calls"))<=220);
                if(form.equals("recursion")) {
                    assertEquals("0",last(trace).get("cacheHits"));
                    assertEquals(expectedCalls(id,probe),Integer.parseInt(last(trace).get("calls")));
                }
            }
            assertEquals(new HashSet<>(AnnotatedCode.parse(tracer.annotatedCode()).getAnchors().values()),lines);
            if(form.equals("memoization")) assertTrue(zeroHit,"Known zero must be reusable, never an unknown sentinel");
        }
    }

    @ParameterizedTest @ValueSource(strings={"knapsack-01","unbounded-knapsack","rod-cutting-problem"})
    void measuredMaximaCompleteInBothEncodingsAndOneOverFailsBeforeSteps(String id) throws Exception {
        for(String form:FORMS) {
            var tracer=approaches.resolve(id,form).tracer();var maximumTrace=assertAnswer(tracer,maximum(id,form));
            if(form.equals("recursion"))assertEquals(rod(id)?219:220,Integer.parseInt(last(maximumTrace).get("calls")));
            for(String encoding:List.of("full","delta")) {
                String client="capacity-max-"+id+form+encoding;
                var body=http.perform(post("/api/problems/{id}/execute",id).param("approach",form).param("encoding",encoding)
                        .contentType("application/json").content(json.writeValueAsString(maximum(id,form))).with(r->{r.setRemoteAddr(client);return r;}))
                        .andExpect(status().isOk()).andDo(r->assertEquals(client,r.getRequest().getRemoteAddr())).andReturn().getResponse().getContentAsByteArray();
                assertTrue(body.length<2_000_000,id+"/"+form+"/"+encoding+" bytes="+body.length);
                assertFalse(json.readTree(body).path("truncated").asBoolean());
            }
            var over=rod(id)?Map.<String,Object>of("prices",Collections.nCopies(form.equals("recursion")?10:form.equals("memoization")?11:13,1)):
                    form.equals("recursion")?(single(id)?input(List.of(17,10,5,5,11,17,12,2),Collections.nCopies(8,1),30):input(List.of(3,6,11,13,1,5),Collections.nCopies(6,1),12)):
                    form.equals("memoization")?input(Collections.nCopies(8,1),Collections.nCopies(8,1),8):input(Collections.nCopies(8,1),Collections.nCopies(8,1),25);
            if(form.equals("recursion")&&!rod(id))assertEquals(221,expectedCalls(id,over));
            assertThrows(InputValidationException.class,()->runner.run(tracer,over));
            String client="capacity-over-"+id+form;
            http.perform(post("/api/problems/{id}/execute",id).param("approach",form).contentType("application/json").content(json.writeValueAsString(over))
                    .with(r->{r.setRemoteAddr(client);return r;})).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors."+(rod(id)?"prices":form.equals("canonical")?"capacity":"weights")).isString())
                    .andExpect(jsonPath("$.steps").doesNotExist()).andDo(r->assertEquals(client,r.getRequest().getRemoteAddr()));
        }
        // Shape-dependent recursion must still accept the full declared domain on cheap trees.
        if(!rod(id)) assertAnswer(approaches.resolve(id,"recursion").tracer(),input(Collections.nCopies(8,20),Collections.nCopies(8,100),30));
    }

    @ParameterizedTest @ValueSource(strings={"knapsack-01","unbounded-knapsack","rod-cutting-problem"})
    void numericDomainsPairingAndUnknownFieldsRemainHonest(String id) {
        for(String form:FORMS) {
            var tracer=approaches.resolve(id,form).tracer();
            if(rod(id)) {
                assertAnswer(tracer,Map.of("prices",List.of(0,0,0,0)));
                for(var prices:List.of(List.of(-1),List.of(101),List.of())) assertThrows(InputValidationException.class,()->runner.run(tracer,Map.of("prices",prices)));
            } else {
                for(var invalid:List.of(input(List.of(1,2),List.of(1),4),input(List.of(1),List.of(1,2),4))) {
                    var error=assertThrows(InputValidationException.class,()->runner.run(tracer,invalid));
                    assertTrue(error.getFieldErrors().containsKey("values"));
                }
                for(var invalid:List.of(input(List.of(0),List.of(1),4),input(List.of(21),List.of(1),4),input(List.of(1),List.of(0),4),input(List.of(1),List.of(101),4),input(List.of(1),List.of(1),0),input(List.of(1),List.of(1),31)))
                    assertThrows(InputValidationException.class,()->runner.run(tracer,invalid));
                assertEquals(single(id)?5:10,oracle(id,input(List.of(2),List.of(5),4)));
                assertAnswer(tracer,input(List.of(2),List.of(5),4));
            }
            var extra=new HashMap<>(tracer.alternateInput());extra.put("invented",7);
            assertThrows(InputValidationException.class,()->runner.run(tracer,extra));
            var overflow=new HashMap<>(tracer.alternateInput());overflow.put(rod(id)?"prices":"weights",List.of(4294967297L));
            assertThrows(InputValidationException.class,()->runner.run(tracer,overflow));
        }
    }

    @ParameterizedTest @ValueSource(strings={"knapsack-01","unbounded-knapsack","rod-cutting-problem"})
    void everyMemoCellIsKnownOnlyAfterItsRealComputationAndHasTheIndependentValue(String id) {
        var tracer=approaches.resolve(id,"memoization").tracer();
        var probes=List.of(InputValidator.defaults(tracer.inputSpec()).asMap(),
                rod(id)?Map.<String,Object>of("prices",List.of(0,5,0,11,0)):input(List.of(2,3,2),List.of(4,5,7),6));
        for(var probe:probes) {
            var trace=assertAnswer(tracer,probe);var known=new HashMap<String,Long>();
            int size=array(probe,rod(id)?"prices":"weights").length,capacity=rod(id)?size:(int)probe.get("capacity");
            for(var step:trace.getSteps()) {
                var vars=step.getVariables();String event=vars.get("event");
                var coordinates=java.util.regex.Pattern.compile("n=(\\d+),[wl]=(\\d+)").matcher(vars.get("state"));
                assertTrue(coordinates.matches());int n=Integer.parseInt(coordinates.group(1)),left=Integer.parseInt(coordinates.group(2));
                String key=n+","+left;
                if(Set.of("base","store").contains(event)) {
                    long expected=prefixOracle(id,probe,n,left);
                    assertEquals(String.valueOf(expected),vars.get("value"));assertNull(known.putIfAbsent(key,expected));
                } else if("cache-hit".equals(event)) assertEquals(String.valueOf(known.get(key)),vars.get("value"));
                var table=step.getDpTable();assertNotNull(table);
                assertEquals(java.util.stream.IntStream.rangeClosed(0,size).mapToObj(i->"n="+i).toList(),table.rowLabels());
                assertEquals(java.util.stream.IntStream.rangeClosed(0,capacity).mapToObj(i->(rod(id)?"l=":"w=")+i).toList(),table.colLabels());
                assertEquals(size+1,table.cells().size());
                for(int r=0;r<=size;r++) {
                    assertEquals(capacity+1,table.cells().get(r).size());
                    for(int c=0;c<=capacity;c++) {
                        var cell=table.cells().get(r).get(c);Long value=known.get(r+","+c);
                        assertEquals(value==null?"·":String.valueOf(value),cell.value(),id+" "+event+" cell "+r+","+c);
                        if(value==null)assertEquals("void",cell.state());else assertNotEquals("void",cell.state());
                    }
                }
            }
        }
    }

    @ParameterizedTest @ValueSource(strings={"knapsack-01","unbounded-knapsack","rod-cutting-problem"})
    void tinyBudgetStopsBeforeClaimingACompletedAnswer(String id) {
        for(String form:List.of("recursion","memoization")) {
            var actual=approaches.resolve(id,form).tracer();
            var capped=new AlgorithmTracer(){public String id(){return actual.id();}public DsType dsType(){return actual.dsType();}public String annotatedCode(){return actual.annotatedCode();}public InputSpec inputSpec(){return actual.inputSpec().withMaxSteps(3);}public Map<String,Object> alternateInput(){return actual.alternateInput();}public void run(Inputs in,StepEmitter emit){actual.run(in,emit);}};
            var trace=runner.run(capped,maximum(id,form));assertTrue(trace.isTruncated());assertEquals(3,trace.getStepCount());
            assertTrue(trace.getSteps().stream().noneMatch(s->s.getVariables().containsKey("answer")));
        }
    }

    private ExecutionTrace assertAnswer(AlgorithmTracer tracer,Map<String,Object> input) {
        var trace=runner.run(tracer,input);assertFalse(trace.isTruncated(),tracer.id()+input);
        assertEquals(String.valueOf(oracle(tracer.id(),input)),last(trace).get("answer"));assertEquals(input,trace.getResolvedInput());
        assertTrue(trace.getSteps().get(trace.getStepCount()-1).getCallStack().isEmpty());return trace;
    }
    private static Map<String,String> last(ExecutionTrace trace){return trace.getSteps().get(trace.getStepCount()-1).getVariables();}
    private static boolean rod(String id){return id.equals("rod-cutting-problem");}
    private static boolean single(String id){return id.equals("knapsack-01");}
    private static int[] array(Map<String,Object> input,String key){return ((List<Integer>)input.get(key)).stream().mapToInt(Integer::intValue).toArray();}
    static Map<String,Object> input(List<Integer> weights,List<Integer> values,int capacity){return Map.of("weights",weights,"values",values,"capacity",capacity);}
    static Map<String,Object> maximum(String id,String form) {
        if(rod(id))return Map.of("prices",Collections.nCopies(form.equals("recursion")?9:form.equals("memoization")?10:12,100));
        if(form.equals("recursion"))return single(id)?input(List.of(3,1,15,5,3,19,7,17),Collections.nCopies(8,100),29):input(List.of(17,2,4,15),Collections.nCopies(4,100),26); // exactly220 calls
        if(form.equals("memoization"))return input(single(id)?List.of(1,3,1,1,5,1,2):Collections.nCopies(7,1),Collections.nCopies(7,100),9); // exactly80 allocated cells
        return input(Collections.nCopies(8,20),Collections.nCopies(8,100),24); // exactly225 allocated cells
    }
    // Independent references: index masks, quantity vectors, and ordered compositions, not the prefix recurrence.
    private static long oracle(String id,Map<String,Object> input) {
        if(rod(id))return compositions(array(input,"prices"),array(input,"prices").length);
        int[] weights=array(input,"weights"),values=array(input,"values");int capacity=(int)input.get("capacity");
        if(!single(id))return quantities(weights,values,0,capacity);
        long best=0;for(int mask=0;mask<(1<<weights.length);mask++){int weight=0,value=0;for(int i=0;i<weights.length;i++)if((mask&(1<<i))!=0){weight+=weights[i];value+=values[i];}if(weight<=capacity)best=Math.max(best,value);}return best;
    }
    private static long compositions(int[] prices,int length){long best=0;for(int cut=1;cut<=length;cut++)best=Math.max(best,prices[cut-1]+compositions(prices,length-cut));return best;}
    private static long prefixOracle(String id,Map<String,Object> input,int n,int left){
        if(rod(id))return quantities(java.util.stream.IntStream.rangeClosed(1,n).toArray(),Arrays.copyOf(array(input,"prices"),n),0,left);
        return oracle(id,input(Arrays.stream(array(input,"weights")).limit(n).boxed().toList(),Arrays.stream(array(input,"values")).limit(n).boxed().toList(),left));
    }
    private static long quantities(int[] weights,int[] values,int item,int capacity){if(item==weights.length)return 0;long best=0;for(int quantity=0;quantity*weights[item]<=capacity;quantity++)best=Math.max(best,(long)quantity*values[item]+quantities(weights,values,item+1,capacity-quantity*weights[item]));return best;}
    private static int expectedCalls(String id,Map<String,Object> input){int[] weights=rod(id)?java.util.stream.IntStream.rangeClosed(1,array(input,"prices").length).toArray():array(input,"weights");return countCalls(weights,weights.length,rod(id)?weights.length:(int)input.get("capacity"),!single(id));}
    private static int countCalls(int[] weights,int n,int capacity,boolean reuse){if(n==0||capacity==0)return 1;return 1+countCalls(weights,n-1,capacity,reuse)+(weights[n-1]<=capacity?countCalls(weights,reuse?n:n-1,capacity-weights[n-1],reuse):0);}
    private static List<Map<String,Object>> cases(String id) {
        var result=new ArrayList<Map<String,Object>>();
        if(rod(id)){for(int length=1;length<=5;length++)for(int code=0;code<(int)Math.pow(3,length);code++){int rest=code;var prices=new ArrayList<Integer>();for(int i=0;i<length;i++,rest/=3)prices.add(rest%3);result.add(Map.of("prices",prices));}}
        else for(int length=1;length<=3;length++)for(int code=0;code<(1<<(2*length));code++) {
            int rest=code;var weights=new ArrayList<Integer>();var values=new ArrayList<Integer>();
            for(int i=0;i<length;i++,rest/=4){weights.add(1+rest%2);values.add(1+(rest/2)%2);}
            for(int capacity=1;capacity<=4;capacity++)result.add(input(weights,values,capacity));
        }
        var random=new Random(20927);
        for(int trial=0;trial<30;trial++) {
            int length=1+random.nextInt(rod(id)?6:3);var a=new ArrayList<Integer>();var b=new ArrayList<Integer>();
            for(int i=0;i<length;i++){a.add(rod(id)?random.nextInt(101):1+random.nextInt(20));b.add(1+random.nextInt(100));}
            result.add(rod(id)?Map.of("prices",a):input(a,b,1+random.nextInt(19)));
        }
        return result;
    }
}
