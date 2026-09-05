package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ListNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LRU Cache (LeetCode 146) is a sequence of {@code put}/{@code get} calls on one stateful
 * object, not a single before/after computation — the same shape that got the entire Heaps
 * &amp; PriorityQueue topic rejected (see {@code PROMPT-J-full-roadmap.md}). It is made to
 * fit the tracer contract's one-input-one-trace shape by encoding the WHOLE operation
 * sequence as a single {@link FieldType#STRING}: {@code "put 1 1;put 2 2;get 1"}. No new
 * {@code FieldType} is introduced — this is the same regex-constrained STRING pattern
 * {@link KmpLpsTracer} uses for its alphabet, just with a richer mini-language.
 *
 * <p>The cache's actual shape — recency order backed by a hash map — is a doubly linked
 * list from most- to least-recently-used, so this traces as {@link DsType#LINKED_LIST}
 * rather than {@code Stack}: each node's label carries {@code "key:value"}, and the chain
 * itself IS the eviction order.
 */
@Component
public class LruCacheTracer implements AlgorithmTracer {

    private static final String OP = "(put \\d{1,4} \\d{1,4}|get \\d{1,4})";

    @Override
    public String id() {
        return "lru-cache";
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
                        .help("How many keys the cache holds before a new key evicts one.")
                        .range(1, 6)
                        .defaultValue(2)
                        .build(),
                InputField.of("operations", FieldType.STRING)
                        .label("Operations")
                        .help("Semicolon-separated ops, each \"put K V\" or \"get K\".")
                        .length(1, 200)
                        .constraint("pattern", OP + "(;" + OP + ")*")
                        .constraint("operationsHint",
                                "Semicolon-separated ops, each \"put K V\" or \"get K\" with whole "
                                        + "numbers 0-9999, e.g. \"put 1 1;put 2 2;get 1\".")
                        .defaultValue("put 1 1;put 2 2;get 1;put 3 3;get 2;put 4 4;get 1;get 3;get 4")
                        .build());
    }

    /**
     * A bigger cache (3 vs 2), a longer sequence, and — critically — a {@code put} on a key
     * already present, which the default never exercises (every default {@code put} is a
     * new key). Verified by hand: puts 1,2,3 fill the cache; get(2) makes 1 the LRU; put(4)
     * evicts 1; put(5) evicts 3 (the new LRU after 4 displaced 2 from the tail); get(3) and
     * get(1) miss on now-evicted keys; put(2,99) updates the still-resident key 2 in place.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "capacity", 3,
                "operations", "put 1 10;put 2 20;put 3 30;get 2;put 4 40;get 1;put 5 50;"
                        + "get 3;get 4;get 5;put 2 99;get 2");
    }

    @Override
    public String annotatedCode() {
        return """
               public class LRUCache {
                   private final int capacity;
                   private final Map<Integer, Integer> cache = new HashMap<>();
                   private final LinkedList<Integer> order = new LinkedList<>(); // front = MRU

                   public LRUCache(int capacity) {
                       // @a init
                       this.capacity = capacity;
                   }

                   public int get(int key) {
                       if (!cache.containsKey(key)) {
                           // @a get.miss
                           return -1;
                       }
                       // @a get.hit
                       order.remove((Integer) key);
                       order.addFirst(key);
                       return cache.get(key);
                   }

                   public void put(int key, int value) {
                       if (cache.containsKey(key)) {
                           // @a put.update
                           cache.put(key, value);
                           order.remove((Integer) key);
                           order.addFirst(key);
                           return;
                       }
                       if (cache.size() == capacity) {
                           // @a put.evict
                           int lruKey = order.removeLast();
                           cache.remove(lruKey);
                       }
                       // @a put.insert
                       cache.put(key, value);
                       order.addFirst(key);
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int capacity = in.getInt("capacity");
        String operations = in.getString("operations");

        Map<Integer, Integer> cache = new LinkedHashMap<>();
        List<Integer> order = new ArrayList<>(); // index 0 = most recently used

        emit.at("init")
                .say("Capacity %d. The cache starts empty; the chain will track recency "
                        + "from most- to least-recently-used, front to back.", capacity)
                .var("capacity", capacity).var("cache", "{}")
                .list(render(order, cache, -1)).step();

        for (String rawOp : operations.split(";")) {
            String[] parts = rawOp.trim().split("\\s+");
            if (parts[0].equals("get")) {
                int key = Integer.parseInt(parts[1]);
                if (!cache.containsKey(key)) {
                    emit.at("get.miss")
                            .say("get(%d): key %d is not in the cache — miss, return -1.", key, key)
                            .var("key", key).var("result", -1).var("cache", cache.toString())
                            .list(render(order, cache, -1)).step();
                } else {
                    order.remove(Integer.valueOf(key));
                    order.add(0, key);
                    emit.at("get.hit")
                            .say("get(%d): hit, value %d. Move %d to the front — it is now the "
                                    + "most recently used.", key, cache.get(key), key)
                            .var("key", key).var("result", cache.get(key)).var("cache", cache.toString())
                            .list(render(order, cache, key)).step();
                }
            } else {
                int key = Integer.parseInt(parts[1]);
                int value = Integer.parseInt(parts[2]);
                if (cache.containsKey(key)) {
                    cache.put(key, value);
                    order.remove(Integer.valueOf(key));
                    order.add(0, key);
                    emit.at("put.update")
                            .say("put(%d,%d): key %d already present — update its value in place "
                                    + "and move it to the front.", key, value, key)
                            .var("key", key).var("value", value).var("cache", cache.toString())
                            .list(render(order, cache, key)).step();
                } else {
                    if (cache.size() == capacity) {
                        int evicted = order.remove(order.size() - 1);
                        cache.remove(evicted);
                        emit.at("put.evict")
                                .say("put(%d,%d): cache is full at capacity %d. Evict %d, the "
                                        + "least recently used key.", key, value, capacity, evicted)
                                .var("evicted", evicted).var("cache", cache.toString())
                                .list(render(order, cache, -1)).step();
                    }
                    cache.put(key, value);
                    order.add(0, key);
                    emit.at("put.insert")
                            .say("put(%d,%d): new key, inserted at the front.", key, value)
                            .var("key", key).var("value", value).var("cache", cache.toString())
                            .list(render(order, cache, key)).step();
                }
            }
        }
    }

    /** {@code order} rendered MRU-first as the chain the cache's own recency list is. */
    private List<ListNode> render(List<Integer> order, Map<Integer, Integer> cache, int highlight) {
        List<ListNode> nodes = new ArrayList<>(order.size());
        for (int i = 0; i < order.size(); i++) {
            int key = order.get(i);
            Integer nextId = i + 1 < order.size() ? order.get(i + 1) : null;
            String state = key == highlight ? "active" : "default";
            nodes.add(new ListNode(key, key + ":" + cache.get(key), nextId, null, state));
        }
        return nodes;
    }
}
