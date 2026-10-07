public class Solution {
    public int climbStairs(int n) {
        // @a init
        return ways(n);
    }

    private int ways(int n) {
        // @a enter
        if (n <= 1) {
            // @a base
            return 1;
        }
        // @a left
        int oneStep = ways(n - 1);
        // @a right
        int twoSteps = ways(n - 2);
        // @a combine
        return oneStep + twoSteps;
    }
}
