package com.dsa.ui.approach;

import com.dsa.ui.model.*;
import com.dsa.ui.tracer.StepEmitter;
import java.util.*;
import java.util.function.LongSupplier;

/** Request-local long-valued instrumentation; null is unknown, zero is a real result. */
final class RecursiveLongDpExecution {
    record State(int row,int col,String label) {}
    private final StepEmitter emit;
    private final Long[][] memo;
    private final List<String> rows,cols;
    private final Set<State> computed=new HashSet<>();
    private int calls,hits,evaluations;
    RecursiveLongDpExecution(StepEmitter emit,boolean cached,List<String> rows,List<String> cols){
        this.emit=emit;this.rows=rows;this.cols=cols;memo=cached?new Long[rows.size()][cols.size()]:null;
    }
    void start(State root,String description){event("start","start",root,-1,null,description);}
    void done(State root,long answer){step("done","done",root,-1,answer,"All calls returned. Answer = "+answer+".").var("answer",answer).step();}
    long evaluate(State state,boolean base,String choices,LongSupplier recurrence){
        int call=++calls;emit.push("best("+state.label+") #"+call);long answer;String anchor;
        try{
            event("enter","enter",state,call,null,"Enter call "+call+": "+state.label+".");
            Long known=memo==null?null:memo[state.row][state.col];
            if(known!=null){
                answer=known;hits++;anchor="hit";
                event(anchor,"cache-hit",state,call,answer,"Reuse cached "+state.label+" = "+answer+"; no children are expanded.");
            }else{
                if(!base)event("branch","branch",state,call,null,choices);
                answer=recurrence.getAsLong();evaluations++;computed.add(state);
                if(memo!=null)memo[state.row][state.col]=answer;
                anchor=base?"base":"combine";
                event(anchor,base?"base":memo!=null?"store":"combine",state,call,answer,
                        (base?"Base case: ":"Combine returned dependencies: ")+state.label+" = "+answer
                                +(memo!=null?". Store this known value, including zero.":". No result is cached."));
            }
        }finally{emit.pop();}
        event(anchor,"return",state,call,answer,"Call "+call+" returns "+answer+"; its frame has left the stack.");return answer;
    }
    private void event(String anchor,String event,State state,int call,Long value,String description){step(anchor,event,state,call,value,description).step();}
    private StepEmitter.Step step(String anchor,String event,State state,int call,Long value,String description){
        var step=emit.at(anchor).say(description).var("event",event).var("state",state.label)
                .var("calls",calls).var("cacheHits",hits).var("stateEvaluations",evaluations).var("computedStates",computed.size());
        if(call>=0)step.var("callId",call);if(value!=null)step.var("value",value);
        if(memo!=null){
            List<List<DpCell>> cells=new ArrayList<>();
            for(int r=0;r<rows.size();r++){
                List<DpCell> row=new ArrayList<>();
                for(int c=0;c<cols.size();c++){
                    Long v=memo[r][c];String colour=v==null?"void":r==state.row&&c==state.col
                            ?"cache-hit".equals(event)?"read":Set.of("base","store").contains(event)?"probe":"known":"known";
                    row.add(new DpCell(v==null?"·":String.valueOf(v),colour));
                }
                cells.add(row);
            }
            step.dpTable(new DpTable(rows,cols,cells));
        }
        return step;
    }
}
