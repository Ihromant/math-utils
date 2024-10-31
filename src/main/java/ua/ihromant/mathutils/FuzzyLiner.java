package ua.ihromant.mathutils;

import java.util.Arrays;

public class FuzzyLiner {
    private final int pc;
    private final boolean[][] s;
    private final boolean[][] d;
    private final boolean[][][] l;
    private final boolean[][][] t;

    public FuzzyLiner(int pc) {
        this.pc = pc;
        this.s = new boolean[pc][pc];
        this.d = new boolean[pc][pc];
        this.l = new boolean[pc][pc][pc];
        this.t = new boolean[pc][pc][pc];
    }

    public FuzzyLiner(int[][] lines) {
        this(Arrays.stream(lines).mapToInt(l -> Arrays.stream(l).max().orElseThrow()).max().orElseThrow() + 1);
        for (int[] line : lines) {
            for (int i = 0; i < l.length; i++) {
                int p1 = line[i];
                for (int j = i + 1; j < l.length; j++) {
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
        if (s[i][j]) {
            throw new IllegalArgumentException(i + " " + j);
        }
        boolean res = !d[i][j];
        d[i][j] = true;
        return res;
    }

    public boolean unite(int i, int j) {
        if (d[i][j]) {
            throw new IllegalArgumentException(i + " " + j);
        }
        boolean res = !s[i][j];
        s[i][j] = true;
        return res;
    }

    public boolean colline(int a, int b, int c) {
        if (t[a][b][c]) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        boolean res = !l[a][b][c];
        l[a][b][c] = true;
        return res;
    }

    public boolean triangle(int a, int b, int c) {
        if (l[a][b][c]) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        boolean res = !t[a][b][c];
        t[a][b][c] = true;
        return res;
    }

    public boolean collinear(int a, int b, int c) {
        return l[a][b][c];
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
