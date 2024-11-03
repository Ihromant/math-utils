package ua.ihromant.mathutils;

import lombok.Getter;
import ua.ihromant.mathutils.plane.Quad;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class FuzzyLiner {
    private final int pc;
    private final Set<Pair> d;
    private final Set<Triple> l;
    private final Set<Triple> t;

    public FuzzyLiner(int pc, Set<Pair> d, Set<Triple> s, Set<Triple> t) {
        this.pc = pc;
        this.d = new HashSet<>(d);
        this.l = new HashSet<>(s);
        this.t = new HashSet<>(t);
    }

    public FuzzyLiner(int[][] lines, Triple[] triangles) {
        this(Arrays.stream(lines).mapToInt(l -> Arrays.stream(l).max().orElseThrow()).max().orElseThrow() + 1, Set.of(), Set.of(), Set.of());
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

    public FuzzyLiner addPoint() {
        return new FuzzyLiner(pc + 1, d, l, t);
    }

    public FuzzyLiner copy() {
        return new FuzzyLiner(pc, d, l, t);
    }

    public boolean distinguish(int i, int j) {
        if (i == j) {
            throw new IllegalArgumentException(i + " " + j);
        }
        return d.add(new Pair(i, j));
    }

    public boolean colline(int a, int b, int c) {
        if (a == b || a == c || b == c || triangle(a, b, c)) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        return l.add(new Triple(a, b, c));
    }

    public boolean triangule(int a, int b, int c) {
        if (a == b || a == c || b == c || collinear(a, b, c)) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        return t.add(new Triple(a, b, c));
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
                for (int z = 0; z < pc; z++) {
                    boolean cxyz = collinear(x, y, z);
                    if (triangle(x, y, z)) {
                        result = result | distinguish(x, y) | distinguish(y, z) | distinguish(x, z);
                    }
                    if (!cxyz) {
                        continue;
                    }
                    result = result | distinguish(x, y) | distinguish(y, z) | distinguish(x, z);
                    for (int w = 0; w < pc; w++) {
                        if (distinct(z, w) && collinear(x, y, w)) {
                            result = result | colline(z, w, x) | colline(z, w, y);
                        }
                        if (triangle(x, y, w)) {
                            result = result | triangule(x, z, w) | triangule(y, z, w);
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
                //if (res.cardinality() > 2) {
                    lines.add(res);
                //}
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

    public FuzzyLiner intersectLines() {
        FuzzyLiner base = copy();
        List<FixBS> lines = new ArrayList<>(base.lines());
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
                int pt = base.pc;
                base = base.addPoint();
                base.colline(a, b, pt);
                base.colline(c, d, pt);
            }
        }
        base.update();
        System.out.println(base.pc);
        return base;
    }

    public FuzzyLiner subLiner(int cap) {
        return new FuzzyLiner(cap, d.stream().filter(p -> p.s() < cap).collect(Collectors.toSet()),
                l.stream().filter(tr -> tr.t() < cap).collect(Collectors.toSet()),
                t.stream().filter(tr -> tr.t() < cap).collect(Collectors.toSet()));
    }
}
