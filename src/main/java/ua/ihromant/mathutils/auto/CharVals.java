package ua.ihromant.mathutils.auto;

import ua.ihromant.mathutils.plane.Characteristic;
import ua.ihromant.mathutils.plane.TernaryRing;

public record CharVals(Characteristic chr, int[][] vals, boolean induced) {
    public static CharVals of(TernaryRing ring, int two, int order) {
        int a = 0;
        int b = 0;
        int c = 0;
        int d = 0;
        int e = 0;
        int[][] vals = new int[5][order + 1];
        for (int i = 0; i < 5; i++) {
            vals[i][2] = two;
        }
        for (int i = 3; i <= order; i++) {
            if (a == 0) {
                if ((vals[0][i] = ring.op(1, 1, vals[0][i - 1])) == 0) {
                    a = i;
                }
            }
            if (b == 0) {
                if ((vals[1][i] = ring.op(vals[1][i - 1], 1, 1)) == 0) {
                    b = i;
                }
            }
            if (c == 0) {
                if ((vals[2][i] = ring.op(1, vals[2][i - 1], 1)) == 0) {
                    c = i;
                }
            }
            if (d == 0) {
                if ((vals[3][i] = ring.op(two, vals[3][i - 1], 0)) == 1) {
                    d = i;
                }
            }
            if (e == 0) {
                if ((vals[4][i] = ring.op(vals[4][i - 1], two, 0)) == 1) {
                    e = i;
                }
            }
        }
        return new CharVals(new Characteristic(a, b, c, d - 1, e - 1), vals, a == order || b == order || c == order || d == order || e == order);
    }
}
