package ua.ihromant.mathutils;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.stream.Collectors;

public record Matrix(int[][] vals) {
    public Matrix sqr() {
        int len = vals.length;
        int[][] res = new int[vals.length][vals.length];
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len; j++) {
                for (int k = 0; k < len; k++) {
                    res[i][j] += vals[i][k] * vals[k][j];
                }
            }
        }
        return new Matrix(res);
    }

    public Matrix subMatrix(int k) {
        int[][] res = new int[k][k];
        for (int i = 0; i < k; i++) {
            System.arraycopy(vals[i], 0, res[i], 0, k);
        }
        return new Matrix(res);
    }

    @Override
    public String toString() {
        int ml = String.valueOf(Arrays.stream(vals).mapToInt(arr -> Arrays.stream(arr).max().orElseThrow()).max().orElseThrow()).length();
        return Arrays.stream(vals).map(arr -> Arrays.stream(arr).mapToObj(i -> {
            String s = String.valueOf(i);
            return " ".repeat(ml - s.length()) + s;
        }).collect(Collectors.joining(" "))).collect(Collectors.joining("\n"));
    }

    public static Matrix sqrInc(FixInc inc) {
        int v = inc.v();
        int b = inc.b();
        int[][] res = new int[v + b][v + b];
        for (int i = 0; i < b; i++) {
            for (int j = i; j < b; j++) {
                FixBS fbs = inc.lines()[i].copy();
                fbs.and(inc.lines()[j]);
                res[v + i][v + j] = res[v + j][v + i] = fbs.cardinality();
            }
        }
        for (int i = 0; i < v; i++) {
            for (int j = i; j < v; j++) {
                int cnt = 0;
                for (int l = 0; l < b; l++) {
                    if (inc.inc(l, i) && inc.inc(l, j)) {
                        cnt++;
                    }
                }
                res[i][j] = res[j][i] = cnt;
            }
        }
        return new Matrix(res);
    }
}
