package ua.ihromant.mathutils.fuzzy;

import lombok.Getter;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Getter
public class FuzzySLiner {
    private final int pc;
    private final boolean[][] s;
    private final boolean[][] d;
    private final boolean[][][] l;
    private final boolean[][][] t;

    public FuzzySLiner(int pc) {
        this.pc = pc;
        this.s = new boolean[pc][pc];
        this.d = new boolean[pc][pc];
        this.l = new boolean[pc][pc][pc];
        this.t = new boolean[pc][pc][pc];
    }

    public static FuzzySLiner of(int[][] lines, Triple[] triangles) {
        int pc = Arrays.stream(lines).mapToInt(l -> Arrays.stream(l).max().orElseThrow()).max().orElseThrow() + 1;
        FuzzySLiner res = new FuzzySLiner(pc);
        ArrayDeque<Rel> queue = new ArrayDeque<>(pc * pc);
        for (int[] line : lines) {
            for (int i = 0; i < line.length; i++) {
                int p1 = line[i];
                for (int j = i + 1; j < line.length; j++) {
                    int p2 = line[j];
                    for (int k = j + 1; k < line.length; k++) {
                        queue.add(new Col(p1, p2, line[k]));
                    }
                }
            }
        }
        for (Triple t : triangles) {
            queue.add(new Trg(t.f(), t.s(), t.t()));
        }
        res.update(queue);
        return res;
    }

    public FuzzySLiner addPoints(int cnt) {
        FuzzySLiner res = new FuzzySLiner(pc + cnt);
        for (int i = 0; i < pc; i++) {
            System.arraycopy(this.s[i], 0, res.s[i], 0, pc);
            System.arraycopy(this.d[i], 0, res.d[i], 0, pc);
            for (int j = 0; j < pc; j++) {
                System.arraycopy(this.l[i][j], 0, res.l[i][j], 0, pc);
                System.arraycopy(this.t[i][j], 0, res.t[i][j], 0, pc);
            }
        }
        return res;
    }

    public FuzzySLiner copy() {
        return addPoints(0);
    }

    public boolean merge(int i, int j) {
        if (d[i][j]) {
            throw new IllegalArgumentException(i + " " + j);
        }
        if (s[i][j]) {
            return false;
        }
        s[i][j] = true;
        s[j][i] = true;
        return true;
    }

    public boolean distinguish(int i, int j) {
        if (i == j || s[i][j]) {
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

    public boolean same(int a, int b) {
        return s[a][b];
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

    public void update(Queue<Rel> queue) {
        while (!queue.isEmpty()) {
            switch (queue.poll()) {
                case Same sm -> checkSame(queue, sm);
                case Dist dt -> checkDist(queue, dt);
                case Col cl -> checkCol(queue, cl);
                case Trg tr -> checkTrg(queue, tr);
            }
        }
    }

    private void checkDist(Queue<Rel> queue, Dist d) {
        int x = d.f();
        int y = d.s();
        if (!distinguish(x, y)) {
            return;
        }
        for (int a = 0; a < pc; a++) {
            if (same(a, x)) {
                queue.add(new Dist(a, y));
            }
            if (same(a, y)) {
                queue.add(new Dist(a, x));
            }
            for (int b = 0; b < pc; b++) {
                if (collinear(a, x, b) && collinear(a, y, b)) {
                    queue.add(new Col(x, a, y));
                    queue.add(new Col(x, b, y));
                }
            }
        }
    }

    private void checkSame(Queue<Rel> queue, Same s) {
        int x = s.f();
        int y = s.s();
        if (!merge(x, y)) {
            return;
        }
        for (int a = 0; a < pc; a++) {
            if (same(x, a)) {
                queue.add(new Same(a, y));
            }
            if (same(y, a)) {
                queue.add(new Same(a, x));
            }
            if (distinct(x, a)) {
                queue.add(new Dist(a, y));
            }
            if (distinct(y, a)) {
                queue.add(new Dist(a, x));
            }
            for (int b = 0; b < pc; b++) {
                if (collinear(x, a, b)) {
                    queue.add(new Col(y, a, b));
                }
                if (collinear(y, a, b)) {
                    queue.add(new Col(x, a, b));
                }
                if (triangle(x, a, b)) {
                    queue.add(new Trg(y, a, b));
                }
                if (triangle(y, a, b)) {
                    queue.add(new Trg(x, a, b));
                }
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
        for (int w = 0; w < pc; w++) {
            if (same(w, x)) {
                queue.add(new Col(w, y, z));
            }
            if (same(w, y)) {
                queue.add(new Col(w, x, z));
            }
            if (same(w, z)) {
                queue.add(new Col(w, x, y));
            }
            if (distinct(w, x) && collinear(w, y, z)) {
                queue.add(new Col(w, x, y));
                queue.add(new Col(w, x, z));
            }
            if (distinct(w, y) && collinear(w, x, z)) {
                queue.add(new Col(w, y, x));
                queue.add(new Col(w, y, z));
            }
            if (distinct(w, z) && collinear(w, x, y)) {
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
        for (int w = 0; w < pc; w++) {
            if (same(w, x)) {
                queue.add(new Trg(w, y, z));
            }
            if (same(w, y)) {
                queue.add(new Trg(w, x, z));
            }
            if (same(w, z)) {
                queue.add(new Trg(w, x, y));
            }
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
        for (int i = 0; i < pc; i++) {
            for (int j = i + 1; j < pc; j++) {
                for (int k = j + 1; k < pc; k++) {
                    if (!collinear(i, j, k) && !triangle(i, j, k)) {
                        result.add(new Triple(i, j, k));
                    }
                }
            }
        }
        return result;
    }

    public List<Pair> undefinedPairs() {
        List<Pair> result = new ArrayList<>();
        for (int i = 0; i < pc; i++) {
            for (int j = i + 1; j < pc; j++) {
                if (!distinct(i, j) && !same(i, j)) {
                    result.add(new Pair(i, j));
                }
            }
        }
        return result;
    }

    public Triple undefinedTriple() {
        for (int i = 0; i < pc; i++) {
            for (int j = i + 1; j < pc; j++) {
                for (int k = j + 1; k < pc; k++) {
                    if (!collinear(i, j, k) && !triangle(i, j, k)) {
                        return new Triple(i, j, k);
                    }
                }
            }
        }
        return null;
    }

    public Pair undefinedPair() {
        for (int i = 0; i < pc; i++) {
            for (int j = i + 1; j < pc; j++) {
                if (!distinct(i, j) && !same(i, j)) {
                    return new Pair(i, j);
                }
            }
        }
        return null;
    }


    public Set<FixBS> lines() {
        Set<FixBS> lines = new HashSet<>();
        for (int i = 0; i < pc; i++) {
            for (int j = i + 1; j < pc; j++) {
                if (!distinct(i, j)) {
                    continue;
                }
                FixBS res = new FixBS(pc);
                res.set(i);
                res.set(j);
                for (int k = 0; k < pc; k++) {
                    if (collinear(i, j, k)) {
                        res.set(k);
                    }
                }
                lines.add(res);
            }
        }
        return lines;
    }

    public FuzzySLiner quotient() {
        int[] newMap = new int[pc];
        int newPc = 0;
        ex: for (int i = 0; i < pc; i++) {
            for (int j = 0; j < i; j++) {
                if (same(i, j)) {
                    newMap[i] = newMap[j];
                    continue ex;
                }
            }
            newMap[i] = newPc++;
        }
        if (newPc == pc) {
            return this;
        }
        FuzzySLiner result = new FuzzySLiner(newPc);
        for (int i = 0; i < pc; i++) {
            for (int j = 0; j < pc; j++) {
                if (s[i][j]) {
                    result.s[newMap[i]][newMap[j]] = true;
                }
                if (d[i][j]) {
                    result.d[newMap[i]][newMap[j]] = true;
                }
                for (int k = 0; k < pc; k++) {
                    if (l[i][j][k]) {
                        result.l[newMap[i]][newMap[j]][newMap[k]] = true;
                    }
                    if (t[i][j][k]) {
                        result.t[newMap[i]][newMap[j]][newMap[k]] = true;
                    }
                }
            }
        }
        return result;
    }

    public FuzzySLiner subLiner(int cap) {
        FuzzySLiner res = new FuzzySLiner(cap);
        for (int i = 0; i < cap; i++) {
            System.arraycopy(s[i], 0, res.s[i], 0, cap);
            System.arraycopy(d[i], 0, res.d[i], 0, cap);
            for (int j = 0; j < cap; j++) {
                System.arraycopy(l[i][j], 0, res.l[i][j], 0, cap);
                System.arraycopy(t[i][j], 0, res.t[i][j], 0, cap);
            }
        }
        return res;
    }

    public FuzzySLiner subLiner(FixBS pts) {
        Map<Integer, Integer> idxes = new HashMap<>();
        int counter = 0;
        for (int i = pts.nextSetBit(0); i >= 0; i = pts.nextSetBit(i + 1)) {
            idxes.put(i, counter++);
        }
        FuzzySLiner res = new FuzzySLiner(idxes.size());
        for (int i = pts.nextSetBit(0); i >= 0; i = pts.nextSetBit(i + 1)) {
            for (int j = pts.nextSetBit(0); j >= 0; j = pts.nextSetBit(j + 1)) {
                res.s[idxes.get(i)][idxes.get(j)] = s[i][j];
                res.d[idxes.get(i)][idxes.get(j)] = d[i][j];
                for (int k = pts.nextSetBit(0); k >= 0; k = pts.nextSetBit(k + 1)) {
                    res.l[idxes.get(i)][idxes.get(j)][idxes.get(k)] = l[i][j][k];
                    res.t[idxes.get(i)][idxes.get(j)][idxes.get(k)] = t[i][j][k];
                }
            }
        }
        return res;
    }

    public FuzzySLiner intersectLines() {
        Queue<Rel> queue = new ArrayDeque<>(2 * pc * pc);
        List<FixBS> lines = new ArrayList<>(lines());
        int pt = pc;
        for (int i = 0; i < lines.size(); i++) {
            FixBS l1 = lines.get(i);
            int a = l1.nextSetBit(0);
            int b = l1.nextSetBit(a + 1);
            for (int j = i + 1; j < lines.size(); j++) {
                FixBS l2 = lines.get(j);
                if (l1.intersects(l2)) {
                    continue;
                }
                int c = l2.nextSetBit(0);
                int d = l2.nextSetBit(c + 1);
                queue.add(new Col(a, b, pt));
                queue.add(new Col(c, d, pt));
                pt++;
            }
        }
        FuzzySLiner res = addPoints(pt - pc);
        res.update(queue);
        return res;
    }

    public void printChars() {
        int s = 0;
        int d = 0;
        int c = 0;
        int t = 0;
        for (int i = 0; i < pc; i++) {
            for (int j = i + 1; j < pc; j++) {
                if (distinct(i, j)) {
                    d++;
                }
                if (same(i, j)) {
                    s++;
                }
                for (int k = j + 1; k < pc; k++) {
                    if (collinear(i, j, k)) {
                        c++;
                    }
                    if (triangle(i, j, k)) {
                        t++;
                    }
                }
            }
        }
        System.out.println(pc + " " + s + " " + d + " " + (pc * (pc - 1) / 2 - s - d) + " " + c + " " + t
                + " " + (pc * (pc - 1) * (pc - 2) / 6 - c - t));
    }

    public int intersection(int a, int b, int c, int d) {
        for (int i = 0; i < pc; i++) {
            if (collinear(a, b, i) && collinear(c, d, i)) {
                return i;
            }
        }
        return -1;
    }

    public Set<FixBS> determinedLines(FixBS determined) {
        Set<FixBS> lines = new HashSet<>();
        for (int i = 0; i < pc; i++) {
            for (int j = i + 1; j < pc; j++) {
                if (!distinct(i, j)) {
                    continue;
                }
                FixBS res = new FixBS(pc);
                res.set(i);
                res.set(j);
                for (int k = 0; k < pc; k++) {
                    if (collinear(i, j, k)) {
                        res.set(k);
                    }
                }
                if ((!determined.get(i) || !determined.get(j)) && res.intersection(determined).cardinality() > 1) {
                    continue;
                }
                lines.add(res);
            }
        }
        return lines;
    }

    public FixBS determinedSet() {
        FixBS res = new FixBS(pc);
        for (int i = 0; i < pc; i++) {
            for (int j = i + 1; j < pc; j++) {
                if (!same(i, j) && !distinct(i, j)) {
                    res.set(i);
                    res.set(j);
                }
            }
        }
        res.flip(0, pc);
        return res;
    }
}
