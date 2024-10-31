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
            for (int i = 0; i < line.length; i++) {
                int p1 = line[i];
                for (int j = i + 1; j < line.length; j++) {
                    int p2 = line[j];
                    for (int k = 0; k < pc; k++) {
                        if (k == p1 || k == p2) {
                            continue;
                        }
                        if (Arrays.binarySearch(line, k) >= 0) {
                            colline(p1, p2, k);
                        } else {
                            triangule(p1, p2, k);
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

    public boolean triangule(int a, int b, int c) {
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

    public boolean distinct(int a, int b) {
        return d[a][b];
    }

    public boolean collinear(int a, int b, int c) {
        return l[a][b][c];
    }

    public boolean triangle(int a, int b, int c) {
        return t[a][b][c];
    }

    public int intersection(Pair fst, Pair snd) {
        for (int i = 0; i < pc; i++) {
            if (l[fst.f()][fst.s()][i] && l[snd.f()][snd.s()][i]) {
                return i;
            }
        }
        return -1;
    }

    public void update() {
        int counter = 0;
        while (updateStep()) {
            System.out.println("Updating... " + counter);
        }
    }

    private boolean updateStep() {
        boolean result = false;
        for (int x = 0; x < pc; x++) {
            for (int y = x + 1; y < pc; y++) {
                for (int z = y + 1; z < pc; z++) {
                    boolean cxyz = collinear(x, y, z);
                    if (triangle(x, y, z)) {
                        result = result | distinguish(x, y) | distinguish(y, z) | distinguish(x, z);
                    }
                    if (!cxyz) {
                        continue;
                    }
                    result = result | distinguish(x, y) | distinguish(y, z) | distinguish(x, z);
                    for (int w = z + 1; w < pc; w++) {
                        if (distinct(z, w) && collinear(x, y, w)) {
                            result = result | colline(z, w, x) | colline(z, w, y);
                        }
                        if (distinct(y, w) && collinear(x, z, w)) {
                            result = result | colline(y, w, x) | colline(y, w, z);
                        }
                        if (distinct(x, w) && collinear(z, y, w)) {
                            result = result | colline(x, w, y) | colline(x, w, z);
                        }
                        if (triangle(x, y, w)) {
                            result = result | triangule(x, z, w) | triangule(y, z, w);
                        }
                        if (triangle(x, z, w)) {
                            result = result | triangule(x, y, w) | triangule(y, z, w);
                        }
                        if (triangle(y, z, w)) {
                            result = result | triangule(x, z, w) | triangule(x, y, w);
                        }
                    }
                }
            }
        }
        return result;
    }
}
