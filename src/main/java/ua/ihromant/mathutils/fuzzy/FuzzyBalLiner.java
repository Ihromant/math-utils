package ua.ihromant.mathutils.fuzzy;

import lombok.Getter;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.IntStream;

@Getter
public class FuzzyBalLiner {
    private final int v;
    private final int k;
    private final FixBS l;
    private final FixBS t;

    private FuzzyBalLiner(int v, int k) {
        this(v, k, new FixBS(v * v * v), new FixBS(v * v * v));
    }

    private FuzzyBalLiner(int v, int k, FixBS l, FixBS t) {
        this.v = v;
        this.k = k;
        this.l = l;
        this.t = t;
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
        return new FuzzyBalLiner(v, k, l.copy(), t.copy());
    }

    private int idx(int a, int b, int c) {
        return (v * a + b) * v + c;
    }

    private boolean colline(int a, int b, int c) {
        int idx = idx(a, b, c);
        if (a == b || a == c || b == c || t.get(idx)) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        if (l.get(idx)) {
            return false;
        }
        l.set(idx);
        l.set(idx(a, c, b));
        l.set(idx(b, a, c));
        l.set(idx(b, c, a));
        l.set(idx(c, a, b));
        l.set(idx(c, b, a));
        return true;
    }

    private boolean triangule(int a, int b, int c) {
        int idx = idx(a, b, c);
        if (a == b || a == c || b == c || l.get(idx)) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        if (t.get(idx)) {
            return false;
        }
        t.set(idx);
        t.set(idx(a, c, b));
        t.set(idx(b, a, c));
        t.set(idx(b, c, a));
        t.set(idx(c, a, b));
        t.set(idx(c, b, a));
        return true;
    }

    public boolean collinear(int a, int b, int c) {
        return l.get(idx(a, b, c));
    }

    public boolean triangle(int a, int b, int c) {
        return t.get(idx(a, b, c));
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
                if (res.cardinality() > 2) {
                    lines.add(res);
                }
            }
        }
        return lines;
    }

    public int intersection(int a, int b, int c, int d) {
        for (int i = 0; i < v; i++) {
            if (collinear(a, b, i) && collinear(c, d, i)) {
                return i;
            }
        }
        return -1;
    }

    public int[][] pointChars() {
        int[][] result = new int[v][2];
        for (int a = 0; a < v; a++) {
            for (int b = 0; b < v; b++) {
                for (int c = b + 1; c < v; c++) {
                    if (collinear(a, b, c)) {
                        result[a][0]++;
                    }
                    if (triangle(a, b, c)) {
                        result[a][1]++;
                    }
                }
            }
        }
        return result;
    }

    public List<Col> undefinedTriples(int pt) {
        Set<FixBS> lines = new HashSet<>();
        for (int j = 0; j < v; j++) {
            FixBS res = new FixBS(v);
            res.set(pt);
            res.set(j);
            for (int k = 0; k < v; k++) {
                if (collinear(pt, j, k)) {
                    res.set(k);
                }
            }
            if (res.cardinality() > 2) {
                lines.add(res);
            }
        }
        FixBS smaller = lines.stream().filter(l -> l.cardinality() < k).findAny().orElse(null);
        List<Col> result = new ArrayList<>();
        if (smaller == null) {
            for (int i = 0; i < v; i++) {
                if (i == pt) {
                    continue;
                }
                for (int j = i + 1; j < v; j++) {
                    if (j == pt) {
                        continue;
                    }
                    if (!collinear(i, j, pt) && !triangle(i, j, pt)) {
                        result.add(new Col(i, j, pt));
                    }
                }
            }
        } else {
            for (int i = smaller.nextSetBit(0); i >= 0; i = smaller.nextSetBit(i + 1)) {
                if (i == pt) {
                    continue;
                }
                for (int j = 0; j < v; j++) {
                    if (j == pt || j == i) {
                        continue;
                    }
                    if (!collinear(i, j, pt) && !triangle(i, j, pt)) {
                        result.add(new Col(i, j, pt));
                    }
                }
            }
        }
        return result;
    }

    public FuzzyBalLiner removeTwins() {
        int[] beamCounts = new int[v];
        List<FixBS> lines = new ArrayList<>(lines());
        for (FixBS line : lines) {
            for (int pt = line.nextSetBit(0); pt >= 0; pt = line.nextSetBit(pt + 1)) {
                beamCounts[pt]++;
            }
        }
        FixBS filtered = new FixBS(v);
        IntStream.range(0, v).filter(i -> beamCounts[i] > 1).forEach(filtered::set);
        int pCard = filtered.cardinality();
        if (v == pCard) {
            return this;
        } else {
            FixBS[] newLines = IntStream.range(0, lines.size()).mapToObj(i -> new FixBS(pCard)).toArray(FixBS[]::new);
            int idx = 0;
            for (int pt = filtered.nextSetBit(0); pt >= 0; pt = filtered.nextSetBit(pt + 1)) {
                for (int l = 0; l < lines.size(); l++) {
                    if (lines.get(l).get(pt)) {
                        newLines[l].set(idx);
                    }
                }
                idx++;
            }
            FixBS filteredLines = new FixBS(lines.size());
            for (int l = 0; l < newLines.length; l++) {
                if (newLines[l].cardinality() > 1) {
                    filteredLines.set(l);
                }
            }
            int fCard = filteredLines.cardinality();
            if (fCard == lines.size()) {
                return FuzzyBalLiner.of(pCard, k, Arrays.stream(newLines).map(l -> l.stream().toArray()).toArray(int[][]::new));
            } else {
                FixBS[] res = new FixBS[fCard];
                int lIdx = 0;
                for (int ln = filteredLines.nextSetBit(0); ln >= 0; ln = filteredLines.nextSetBit(ln + 1)) {
                    res[lIdx++] = newLines[ln];
                }
                return FuzzyBalLiner.of(pCard, k, Arrays.stream(res).map(l -> l.stream().toArray()).toArray(int[][]::new));
            }
        }
    }
}
