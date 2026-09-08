package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Simulates LRU page replacement with the cache ordered most- to least-recently used. */
@Component
public class LruPageReplacementTracer implements AlgorithmTracer {
    @Override public String id() { return "lru-page-replacement"; }
    @Override public DsType dsType() { return DsType.ARRAY; }
    @Override public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("pages", FieldType.INT_ARRAY).label("Page references")
                        .length(1, 80).values(0, 999).defaultValue(List.of(1, 2, 3, 1, 4, 5)).build(),
                InputField.of("capacity", FieldType.INT).label("Frame capacity")
                        .range(1, 12).defaultValue(3).build());
    }
    @Override public Map<String, Object> alternateInput() {
        return Map.of("pages", List.of(1, 2, 1, 3, 1, 2, 4), "capacity", 2);
    }
    @Override public String annotatedCode() {
        return """
               public int pageFaults(int[] pages, int capacity) {
                   // @a init
                   Deque<Integer> cache = new ArrayDeque<>();
                   int faults = 0;
                   for (int page : pages) {
                       // @a request
                       if (cache.remove(page)) {
                           // @a hit
                           cache.addFirst(page);
                           continue;
                       }
                       // @a miss
                       faults++;
                       if (cache.size() == capacity) {
                           // @a evict
                           cache.removeLast();
                       }
                       cache.addFirst(page);
                   }
                   // @a done
                   return faults;
               }""";
    }
    @Override public void run(Inputs in, StepEmitter emit) {
        int[] pages = in.getIntArray("pages");
        int capacity = in.getInt("capacity");
        Deque<Integer> cache = new ArrayDeque<>();
        int faults = 0;
        emit.at("init").say("%d empty frames. Cache order is MRU at the front, LRU at the back.", capacity)
                .var("faults", faults).array(pages).stack(cache).step();
        for (int i = 0; i < pages.length; i++) {
            int page = pages[i];
            emit.at("request").say("Request page %d at reference %d.", page, i)
                    .var("faults", faults).array(pages, i).stack(cache).step();
            if (cache.remove(page)) {
                cache.addFirst(page);
                emit.at("hit").say("Page %d is resident: move it to MRU, no fault.", page)
                        .var("faults", faults).array(pages, i).stack(cache).step();
                continue;
            }
            faults++;
            emit.at("miss").say("Page %d is absent: fault #%d.", page, faults)
                    .var("faults", faults).array(pages, i).stack(cache).step();
            if (cache.size() == capacity) {
                int evicted = cache.removeLast();
                emit.at("evict").say("Frames full: evict least-recently-used page %d.", evicted)
                        .var("evicted", evicted).var("faults", faults)
                        .array(pages, i).stack(cache).step();
            }
            cache.addFirst(page);
        }
        emit.at("done").say("Reference string complete with %d page fault(s). Final MRU→LRU order: %s.", faults, cache)
                .var("faults", faults).array(pages).stack(cache).step();
    }
}
