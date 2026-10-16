public class Solution {
    public int solve(int[] nums) {
        // @a start
        Integer[] memo = new Integer[nums.length + 1];
        // @a done
        return best(nums.length, nums, memo);
    }
    private int best(int count, int[] nums, Integer[] memo) {
        // @a enter
        if (memo[count] != null) {
            // @a hit
            return memo[count];
        }
        if (count <= 1) {
            // @a base
            return memo[count] = count == 0 ? 0 : nums[0];
        }
        // @a branch
        int skip = best(count - 1, nums, memo);
        int take = nums[count - 1] + best(count - 2, nums, memo);
        // @a combine
        return memo[count] = Math.max(skip, take);
    }
}
