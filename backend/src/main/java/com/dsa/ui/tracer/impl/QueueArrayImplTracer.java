package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implement Queue using an array — as a circular buffer, which is the whole reason this
 * problem is worth doing.
 *
 * <p>The naive version shifts every element down on each dequeue, making dequeue O(n). The
 * circular version instead moves the {@code front} index forward and lets {@code rear} wrap
 * back to slot 0 with {@code % capacity}, so both ends are O(1) and the live region is the
 * ring segment from {@code front} spanning {@code size} slots — which may straddle the end
 * of the array. Fullness cannot be read off the two indices alone once they wrap, so
 * {@code size} is tracked explicitly.
 */
@Component
public class QueueArrayImplTracer implements AlgorithmTracer {

    private static final String OP = "(enqueue -?\\d{1,3}|dequeue|front)";

    @Override
    public String id() {
        return "queue-array-impl";
    }

    @Override
    public DsType dsType() {
        return DsType.QUEUE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("capacity", FieldType.INT)
                        .label("Array capacity")
                        .help("Slots in the ring. Enqueueing past it overflows.")
                        .range(1, 8)
                        .defaultValue(4)
                        .build(),
                InputField.of("operations", FieldType.STRING)
                        .label("Operations")
                        .help("Semicolon-separated, each \"enqueue V\", \"dequeue\" or \"front\".")
                        .length(1, 200)
                        .constraint("pattern", OP + "(;" + OP + ")*")
                        .constraint("patternHint",
                                "Semicolon-separated ops, each \"enqueue V\", \"dequeue\" or "
                                        + "\"front\", e.g. \"enqueue 10;dequeue;front\".")
                        .defaultValue("enqueue 10;enqueue 20;enqueue 30;front;dequeue;dequeue;"
                                + "enqueue 40;enqueue 50;enqueue 60;enqueue 70")
                        .build());
    }

    /**
     * Half the capacity and a sequence that empties the queue twice over, reaching the
     * empty-queue branches of dequeue and front that the default never touches. The default
     * ends full, wrapped and overflowing; this one ends with one free slot.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "capacity", 3,
                "operations", "front;enqueue 5;dequeue;dequeue;enqueue 6;enqueue 7;front");
    }

    @Override
    public String annotatedCode() {
        return """
               class ArrayQueue {
                   int[] ring;
                   int front = 0, rear = -1, size = 0;

                   ArrayQueue(int capacity) {
                       // @a init
                       ring = new int[capacity];
                   }

                   void enqueue(int value) {
                       if (size == ring.length) {
                           // @a overflow
                           throw new IllegalStateException();
                       }
                       // @a enqueue
                       rear = (rear + 1) % ring.length;
                       ring[rear] = value;
                       size++;
                   }

                   int dequeue() {
                       if (size == 0) {
                           // @a underflow
                           throw new NoSuchElementException();
                       }
                       // @a dequeue
                       int value = ring[front];
                       front = (front + 1) % ring.length;
                       size--;
                       return value;
                   }

                   int front() {
                       if (size == 0) {
                           // @a frontEmpty
                           throw new NoSuchElementException();
                       }
                       // @a front
                       return ring[front];
                   }

                   int size() {
                       // @a done
                       return size;
                   }
               }""";
    }

    /** The raw ring, so the wrap-around is visible; live slots are the ones inside the segment. */
    private List<ArrayElement> ring(int[] ring, int front, int size, int highlight) {
        List<ArrayElement> state = new ArrayList<>(ring.length);
        boolean[] live = new boolean[ring.length];
        for (int k = 0; k < size; k++) {
            live[(front + k) % ring.length] = true;
        }
        for (int i = 0; i < ring.length; i++) {
            String s = i == highlight ? "current" : live[i] ? "sorted" : "visited";
            state.add(new ArrayElement(i, ring[i], s));
        }
        return state;
    }

    /** The queue in logical order, front first — the order StepEmitter.queue() expects. */
    private List<String> logical(int[] ring, int front, int size) {
        List<String> out = new ArrayList<>(size);
        for (int k = 0; k < size; k++) {
            out.add(String.valueOf(ring[(front + k) % ring.length]));
        }
        return out;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int capacity = in.getInt("capacity");
        String operations = in.getString("operations");
        int[] ring = new int[capacity];
        int front = 0;
        int rear = -1;
        int size = 0;

        emit.at("init")
                .say("A %d-slot ring with front = 0, rear = -1 and size = 0. Both ends move "
                        + "forward modulo %d instead of shifting the contents.", capacity, capacity)
                .var("capacity", capacity).var("front", front).var("rear", rear).var("size", size)
                .arrayState(ring(ring, front, size, -1)).queue(logical(ring, front, size)).step();

        for (String rawOp : operations.split(";")) {
            String[] parts = rawOp.trim().split("\\s+");
            switch (parts[0]) {
                case "enqueue" -> {
                    int value = Integer.parseInt(parts[1]);
                    if (size == capacity) {
                        emit.at("overflow")
                                .say("enqueue(%d): size is %d, every slot is live. Overflow; "
                                        + "%d is rejected.", value, size, value)
                                .var("rejected", value).var("front", front).var("rear", rear).var("size", size)
                                .arrayState(ring(ring, front, size, rear))
                                .queue(logical(ring, front, size)).step();
                    } else {
                        int previousRear = rear;
                        rear = (rear + 1) % capacity;
                        ring[rear] = value;
                        size++;
                        emit.at("enqueue")
                                .say(rear == 0 && previousRear == capacity - 1
                                        ? String.format("enqueue(%d): rear wraps from %d back to slot 0 — "
                                                + "that slot was freed by an earlier dequeue. size is now %d.",
                                                value, previousRear, size)
                                        : String.format("enqueue(%d): rear moves to slot %d and takes the "
                                                + "value. size is now %d.", value, rear, size))
                                .var("enqueued", value).var("front", front).var("rear", rear).var("size", size)
                                .arrayState(ring(ring, front, size, rear))
                                .queue(logical(ring, front, size)).step();
                    }
                }
                case "dequeue" -> {
                    if (size == 0) {
                        emit.at("underflow")
                                .say("dequeue(): size is 0, so there is nothing to serve. Underflow.")
                                .var("front", front).var("rear", rear).var("size", size)
                                .arrayState(ring(ring, front, size, -1))
                                .queue(logical(ring, front, size)).step();
                    } else {
                        int served = ring[front];
                        int servedSlot = front;
                        front = (front + 1) % capacity;
                        size--;
                        emit.at("dequeue")
                                .say("dequeue(): slot %d holds %d — serve it and move front to %d. "
                                        + "Nothing is shifted; slot %d simply stops being live. "
                                        + "size is now %d.", servedSlot, served, front, servedSlot, size)
                                .var("dequeued", served).var("front", front).var("rear", rear).var("size", size)
                                .arrayState(ring(ring, front, size, servedSlot))
                                .queue(logical(ring, front, size)).step();
                    }
                }
                default -> {
                    if (size == 0) {
                        emit.at("frontEmpty")
                                .say("front(): size is 0, so there is no front element to read.")
                                .var("front", front).var("rear", rear).var("size", size)
                                .arrayState(ring(ring, front, size, -1))
                                .queue(logical(ring, front, size)).step();
                    } else {
                        emit.at("front")
                                .say("front(): slot %d holds %d. Read it without moving anything.",
                                        front, ring[front])
                                .var("peeked", ring[front]).var("front", front).var("rear", rear).var("size", size)
                                .arrayState(ring(ring, front, size, front))
                                .queue(logical(ring, front, size)).step();
                    }
                }
            }
        }

        emit.at("done")
                .say("Sequence finished. front = %d, rear = %d, size = %d in a %d-slot ring.",
                        front, rear, size, capacity)
                .var("front", front).var("rear", rear).var("size", size)
                .arrayState(ring(ring, front, size, -1)).queue(logical(ring, front, size)).step();
    }
}
