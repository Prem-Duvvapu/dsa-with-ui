public class Solution {
    public boolean subsetSumEqualTarget(int[] nums, int target) {
        Boolean[][] memo = new Boolean[nums.length + 1][target + 1];
        // @a start
        boolean answer = possible(nums, nums.length, target, memo);
        // @a done
        return answer;
    }

    private boolean possible(int[] nums, int n, int s, Boolean[][] memo) {
        // @a enter
        if (memo[n][s] != null) {
            // @a hit
            return memo[n][s];
        }
        if (n == 0) {
            // @a base
            return memo[n][s] = s == 0;
        }
        // @a branch
        boolean skip = possible(nums, n - 1, s, memo);
        boolean take = nums[n - 1] <= s && possible(nums, n - 1, s - nums[n - 1], memo);
        // @a combine
        return memo[n][s] = skip || take;
    }
}

