public class Solution {
    public int solve(int[] nums, int d) {
        int total = 0;
        for (int value : nums) total += value;
        if (d > total || (total + d) % 2 != 0) {
            // @a zero
            return 0;
        }
        int n = nums.length, target = (total + d) / 2;
        Integer[][] memo = new Integer[n + 1][target + 1];
        // @a start
        int answer = solve(n, target, nums, memo);
        // @a done
        return answer;
    }

    private int solve(int items, int remaining, int[] nums, Integer[][] memo) {
        // @a enter
        if (memo[items][remaining] != null) {
            // @a hit
            return memo[items][remaining];
        }
        if (items == 0) {
            // @a base
            return memo[items][remaining] = remaining == 0 ? 1 : 0;
        }
        // @a branch
        int skip = solve(items - 1, remaining, nums, memo);
        int take = nums[items - 1] <= remaining
                ? solve(items - 1, remaining - nums[items - 1], nums, memo) : 0;
        // @a combine
        return memo[items][remaining] = skip + take;
    }
}
