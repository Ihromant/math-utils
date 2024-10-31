package ua.ihromant.mathutils;

import java.util.Arrays;

public class FuzzyLiner {
    private final int pc;
    private final boolean[][] d;
    private final boolean[][][] l;
    private final boolean[][][] t;

    public FuzzyLiner(int pc) {
        this.pc = pc;
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
        if (i == j) {
            throw new IllegalArgumentException(i + " " + j);
        }
        if (d[i][j]) {
            return false;
        }
        d[i][j] = true;
        d[j][i] = true;
        return true;
    }

    public boolean colline(int a, int b, int c) {
        if (a == b || a == c || b == c || t[a][b][c]) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        if (l[a][b][c]) {
            return false;
        }
        l[a][b][c] = true;
        l[a][c][b] = true;
        l[b][a][c] = true;
        l[b][c][a] = true;
        l[c][a][b] = true;
        l[c][b][a] = true;
        return true;
    }

    public boolean triangle(int a, int b, int c) {
        if (a == b || a == c || b == c || l[a][b][c]) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        if (t[a][b][c]) {
            return false;
        }
        t[a][b][c] = true;
        t[a][c][b] = true;
        t[b][a][c] = true;
        t[b][c][a] = true;
        t[c][a][b] = true;
        t[c][b][a] = true;
        return true;
    }

    public boolean collinear(int a, int b, int c) {
        return l[a][b][c];
    }

    public int intersection(Pair fst, Pair snd) {
        for (int i = 0; i < pc; i++) {
            if (l[fst.f()][fst.s()][i] && l[snd.f()][snd.s()][i]) {
                return i;
            }
        }
        return -1;
    }
}
