public class Solution {
    public boolean subsetSumEqualTarget(int[] nums, int target) {
        // @a start
        boolean answer = possible(nums, nums.length, target);
        // @a done
        return answer;
    }

    private boolean possible(int[] nums, int n, int s) {
        // @a enter
        // This is a real call; repeated (n,s) states are evaluated again.
        if (n == 0) {
            // @a base
            return s == 0;
        }
        // @a branch
        boolean skip = possible(nums, n - 1, s);
        boolean take = nums[n - 1] <= s && possible(nums, n - 1, s - nums[n - 1]);
        // @a combine
        return skip || take;
    }
}

