package ua.ihromant.mathutils.fuzzy;

import lombok.Getter;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

@Getter
public class FuzzyBalLiner {
    private final int v;
    private final int k;
    private final boolean[][][] l;
    private final boolean[][][] t;

    public FuzzyBalLiner(int v, int k) {
        this.v = v;
        this.k = k;
        this.l = new boolean[v][v][v];
        this.t = new boolean[v][v][v];
    }

    public static FuzzyBalLiner of(int v, int k, int[][] lines) {
        FuzzyBalLiner res = new FuzzyBalLiner(v, k);
        ArrayDeque<Rel> queue = new ArrayDeque<>(2 * v * k);
        for (int[] line : lines) {
            for (int a = 0; a < line.length; a++) {
                int p1 = line[a];
                for (int b = a + 1; b < line.length; b++) {
                    int p2 = line[b];
                    for (int c = b + 1; c < line.length; c++) {
                        queue.add(new Col(p1, p2, line[c]));
                    }
                }
            }
        }
        res.update(queue);
        return res;
    }

    public FuzzyBalLiner copy() {
        FuzzyBalLiner res = new FuzzyBalLiner(v, k);
        for (int i = 0; i < v; i++) {
            for (int j = 0; j < v; j++) {
                System.arraycopy(this.l[i][j], 0, res.l[i][j], 0, v);
                System.arraycopy(this.t[i][j], 0, res.t[i][j], 0, v);
            }
        }
        return res;
    }

    private boolean colline(int a, int b, int c) {
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

    private boolean triangule(int a, int b, int c) {
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

    public boolean triangle(int a, int b, int c) {
        return t[a][b][c];
    }

    public void update(Queue<Rel> queue) {
        while (!queue.isEmpty()) {
            switch (queue.poll()) {
                case Col cl -> checkCol(queue, cl);
                case Trg tr -> checkTrg(queue, tr);
                default -> throw new IllegalArgumentException();
            }
        }
    }

    private void checkCol(Queue<Rel> queue, Col c) {
        int x = c.f();
        int y = c.s();
        int z = c.t();
        if (!colline(x, y, z)) {
            return;
        }
        queue.add(new Dist(x, y));
        queue.add(new Dist(y, z));
        queue.add(new Dist(x, z));
        FixBS line = new FixBS(v);
        for (int w = 0; w < v; w++) {
            boolean wxy = collinear(w, x, y);
            boolean wxz = collinear(w, x, z);
            boolean wyz = collinear(w, y, z);
            if (wxy || wxz || wyz) {
                line.set(w);
            }
            if (w != x && wyz) {
                queue.add(new Col(w, x, y));
                queue.add(new Col(w, x, z));
            }
            if (w != y && wxz) {
                queue.add(new Col(w, y, x));
                queue.add(new Col(w, y, z));
            }
            if (w != z && wxy) {
                queue.add(new Col(w, z, x));
                queue.add(new Col(w, z, y));
            }
            if (triangle(x, y, w)) {
                queue.add(new Trg(x, z, w));
                queue.add(new Trg(y, z, w));
            }
            if (triangle(x, z, w)) {
                queue.add(new Trg(x, y, w));
                queue.add(new Trg(y, z, w));
            }
            if (triangle(y, z, w)) {
                queue.add(new Trg(x, z, w));
                queue.add(new Trg(x, y, w));
            }
        }
        if (line.cardinality() == k) {
            for (int i = line.nextSetBit(0); i >= 0; i = line.nextSetBit(i + 1)) {
                for (int j = line.nextSetBit(i + 1); j >= 0; j = line.nextSetBit(j + 1)) {
                    for (int p = 0; p < v; p++) {
                        if (!line.get(p)) {
                            queue.add(new Trg(i, j, p));
                        }
                    }
                }
            }
        }
    }

    private void checkTrg(Queue<Rel> queue, Trg t) {
        int x = t.f();
        int y = t.s();
        int z = t.t();
        if (!triangule(x, y, z)) {
            return;
        }
        queue.add(new Dist(x, y));
        queue.add(new Dist(y, z));
        queue.add(new Dist(x, z));
        for (int w = 0; w < v; w++) {
            if (collinear(x, y, w)) {
                queue.add(new Trg(x, z, w));
                queue.add(new Trg(y, z, w));
            }
            if (collinear(x, z, w)) {
                queue.add(new Trg(x, y, w));
                queue.add(new Trg(y, z, w));
            }
            if (collinear(y, z, w)) {
                queue.add(new Trg(x, z, w));
                queue.add(new Trg(x, y, w));
            }
        }
    }

    public List<Triple> undefinedTriples() {
        List<Triple> result = new ArrayList<>();
        for (int i = 0; i < v; i++) {
            for (int j = i + 1; j < v; j++) {
                for (int k = j + 1; k < v; k++) {
                    if (!collinear(i, j, k) && !triangle(i, j, k)) {
                        result.add(new Triple(i, j, k));
                    }
                }
            }
        }
        return result;
    }

    public Triple undefinedTriple() {
        for (int i = 0; i < v; i++) {
            for (int j = i + 1; j < v; j++) {
                for (int k = j + 1; k < v; k++) {
                    if (!collinear(i, j, k) && !triangle(i, j, k)) {
                        return new Triple(i, j, k);
                    }
                }
            }
        }
        return null;
    }


    public Set<FixBS> lines() {
        Set<FixBS> lines = new HashSet<>();
        for (int i = 0; i < v; i++) {
            for (int j = i + 1; j < v; j++) {
                FixBS res = new FixBS(v);
                res.set(i);
                res.set(j);
                for (int k = 0; k < v; k++) {
                    if (collinear(i, j, k)) {
                        res.set(k);
                    }
                }
                lines.add(res);
            }
        }
        return lines;
    }

    public void printChars() {
        int c = 0;
        int t = 0;
        for (int i = 0; i < v; i++) {
            for (int j = i + 1; j < v; j++) {
                for (int k = j + 1; k < v; k++) {
                    if (collinear(i, j, k)) {
                        c++;
                    }
                    if (triangle(i, j, k)) {
                        t++;
                    }
                }
            }
        }
        System.out.println(v + c + " " + t + " " + (v * (v - 1) * (v - 2) / 6 - c - t));
    }

    public int intersection(int a, int b, int c, int d) {
        for (int i = 0; i < v; i++) {
            if (collinear(a, b, i) && collinear(c, d, i)) {
                return i;
            }
        }
        return -1;
    }
}
