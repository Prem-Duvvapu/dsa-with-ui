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
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class SubsetApproachesTest {
    @Autowired SolutionApproachRegistry approaches;
    @Autowired TracerRegistry canonical;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;
    @TempDir Path temporary;
    private final ObjectMapper json=new ObjectMapper();
    private static final List<String> FORMS=List.of("recursion","memoization","canonical");

    @ParameterizedTest @ValueSource(strings={"subset-sum-equal-target","partition-equal-subset-sum","count-subsets-with-sum-k"})
    void discoveryAndHttpKeepActualTypesSourcesAndCanonicalIdentity(String id) throws Exception {
        assertEquals(new HashSet<>(FORMS),new HashSet<>(approaches.available(id).stream().map(SolutionApproach::id).toList()));
        assertSame(canonical.find(id).orElseThrow(),approaches.resolve(id,null).tracer());
        assertEquals("Tabulation",approaches.resolve(id,null).label());
        for(String form:FORMS){
            var tracer=approaches.resolve(id,form).tracer();
            assertEquals(form.equals("canonical")?DsType.DP_TABLE:DsType.RECURSION_TREE,tracer.dsType());
            var detail=json.readTree(http.perform(get("/api/problems/{id}",id).param("approach",form)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            assertEquals(3,detail.path("approaches").size());
            for(String encoding:List.of("full","delta")){
                String client="subset-identity-"+id+form+encoding;
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

    @ParameterizedTest @ValueSource(strings={"subset-sum-equal-target","partition-equal-subset-sum","count-subsets-with-sum-k"})
    void everyTracerAndExactDisplayedSourceMatchIndexSubsetEnumeration(String id) throws Exception {
        for(String form:FORMS){
            var tracer=approaches.resolve(id,form).tracer();String source=AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode();
            Path directory=Files.createTempDirectory(temporary,"solution"),file=directory.resolve("Solution.java");
            Files.writeString(file,form.equals("canonical")?"public class Solution {\n"+source+"\n}":source);
            var errors=new ByteArrayOutputStream();
            assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,errors,"--release","17","-classpath",directory.toString(),"-d",directory.toString(),file.toString()),errors::toString);
            try(var loader=new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()},ClassLoader.getPlatformClassLoader())){
                var type=loader.loadClass("Solution");var method=Arrays.stream(type.getDeclaredMethods()).filter(m->java.lang.reflect.Modifier.isPublic(m.getModifiers())).findFirst().orElseThrow();
                assertEquals(counting(id)?int.class:boolean.class,method.getReturnType());
                var inputs=new ArrayList<>(cases(id));inputs.add(InputValidator.defaults(tracer.inputSpec()).asMap());inputs.add(tracer.alternateInput());inputs.add(maximum(id,form));
                for(var input:inputs){
                    assertAnswer(tracer,input);
                    Object[] args=partition(id)?new Object[]{numbers(input)}:new Object[]{numbers(input),target(id,input)};
                    assertEquals(oracle(id,input),method.invoke(type.getConstructor().newInstance(),args),id+input);
                }
            }
        }
    }

    @ParameterizedTest @ValueSource(strings={"subset-sum-equal-target","partition-equal-subset-sum","count-subsets-with-sum-k"})
    void completeStatesReuseFalseOrZeroAndReachAllRealAnchors(String id){
        for(String form:List.of("recursion","memoization")){
            var tracer=approaches.resolve(id,form).tracer();var lines=new HashSet<Integer>();boolean falseOrZeroHit=false;
            var probes=new ArrayList<Map<String,Object>>();
            probes.add(input(id,counting(id)?List.of(1,1,1,1):partition(id)?List.of(0,0,0,2):List.of(0,0,0,0),counting(id)?5:1));
            probes.add(input(id,counting(id)?List.of(1,1,1,1):List.of(0,0,0,0),0));
            probes.add(tracer.alternateInput());probes.add(maximum(id,form));
            for(var probe:probes){
                var trace=assertAnswer(tracer,probe);var computed=new HashSet<String>();int roots=0;
                for(int i=0;i<trace.getStepCount();i++){
                    var step=trace.getSteps().get(i);lines.add(step.getActiveLine());var vars=step.getVariables();String event=vars.get("event");
                    if("enter".equals(event)&&step.getCallStack().size()==1)roots++;
                    if(form.equals("recursion"))assertNull(step.getDpTable());
                    else if(!"0".equals(last(trace).get("calls"))){
                        assertNotNull(step.getDpTable());
                        if(Set.of("base","store").contains(event))assertTrue(computed.add(vars.get("state")),"Each complete state is computed once");
                        if("cache-hit".equals(event)){
                            falseOrZeroHit|=(counting(id)?"0":"false").equals(vars.get("value"));
                            assertEquals("return",trace.getSteps().get(i+1).getVariables().get("event"));
                        }
                        if(!counting(id)){
                            assertTrue(vars.get("memoDomain").contains("false"));
                            for(var row:step.getDpTable().cells())for(var cell:row)assertTrue(Set.of("·","T","F").contains(cell.value()),"Boolean cache values are not numeric aliases");
                        }
                    }
                }
                assertEquals("0".equals(last(trace).get("calls"))?0:1,roots);
                if(form.equals("recursion")&&!"0".equals(last(trace).get("calls")))assertEquals(0,Integer.parseInt(last(trace).get("cacheHits")));
            }
            assertEquals(new HashSet<>(AnnotatedCode.parse(tracer.annotatedCode()).getAnchors().values()),lines);
            if(form.equals("memoization"))assertTrue(falseOrZeroHit,"False and zero are reusable values, not unknown sentinels");
        }
    }

    @ParameterizedTest @ValueSource(strings={"subset-sum-equal-target","partition-equal-subset-sum","count-subsets-with-sum-k"})
    void declaredAndJointBoundsRejectRatherThanRepair(String id) throws Exception {
        for(String form:FORMS){
            var tracer=approaches.resolve(id,form).tracer();var trace=assertAnswer(tracer,maximum(id,form));
            if(!form.equals("canonical")){
                assertTrue(Integer.parseInt(last(trace).get("calls"))<=220);
                if(form.equals("recursion"))assertEquals("127",last(trace).get("calls"));
            }
            for(String encoding:List.of("full","delta")){
                String client="subset-payload-"+id+form+encoding;
                var body=http.perform(post("/api/problems/{id}/execute",id).param("approach",form).param("encoding",encoding)
                        .contentType("application/json").content(json.writeValueAsString(maximum(id,form)))
                        .with(r->{r.setRemoteAddr(client);return r;})).andExpect(status().isOk())
                        .andDo(r->assertEquals(client,r.getRequest().getRemoteAddr())).andReturn().getResponse().getContentAsByteArray();
                assertTrue(body.length<2_000_000,id+"/"+form+"/"+encoding+" bytes="+body.length);
                assertFalse(json.readTree(body).path("truncated").asBoolean());
            }
            var over=input(id,Collections.nCopies(form.equals("recursion")?7:11,1),0);
            assertThrows(InputValidationException.class,()->runner.run(tracer,over));
            String client="subset-bounds-"+id+form;
            http.perform(post("/api/problems/{id}/execute",id).param("approach",form).contentType("application/json").content(json.writeValueAsString(over))
                    .with(r->{r.setRemoteAddr(client);return r;})).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.nums").isString()).andExpect(jsonPath("$.steps").doesNotExist())
                    .andDo(r->assertEquals(client,r.getRequest().getRemoteAddr()));
            for(var invalid:List.of(input(id,List.of(-1),0),input(id,List.of(counting(id)?16:21),0),input(id,List.of(),0)))
                assertThrows(InputValidationException.class,()->runner.run(tracer,invalid));
            if(!partition(id))for(int invalid:List.of(-1,counting(id)?61:41))assertThrows(InputValidationException.class,()->runner.run(tracer,input(id,List.of(1),invalid)));
            var extra=new HashMap<>(input(id,List.of(1),0));extra.put("invented",7);assertThrows(InputValidationException.class,()->runner.run(tracer,extra));
        }
        var memo=approaches.resolve(id,"memoization").tracer();
        assertAnswer(memo,maximum(id,"memoization")); // exactly 80 cells
        assertAnswer(memo,input(id,counting(id)?List.of(1,1,1,1,1,1,1,1,2,3):partition(id)?List.of(0,0,0,0,0,0,6,1,2,3):List.of(0,0,0,0,0,0,0,1,2,3),6)); // 10 items still allowed
        assertThrows(InputValidationException.class,()->runner.run(memo,input(id,Collections.nCopies(10,2),9)));
        var current=canonical.find(id).orElseThrow();
        assertAnswer(current,input(id,partition(id)?Collections.nCopies(8,6):Collections.nCopies(8,1),24)); // exactly 225 cells
        assertThrows(InputValidationException.class,()->runner.run(current,input(id,Collections.nCopies(10,counting(id)?15:20),counting(id)?60:40)));
    }

    @ParameterizedTest @ValueSource(strings={"subset-sum-equal-target","partition-equal-subset-sum","count-subsets-with-sum-k"})
    void tinyBudgetStopsBeforeReportingAnyAnswer(String id){
        for(String form:List.of("recursion","memoization")){
            var actual=approaches.resolve(id,form).tracer();
            var capped=new AlgorithmTracer(){public String id(){return actual.id();}public DsType dsType(){return actual.dsType();}public String annotatedCode(){return actual.annotatedCode();}public InputSpec inputSpec(){return actual.inputSpec().withMaxSteps(3);}public Map<String,Object> alternateInput(){return actual.alternateInput();}public void run(Inputs in,StepEmitter emit){actual.run(in,emit);}};
            var trace=runner.run(capped,maximum(id,form));assertTrue(trace.isTruncated());assertEquals(3,trace.getStepCount());assertTrue(trace.getSteps().stream().noneMatch(s->s.getVariables().containsKey("answer")));
        }
    }

    @Test void oddPartitionHasNoFabricatedRecursionOrHalfIntegerTarget(){
        String id="partition-equal-subset-sum";
        for(String form:List.of("recursion","memoization")){
            var trace=assertAnswer(approaches.resolve(id,form).tracer(),Map.of("nums",List.of(1,2)));
            assertEquals(1,trace.getStepCount());assertEquals("0",last(trace).get("calls"));
            assertNull(trace.getSteps().get(0).getDpTable());assertTrue(trace.getSteps().get(0).getCallStack().isEmpty());
            assertFalse(last(trace).containsKey("target"));
        }
    }

    @Test void positiveCountingDomainAndIndexedDuplicatesStayDistinct(){
        String id="count-subsets-with-sum-k";
        for(String form:FORMS){var tracer=approaches.resolve(id,form).tracer();
            assertThrows(InputValidationException.class,()->runner.run(tracer,input(id,List.of(0,0),0)));
            assertEquals("2",last(assertAnswer(tracer,input(id,List.of(1,1),1))).get("answer"));
            assertEquals("1",last(assertAnswer(tracer,input(id,List.of(1,1),0))).get("answer"));
        }
    }

    @ParameterizedTest @ValueSource(strings={"subset-sum-equal-target","partition-equal-subset-sum","count-subsets-with-sum-k"})
    void denseMemoInputIsRejectedBeforeItCanExhaustTheResponseBudget(String id) throws Exception {
        var dense=input(id,counting(id)?List.of(1,1,1,1,1,1,1,4,2,1):partition(id)?List.of(0,0,0,0,0,1,8,1,2,4):List.of(0,0,0,0,0,0,1,4,2,1),8);
        var oneOver=input(id,partition(id)?List.of(0,0,0,0,0,0,0,16):Collections.nCopies(8,1),8);
        int index=0;
        for(var invalid:List.of(dense,oneOver)){
            assertThrows(InputValidationException.class,()->runner.run(approaches.resolve(id,"memoization").tracer(),invalid));
            String client="subset-joint-budget-"+id+index++;
            http.perform(post("/api/problems/{id}/execute",id).param("approach","memoization")
                    .contentType("application/json").content(json.writeValueAsString(invalid)).with(r->{r.setRemoteAddr(client);return r;}))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.nums").isString())
                    .andExpect(jsonPath("$.steps").doesNotExist()).andDo(r->assertEquals(client,r.getRequest().getRemoteAddr()));
        }
    }

    private ExecutionTrace assertAnswer(AlgorithmTracer tracer,Map<String,Object> input){var trace=runner.run(tracer,input);assertFalse(trace.isTruncated(),tracer.id()+input);assertEquals(String.valueOf(oracle(tracer.id(),input)),last(trace).get("answer"));assertEquals(input,trace.getResolvedInput());assertTrue(trace.getSteps().get(trace.getStepCount()-1).getCallStack().isEmpty());return trace;}
    private static Map<String,String> last(ExecutionTrace trace){return trace.getSteps().get(trace.getStepCount()-1).getVariables();}
    private static boolean counting(String id){return id.equals("count-subsets-with-sum-k");}
    private static boolean partition(String id){return id.equals("partition-equal-subset-sum");}
    private static int[] numbers(Map<String,Object> input){return ((List<Integer>)input.get("nums")).stream().mapToInt(Integer::intValue).toArray();}
    private static int target(String id,Map<String,Object> input){return partition(id)?Arrays.stream(numbers(input)).sum()/2:((Number)input.get(counting(id)?"k":"target")).intValue();}
    private static Map<String,Object> input(String id,List<Integer> nums,int target){return partition(id)?Map.of("nums",nums):Map.of("nums",nums,counting(id)?"k":"target",target);}
    static Map<String,Object> maximum(String id,String form){
        if(form.equals("recursion"))return input(id,Collections.nCopies(6,counting(id)?1:0),counting(id)?6:0);
        if(form.equals("memoization"))return input(id,counting(id)?List.of(1,1,1,1,1,1,1,2,4):partition(id)?List.of(0,0,0,0,0,7,1,2,4):List.of(0,0,0,0,0,0,1,2,4),7);
        return input(id,partition(id)?List.of(4,4,4,4,4,4,4,4,4,2):Collections.nCopies(10,1),19);
    }
    private static Object oracle(String id,Map<String,Object> input){int[] values=numbers(input);int sum=Arrays.stream(values).sum();if(partition(id)&&sum%2!=0)return false;int matches=0,target=target(id,input);for(int mask=0;mask<(1<<values.length);mask++){int candidate=0;for(int i=0;i<values.length;i++)if((mask&(1<<i))!=0)candidate+=values[i];if(candidate==target)matches++;}return counting(id)?Integer.valueOf(matches):Boolean.valueOf(matches>0);}
    private static List<Map<String,Object>> cases(String id){
        var result=new ArrayList<Map<String,Object>>();
        for(int length=1;length<=4;length++)for(int code=0;code<Math.pow(3,length);code++){
            int rest=code;var values=new ArrayList<Integer>();for(int i=0;i<length;i++,rest/=3)values.add(rest%3+(counting(id)?1:0));
            if(partition(id))result.add(input(id,values,0));else for(int target=0;target<=6;target++)result.add(input(id,values,target));
        }
        var random=new Random(8121);int added=0;
        while(added<30){int length=1+random.nextInt(6);var values=new ArrayList<Integer>();for(int i=0;i<length;i++)values.add(counting(id)?1+random.nextInt(15):random.nextInt(21));int desired=random.nextInt(counting(id)?61:41);var next=input(id,values,desired);int sum=values.stream().mapToInt(Integer::intValue).sum();if((!partition(id)||sum%2==0)&&(long)(length+1)*(target(id,next)+1)>80)continue;result.add(next);added++;}
        return result;
    }
}
