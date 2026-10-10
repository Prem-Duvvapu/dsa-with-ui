public class Solution {
    public int solve(int[] pages, int m) {
        // @a impossible
        if (m > pages.length) return -1;
        int low = max(pages), high = sum(pages), ans = high;
        // @a init
        while (low <= high) {
            // @a mid
            int mid = low + (high - low) / 2;
            if (countStudents(pages, mid) <= m) {
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

    private int countStudents(int[] pages, int cap) {
        int students = 1, pageSum = 0;
        for (int p : pages) {
            if (pageSum + p > cap) {
                // @a newStudent
                students++;
                pageSum = p;
            } else {
                // @a addToCurrent
                pageSum += p;
            }
        }
        return students;
    }

    private int max(int[] pages) {
        int largest = pages[0];
        for (int p : pages) largest = Math.max(largest, p);
        return largest;
    }

    private int sum(int[] pages) {
        int total = 0;
        for (int p : pages) total += p;
        return total;
    }
}
