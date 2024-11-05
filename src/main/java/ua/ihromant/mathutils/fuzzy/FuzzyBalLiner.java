package ua.ihromant.mathutils.fuzzy;

import lombok.Getter;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class FuzzyBalLiner {
    private final int v;
    private final int k;
    private final Set<Triple> l;
    private final Set<Triple> t;

    public FuzzyBalLiner(int v, int k, Set<Triple> s, Set<Triple> t) {
        this.v = v;
        this.k = k;
        this.l = new HashSet<>(s);
        this.t = new HashSet<>(t);
    }

    public FuzzyBalLiner(int[][] lines, Triple[] triangles) {
        this(Arrays.stream(lines).mapToInt(l -> Arrays.stream(l).max().orElseThrow()).max().orElseThrow() + 1, lines[0].length, Set.of(), Set.of());
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

    public FuzzyBalLiner copy() {
        return new FuzzyBalLiner(v, k, l, t);
    }

    public boolean colline(int a, int b, int c) {
        if (a == b || a == c || b == c || triangle(a, b, c)) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        boolean added = l.add(new Triple(a, b, c));
        FixBS bc;
        if (added && (bc = line(b, c)).cardinality() == k) {
            for (int i = bc.nextSetBit(0); i >= 0; i = bc.nextSetBit(i + 1)) {
                for (int j = bc.nextSetBit(i + 1); j >= 0; j = bc.nextSetBit(j + 1)) {
                    for (int l = 0; l < v; l++) {
                        if (!bc.get(l)) {
                            triangule(i, j, l);
                        }
                    }
                }
            }
        }
        return added;
    }

    public boolean triangule(int a, int b, int c) {
        if (a == b || a == c || b == c || collinear(a, b, c)) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        return t.add(new Triple(a, b, c));
    }

    public boolean collinear(int a, int b, int c) {
        return l.contains(new Triple(a, b, c));
    }

    public boolean triangle(int a, int b, int c) {
        return t.contains(new Triple(a, b, c));
    }

    public int intersection(Pair fst, Pair snd) {
        for (int i = 0; i < v; i++) {
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
        for (int x = 0; x < v; x++) {
            for (int y = x + 1; y < v; y++) {
                for (int z = y + 1; z < v; z++) {
                    if (!collinear(x, y, z)) {
                        continue;
                    }
                    for (int w = 0; w < v; w++) {
                        if (collinear(x, y, w)) {
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

    public boolean isFull() {
        return l.size() + t.size() == v * (v - 1) * (v - 2) / 6;
    }

    public Liner toLiner() {
        if (!isFull()) {
            throw new IllegalStateException();
        }
        Set<FixBS> lines = lines();
        int[][] lns = lines.stream().map(l -> l.stream().toArray()).toArray(int[][]::new);
        return new Liner(Arrays.stream(lns).mapToInt(l -> Arrays.stream(l).max().orElseThrow()).max().orElseThrow() + 1, lns);
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

    public FixBS line(int a, int b) {
        FixBS res = new FixBS(v);
        res.set(a);
        res.set(b);
        for (int i = 0; i < v; i++) {
            if (collinear(a, b, i)) {
                res.set(i);
            }
        }
        return res;
    }
}
