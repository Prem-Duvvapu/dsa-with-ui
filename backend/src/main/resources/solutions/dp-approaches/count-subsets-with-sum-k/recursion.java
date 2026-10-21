public class Solution {
    public int countSubsetsWithSumK(int[] nums, int k) {
        // @a start
        int answer = count(nums, nums.length, k);
        // @a done
        return answer;
    }

    private int count(int[] nums, int n, int s) {
        // @a enter
        // This is a real call; repeated (n,s) states are evaluated again.
        if (n == 0) {
            // @a base
            return s == 0 ? 1 : 0;
        }
        // @a branch
        int skip = count(nums, n - 1, s);
        int take = nums[n - 1] <= s ? count(nums, n - 1, s - nums[n - 1]) : 0;
        // @a combine
        return skip + take;
    }
}

