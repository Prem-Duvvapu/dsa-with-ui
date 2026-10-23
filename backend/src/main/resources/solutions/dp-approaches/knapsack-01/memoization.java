public class Solution {
    public int knapsack(int[] wt, int[] val, int n, int W) {
        Integer[][] memo = new Integer[n + 1][W + 1];
        // @a start
        int answer = best(wt, val, n, W, memo);
        // @a done
        return answer;
    }

    private int best(int[] wt, int[] val, int n, int w, Integer[][] memo) {
        // @a enter
        if (memo[n][w] != null) {
            // @a hit
            return memo[n][w];
        }
        if (n == 0 || w == 0) {
            // @a base
            return memo[n][w] = 0;
        }
        // @a branch
        int skip = best(wt, val, n - 1, w, memo);
        int take = wt[n - 1] <= w ? val[n - 1] + best(wt, val, n - 1, w - wt[n - 1], memo) : 0;
        // @a combine
        return memo[n][w] = Math.max(skip, take);
    }
}
