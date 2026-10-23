public class Solution {
    public int knapsack(int[] wt, int[] val, int n, int W) {
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
        // n-1 forbids taking this indexed item again.
        int take = wt[n - 1] <= w ? val[n - 1] + best(wt, val, n - 1, w - wt[n - 1]) : 0;
        // @a combine
        return Math.max(skip, take);
    }
}
