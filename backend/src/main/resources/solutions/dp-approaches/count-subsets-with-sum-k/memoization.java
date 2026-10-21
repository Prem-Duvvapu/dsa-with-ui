public class Solution {
    public int countSubsetsWithSumK(int[] nums, int k) {
        Integer[][] memo = new Integer[nums.length + 1][k + 1];
        // @a start
        int answer = count(nums, nums.length, k, memo);
        // @a done
        return answer;
    }

    private int count(int[] nums, int n, int s, Integer[][] memo) {
        // @a enter
        if (memo[n][s] != null) {
            // @a hit
            return memo[n][s];
        }
        if (n == 0) {
            // @a base
            return memo[n][s] = s == 0 ? 1 : 0;
        }
        // @a branch
        int skip = count(nums, n - 1, s, memo);
        int take = nums[n - 1] <= s ? count(nums, n - 1, s - nums[n - 1], memo) : 0;
        // @a combine
        return memo[n][s] = skip + take;
    }
}

