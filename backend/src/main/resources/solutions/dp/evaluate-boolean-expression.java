public class Solution {
    // expression alternates T/F operands and &, |, ^ operators.
    public long solve(String expression) {
        int n = (expression.length() + 1) / 2;
        long[][] trueWays = new long[n][n], falseWays = new long[n][n];
        // @a init
        for (int i = 0; i < n; i++) {
            trueWays[i][i] = expression.charAt(2 * i) == 'T' ? 1 : 0;
            falseWays[i][i] = 1 - trueWays[i][i];
        }
        for (int gap = 1; gap < n; gap++) {
            for (int i = 0; i + gap < n; i++) {
                int j = i + gap;
                long t = 0, f = 0;
                for (int k = i; k < j; k++) {
                    long lt = trueWays[i][k], lf = falseWays[i][k];
                    long rt = trueWays[k + 1][j], rf = falseWays[k + 1][j];
                    char op = expression.charAt(2 * k + 1);
                    // @a fill
                    if (op == '&') {
                        t += lt * rt;
                        f += lt * rf + lf * rt + lf * rf;
                    } else if (op == '|') {
                        t += lt * rt + lt * rf + lf * rt;
                        f += lf * rf;
                    } else {
                        t += lt * rf + lf * rt;
                        f += lt * rt + lf * rf;
                    }
                }
                trueWays[i][j] = t;
                falseWays[i][j] = f;
            }
        }
        // @a done
        return trueWays[0][n - 1];
    }
}
