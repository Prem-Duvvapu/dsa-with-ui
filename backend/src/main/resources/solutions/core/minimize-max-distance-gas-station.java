public class Solution {
    // For the visualizer's initial gap <=1000,40 halvings reduce width below1e-9.
    private static final int ITERATIONS = 40;

    public double minimizeMaxDistance(int[] stations, int k) {
        int n = stations.length;
        double[] gaps = new double[n - 1];
        double maxGap = 0;
        for (int i = 0; i < n - 1; i++) {
            gaps[i] = stations[i + 1] - stations[i];
            maxGap = Math.max(maxGap, gaps[i]);
        }
        // @a init
        double low = 0, high = maxGap;
        for (int iter = 0; iter < ITERATIONS; iter++) {
            // @a mid
            double mid = (low + high) / 2;
            int needed = 0;
            for (double gap : gaps) {
                // @a gapTally
                needed += (int) Math.ceil(gap / mid) - 1;
            }
            if (needed <= k) {
                // @a feasible
                high = mid;
            } else {
                // @a infeasible
                low = mid;
            }
        }
        // @a done
        return high;
    }
}
