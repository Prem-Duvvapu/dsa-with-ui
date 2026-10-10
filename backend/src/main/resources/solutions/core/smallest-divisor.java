public class Solution {
    public int solve(int[] nums, int threshold) {
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("At least one positive value is required.");
        }
        if (threshold < nums.length) {
            throw new IllegalArgumentException("Threshold must be at least the number of elements.");
        }
        // @a init
        int low = 1, high = max(nums), ans = high;
        while (low <= high) {
            // @a mid
            int mid = low + (high - low) / 2;
            long sum = 0;
            for (int x : nums) {
                // @a tally
                sum += ((long) x + mid - 1) / mid;
            }
            if (sum <= threshold) {
                // @a feasible
                ans = mid;
                high = mid - 1;
            } else {
                // @a infeasible
                low = mid + 1;
            }
        }
        // @a done
        return ans;
    }

    private int max(int[] nums) {
        int largest = nums[0];
        for (int x : nums) {
            if (x <= 0) throw new IllegalArgumentException("Every value must be positive.");
            largest = Math.max(largest, x);
        }
        return largest;
    }
}
