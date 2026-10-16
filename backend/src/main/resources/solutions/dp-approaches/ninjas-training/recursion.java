public class Solution {
    public int solve(int[][] points) {
        // @a start
        int answer = 0;
        for (int task = 0; task < 3; task++) {
            // @a reduce
            answer = Math.max(answer, score(points.length - 1, task, points));
        }
        // @a done
        return answer;
    }
    private int score(int day, int task, int[][] points) {
        // @a enter
        if (day == 0) {
            // @a base
            return points[0][task];
        }
        // @a branch
        int previous = 0;
        for (int other = 0; other < 3; other++) {
            if (other != task) previous = Math.max(previous, score(day - 1, other, points));
        }
        // @a combine
        return points[day][task] + previous;
    }
}
