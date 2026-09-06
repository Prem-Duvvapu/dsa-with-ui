package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ListNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LFU Cache (LeetCode 460). Evicts the least-frequently-used key; ties broken by
 * least-recently-used. Same mini-language input shape as LRU Cache.
 *
 * <p>Internally: a HashMap for key→value/freq, a frequency→LinkedHashSet of keys,
 * and a minFreq counter. Traced as LINKED_LIST to show the eviction order by frequency
 * group.
 */
@Component
public class LfuCacheTracer implements AlgorithmTracer {

    private static final String OP = "(put \\d{1,4} \\d{1,4}|get \\d{1,4})";

    @Override
    public String id() {
        return "lfu-cache";
    }

    @Override
    public DsType dsType() {
        return DsType.LINKED_LIST;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("capacity", FieldType.INT)
                        .label("Capacity")
                        .help("Max keys before eviction of the least-frequently-used.")
                        .range(1, 6)
                        .defaultValue(2)
                        .build(),
                InputField.of("operations", FieldType.STRING)
                        .label("Operations")
                        .help("Semicolon-separated ops, each \"put K V\" or \"get K\".")
                        .length(1, 200)
                        .constraint("pattern", OP + "(;" + OP + ")*")
                        .constraint("operationsHint",
                                "Semicolon-separated ops, each \"put K V\" or \"get K\" with numbers "
                                        + "0-9999, e.g. \"put 1 1;put 2 2;get 1\".")
                        .defaultValue("put 1 1;put 2 2;get 1;put 3 3;get 2;get 3")
                        .build());
    }

    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "capacity", 3,
                "operations", "put 1 10;put 2 20;put 1 15;get 1;put 3 30;put 4 40;get 2");
    }

    @Override
    public String annotatedCode() {
        return """
               class LFUCache {
                   int capacity, minFreq;
                   Map<Integer, int[]> cache;     // key -> {value, freq}
                   Map<Integer, LinkedHashSet<Integer>> freqMap;

                   // @a init
                   LFUCache(int capacity) {
                       this.capacity = capacity;
                       this.minFreq = 0;
                       this.cache = new HashMap<>();
                       this.freqMap = new HashMap<>();
                   }

                   int get(int key) {
                       if (!cache.containsKey(key)) {
                           // @a miss
                           return -1;
                       }
                       // @a hit
                       increaseFreq(key);
                       return cache.get(key)[0];
                   }

                   void put(int key, int value) {
                       if (cache.containsKey(key)) {
                           cache.get(key)[0] = value;
                           // @a update
                           increaseFreq(key);
                           return;
                       }
                       if (cache.size() >= capacity) {
                           // @a evict
                           int evictKey = freqMap.get(minFreq).iterator().next();
                           freqMap.get(minFreq).remove(evictKey);
                           cache.remove(evictKey);
                       }
                       cache.put(key, new int[]{value, 1});
                       freqMap.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
                       // @a insert
                       minFreq = 1;
                   }

                   void increaseFreq(int key) {
                       int freq = cache.get(key)[1];
                       cache.get(key)[1] = freq + 1;
                       freqMap.get(freq).remove(key);
                       if (freqMap.get(freq).isEmpty() && freq == minFreq) minFreq++;
                       // @a freqUp
                       freqMap.computeIfAbsent(freq + 1, k -> new LinkedHashSet<>()).add(key);
                   }
               }""";
    }

    // Internal LFU state
    private static class LFU {
        int capacity, minFreq;
        Map<Integer, int[]> cache = new HashMap<>(); // key -> {value, freq}
        Map<Integer, java.util.LinkedHashSet<Integer>> freqMap = new HashMap<>();

        LFU(int capacity) {
            this.capacity = capacity;
            this.minFreq = 0;
        }

        int get(int key) {
            if (!cache.containsKey(key)) return -1;
            increaseFreq(key);
            return cache.get(key)[0];
        }

        void put(int key, int value) {
            if (cache.containsKey(key)) {
                cache.get(key)[0] = value;
                increaseFreq(key);
                return;
            }
            int evictedKey = -1;
            if (cache.size() >= capacity) {
                var bucket = freqMap.get(minFreq);
                evictedKey = bucket.iterator().next();
                bucket.remove(evictedKey);
                if (bucket.isEmpty()) freqMap.remove(minFreq);
                cache.remove(evictedKey);
            }
            cache.put(key, new int[]{value, 1});
            freqMap.computeIfAbsent(1, k -> new java.util.LinkedHashSet<>()).add(key);
            minFreq = 1;
        }

        void increaseFreq(int key) {
            int freq = cache.get(key)[1];
            cache.get(key)[1] = freq + 1;
            var bucket = freqMap.get(freq);
            bucket.remove(key);
            if (bucket.isEmpty()) {
                freqMap.remove(freq);
                if (freq == minFreq) minFreq++;
            }
            freqMap.computeIfAbsent(freq + 1, k -> new java.util.LinkedHashSet<>()).add(key);
        }
    }

    private List<ListNode> snapshot(LFU lfu) {
        // Show cache entries ordered by frequency, then recency
        List<ListNode> nodes = new ArrayList<>();
        int id = 0;
        // Collect all entries sorted by freq ascending
        List<Map.Entry<Integer, int[]>> entries = new ArrayList<>(lfu.cache.entrySet());
        entries.sort((a, b) -> {
            int cmp = Integer.compare(a.getValue()[1], b.getValue()[1]);
            return cmp != 0 ? cmp : Integer.compare(a.getKey(), b.getKey());
        });
        for (var entry : entries) {
            int key = entry.getKey();
            int val = entry.getValue()[0];
            int freq = entry.getValue()[1];
            String label = String.format("k%d:v%d(f%d)", key, val, freq);
            ListNode node = new ListNode(
                    id, label, id < entries.size() - 1 ? id + 1 : null, null,
                    freq == lfu.minFreq ? "active" : "default");
            nodes.add(node);
            id++;
        }
        return nodes;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int capacity = in.getInt("capacity");
        String opsStr = in.getString("operations");
        String[] ops = opsStr.split(";");

        LFU lfu = new LFU(capacity);

        emit.at("init")
                .say("LFU Cache with capacity %d. Process %d operations.", capacity, ops.length)
                .var("capacity", capacity).var("ops", ops.length)
                .list(snapshot(lfu)).step();

        for (String op : ops) {
            String[] parts = op.trim().split(" ");
            if (parts[0].equals("get")) {
                int key = Integer.parseInt(parts[1]);
                int result = lfu.get(key);
                if (result == -1) {
                    emit.at("miss")
                            .say("get(%d) → cache miss, return -1.", key)
                            .var("key", key).var("result", -1)
                            .list(snapshot(lfu)).step();
                } else {
                    emit.at("hit")
                            .say("get(%d) → %d. Frequency of key %d increased.", key, result, key)
                            .var("key", key).var("result", result)
                            .list(snapshot(lfu)).step();

                    emit.at("freqUp")
                            .say("Key %d frequency bumped. minFreq = %d.", key, lfu.minFreq)
                            .var("key", key).var("minFreq", lfu.minFreq)
                            .list(snapshot(lfu)).step();
                }
            } else {
                int key = Integer.parseInt(parts[1]);
                int value = Integer.parseInt(parts[2]);
                boolean existed = lfu.cache.containsKey(key);
                boolean willEvict = !existed && lfu.cache.size() >= capacity;
                int evictKey = -1;
                if (willEvict) {
                    var bucket = lfu.freqMap.get(lfu.minFreq);
                    evictKey = bucket.iterator().next();
                }

                lfu.put(key, value);

                if (existed) {
                    emit.at("update")
                            .say("put(%d, %d) — key exists, update value and bump frequency.", key, value)
                            .var("key", key).var("value", value)
                            .list(snapshot(lfu)).step();

                    emit.at("freqUp")
                            .say("Key %d frequency bumped. minFreq = %d.", key, lfu.minFreq)
                            .var("key", key).var("minFreq", lfu.minFreq)
                            .list(snapshot(lfu)).step();
                } else {
                    if (willEvict) {
                        emit.at("evict")
                                .say("Cache full — evict key %d (least frequent, then least recent).", evictKey)
                                .var("evicted", evictKey)
                                .list(snapshot(lfu)).step();
                    }
                    emit.at("insert")
                            .say("put(%d, %d) — insert new key with frequency 1. minFreq reset to 1.",
                                    key, value)
                            .var("key", key).var("value", value).var("minFreq", 1)
                            .list(snapshot(lfu)).step();
                }
            }
        }
    }
}
