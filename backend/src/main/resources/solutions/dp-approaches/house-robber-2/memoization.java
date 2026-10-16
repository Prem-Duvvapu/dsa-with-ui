public class Solution {
    public int solve(int[] nums) {
        // @a start
        Integer[][] memo = new Integer[2][nums.length];
        // @a passOne
        int skipLast = best(0, nums.length - 1, nums, memo);
        // @a passTwo
        int skipFirst = best(1, nums.length - 1, nums, memo);
        // @a done
        return Math.max(skipLast, skipFirst);
    }
    private int best(int pass, int n, int[] nums, Integer[][] memo) {
        // @a enter
        if (memo[pass][n] != null) {
            // @a hit
            return memo[pass][n];
        }
        if (n <= 1) {
            // @a base
            return memo[pass][n] = n == 0 ? 0 : nums[pass];
        }
        // @a branch
        int skip = best(pass, n - 1, nums, memo);
        int take = nums[pass + n - 1] + best(pass, n - 2, nums, memo);
        // @a combine
        return memo[pass][n] = Math.max(skip, take);
    }
}
