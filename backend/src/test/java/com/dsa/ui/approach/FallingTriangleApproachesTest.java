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
class FallingTriangleApproachesTest {
    @Autowired SolutionApproachRegistry approaches;
    @Autowired TracerRegistry canonical;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;
    @TempDir Path temporary;
    private final ObjectMapper json=new ObjectMapper();
    private static final List<String> FORMS=List.of("recursion","memoization","canonical");

    @ParameterizedTest @ValueSource(strings={"minimum-falling-path-sum","triangle-min-path-sum"})
    void discoveryAndBothHttpEncodingsKeepTheCanonicalIdentity(String id) throws Exception {
        assertEquals(new HashSet<>(FORMS),new HashSet<>(approaches.available(id).stream().map(SolutionApproach::id).toList()));
        assertSame(canonical.find(id).orElseThrow(),approaches.resolve(id,null).tracer());
        assertEquals("Tabulation",approaches.resolve(id,null).label());
        for(String form:FORMS){
            var tracer=approaches.resolve(id,form).tracer();
            assertEquals(form.equals("canonical")?DsType.DP_TABLE:DsType.RECURSION_TREE,tracer.dsType());
            var detail=json.readTree(http.perform(get("/api/problems/{id}",id).param("approach",form)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            assertEquals(3,detail.path("approaches").size());
            for(String encoding:List.of("full","delta")){
                String client="falling-triangle-identity-"+id+form+encoding;
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

    @ParameterizedTest @ValueSource(strings={"minimum-falling-path-sum","triangle-min-path-sum"})
    void exactJavaSourcesAndEveryTracerMatchIndependentForwardPaths(String id) throws Exception {
        for(String form:FORMS){
            var tracer=approaches.resolve(id,form).tracer();String source=AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode();
            Path directory=Files.createTempDirectory(temporary,"solution"),file=directory.resolve("Solution.java");
            Files.writeString(file,form.equals("canonical")?"public class Solution {\n"+source+"\n}":source);
            var errors=new ByteArrayOutputStream();
            assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,errors,"--release","17","-classpath",directory.toString(),"-d",directory.toString(),file.toString()),errors::toString);
            try(var loader=new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()},ClassLoader.getPlatformClassLoader())){
                var type=loader.loadClass("Solution");var method=Arrays.stream(type.getDeclaredMethods()).filter(m->java.lang.reflect.Modifier.isPublic(m.getModifiers())).findFirst().orElseThrow();
                assertEquals(int.class,method.getReturnType());
                var inputs=new ArrayList<>(cases(id));inputs.add(InputValidator.defaults(tracer.inputSpec()).asMap());inputs.add(tracer.alternateInput());inputs.add(maximum(id,form));
                for(var input:inputs){assertAnswer(tracer,input);assertEquals(oracle(id,input),method.invoke(type.getConstructor().newInstance(),(Object)grid(input)),id+input);}
            }
        }
    }

    @ParameterizedTest @ValueSource(strings={"minimum-falling-path-sum","triangle-min-path-sum"})
    void measuredMaximaAndValueShapeBoundsAreEnforced(String id) throws Exception {
        for(String form:FORMS){
            var tracer=approaches.resolve(id,form).tracer();var input=maximum(id,form);var trace=assertAnswer(tracer,input);
            if(!form.equals("canonical"))assertEquals(id.equals("minimum-falling-path-sum")?(form.equals("recursion")?188:162):(form.equals("recursion")?127:57),Integer.parseInt(last(trace).get("calls")));
            var rows=new ArrayList<>((List<List<Integer>>)input.get(field(id)));rows.add(rows.get(0));var over=Map.<String,Object>of(field(id),rows);
            assertThrows(InputValidationException.class,()->runner.run(tracer,over));
            String client="falling-triangle-bounds-"+id+form;
            http.perform(post("/api/problems/{id}/execute",id).param("approach",form).contentType("application/json").content(json.writeValueAsString(over))
                    .with(r->{r.setRemoteAddr(client);return r;})).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors."+field(id)).isString()).andExpect(jsonPath("$.steps").doesNotExist())
                    .andDo(r->assertEquals(client,r.getRequest().getRemoteAddr()));
            var wide=grid(input);var wider=new ArrayList<List<Integer>>();for(int[] row:wide){var values=new ArrayList<Integer>();for(int v:row)values.add(v);values.add(0);wider.add(values);}
            assertThrows(InputValidationException.class,()->runner.run(tracer,Map.of(field(id),wider)));
            for(var invalid:List.of(List.of(List.of(-101)),List.of(List.of(101)),List.of(List.of(0),List.of(0,0)),List.of(),List.of(List.of())))
                assertThrows(InputValidationException.class,()->runner.run(tracer,Map.of(field(id),invalid)));
        }
    }

    @ParameterizedTest @ValueSource(strings={"minimum-falling-path-sum","triangle-min-path-sum"})
    void cachesRealZeroAndMinusOneWithCompleteStatesAndAllAnchors(String id){
        for(int value:List.of(0,-1))for(String form:List.of("recursion","memoization")){
            var tracer=approaches.resolve(id,form).tracer();var trace=assertAnswer(tracer,constant(id,4,4,value));
            var lines=new HashSet<Integer>();var computed=new HashSet<String>();int roots=0,hits=0;boolean minusOneHit=false;
            for(int i=0;i<trace.getStepCount();i++){
                var step=trace.getSteps().get(i);lines.add(step.getActiveLine());var vars=step.getVariables();String event=vars.get("event");
                if("enter".equals(event)&&step.getCallStack().size()==1)roots++;
                if(form.equals("recursion"))assertNull(step.getDpTable());
                else{
                    assertNotNull(step.getDpTable());
                    if(Set.of("base","store").contains(event))assertTrue(computed.add(vars.get("state")));
                    if("cache-hit".equals(event)){hits++;minusOneHit|="-1".equals(vars.get("value"));assertEquals("return",trace.getSteps().get(i+1).getVariables().get("event"));}
                    if(id.equals("triangle-min-path-sum")){
                        assertTrue(vars.get("memoDomain").contains("excluded"));
                        var cells=step.getDpTable().cells();
                        for(int r=0;r<cells.size();r++)for(int c=r+1;c<cells.get(r).size();c++){assertEquals("#",cells.get(r).get(c).value());assertEquals("void",cells.get(r).get(c).state());}
                    }
                }
            }
            assertEquals(new HashSet<>(AnnotatedCode.parse(tracer.annotatedCode()).getAnchors().values()),lines);
            assertEquals(id.equals("minimum-falling-path-sum")?4:1,roots);
            if(form.equals("memoization")){assertTrue(hits>0);if(value==-1)assertTrue(minusOneHit,"-1 is a known cache value, not an unknown sentinel");}
        }
    }

    @ParameterizedTest @ValueSource(strings={"minimum-falling-path-sum","triangle-min-path-sum"})
    void threeStepBudgetStopsBeforeAnswer(String id){
        for(String form:List.of("recursion","memoization")){
            var actual=approaches.resolve(id,form).tracer();
            var capped=new AlgorithmTracer(){public String id(){return actual.id();}public DsType dsType(){return actual.dsType();}public String annotatedCode(){return actual.annotatedCode();}public InputSpec inputSpec(){return actual.inputSpec().withMaxSteps(3);}public Map<String,Object> alternateInput(){return actual.alternateInput();}public void run(Inputs in,StepEmitter emit){actual.run(in,emit);}};
            var trace=runner.run(capped,maximum(id,form));assertTrue(trace.isTruncated());assertEquals(3,trace.getStepCount());assertTrue(trace.getSteps().stream().noneMatch(s->s.getVariables().containsKey("answer")));
        }
    }

    @Test void fallingSingleColumnNarratesOnlyItsRealPredecessor(){
        var tracer=canonical.find("minimum-falling-path-sum").orElseThrow();
        var trace=assertAnswer(tracer,Map.of("matrix",List.of(List.of(2),List.of(3))));
        var step=trace.getSteps().stream().filter(s->s.getVariables().containsKey("candidates")).findFirst().orElseThrow();
        assertEquals("1",step.getVariables().get("candidates"));assertTrue(step.getDescription().contains("only the cell directly above"));
        assertFalse(step.getDescription().contains("two above"));assertFalse(step.getDescription().contains("upper-right"));
        for(int rows=1;rows<=3;rows++)for(int cols=1;cols<=3;cols++){
            var shaped=assertAnswer(tracer,constant(tracer.id(),rows,cols,2));
            var combinations=shaped.getSteps().stream().filter(s->s.getVariables().containsKey("candidates")).toList();
            assertEquals((rows-1)*cols,combinations.size());
            for(var combination:combinations){
                int row=Integer.parseInt(combination.getVariables().get("row")),col=Integer.parseInt(combination.getVariables().get("col"));
                int expected=1+(col>0?1:0)+(col+1<cols?1:0),reads=0;
                assertEquals(String.valueOf(expected),combination.getVariables().get("candidates"));
                var cells=combination.getDpTable().cells();
                for(int r=0;r<cells.size();r++)for(int c=0;c<cells.get(r).size();c++)if(cells.get(r).get(c).state().equals("read")){reads++;assertEquals(row-1,r);assertTrue(Math.abs(c-col)<=1);}
                assertEquals(expected,reads);
                assertTrue(combination.getDescription().contains(expected==1?"only the cell directly above":expected==2?"only the two":"all three"));
            }
        }
    }

    @Test void rectangularTrianglePaddingAndExtraRowsAreNeverReinterpreted(){
        var inputs=List.of(Map.<String,Object>of("triangle",List.of(List.of(2),List.of(100),List.of(-100))),
                Map.<String,Object>of("triangle",List.of(List.of(2,99),List.of(3,4),List.of(-100,-100))),
                Map.<String,Object>of("triangle",List.of(List.of(2,-100,-100),List.of(3,4,-100))));
        for(String form:FORMS)for(var input:inputs){var trace=assertAnswer(approaches.resolve("triangle-min-path-sum",form).tracer(),input);assertEquals(input,trace.getResolvedInput());}
    }

    private ExecutionTrace assertAnswer(AlgorithmTracer tracer,Map<String,Object> input){var trace=runner.run(tracer,input);assertFalse(trace.isTruncated(),tracer.id()+input);assertEquals(String.valueOf(oracle(tracer.id(),input)),last(trace).get("answer"));assertTrue(trace.getSteps().get(trace.getStepCount()-1).getCallStack().isEmpty());return trace;}
    private static Map<String,String> last(ExecutionTrace trace){return trace.getSteps().get(trace.getStepCount()-1).getVariables();}
    private static String field(String id){return id.equals("minimum-falling-path-sum")?"matrix":"triangle";}
    private static int[][] grid(Map<String,Object> in){return ((List<List<Integer>>)in.values().iterator().next()).stream().map(row->row.stream().mapToInt(Integer::intValue).toArray()).toArray(int[][]::new);}
    private static Map<String,Object> constant(String id,int rows,int cols,int value){return Map.of(field(id),Collections.nCopies(rows,Collections.nCopies(cols,value)));}
    static Map<String,Object> maximum(String id,String form){return constant(id,form.equals("recursion")?(id.equals("minimum-falling-path-sum")?4:7):8,form.equals("recursion")?(id.equals("minimum-falling-path-sum")?6:7):8,-100);}
    private static List<Map<String,Object>> cases(String id){
        var cases=new ArrayList<Map<String,Object>>();int[] alphabet={-1,0,2};
        for(int rows=1;rows<=3;rows++)for(int cols=1;cols<=3;cols++)if(rows*cols<=4)for(int code=0;code<Math.pow(3,rows*cols);code++){
            int n=code;var values=new ArrayList<List<Integer>>();for(int r=0;r<rows;r++){var row=new ArrayList<Integer>();for(int c=0;c<cols;c++,n/=3)row.add(alphabet[n%3]);values.add(row);}cases.add(Map.of(field(id),values));
        }
        if(id.equals("minimum-falling-path-sum"))for(int bits=0;bits<512;bits++){var rows=new ArrayList<List<Integer>>();for(int r=0;r<3;r++)rows.add(List.of(((bits>>(r*3))&1)-1,((bits>>(r*3+1))&1)-1,((bits>>(r*3+2))&1)-1));cases.add(Map.of("matrix",rows));}
        else for(int code=0;code<729;code++){int n=code;var rows=new ArrayList<List<Integer>>();for(int r=0;r<3;r++){var row=new ArrayList<Integer>();for(int c=0;c<3;c++){row.add(c<=r?alphabet[n%3]:99);if(c<=r)n/=3;}rows.add(row);}cases.add(Map.of("triangle",rows));}
        var random=new Random(7319);for(int sample=0;sample<30;sample++){int rows=1+random.nextInt(id.equals("minimum-falling-path-sum")?4:7),cols=1+random.nextInt(id.equals("minimum-falling-path-sum")?6:7);var values=new ArrayList<List<Integer>>();for(int r=0;r<rows;r++){var row=new ArrayList<Integer>();for(int c=0;c<cols;c++)row.add(random.nextInt(201)-100);values.add(row);}cases.add(Map.of(field(id),values));}
        return cases;
    }
    private static int oracle(String id,Map<String,Object> input){int[][] values=grid(input);if(id.equals("triangle-min-path-sum"))return triangle(values,0,0,0,Math.min(values.length,values[0].length));int best=Integer.MAX_VALUE;for(int start=0;start<values[0].length;start++)best=Math.min(best,fall(values,0,start,0));return best;}
    private static int triangle(int[][] values,int row,int col,int sum,int n){sum+=values[row][col];return row==n-1?sum:Math.min(triangle(values,row+1,col,sum,n),triangle(values,row+1,col+1,sum,n));}
    private static int fall(int[][] values,int row,int col,int sum){sum+=values[row][col];if(row==values.length-1)return sum;int best=Integer.MAX_VALUE;for(int next=Math.max(0,col-1);next<=Math.min(values[0].length-1,col+1);next++)best=Math.min(best,fall(values,row+1,next,sum));return best;}
}
