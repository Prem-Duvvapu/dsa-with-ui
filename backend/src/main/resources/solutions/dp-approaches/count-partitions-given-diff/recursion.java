public class Solution {
    public int solve(int[] nums, int d) {
        int total = 0;
        for (int value : nums) total += value;
        if (d > total || (total + d) % 2 != 0) {
            // @a zero
            return 0;
        }
        int n = nums.length, target = (total + d) / 2;
        // @a start
        int answer = solve(n, target, nums);
        // @a done
        return answer;
    }

    private int solve(int items, int remaining, int[] nums) {
        // @a enter
        if (items == 0) {
            // @a base
            return remaining == 0 ? 1 : 0;
        }
        // @a branch
        int skip = solve(items - 1, remaining, nums);
        int take = nums[items - 1] <= remaining
                ? solve(items - 1, remaining - nums[items - 1], nums) : 0;
        // @a combine
        return skip + take;
    }
}
