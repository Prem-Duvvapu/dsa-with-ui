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
class CircularActivityObstaclesTest {
    @Autowired SolutionApproachRegistry approaches;
    @Autowired TracerRegistry canonical;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;
    @TempDir Path temporary;
    private final ObjectMapper json=new ObjectMapper();
    private static final List<String> FORMS=List.of("recursion","memoization","canonical");

    @ParameterizedTest @ValueSource(strings={"house-robber-2","ninjas-training","unique-paths-2"})
    void formsAndHttpIdentityKeepTheCanonicalExecutable(String problem) throws Exception {
        assertEquals(new HashSet<>(FORMS),new HashSet<>(approaches.available(problem).stream().map(SolutionApproach::id).toList()));
        assertSame(canonical.find(problem).orElseThrow(),approaches.resolve(problem,null).tracer());
        assertEquals("Tabulation",approaches.resolve(problem,null).label());
        for(String form:FORMS){
            var option=approaches.resolve(problem,form);assertNotNull(option.teaching());
            var detail=json.readTree(http.perform(get("/api/problems/{id}",problem).param("approach",form)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            assertEquals(3,detail.path("approaches").size());
            for(String encoding:List.of("full","delta")){
                var body=json.readTree(http.perform(get("/api/problems/{id}/execute",problem).param("approach",form).param("encoding",encoding)
                        .with(r->{r.setRemoteAddr("cao-"+problem+form);return r;})).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
                assertEquals(problem,body.path("problemId").asText());assertEquals(form,body.path("approachId").asText());
                assertEquals(detail.path("javaCode"),body.path("code"));assertEquals(detail.path("complexity"),body.path("complexity"));
                assertEquals(detail.path("dsType"),body.path("dsType"));assertFalse(body.path("truncated").asBoolean());
            }
        }
    }

    @ParameterizedTest @ValueSource(strings={"house-robber-2","ninjas-training","unique-paths-2"})
    void exactDisplayedSourcesAndTracesMatchIndependentEnumeration(String problem) throws Exception {
        for(String form:FORMS){
            var tracer=approaches.resolve(problem,form).tracer();String code=AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode();
            Path dir=Files.createTempDirectory(temporary,"solution"),file=dir.resolve("Solution.java");
            Files.writeString(file,form.equals("canonical")?"public class Solution {\n"+code+"\n}":code);
            var errors=new ByteArrayOutputStream();
            assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,errors,"--release","17","-classpath",dir.toString(),"-d",dir.toString(),file.toString()),errors::toString);
            try(var loader=new URLClassLoader(new java.net.URL[]{dir.toUri().toURL()},ClassLoader.getPlatformClassLoader())){
                var type=loader.loadClass("Solution");var method=Arrays.stream(type.getDeclaredMethods()).filter(m->java.lang.reflect.Modifier.isPublic(m.getModifiers())).findFirst().orElseThrow();
                assertEquals(int.class,method.getReturnType());
                var inputs=new ArrayList<>(cases(problem));
                inputs.add(InputValidator.defaults(tracer.inputSpec()).asMap());
                inputs.add(tracer.alternateInput());
                inputs.add(maximum(problem,form));
                for(var input:inputs){
                    assertAnswer(tracer,input);Object[] args;
                    if(problem.equals("house-robber-2"))args=new Object[]{array(input)};
                    else {int[][] grid=grid(input);args=problem.equals("ninjas-training")&&form.equals("canonical")?new Object[]{grid.length,grid}:new Object[]{grid};}
                    assertEquals(oracle(problem,input),method.invoke(type.getConstructor().newInstance(),args),problem+input);
                }
            }
        }
    }

    @ParameterizedTest @ValueSource(strings={"house-robber-2","ninjas-training","unique-paths-2"})
    void maximumInputsCompleteWithAllCallsAndOneOverIsRefused(String problem) throws Exception {
        for(String form:FORMS){
            var tracer=approaches.resolve(problem,form).tracer();var input=maximum(problem,form);var trace=assertAnswer(tracer,input);
            if(!form.equals("canonical")){
                int expected=problem.equals("house-robber-2")?(form.equals("recursion")?218:58):problem.equals("ninjas-training")?(form.equals("recursion")?189:57):form.equals("recursion")?69:113;
                assertEquals(expected,Integer.parseInt(last(trace).get("calls")));assertTrue(expected<=220);
            }
            String field=input.keySet().iterator().next();var over=new LinkedHashMap<>(input);
            if(field.equals("nums")){var values=new ArrayList<>((List<Integer>)input.get(field));values.add(0);over.put(field,values);}
            else{var rows=new ArrayList<>((List<List<Integer>>)input.get(field));rows.add(rows.get(0));over.put(field,rows);}
            assertThrows(InputValidationException.class,()->runner.run(tracer,over));
            http.perform(post("/api/problems/{id}/execute",problem).param("approach",form).contentType("application/json").content(json.writeValueAsString(over)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors."+field).isString()).andExpect(jsonPath("$.steps").doesNotExist());
            for(var invalid:invalidInputs(problem))assertThrows(InputValidationException.class,()->runner.run(tracer,invalid));
        }
    }

    @ParameterizedTest @ValueSource(strings={"house-robber-2","ninjas-training","unique-paths-2"})
    void zeroCacheReuseSeparateRootsAndAllAnchorsAreReal(String problem){
        var zero=problem.equals("house-robber-2")?Map.<String,Object>of("nums",Collections.nCopies(7,0)):problem.equals("ninjas-training")?Map.<String,Object>of("points",Collections.nCopies(5,List.of(0,0,0))):Map.<String,Object>of("grid",List.of(List.of(1,0,0),List.of(0,0,0),List.of(0,0,0)));
        for(String form:FORMS){
            var tracer=approaches.resolve(problem,form).tracer();var trace=assertAnswer(tracer,zero);var lines=new HashSet<Integer>();
            for(var input:List.of(zero,Map.<String,Object>of()))for(var step:runner.run(tracer,input).getSteps())lines.add(step.getActiveLine());
            assertEquals(new HashSet<>(AnnotatedCode.parse(tracer.annotatedCode()).getAnchors().values()),lines);
            if(form.equals("canonical"))continue;
            var computed=new HashSet<String>();var calls=new HashSet<String>();int hits=0,roots=0;
            for(int i=0;i<trace.getSteps().size();i++){
                var step=trace.getSteps().get(i);var vars=step.getVariables();String event=vars.getOrDefault("event","");
                if(event.equals("enter")){assertTrue(calls.add(vars.get("callId")));if(step.getCallStack().size()==1)roots++;}
                if(form.equals("recursion"))assertNull(step.getDpTable());
                else{
                    assertNotNull(step.getDpTable());if(Set.of("base","store").contains(event))assertTrue(computed.add(vars.get("state")));
                    if(event.equals("cache-hit")){hits++;assertEquals("0",vars.get("value"));assertEquals("return",trace.getSteps().get(i+1).getVariables().get("event"));assertEquals(vars.get("callId"),trace.getSteps().get(i+1).getVariables().get("callId"));}
                }
            }
            assertEquals(problem.equals("house-robber-2")?2:problem.equals("ninjas-training")?3:1,roots);
            assertEquals(calls.size(),Integer.parseInt(last(trace).get("calls")));if(form.equals("memoization"))assertTrue(hits>0);
        }
        if(problem.equals("house-robber-2"))for(String form:FORMS){assertAnswer(approaches.resolve(problem,form).tracer(),Map.of("nums",List.of(2,7)));assertAnswer(approaches.resolve(problem,form).tracer(),Map.of("nums",List.of(9,1,1,9)));}
    }

    @ParameterizedTest @ValueSource(strings={"house-robber-2","ninjas-training","unique-paths-2"})
    void tinyBudgetRetiresRecursionBeforeAnyAnswer(String problem){
        for(String form:List.of("recursion","memoization")){
            var actual=approaches.resolve(problem,form).tracer();
            var capped=new AlgorithmTracer(){
                public String id(){return actual.id();}public DsType dsType(){return actual.dsType();}public String annotatedCode(){return actual.annotatedCode();}
                public InputSpec inputSpec(){return actual.inputSpec().withMaxSteps(3);}public Map<String,Object> alternateInput(){return actual.alternateInput();}public void run(Inputs in,StepEmitter emit){actual.run(in,emit);}
            };
            var trace=runner.run(capped,maximum(problem,form));assertTrue(trace.isTruncated());assertEquals(3,trace.getStepCount());assertTrue(trace.getSteps().stream().noneMatch(s->s.getVariables().containsKey("answer")));
        }
    }
    @Test void circularStateArgumentsFitTheExistingNodeLabel(){
        for(String form:List.of("recursion","memoization")){
            var tracer=approaches.resolve("house-robber-2",form).tracer();
            for(var step:runner.run(tracer,maximum(tracer.id(),form)).getSteps())for(String frame:step.getCallStack()){
                String arguments=frame.substring(frame.indexOf('(')+1,frame.lastIndexOf(')'));
                assertTrue(arguments.length()<=13,"Renderer would hide a circular state coordinate: "+frame);
            }
        }
    }
    @Test void ninjaZeroScoresNeverInventAnActivity(){
        var trace=runner.run(canonical.find("ninjas-training").orElseThrow(),Map.of("points",List.of(List.of(0,0,0),List.of(0,0,0))));
        assertEquals("0",last(trace).get("answer"));assertTrue(trace.getSteps().stream().noneMatch(s->s.getDescription().contains("activity -1")));
    }
    private ExecutionTrace assertAnswer(AlgorithmTracer tracer,Map<String,Object> input){var trace=runner.run(tracer,input);assertFalse(trace.isTruncated(),tracer.id()+input);assertEquals(String.valueOf(oracle(tracer.id(),input)),last(trace).get("answer"),input.toString());assertTrue(trace.getSteps().get(trace.getStepCount()-1).getCallStack().isEmpty());return trace;}
    private static Map<String,String> last(ExecutionTrace t){return t.getSteps().get(t.getStepCount()-1).getVariables();}
    private static int[] array(Map<String,Object> in){return ((List<Integer>)in.get("nums")).stream().mapToInt(Integer::intValue).toArray();}
    private static int[][] grid(Map<String,Object> in){return ((List<List<Integer>>)in.get(in.containsKey("points")?"points":"grid")).stream().map(r->r.stream().mapToInt(Integer::intValue).toArray()).toArray(int[][]::new);}
    static Map<String,Object> maximum(String id,String form){
        if(id.equals("house-robber-2"))return Map.of("nums",Collections.nCopies(form.equals("recursion")?10:16,999));
        if(id.equals("ninjas-training"))return Map.of("points",Collections.nCopies(form.equals("recursion")?6:10,List.of(100,100,100)));
        return Map.of("grid",Collections.nCopies(form.equals("recursion")?4:8,Collections.nCopies(form.equals("recursion")?4:8,0)));
    }
    private static List<Map<String,Object>> invalidInputs(String id){
        if(id.equals("house-robber-2"))return List.of(Map.of("nums",List.of(1)),Map.of("nums",List.of(-1,0)),Map.of("nums",List.of(0,1000)));
        String key=id.equals("ninjas-training")?"points":"grid";
        return List.of(Map.of(key,List.of()),Map.of(key,List.of(List.of())),Map.of(key,List.of(List.of(-1,0,0))),Map.of(key,List.of(List.of(0,0,101))),Map.of(key,List.of(List.of(0,0),List.of(0))),id.equals("ninjas-training")?Map.of(key,List.of(List.of(0,0))):Map.of(key,List.of(List.of(2))));
    }
    private static List<Map<String,Object>> cases(String id){
        var result=new ArrayList<Map<String,Object>>();
        if(id.equals("house-robber-2"))for(int n=2;n<=5;n++)for(int encoded=0;encoded<(int)Math.pow(3,n);encoded++){int x=encoded;var values=new ArrayList<Integer>();for(int i=0;i<n;i++,x/=3)values.add(new int[]{0,1,3}[x%3]);result.add(Map.of("nums",values));}
        else if(id.equals("ninjas-training"))for(int days=1;days<=3;days++)for(int bits=0;bits<(1<<(3*days));bits++){var points=new ArrayList<List<Integer>>();for(int d=0;d<days;d++)points.add(List.of((bits>>(3*d))&1,(bits>>(3*d+1))&1,(bits>>(3*d+2))&1));result.add(Map.of("points",points));}
        else for(int rows=1;rows<=3;rows++)for(int cols=1;cols<=3;cols++)for(int bits=0;bits<(1<<(rows*cols));bits++){var grid=new ArrayList<List<Integer>>();for(int r=0;r<rows;r++){var row=new ArrayList<Integer>();for(int c=0;c<cols;c++)row.add((bits>>(r*cols+c))&1);grid.add(row);}result.add(Map.of("grid",grid));}
        var random=new Random(9017);
        for(int sample=0;sample<30;sample++){
            if(id.equals("house-robber-2")){
                var values=new ArrayList<Integer>();int n=2+random.nextInt(7);
                for(int i=0;i<n;i++)values.add(random.nextInt(1000));result.add(Map.of("nums",values));
            }else{
                int rows=1+random.nextInt(id.equals("ninjas-training")?5:4),cols=id.equals("ninjas-training")?3:1+random.nextInt(4);
                var values=new ArrayList<List<Integer>>();for(int r=0;r<rows;r++){var row=new ArrayList<Integer>();for(int c=0;c<cols;c++)row.add(random.nextInt(id.equals("ninjas-training")?101:2));values.add(row);}
                result.add(Map.of(id.equals("ninjas-training")?"points":"grid",values));
            }
        }
        return result;
    }
    private static int oracle(String id,Map<String,Object> input){
        if(id.equals("house-robber-2")){int[] values=array(input);int best=0;for(int mask=0;mask<(1<<values.length);mask++){if((mask&(mask<<1))!=0||(mask&1)!=0&&(mask&(1<<(values.length-1)))!=0)continue;int sum=0;for(int i=0;i<values.length;i++)if((mask&(1<<i))!=0)sum+=values[i];best=Math.max(best,sum);}return best;}
        if(id.equals("ninjas-training"))return schedule(grid(input),0,-1,0);
        return path(grid(input),0,0);
    }
    private static int schedule(int[][] points,int day,int previous,int total){if(day==points.length)return total;int best=0;for(int task=0;task<3;task++)if(task!=previous)best=Math.max(best,schedule(points,day+1,task,total+points[day][task]));return best;}
    private static int path(int[][] grid,int row,int col){if(grid[row][col]==1)return 0;if(row==grid.length-1&&col==grid[0].length-1)return 1;return(row+1<grid.length?path(grid,row+1,col):0)+(col+1<grid[0].length?path(grid,row,col+1):0);}
}
