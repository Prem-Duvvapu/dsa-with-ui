package com.dsa.ui.tracer.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A binary heap that keeps its array, because the array IS what the canvas draws.
 *
 * <p>The tracers here used to hold a {@link java.util.PriorityQueue} and emit a snapshot of
 * it sorted:
 *
 * <pre>
 *   List&lt;Integer&gt; values = new ArrayList&lt;&gt;(heap);
 *   Collections.sort(values);          // <- this
 * </pre>
 *
 * <p>{@code HeapCanvas} lays out slot {@code i}'s children at {@code 2i+1} and {@code 2i+2}
 * and prints the index under every slot, so a sorted snapshot draws a perfectly ordered
 * binary tree at every step — teaching that a priority queue keeps all of its elements in
 * order. It does not, and that is the single most common misconception about heaps. Five of
 * this topic's problems were demonstrating it.
 *
 * <p>Reading {@code PriorityQueue.toArray()} instead would happen to work on every mainstream
 * JVM and is documented to return the elements "in no particular order", which is not
 * something a teaching visualization should rest on. So the heap is kept here, explicitly:
 * what {@link #slots()} returns is the array this class actually sifts through, and the
 * shape the canvas draws is the shape the algorithm really has.
 *
 * <p>{@code TaskSchedulerTracer} and {@code ImplementMinHeapTracer} already owned their heap
 * array this way and were always honest; this is the same thing, shared.
 */
final class ArrayHeap<T> {

    private final List<T> slots = new ArrayList<>();
    private final Comparator<? super T> order;

    /** @param order the head of the heap is the minimum under this comparator */
    ArrayHeap(Comparator<? super T> order) {
        this.order = order;
    }

    static ArrayHeap<Integer> minHeap() {
        return new ArrayHeap<>(Comparator.naturalOrder());
    }

    int size() {
        return slots.size();
    }

    boolean isEmpty() {
        return slots.isEmpty();
    }

    T peek() {
        return slots.isEmpty() ? null : slots.get(0);
    }

    /** The live array, index order. @return an unmodifiable view */
    List<T> slots() {
        return Collections.unmodifiableList(slots);
    }

    /** @return the index the new value settled at, which is the slot worth highlighting */
    int offer(T value) {
        slots.add(value);
        int i = slots.size() - 1;
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (order.compare(slots.get(i), slots.get(parent)) >= 0) {
                break;
            }
            swap(i, parent);
            i = parent;
        }
        return i;
    }

    T poll() {
        if (slots.isEmpty()) {
            return null;
        }
        T head = slots.get(0);
        T last = slots.remove(slots.size() - 1);
        if (!slots.isEmpty()) {
            slots.set(0, last);
            siftDown();
        }
        return head;
    }

    private void siftDown() {
        int i = 0;
        int n = slots.size();
        while (true) {
            int l = 2 * i + 1;
            int r = 2 * i + 2;
            int best = i;
            if (l < n && order.compare(slots.get(l), slots.get(best)) < 0) best = l;
            if (r < n && order.compare(slots.get(r), slots.get(best)) < 0) best = r;
            if (best == i) {
                return;
            }
            swap(i, best);
            i = best;
        }
    }

    private void swap(int i, int j) {
        T tmp = slots.get(i);
        slots.set(i, slots.get(j));
        slots.set(j, tmp);
    }
}
