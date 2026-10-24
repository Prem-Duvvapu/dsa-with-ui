package com.dsa.ui.approach;

import com.dsa.ui.model.*;
import com.dsa.ui.tracer.StepEmitter;
import java.util.*;
import java.util.function.IntSupplier;

/** Request-local minimum-cost instrumentation; null, zero and unreachable are distinct. */
final class RecursiveMinimumDpExecution {
    static final int INFINITY=1_000_000;
    record State(int row,int col,String label) {}
    private final StepEmitter emit;
    private final Integer[][] memo;
    private final List<String> rows,cols;
    private final Set<State> computed=new HashSet<>();
    private int calls,hits,evaluations;
    RecursiveMinimumDpExecution(StepEmitter emit,boolean cached,int n,int amount){
        this.emit=emit;rows=RecursiveDpExecution.labels("n=",n+1);cols=RecursiveDpExecution.labels("a=",amount+1);
        memo=cached?new Integer[n+1][amount+1]:null;
    }
    void start(State root,String description){event("start","start",root,-1,null,description);}
    void done(State root,int value){
        int answer=value>=INFINITY?-1:value;
        step("done","done",root,-1,value,"All calls returned. "+(answer<0?"The target is unreachable (∞), so the public answer is -1.":"Fewest coins = "+answer+"."))
                .var("answer",answer).step();
    }
    int evaluate(State state,boolean base,String choices,IntSupplier recurrence){
        int call=++calls;emit.push("fewest("+state.label+") #"+call);int answer;String anchor;
        try{
            event("enter","enter",state,call,null,"Enter call "+call+": "+state.label+".");
            Integer known=memo==null?null:memo[state.row][state.col];
            if(known!=null){
                answer=known;hits++;anchor="hit";
                event(anchor,"cache-hit",state,call,answer,"Reuse cached "+state.label+" = "+display(answer)+"; no children are expanded.");
            }else{
                if(!base)event("branch","branch",state,call,null,choices);
                answer=recurrence.getAsInt();evaluations++;computed.add(state);
                if(memo!=null)memo[state.row][state.col]=answer;
                anchor=base?"base":"combine";
                event(anchor,base?"base":memo!=null?"store":"combine",state,call,answer,
                        (base?"Base case: ":"Combine returned dependencies: ")+state.label+" = "+display(answer)
                                +(memo!=null?". Store this known result; zero and unreachable are reusable.":". No result is cached."));
            }
        }finally{emit.pop();}
        event(anchor,"return",state,call,answer,"Call "+call+" returns "+display(answer)+"; its frame has left the stack.");return answer;
    }
    private static String display(int value){return value>=INFINITY?"∞ (unreachable)":String.valueOf(value);}
    private void event(String anchor,String event,State state,int call,Integer value,String description){step(anchor,event,state,call,value,description).step();}
    private StepEmitter.Step step(String anchor,String event,State state,int call,Integer value,String description){
        var step=emit.at(anchor).say(description).var("event",event).var("state",state.label)
                .var("calls",calls).var("cacheHits",hits).var("stateEvaluations",evaluations).var("computedStates",computed.size());
        if(call>=0)step.var("callId",call);if(value!=null)step.var("value",value).var("valueDisplay",display(value));
        if(memo!=null){
            List<List<DpCell>> cells=new ArrayList<>();
            for(int r=0;r<rows.size();r++){
                List<DpCell> row=new ArrayList<>();
                for(int c=0;c<cols.size();c++){
                    Integer v=memo[r][c];String colour=v==null?"void":r==state.row&&c==state.col
                            ?"cache-hit".equals(event)?"read":Set.of("base","store").contains(event)?"probe":"known":"known";
                    row.add(new DpCell(v==null?"·":v>=INFINITY?"∞":String.valueOf(v),colour));
                }
                cells.add(row);
            }
            step.dpTable(new DpTable(rows,cols,cells)).var("memoDomain","· uncomputed; ∞ unreachable (INFINITY=1000000); zero is a known result.");
        }
        return step;
    }
}
