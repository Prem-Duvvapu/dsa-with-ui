public class Solution {
    public int unboundedKnapsack(int[] wt, int[] val, int n, int W) {
        // @a start
        int answer = best(wt, val, n, W);
        // @a done
        return answer;
    }

    private int best(int[] wt, int[] val, int n, int w) {
        // @a enter
        // A real call; repeated states are evaluated again.
        if (n == 0 || w == 0) {
            // @a base
            return 0;
        }
        // @a branch
        int skip = best(wt, val, n - 1, w);
        // Keep n: this type can be taken again. Positive weight makes progress.
        int take = wt[n - 1] <= w ? val[n - 1] + best(wt, val, n, w - wt[n - 1]) : 0;
        // @a combine
        return Math.max(skip, take);
    }
}
