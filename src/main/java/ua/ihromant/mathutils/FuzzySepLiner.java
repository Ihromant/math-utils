package ua.ihromant.mathutils;

import lombok.Getter;
import ua.ihromant.mathutils.plane.Quad;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class FuzzySepLiner {
    private final int pc;
    private final Set<Pair> s;
    private final Set<Pair> d;
    private final Set<Triple> l;
    private final Set<Triple> t;

    public FuzzySepLiner(int pc, Set<Pair> s, Set<Pair> d, Set<Triple> l, Set<Triple> t) {
        this.pc = pc;
        this.s = new HashSet<>(s);
        this.d = new HashSet<>(d);
        this.l = new HashSet<>(l);
        this.t = new HashSet<>(t);
    }

    public FuzzySepLiner(int[][] lines, Triple[] triangles) {
        this(Arrays.stream(lines).mapToInt(l -> Arrays.stream(l).max().orElseThrow()).max().orElseThrow() + 1, Set.of(), Set.of(), Set.of(), Set.of());
        for (int[] line : lines) {
            for (int i = 0; i < line.length; i++) {
                int p1 = line[i];
                for (int j = i + 1; j < line.length; j++) {
                    int p2 = line[j];
                    for (int k = j + 1; k < line.length; k++) {
                        colline(p1, p2, line[k]);
                    }
                }
            }
        }
        for (Triple t : triangles) {
            triangule(t.f(), t.s(), t.t());
        }
        update();
    }

    public FuzzySepLiner addPoint() {
        return new FuzzySepLiner(pc + 1, s, d, l, t);
    }

    public FuzzySepLiner copy() {
        return new FuzzySepLiner(pc, s, d, l, t);
    }

    public boolean merge(int i, int j) {
        Pair p = new Pair(i, j);
        if (d.contains(p)) {
            throw new IllegalArgumentException(i + " " + j);
        }
        return s.add(p);
    }

    public boolean distinguish(int i, int j) {
        Pair p = new Pair(i, j);
        if (i == j || s.contains(p)) {
            throw new IllegalArgumentException(i + " " + j);
        }
        return d.add(p);
    }

    public boolean colline(int a, int b, int c) {
        Triple tr = new Triple(a, b, c);
        if (a == b || a == c || b == c || t.contains(tr)) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        return l.add(tr);
    }

    public boolean triangule(int a, int b, int c) {
        Triple tr = new Triple(a, b, c);
        if (a == b || a == c || b == c || l.contains(tr)) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        return t.add(tr);
    }

    public boolean same(int a, int b) {
        return s.contains(new Pair(a, b));
    }

    public boolean distinct(int a, int b) {
        return d.contains(new Pair(a, b));
    }

    public boolean collinear(int a, int b, int c) {
        return l.contains(new Triple(a, b, c));
    }

    public boolean triangle(int a, int b, int c) {
        return t.contains(new Triple(a, b, c));
    }

    public int intersection(Pair fst, Pair snd) {
        for (int i = 0; i < pc; i++) {
            if (collinear(fst.f(), fst.s(), i) && collinear(snd.f(), snd.s(), i)) {
                return i;
            }
        }
        return -1;
    }

    public void update() {
        //int counter = 0;
        while (updateStep()) {
            //System.out.println("Updating... " + counter++);
        }
    }

    private boolean updateStep() {
        boolean result = false;
        for (int x = 0; x < pc; x++) {
            for (int y = 0; y < pc; y++) {
                if (x == y) {
                    result = result | merge(x, y);
                }
                boolean sxy = same(x, y);
                for (int z = 0; z < pc; z++) {
                    if (sxy) {
                        if (same(y, z)) {
                            result = result | merge(x, z);
                        }
                        if (distinct(y, z)) {
                            result = result | distinguish(x, z);
                        }
                        continue;
                    }
                    Triple tr = new Triple(x, y, z);
                    boolean cxyz = l.contains(tr);
                    boolean txyz = t.contains(tr);
                    if (!cxyz && !txyz) {
                        continue;
                    }
                    result = result | distinguish(x, y) | distinguish(y, z) | distinguish(x, z);
                    for (int w = 0; w < pc; w++) {
                        if (same(z, w)) {
                            if (cxyz) {
                                result = result | colline(x, y, w);
                            }
                            if (txyz) {
                                result = result | triangule(x, y, w);
                            }
                            continue;
                        }
                        if (distinct(z, w) && collinear(x, y, w)) {
                            result = result | colline(z, w, x) | colline(z, w, y);
                        }
                        if (triangle(x, y, w)) {
                            result = result | triangule(x, z, w) | triangule(y, z, w);
                            if (!cxyz) {
                                continue;
                            }
                            // a -> x, b -> y, x -> w, a' -> z, x' -> u, b' -> v
                            for (int u = 0; u < pc; u++) {
                                if (!collinear(x, z, u)) {
                                    continue;
                                }
                                for (int v = 0; v < pc; v++) {
                                    if (collinear(y, v, w) && collinear(y, v, u)) {
                                        result = result | merge(w, u);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public Quad quad(int desiredCount) {
        for (int a = 0; a < pc; a++) {
            for (int b = a + 1; b < pc; b++) {
                if (!distinct(a, b)) {
                    continue;
                }
                for (int c = b + 1; c < pc; c++) {
                    if (!triangle(a, b, c)) {
                        continue;
                    }
                    for (int d = c + 1; d < pc; d++) {
                        if (!triangle(a, c, d) || !triangle(a, b, d) || !triangle(b, c, d)) {
                            continue;
                        }
                        int cnt = 0;
                        if (intersection(new Pair(a, b), new Pair(c, d)) >= 0) {
                            cnt++;
                        }
                        if (intersection(new Pair(a, c), new Pair(b, d)) >= 0) {
                            cnt++;
                        }
                        if (intersection(new Pair(a, d), new Pair(b, c)) >= 0) {
                            cnt++;
                        }
                        if (cnt == desiredCount) {
                            return new Quad(a, b, c, d);
                        }
                    }
                }
            }
        }
        return null;
    }

    public List<Quad> quads(int desiredCount) {
        List<Quad> result = new ArrayList<>();
        for (int a = 0; a < pc; a++) {
            for (int b = a + 1; b < pc; b++) {
                if (!distinct(a, b)) {
                    continue;
                }
                for (int c = b + 1; c < pc; c++) {
                    if (!triangle(a, b, c)) {
                        continue;
                    }
                    for (int d = c + 1; d < pc; d++) {
                        if (!triangle(a, c, d) || !triangle(a, b, d) || !triangle(b, c, d)) {
                            continue;
                        }
                        int cnt = 0;
                        if (intersection(new Pair(a, b), new Pair(c, d)) >= 0) {
                            cnt++;
                        }
                        if (intersection(new Pair(a, c), new Pair(b, d)) >= 0) {
                            cnt++;
                        }
                        if (intersection(new Pair(a, d), new Pair(b, c)) >= 0) {
                            cnt++;
                        }
                        if (cnt == desiredCount) {
                            result.add(new Quad(a, b, c, d));
                        }
                    }
                }
            }
        }
        return result;
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

    public boolean isFull() {
        return d.size() == pc * (pc - 1) / 2 && l.size() + t.size() == pc * (pc - 1) * (pc - 2) / 6;
    }

    public Liner toLiner() {
        if (!isFull()) {
            throw new IllegalStateException();
        }
        Set<FixBS> lines = new HashSet<>();
        for (int i = 0; i < pc; i++) {
            for (int j = i + 1; j < pc; j++) {
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
        int[][] lns = lines.stream().map(l -> l.stream().toArray()).toArray(int[][]::new);
        return new Liner(Arrays.stream(lns).mapToInt(l -> Arrays.stream(l).max().orElseThrow()).max().orElseThrow() + 1, lns);
    }

    public Set<FixBS> lines() {
        Set<FixBS> lines = new HashSet<>();
        for (int i = 0; i < pc; i++) {
            for (int j = i + 1; j < pc; j++) {
                FixBS res = new FixBS(pc);
                res.set(i);
                res.set(j);
                for (int k = 0; k < pc; k++) {
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

    public FixBS line(int a, int b) {
        FixBS res = new FixBS(pc);
        res.set(a);
        res.set(b);
        for (int i = 0; i < pc; i++) {
            if (collinear(a, b, i)) {
                res.set(i);
            }
        }
        return res;
    }
}
