package ua.ihromant.mathutils;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;

public class FuzzyLiner {
    private final int pc;
    private final FixBS s;
    private final FixBS d;
    private final FixBS l;
    private final FixBS t;

    public FuzzyLiner(int pc) {
        this.pc = pc;
        this.s = new FixBS(pc * pc);
        this.d = new FixBS(pc * pc);
        this.l = new FixBS(pc * pc * pc);
        this.t = new FixBS(pc * pc * pc);
    }

    public FuzzyLiner(int pc, int[][] lines) {
        this(pc);
        for (int[] line : lines) {
            for (int i = 0; i < l.length(); i++) {
                int p1 = line[i];
                for (int j = i + 1; j < l.length(); j++) {
                    int p2 = line[j];
                    for (int k = 0; k < pc; k++) {
                        if (k == p1 || k == p2) {
                            continue;
                        }
                        if (Arrays.binarySearch(line, k) >= 0) {
                            colline(p1, p2, k);
                        } else {
                            triangle(p1, p2, k);
                        }
                    }
                }
            }
        }
    }

    public boolean distinguish(int i, int j) {
        int idx = idx(i, j);
        if (s.get(idx)) {
            throw new IllegalArgumentException(i + " " + j);
        }
        boolean res = !d.get(idx);
        d.set(idx);
        return res;
    }

    public boolean unite(int i, int j) {
        int idx = idx(i, j);
        if (d.get(idx)) {
            throw new IllegalArgumentException(i + " " + j);
        }
        boolean res = !s.get(idx);
        s.set(idx);
        return res;
    }

    public boolean colline(int a, int b, int c) {
        int idx = idx(a, b, c);
        if (t.get(idx)) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        boolean res = !l.get(idx);
        l.set(idx);
        return res;
    }

    public boolean triangle(int a, int b, int c) {
        int idx = idx(a, b, c);
        if (l.get(idx)) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        boolean res = !t.get(idx);
        t.set(idx);
        return res;
    }

    private int idx(int i, int j) {
        return i * pc + j;
    }

    private int idx(int i, int j, int k) {
        return (i * pc + j) * pc + k;
    }

    public boolean collinear(int a, int b, int c) {
        if (a == b || a == c || b == c) {
            return true;
        }
        return l.get(idx(a, b, c));
    }

    public int intersection(Pair fst, Pair snd) {
        if (fst.f() == snd.f() || fst.s() == snd.f()) {
            return snd.f();
        }
        if (fst.f() == snd.s() || fst.s() == snd.s()) {
            return snd.s();
        }
        for (int i = 0; i < pc; i++) {
            if (collinear(fst.f(), fst.s(), i) && collinear(snd.f(), snd.s(), i)) {
                return i;
            }
        }
        return -1;
    }
}
