package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Binary search on the answer again, like {@link AggressiveCowsTracer}, but searching in
 * the opposite direction: this minimizes the maximum pages any one student gets, so a
 * feasible candidate lowers {@code high} looking for something smaller, where aggressive
 * cows raised {@code low} looking for something larger. The feasibility check itself is
 * also a different shape - a greedy contiguous partition instead of a greedy spacing walk.
 */
@Component
public class BookAllocationTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "book-allocation";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("pages", FieldType.INT_ARRAY)
                        .label("Pages per book")
                        .help("Books are allocated to students in this order - contiguous ranges only.")
                        .length(1, 12).values(1, 1000)
                        .defaultValue(List.of(12, 34, 67, 90))
                        .build(),
                InputField.of("m", FieldType.INT)
                        .label("Students")
                        .range(1, 12)
                        .defaultValue(2)
                        .build());
    }

    /** One student per book: every split is trivially feasible, so the answer is just the largest single book. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("pages", List.of(12, 34, 67, 90), "m", 4);
    }

    @Override
    public String annotatedCode() {
        return """
               public int findPages(int[] pages, int m) {
                   int low = max(pages), high = sum(pages), ans = high;
                   // @a init
                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       if (countStudents(pages, mid) <= m) {
                           // @a feasible
                           ans = mid;
                           high = mid - 1;
                       } else {
                           // @a infeasible
                           low = mid + 1;
                       }
                   }
                   // @a done
                   return ans;
               }

               private int countStudents(int[] pages, int cap) {
                   int students = 1, pageSum = 0;
                   for (int p : pages) {
                       if (pageSum + p > cap) {
                           // @a newStudent
                           students++;
                           pageSum = p;
                       } else {
                           // @a addToCurrent
                           pageSum += p;
                       }
                   }
                   return students;
               }""";
    }

    private List<ArrayElement> pageState(int[] pages, int current, int splitStart) {
        List<ArrayElement> state = new ArrayList<>(pages.length);
        for (int i = 0; i < pages.length; i++) {
            String s = i == current ? "current" : i < splitStart ? "sorted" : "default";
            state.add(new ArrayElement(i, pages[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] pages = in.getIntArray("pages");
        int m = in.getInt("m");
        int max = 0;
        int sum = 0;
        for (int p : pages) {
            max = Math.max(max, p);
            sum += p;
        }
        int low = max;
        int high = sum;
        int ans = high;

        emit.at("init")
                .say("Binary search the answer: the smallest possible cap on any one "
                        + "student's pages. It cannot be below the single largest book "
                        + "(%d, since that book cannot be split) or above giving "
                        + "everything to one student (%d).", low, high)
                .var("low", low).var("high", high).var("ans", ans)
                .arrayState(pageState(pages, -1, pages.length)).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            emit.at("mid")
                    .say("Test whether a cap of %d pages needs at most %d students.", mid, m)
                    .var("low", low).var("high", high).var("mid", mid)
                    .arrayState(pageState(pages, -1, pages.length)).step();

            int students = 1;
            int pageSum = 0;
            int currentStart = 0;
            for (int i = 0; i < pages.length; i++) {
                int p = pages[i];
                if (pageSum + p > mid) {
                    students++;
                    pageSum = p;
                    currentStart = i;
                    emit.at("newStudent")
                            .say("Book %d (%d pages) would push this student past %d - "
                                    + "start student #%d here instead.", i, p, mid, students)
                            .var("i", i).var("students", students)
                            .arrayState(pageState(pages, i, currentStart)).step();
                } else {
                    pageSum += p;
                    emit.at("addToCurrent")
                            .say("Book %d (%d pages) still fits this student's pile (%d so far).",
                                    i, p, pageSum)
                            .var("i", i).var("pageSum", pageSum)
                            .arrayState(pageState(pages, i, currentStart)).step();
                }
            }

            if (students <= m) {
                ans = mid;
                emit.at("feasible")
                        .say("%d students suffice (allowed %d) - cap %d works. Try a "
                                + "smaller cap.", students, m, mid)
                        .var("students", students).var("ans", ans).var("high", mid - 1)
                        .arrayState(pageState(pages, -1, pages.length)).step();
                high = mid - 1;
            } else {
                emit.at("infeasible")
                        .say("%d students are needed (allowed %d) - cap %d is too tight. "
                                + "Try larger.", students, m, mid)
                        .var("students", students).var("low", mid + 1)
                        .arrayState(pageState(pages, -1, pages.length)).step();
                low = mid + 1;
            }
        }

        emit.at("done")
                .say("low passed high. The smallest workable cap is %d.", ans)
                .var("answer", ans)
                .arrayState(pageState(pages, -1, pages.length)).step();
    }
}
