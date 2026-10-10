public class Solution {
    public int solve(int[] nums) {
        int n = nums.length;
        Integer[][] memo = new Integer[n + 1][n + 1];
        // @a start
        int answer = solve(0, 0, nums, memo);
        // @a done
        return answer;
    }

    private int solve(int i, int p, int[] nums, Integer[][] memo) {
        // @a enter
        if (memo[i][p] != null) {
            // @a hit
            return memo[i][p];
        }
        if (i == nums.length) {
            // @a base
            return memo[i][p] = 0;
        }
        // @a branch
        int skip = solve(i + 1, p, nums, memo);
        int take = p == 0 || nums[i] > nums[p - 1]
                ? 1 + solve(i + 1, i + 1, nums, memo) : Integer.MIN_VALUE;
        // @a combine
        return memo[i][p] = Math.max(skip, take);
    }
}
