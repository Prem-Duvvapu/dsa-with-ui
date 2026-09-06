package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lemonade Change — greedy change-making with only two coin sizes worth tracking. A $5
 * bill is always safe change; a $10 asks for exactly one $5; a $20 should spend a $10+$5
 * pair before ever breaking a third $5, because that pair of small bills is useless for
 * anything except making change for another $20 later.
 */
@Component
public class LemonadeChangeTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "lemonade-change";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("bills", FieldType.INT_ARRAY)
                        .label("Bills, in the order customers pay")
                        .help("Each value must be 5, 10, or 20.")
                        .length(1, 40).values(5, 20)
                        .defaultValue(List.of(5, 5, 5, 10, 20))
                        .build());
    }

    /** The very first customer already breaks it — no fives exist yet to change a ten. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("bills", List.of(10, 20));
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean lemonadeChange(int[] bills) {
                   int five = 0, ten = 0;
                   boolean ok = true;
                   for (int bill : bills) {
                       // @a already
                       if (!ok) continue;
                       if (bill == 5) {
                           // @a takeFive
                           five++;
                       } else if (bill == 10) {
                           // @a payTen
                           if (five >= 1) { five--; ten++; }
                           else { ok = false; }
                       } else {
                           // @a payTwenty
                           if (ten >= 1 && five >= 1) { ten--; five--; }
                           else if (five >= 3) { five -= 3; }
                           else { ok = false; }
                       }
                   }
                   // @a done
                   return ok;
               }""";
    }

    private List<ArrayElement> board(int[] bills, int cursor, boolean ok) {
        List<ArrayElement> state = new ArrayList<>(bills.length);
        for (int i = 0; i < bills.length; i++) {
            String s = i < cursor ? "sorted" : i == cursor ? (ok ? "current" : "visited") : "target";
            state.add(new ArrayElement(i, bills[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] bills = in.getIntArray("bills");
        for (int i = 0; i < bills.length; i++) {
            if (bills[i] != 5 && bills[i] != 10 && bills[i] != 20) {
                throw new InputValidationException(Map.of("bills",
                        "Bill " + (i + 1) + " is " + bills[i] + ", but only 5, 10, or 20 exist."));
            }
        }

        int five = 0, ten = 0;
        boolean ok = true;

        for (int i = 0; i < bills.length; i++) {
            int bill = bills[i];

            if (!ok) {
                emit.at("already").say("Change already broke down on an earlier customer — bill %d ($%d) can no longer be served.", i + 1, bill)
                        .var("five", five).var("ten", ten).var("ok", false)
                        .arrayState(board(bills, i, false)).step();
                continue;
            }

            if (bill == 5) {
                five++;
                emit.at("takeFive").say("Customer %d pays with $5 — no change needed. Fives on hand: %d.", i + 1, five)
                        .var("five", five).var("ten", ten).var("ok", true)
                        .arrayState(board(bills, i, true)).step();
            } else if (bill == 10) {
                if (five >= 1) {
                    five--;
                    ten++;
                    emit.at("payTen").say("Customer %d pays with $10 — break one $5 as change. Fives: %d, tens: %d.", i + 1, five, ten)
                            .var("five", five).var("ten", ten).var("ok", true)
                            .arrayState(board(bills, i, true)).step();
                } else {
                    ok = false;
                    emit.at("payTen").say("Customer %d pays with $10 but there is no $5 to give back — change breaks down here.", i + 1)
                            .var("five", five).var("ten", ten).var("ok", false)
                            .arrayState(board(bills, i, false)).step();
                }
            } else {
                if (ten >= 1 && five >= 1) {
                    ten--;
                    five--;
                    emit.at("payTwenty").say("Customer %d pays with $20 — give a $10+$5 pair rather than spend three fives. Fives: %d, tens: %d.",
                                    i + 1, five, ten)
                            .var("five", five).var("ten", ten).var("ok", true)
                            .arrayState(board(bills, i, true)).step();
                } else if (five >= 3) {
                    five -= 3;
                    emit.at("payTwenty").say("Customer %d pays with $20 — no $10 on hand, so break three $5 bills instead. Fives: %d.", i + 1, five)
                            .var("five", five).var("ten", ten).var("ok", true)
                            .arrayState(board(bills, i, true)).step();
                } else {
                    ok = false;
                    emit.at("payTwenty").say("Customer %d pays with $20 but neither a $10+$5 pair nor three $5 bills are available — change breaks down here.", i + 1)
                            .var("five", five).var("ten", ten).var("ok", false)
                            .arrayState(board(bills, i, false)).step();
                }
            }
        }

        emit.at("done").say(ok ? "Every customer got correct change." : "Change broke down partway through — not every customer can be served.")
                .var("five", five).var("ten", ten).var("ok", ok)
                .arrayState(board(bills, bills.length, ok)).step();
    }
}
