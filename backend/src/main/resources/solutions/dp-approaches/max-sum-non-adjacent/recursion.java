public class Solution {
    public int solve(int[] nums) {
        // @a start
        int count = nums.length;
        // @a done
        return best(count, nums);
    }
    private int best(int count, int[] nums) {
        // @a enter
        if (count <= 1) {
            // @a base
            return count == 0 ? 0 : nums[0];
        }
        // @a branch
        int skip = best(count - 1, nums);
        int take = nums[count - 1] + best(count - 2, nums);
        // @a combine
        return Math.max(skip, take);
    }
}
