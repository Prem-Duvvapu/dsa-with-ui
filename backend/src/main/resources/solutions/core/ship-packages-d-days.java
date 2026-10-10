public class Solution {
    public int solve(int[] weights, int days) {
        int low = max(weights), high = sum(weights), ans = high;
        // @a init
        while (low <= high) {
            // @a mid
            int mid = low + (high - low) / 2;
            if (daysNeeded(weights, mid) <= days) {
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

    private int daysNeeded(int[] weights, int capacity) {
        int days = 1, load = 0;
        for (int w : weights) {
            if (load + w > capacity) {
                // @a newDay
                days++;
                load = w;
            } else {
                // @a addToDay
                load += w;
            }
        }
        return days;
    }

    private int max(int[] weights) {
        int largest = weights[0];
        for (int w : weights) largest = Math.max(largest, w);
        return largest;
    }

    private int sum(int[] weights) {
        int total = 0;
        for (int w : weights) total += w;
        return total;
    }
}
