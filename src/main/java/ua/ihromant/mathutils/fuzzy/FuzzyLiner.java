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
public class FuzzyLiner {
    private final int pc;
    private final Map<Rel, Update> relations;

    public FuzzyLiner(int pc) {
        this.pc = pc;
        this.relations = new HashMap<>();
    }

    private FuzzyLiner(int pc, Map<Rel, Update> relations) {
        this.pc = pc;
        this.relations = relations;
    }

    public static FuzzyLiner of(int[][] lines, Triple[] triangles) {
        int pc = Arrays.stream(lines).mapToInt(l -> Arrays.stream(l).max().orElseThrow()).max().orElseThrow() + 1;
        FuzzyLiner res = new FuzzyLiner(pc);
        ArrayDeque<Update> queue = new ArrayDeque<>(pc * pc);
        for (int[] line : lines) {
            for (int i = 0; i < line.length; i++) {
                int p1 = line[i];
                for (int j = i + 1; j < line.length; j++) {
                    int p2 = line[j];
                    for (int k = j + 1; k < line.length; k++) {
                        queue.add(new Update(new Col(p1, p2, line[k]), "Initial"));
                    }
                }
            }
        }
        for (Triple t : triangles) {
            queue.add(new Update(new Trg(t.f(), t.s(), t.t()), "Initial"));
        }
        res.update(queue);
        return res;
    }

    public FuzzyLiner addPoints(int cnt) {
        return new FuzzyLiner(pc + cnt, new HashMap<>(relations));
    }

    public FuzzyLiner copy() {
        return addPoints(0);
    }

    private boolean merge(Same same, Update u) {
        Update old = relations.putIfAbsent(same, u);
        if (relations.containsKey(same.opposite())) {
            throw new ContradictionException(same, relations);
        }
        return old == null;
    }

    private boolean distinguish(Dist dist, Update u) {
        int i = dist.f();
        int j = dist.s();
        Update old = relations.putIfAbsent(dist, u);
        if (i == j || relations.containsKey(new Same(i, j))) {
            throw new ContradictionException(dist, relations);
        }
        return old == null;
    }

    private boolean colline(Col col, Update u) {
        int a = col.f();
        int b = col.s();
        int c = col.t();
        Update old = relations.putIfAbsent(col, u);
        if (a == b || a == c || b == c || relations.containsKey(new Trg(a, b, c))) {
            throw new ContradictionException(col, relations);
        }
        return old == null;
    }

    private boolean triangule(Trg trg, Update u) {
        int a = trg.f();
        int b = trg.s();
        int c = trg.t();
        Update old = relations.putIfAbsent(trg, u);
        if (a == b || a == c || b == c || relations.containsKey(new Col(a, b, c))) {
            throw new ContradictionException(trg, relations);
        }
        return old == null;
    }

    private boolean same(int a, int b) {
        return same(new Same(a, b));
    }

    private boolean same(Same s) {
        return relations.containsKey(s);
    }

    public boolean distinct(int a, int b) {
        return distinct(new Dist(a, b));
    }

    private boolean distinct(Dist d) {
        return relations.containsKey(d);
    }

    public boolean collinear(int a, int b, int c) {
        return collinear(new Col(a, b, c));
    }

    private boolean collinear(Col col) {
        return relations.containsKey(col);
    }

    public boolean triangle(int a, int b, int c) {
        return triangle(new Trg(a, b, c));
    }

    private boolean triangle(Trg t) {
        return relations.containsKey(t);
    }

    public void update(Queue<Update> queue) {
        while (!queue.isEmpty()) {
            Update u = queue.poll();
            switch (u.base()) {
                case Same sm -> {
                    if (merge(sm, u)) {
                        updateForSame(queue, sm);
                    }
                }
                case Dist dt -> {
                    if (distinguish(dt, u)) {
                        updateForDist(queue, dt);
                    }
                }
                case Col cl -> {
                    if (colline(cl, u)) {
                        updateForCol(queue, cl);
                    }
                }
                case Trg tr -> {
                    if (triangule(tr, u)) {
                        updateForTrg(queue, tr);
                    }
                }
            }
        }
    }

    private void updateForDist(Queue<Update> queue, Dist d) {
        int x = d.f();
        int y = d.s();
        for (int a = 0; a < pc; a++) {
            if (same(a, x)) {
                queue.add(new Update(new Dist(a, y), "A4", d, new Same(a, x)));
            }
            if (same(a, y)) {
                queue.add(new Update(new Dist(a, x), "A4", d, new Same(a, y)));
            }
            for (int b = 0; b < pc; b++) {
                if (collinear(a, x, b) && collinear(a, y, b)) {
                    queue.add(new Update(new Col(x, a, y), "A8", d, new Col(a, x, b), new Col(a, y, b)));
                    queue.add(new Update(new Col(x, b, y), "A8", d, new Col(a, x, b), new Col(a, y, b)));
                }
            }
        }
    }

    private void updateForSame(Queue<Update> queue, Same same) {
        int x = same.f();
        int y = same.s();
        for (int a = 0; a < pc; a++) {
            if (same(x, a)) {
                queue.add(new Update(new Same(a, y), "A3", same, new Same(x, a)));
            }
            if (same(y, a)) {
                queue.add(new Update(new Same(a, x), "A3", same, new Same(y, a)));
            }
            if (distinct(x, a)) {
                queue.add(new Update(new Dist(a, y), "A4", same, new Dist(x, a)));
            }
            if (distinct(y, a)) {
                queue.add(new Update(new Dist(a, x), "A4", same, new Dist(y, a)));
            }
            for (int b = 0; b < pc; b++) {
                if (collinear(x, a, b)) {
                    queue.add(new Update(new Col(y, a, b), "A5", same, new Col(x, a, b)));
                }
                if (collinear(y, a, b)) {
                    queue.add(new Update(new Col(x, a, b), "A5", same, new Col(y, a, b)));
                }
                if (triangle(x, a, b)) {
                    queue.add(new Update(new Trg(y, a, b), "A6", same, new Trg(x, a, b)));
                }
                if (triangle(y, a, b)) {
                    queue.add(new Update(new Trg(x, a, b), "A6", same, new Trg(y, a, b)));
                }
            }
        }
    }

    private void updateForCol(Queue<Update> queue, Col col) {
        int x = col.f();
        int y = col.s();
        int z = col.t();
        queue.add(new Update(new Dist(x, y), "A7", col));
        queue.add(new Update(new Dist(y, z), "A7", col));
        queue.add(new Update(new Dist(x, z), "A7", col));
        for (int w = 0; w < pc; w++) {
            if (same(w, x)) {
                queue.add(new Update(new Col(w, y, z), "A5", col, new Same(w, x)));
            }
            if (same(w, y)) {
                queue.add(new Update(new Col(w, x, z), "A5", col, new Same(w, y)));
            }
            if (same(w, z)) {
                queue.add(new Update(new Col(w, x, y), "A5", col, new Same(w, z)));
            }
            if (distinct(w, x) && collinear(w, y, z)) {
                queue.add(new Update(new Col(w, x, y), "A8", col, new Dist(w, x), new Col(w, y, z)));
                queue.add(new Update(new Col(w, x, z), "A8", col, new Dist(w, x), new Col(w, y, z)));
            }
            if (distinct(w, y) && collinear(w, x, z)) {
                queue.add(new Update(new Col(w, y, x), "A8", col, new Dist(w, y), new Col(w, x, z)));
                queue.add(new Update(new Col(w, y, z), "A8", col, new Dist(w, y), new Col(w, x, z)));
            }
            if (distinct(w, z) && collinear(w, x, y)) {
                queue.add(new Update(new Col(w, z, x), "A8", col, new Dist(w, z), new Col(w, x, y)));
                queue.add(new Update(new Col(w, z, y), "A8", col, new Dist(w, z), new Col(w, x, y)));
            }
            if (triangle(x, y, w)) {
                Trg trg = new Trg(x, y, w);
                queue.add(new Update(new Trg(x, z, w), "A9", col, trg));
                queue.add(new Update(new Trg(y, z, w), "A9", col, trg));
                for (int u = 0; u < pc; u++) {
                    if (collinear(x, u, w)) {
                        for (int v = 0; v < pc; v++) {
                            if (collinear(v, u, w) && collinear(v, y, z)) {
                                queue.add(new Update(new Same(x, v), "A10", col, new Col(x, u, w), new Col(v, u, w), new Col(v, y, z), trg));
                            }
                        }
                    }
                    if (collinear(y, u, w)) {
                        for (int v = 0; v < pc; v++) {
                            if (collinear(v, u, w) && collinear(v, x, z)) {
                                queue.add(new Update(new Same(y, v), "A10", col, new Col(y, u, w), new Col(v, u, w), new Col(v, x, z), trg));
                            }
                        }
                    }
                }
            }
            if (triangle(x, z, w)) {
                Trg trg = new Trg(x, z, w);
                queue.add(new Update(new Trg(x, y, w), "A9", col, trg));
                queue.add(new Update(new Trg(y, z, w), "A9", col, trg));
                for (int u = 0; u < pc; u++) {
                    if (collinear(x, u, w)) {
                        for (int v = 0; v < pc; v++) {
                            if (collinear(v, u, w) && collinear(v, y, z)) {
                                queue.add(new Update(new Same(x, v), "A10", col, new Col(x, u, w), new Col(v, u, w), new Col(v, y, z), trg));
                            }
                        }
                    }
                    if (collinear(z, u, w)) {
                        for (int v = 0; v < pc; v++) {
                            if (collinear(v, u, w) && collinear(v, x, y)) {
                                queue.add(new Update(new Same(z, v), "A10", col, new Col(z, u, w), new Col(v, u, w), new Col(v, x, y), trg));
                            }
                        }
                    }
                }
            }
            if (triangle(y, z, w)) {
                Trg trg = new Trg(y, z, w);
                queue.add(new Update(new Trg(x, z, w), "A9", col, trg));
                queue.add(new Update(new Trg(x, y, w), "A9", col, trg));
                for (int u = 0; u < pc; u++) {
                    if (collinear(y, u, w)) {
                        for (int v = 0; v < pc; v++) {
                            if (collinear(v, u, w) && collinear(v, x, z)) {
                                queue.add(new Update(new Same(y, v), "A10", col, new Col(y, u, w), new Col(v, u, w), new Col(v, x, z), trg));
                            }
                        }
                    }
                    if (collinear(z, u, w)) {
                        for (int v = 0; v < pc; v++) {
                            if (collinear(v, u, w) && collinear(v, x, y)) {
                                queue.add(new Update(new Same(z, v), "A10", col, new Col(z, u, w), new Col(v, u, w), new Col(v, x, y), trg));
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateForTrg(Queue<Update> queue, Trg t) {
        int x = t.f();
        int y = t.s();
        int z = t.t();
        queue.add(new Update(new Dist(x, y), "A7", t));
        queue.add(new Update(new Dist(y, z), "A7", t));
        queue.add(new Update(new Dist(x, z), "A7", t));
        for (int w = 0; w < pc; w++) {
            if (same(w, x)) {
                queue.add(new Update(new Trg(w, y, z), "A6", t, new Same(w, x)));
            }
            if (same(w, y)) {
                queue.add(new Update(new Trg(w, x, z), "A6", t, new Same(w, y)));
            }
            if (same(w, z)) {
                queue.add(new Update(new Trg(w, x, y), "A6", t, new Same(w, z)));
            }
            if (collinear(x, y, w)) {
                Col col = new Col(x, y, w);
                queue.add(new Update(new Trg(x, z, w), "A9", t, col));
                queue.add(new Update(new Trg(y, z, w), "A9", t, col));
                for (int u = 0; u < pc; u++) {
                    if (collinear(y, u, z)) {
                        for (int v = 0; v < pc; v++) {
                            if (collinear(v, x, w) && collinear(v, u, z)) {
                                queue.add(new Update(new Same(y, v), "A10", t, col, new Col(y, u, z), new Col(v, x, w), new Col(v, u, z)));
                            }
                        }
                    }
                    if (collinear(x, u, z)) {
                        for (int v = 0; v < pc; v++) {
                            if (collinear(v, y, w) && collinear(v, u, z)) {
                                queue.add(new Update(new Same(x, v), "A10", t, col, new Col(x, u, z), new Col(v, y, w), new Col(v, u, z)));
                            }
                        }
                    }
                }
            }
            if (collinear(x, z, w)) {
                Col col = new Col(x, z, w);
                queue.add(new Update(new Trg(x, y, w), "A9", t, col));
                queue.add(new Update(new Trg(y, z, w), "A9", t, col));
                for (int u = 0; u < pc; u++) {
                    if (collinear(x, u, y)) {
                        for (int v = 0; v < pc; v++) {
                            if (collinear(v, z, w) && collinear(v, u, y)) {
                                queue.add(new Update(new Same(x, v), "A10", t, col, new Col(x, u, y), new Col(v, z, w), new Col(v, u, y)));
                            }
                        }
                    }
                    if (collinear(z, u, y)) {
                        for (int v = 0; v < pc; v++) {
                            if (collinear(v, x, w) && collinear(v, u, y)) {
                                queue.add(new Update(new Same(z, v), "A10", t, col, new Col(z, u, y), new Col(v, x, w), new Col(v, u, y)));
                            }
                        }
                    }
                }
            }
            if (collinear(y, z, w)) {
                Col col = new Col(y, z, w);
                queue.add(new Update(new Trg(x, z, w), "A9", t, col));
                queue.add(new Update(new Trg(x, y, w), "A9", t, col));
                for (int u = 0; u < pc; u++) {
                    if (collinear(y, u, x)) {
                        for (int v = 0; v < pc; v++) {
                            if (collinear(v, z, w) && collinear(v, u, x)) {
                                queue.add(new Update(new Same(y, v), "A10", t, col, new Col(y, u, x), new Col(v, z, w), new Col(v, u, x)));
                            }
                        }
                    }
                    if (collinear(z, u, x)) {
                        for (int v = 0; v < pc; v++) {
                            if (collinear(v, y, w) && collinear(v, u, x)) {
                                queue.add(new Update(new Same(z, v), "A10", t, col, new Col(z, u, x), new Col(v, y, w), new Col(v, u, x)));
                            }
                        }
                    }
                }
            }
        }
    }

    public List<Triple> undefinedTriples() {
        List<Triple> result = new ArrayList<>();
        for (int i = 0; i < pc; i++) {
            for (int j = i + 1; j < pc; j++) {
                for (int k = j + 1; k < pc; k++) {
                    if (distinct(i, j) && distinct(j, k) && distinct(i, k) && !collinear(i, j, k) && !triangle(i, j, k)) {
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
                    if (distinct(i, j) && distinct(j, k) && distinct(i, k) && !collinear(i, j, k) && !triangle(i, j, k)) {
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

    public FuzzyLiner intersectLines() {
        Queue<Update> queue = new ArrayDeque<>(2 * pc * pc);
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
                queue.add(new Update(new Col(a, b, pt), pt + " is intersection of [" + a + " " + b + "] and [" + c + " " + d + "]"));
                queue.add(new Update(new Col(c, d, pt), pt + " is intersection of [" + a + " " + b + "] and [" + c + " " + d + "]"));
                pt++;
            }
        }
        FuzzyLiner res = addPoints(pt - pc);
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
