package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ListNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A min-heap over the K lists' current heads: only the head of each list can possibly be
 * the next output value (every list is already sorted), so the heap never needs to hold
 * more than K candidates at once. With K = 3 that heap is small enough to show as a plain
 * linear scan for the minimum, which is exactly what a 3-entry heap degenerates to.
 *
 * <p>Rendered as three remaining input chains (their heads are the live heap candidates)
 * followed by the merged output built so far - all in one flat node row, since a chain
 * boundary is invisible to the canvas the moment {@code nextId} stops matching the next
 * box (no arrow is drawn), the same mechanism flattening-ll and clone-ll-random-pointer
 * already rely on for their own extra pointer kinds.
 */
@Component
public class MergeKSortedListsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "merge-k-sorted-lists";
    }

    @Override
    public DsType dsType() {
        return DsType.LINKED_LIST;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("list1", FieldType.LINKED_LIST)
                        .label("List 1").sorted()
                        .length(1, 10).values(-1000, 1000)
                        .defaultValue(List.of(1, 4, 5))
                        .build(),
                InputField.of("list2", FieldType.LINKED_LIST)
                        .label("List 2").sorted()
                        .length(1, 10).values(-1000, 1000)
                        .defaultValue(List.of(1, 3, 4))
                        .build(),
                InputField.of("list3", FieldType.LINKED_LIST)
                        .label("List 3").sorted()
                        .length(1, 10).values(-1000, 1000)
                        .defaultValue(List.of(2, 6))
                        .build());
    }

    /** A different value shape across the three lists, not a permutation of the defaults. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "list1", List.of(2, 6),
                "list2", List.of(1, 3, 4, 7),
                "list3", List.of(5));
    }

    @Override
    public String annotatedCode() {
        return """
               public ListNode mergeKLists(ListNode[] lists) {
                   PriorityQueue<ListNode> heap = new PriorityQueue<>((a, b) -> a.val - b.val);
                   for (ListNode head : lists) if (head != null) heap.add(head);

                   ListNode dummy = new ListNode(-1), tail = dummy;
                   while (!heap.isEmpty()) {
                       // @a pickSmallestHead
                       ListNode smallest = heap.poll();
                       // @a append
                       tail.next = smallest;
                       tail = tail.next;
                       if (smallest.next != null) heap.add(smallest.next);
                   }
                   // @a done
                   return dummy.next;
               }""";
    }

    /** The heap's live heads, in its own array order - see ArrayHeap. */
    private static String candidates(ArrayHeap<int[]> heap) {
        StringBuilder out = new StringBuilder("[");
        List<int[]> slots = heap.slots();
        for (int i = 0; i < slots.size(); i++) {
            if (i > 0) out.append(", ");
            out.append(slots.get(i)[0]).append("(L").append(slots.get(i)[1] + 1).append(')');
        }
        return out.append(']').toString();
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] lists = {
                in.getLinkedList("list1"),
                in.getLinkedList("list2"),
                in.getLinkedList("list3")
        };

        int[] idBase = new int[3];
        int nextId = 0;
        for (int i = 0; i < 3; i++) {
            idBase[i] = nextId;
            nextId += lists[i].length;
        }

        int[] head = {0, 0, 0}; // next unconsumed position in each list
        List<Integer> mergedIds = new ArrayList<>();

        // A real heap of the three live heads, because the code panel beside this says
        // PriorityQueue.poll() and a tracer runs the algorithm it shows. This used to scan
        // all three heads linearly for the minimum - same answer with three lists, and no
        // test can tell, but it is not the algorithm on screen and it never mentioned the
        // structure the problem exists to teach. Entries are {value, listIndex}.
        ArrayHeap<int[]> heap = new ArrayHeap<>(
                java.util.Comparator.<int[]>comparingInt(e -> e[0]).thenComparingInt(e -> e[1]));
        for (int i = 0; i < 3; i++) {
            if (lists[i].length > 0) {
                heap.offer(new int[]{lists[i][0], i});
            }
        }

        emit.at("pickSmallestHead")
                .say("Three sorted lists to merge: %s, %s, %s. Only their heads go into the "
                                + "heap - %d of them - so each pick costs log k, not k.",
                        java.util.Arrays.toString(lists[0]),
                        java.util.Arrays.toString(lists[1]),
                        java.util.Arrays.toString(lists[2]), heap.size())
                .var("heap", candidates(heap))
                .list(render(lists, idBase, head, mergedIds, -1)).step();

        int total = lists[0].length + lists[1].length + lists[2].length;
        for (int step = 0; step < total; step++) {
            int[] best = heap.poll();
            int bestValue = best[0];
            int bestList = best[1];

            int poppedId = idBase[bestList] + head[bestList];
            emit.at("pickSmallestHead")
                    .say("Pop the heap root: %d, from list %d. %s",
                            bestValue, bestList + 1,
                            heap.size() == 0
                                    ? "It is the only live head left."
                                    : String.format("It is the smallest of the %d live heads, and the"
                                            + " heap knew that without comparing them.", heap.size() + 1))
                    .var("value", bestValue).var("fromList", bestList + 1)
                    .var("heap", candidates(heap))
                    .list(render(lists, idBase, head, mergedIds, -1)).step();

            head[bestList]++;
            boolean pushed = head[bestList] < lists[bestList].length;
            if (pushed) {
                heap.offer(new int[]{lists[bestList][head[bestList]], bestList});
            }
            mergedIds.add(poppedId);
            emit.at("append")
                    .say("Append %d to the merged output (%d node%s so far). %s",
                            bestValue, mergedIds.size(), mergedIds.size() == 1 ? "" : "s",
                            pushed
                                    ? String.format("List %d's next head, %d, takes its place in the heap.",
                                            bestList + 1, lists[bestList][head[bestList]])
                                    : String.format("List %d is exhausted, so nothing replaces it and the heap shrinks.",
                                            bestList + 1))
                    .var("appended", bestValue).var("mergedSoFar", mergedIds.size())
                    .var("heap", candidates(heap))
                    .list(render(lists, idBase, head, mergedIds, poppedId)).step();
        }

        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < mergedIds.size(); i++) {
            if (i > 0) answer.append(" -> ");
            answer.append(valueOf(lists, idBase, mergedIds.get(i)));
        }
        emit.at("done")
                .say("The heap is empty, so all three lists are exhausted. Merged result: %s.",
                        answer)
                .var("answer", answer.toString())
                .list(render(lists, idBase, head, mergedIds, -1)).step();
    }

    private static int valueOf(int[][] lists, int[] idBase, int id) {
        for (int i = 0; i < 3; i++) {
            int posInList = id - idBase[i];
            if (posInList >= 0 && posInList < lists[i].length) {
                return lists[i][posInList];
            }
        }
        throw new IllegalStateException("id " + id + " not found in any list");
    }

    /**
     * Remaining input chains (one per list, head = live heap candidate) followed by the
     * merged output so far. {@code justAppendedId} is highlighted as the step's focus.
     */
    private static List<ListNode> render(int[][] lists, int[] idBase, int[] head,
                                          List<Integer> mergedIds, int justAppendedId) {
        List<ListNode> nodes = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            for (int pos = head[i]; pos < lists[i].length; pos++) {
                int id = idBase[i] + pos;
                Integer next = pos + 1 < lists[i].length ? id + 1 : null;
                String state = pos == head[i] ? "active" : "default";
                nodes.add(new ListNode(id, String.valueOf(lists[i][pos]), next, null, state));
            }
        }

        for (int i = 0; i < mergedIds.size(); i++) {
            int id = mergedIds.get(i);
            Integer next = i + 1 < mergedIds.size() ? mergedIds.get(i + 1) : null;
            String state = id == justAppendedId ? "curr" : "visited";
            nodes.add(new ListNode(id, String.valueOf(valueOf(lists, idBase, id)), next, null, state));
        }

        return nodes;
    }
}
