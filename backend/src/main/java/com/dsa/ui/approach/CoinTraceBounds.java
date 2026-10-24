package com.dsa.ui.approach;

import com.dsa.ui.tracer.InputValidationException;
import java.util.*;

/** Safety-only counts; this never computes an answer or emits simulated calls. */
final class CoinTraceBounds {
    private CoinTraceBounds() {}
    static void requireFits(int[] coins,int amount,boolean memo) {
        if(memo) {
            if((long)(coins.length+1)*(amount+1)>80)throw new InputValidationException(Map.of("coins",
                    "Memo tracing needs (n+1) × (amount+1) ≤ 80 cells; use fewer denominations or a smaller amount."));
            return;
        }
        int[][] calls=new int[coins.length+1][amount+1];Arrays.fill(calls[0],1);
        for(int n=1;n<=coins.length;n++) {
            calls[n][0]=1;
            for(int a=1;a<=amount;a++)calls[n][a]=Math.min(221,1+calls[n-1][a]+(coins[n-1]<=a?calls[n][a-coins[n-1]]:0));
        }
        if(calls[coins.length][amount]>220)throw new InputValidationException(Map.of("coins",
                "This input needs more than 220 recursive calls. Use fewer denominations, a smaller amount or larger coins, or select Memoization."));
    }
}
