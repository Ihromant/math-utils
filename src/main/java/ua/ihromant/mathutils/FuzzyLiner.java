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
        for (int i = 0; i < pc; i++) {
            for (int j = i + 1; j < pc; j++) {
                distinguish(new Pair(i, j));
            }
        }
        for (int[] line : lines) {
            for (int i = 0; i < l.length(); i++) {
                int p1 = line[i];
                for (int j = i + 1; j < l.length(); j++) {
                    int p2 = line[j];
                    for (int k = 0; k < pc; k++) {
                        if (k == p1 || k == p2) {
                            continue;
                        }
                        Triple tr = new Triple(p1, p2, k);
                        if (Arrays.binarySearch(line, k) >= 0) {
                            colline(tr);
                        } else {
                            triangle(tr);
                        }
                    }
                }
            }
        }
    }

    public void distinguish(Pair p) {
        int idx = idx(p);
        if (s.get(idx)) {
            throw new IllegalArgumentException(p.toString());
        }
        d.set(idx);
    }

    public void unite(Pair p) {
        int idx = idx(p);
        if (d.get(idx)) {
            throw new IllegalArgumentException(p.toString());
        }
        s.set(idx);
    }

    public void colline(Triple tr) {
        int idx = idx(tr);
        if (t.get(idx)) {
            throw new IllegalArgumentException(tr.toString());
        }
        l.set(idx);
    }

    public void triangle(Triple tr) {
        int idx = idx(tr);
        if (l.get(idx)) {
            throw new IllegalArgumentException(tr.toString());
        }
        t.set(idx);
    }

    private int idx(Pair p) {
        return p.f() * pc + p.s();
    }

    private int idx(Triple t) {
        return (t.f() * pc + t.s()) * pc + t.t();
    }

    public boolean collinear(int a, int b, int c) {
        if (a == b || a == c || b == c) {
            return true;
        }
        return l.get(idx(new Triple(a, b, c)));
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
