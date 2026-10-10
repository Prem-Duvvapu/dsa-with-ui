public class Solution {
    public int solve(int[] bloomDay, int m, int k) {
        long need = (long) m * k;
        if (need > bloomDay.length) {
            // @a impossible
            return -1;
        }
        // @a init
        int low = min(bloomDay), high = max(bloomDay), ans = -1;
        while (low <= high) {
            // @a mid
            int mid = low + (high - low) / 2;
            int bouquets = 0, run = 0;
            for (int day : bloomDay) {
                if (day <= mid) {
                    // @a extendRun
                    run++;
                    if (run == k) { bouquets++; run = 0; }
                } else {
                    // @a breakRun
                    run = 0;
                }
            }
            if (bouquets >= m) {
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

    private int min(int[] bloomDay) {
        int earliest = bloomDay[0];
        for (int day : bloomDay) earliest = Math.min(earliest, day);
        return earliest;
    }

    private int max(int[] bloomDay) {
        int latest = bloomDay[0];
        for (int day : bloomDay) latest = Math.max(latest, day);
        return latest;
    }
}
