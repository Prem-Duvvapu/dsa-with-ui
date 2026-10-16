public class Solution {
    public int solve(int[] nums) {
        // @a start
        int n = nums.length - 1;
        // @a passOne
        int skipLast = best(0, n, nums);
        // @a passTwo
        int skipFirst = best(1, n, nums);
        // @a done
        return Math.max(skipLast, skipFirst);
    }
    private int best(int pass, int n, int[] nums) {
        // @a enter
        if (n <= 1) {
            // @a base
            return n == 0 ? 0 : nums[pass];
        }
        // @a branch
        int skip = best(pass, n - 1, nums);
        int take = nums[pass + n - 1] + best(pass, n - 2, nums);
        // @a combine
        return Math.max(skip, take);
    }
}
