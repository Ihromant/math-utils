package ua.ihromant.mathutils;

import ua.ihromant.mathutils.fuzzy.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class AffinePlane {
    private final Liner plane;
    private final int dl;

    public AffinePlane(Liner plane, int dl) {
        this.plane = plane;
        this.dl = dl;
    }

    public int[] line(int line) {
        if (line == dl) {
            throw new IllegalArgumentException();
        }
        return plane.line(line);
    }

    public int line(int p1, int p2) {
        if (plane.flag(dl, p1) || plane.flag(dl, p2)) {
            throw new IllegalArgumentException();
        }
        return plane.line(p1, p2);
    }

    public Iterable<Integer> lines() {
        return () -> IntStream.range(0, plane.lineCount()).filter(l -> l != dl).boxed().iterator();
    }

    public int[] lines(int point) {
        if (plane.flag(dl, point)) {
            throw new IllegalArgumentException();
        }
        return plane.lines(point);
    }

    public int intersection(int l1, int l2) {
        int p = plane.intersection(l1, l2);
        if (plane.flag(dl, p)) {
            throw new IllegalArgumentException();
        }
        return p;
    }

    public Iterable<Integer> points() {
        return () -> IntStream.range(0, plane.pointCount()).filter(p -> !plane.flag(dl, p)).boxed().iterator();
    }

    public Iterable<Integer> points(int line) {
        if (line == dl) {
            throw new IllegalArgumentException();
        }
        return () -> Arrays.stream(plane.line(line)).filter(p -> !plane.flag(dl, p)).boxed().iterator();
    }

    public Iterable<Integer> notLine(int line) {
        if (line == dl) {
            throw new IllegalArgumentException();
        }
        return () -> IntStream.range(0, plane.pointCount()).filter(p -> !plane.flag(line, p) && !plane.flag(dl, p)).boxed().iterator();
    }

    public Liner toLiner() {
        return plane.subPlane(IntStream.range(0, plane.pointCount()).filter(p -> !plane.flag(dl, p)).toArray());
    }

    public Liner subPlane(int[] pointArray) {
        return plane.subPlane(pointArray);
    }

    public boolean isParallel(int l1, int l2) {
        return l1 == l2 || plane.flag(dl, plane.intersection(l1, l2));
    }

    public Iterable<Integer> parallel(int line) {
        if (line == dl) {
            throw new IllegalArgumentException();
        }
        return () -> Arrays.stream(plane.point(plane.intersection(line, dl))).filter(l -> l != dl).boxed().iterator();
    }

    public int parallel(int l, int p) {
        if (l == dl || plane.flag(dl, p)) {
            throw new IllegalArgumentException();
        }
        return plane.line(p, plane.intersection(l, dl));
    }

    public boolean isParaPappus() {
        for (int l1 : lines()) {
            for (int l2 : parallel(l1)) {
                if (l2 <= l1) {
                    continue;
                }
                for (int a1 : points(l1)) {
                    for (int a2 : points(l1)) {
                        if (a2 == a1) {
                            continue;
                        }
                        for (int a3 : points(l1)) {
                            if (a3 == a2 || a3 == a1) {
                                continue;
                            }
                            for (int b2 : points(l2)) {
                                int b1 = intersection(l2, parallel(line(a3, b2), a2));
                                int b3 = intersection(l2, parallel(line(a1, b2), a2));
                                if (!isParallel(line(a1, b1), line(a3, b3))) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean isParaDesargues() {
        for (int l1 : lines()) {
            for (int l2 : parallel(l1)) {
                if (l2 <= l1) {
                    continue;
                }
                for (int l3 : parallel(l1)) {
                    if (l3 <= l2) {
                        continue;
                    }
                    for (int a1 : points(l1)) {
                        for (int a2 : points(l2)) {
                            for (int a3 : points(l3)) {
                                for (int b2 : points(l2)) {
                                    if (b2 == a2) {
                                        continue;
                                    }
                                    int b1 = intersection(l1, parallel(line(a1, a2), b2));
                                    int b3 = intersection(l3, parallel(line(a3, a2), b2));
                                    if (!isParallel(line(a1, a3), line(b1, b3))) {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean isCubeDesargues() {
        int anyPoint = points().iterator().next();
        for (int infl : lines(anyPoint)) {
            for (int infp : lines(anyPoint)) {
                if (infl == infp) {
                    continue;
                }
                for (int l1 : parallel(infl)) {
                    for (int l2 : parallel(infl)) {
                        if (l1 == l2) {
                            continue;
                        }
                        for (int l3 : parallel(infl)) {
                            if (l3 == l2 || l3 == l1) {
                                continue;
                            }
                            for (int l4 : parallel(infl)) {
                                if (l4 == l3 || l4 == l2 || l4 == l1) {
                                    continue;
                                }
                                for (int p1 : parallel(infp)) {
                                    for (int p2 : parallel(infp)) {
                                        if (p1 == p2) {
                                            continue;
                                        }
                                        for (int p3 : parallel(infp)) {
                                            if (p3 == p2 || p3 == p1) {
                                                continue;
                                            }
                                            for (int p4 : parallel(infp)) {
                                                if (p4 == p3 || p4 == p2 || p4 == p1) {
                                                    continue;
                                                }
                                                int a = line(intersection(p1, l3), intersection(p2, l4));
                                                int b = line(intersection(p1, l1), intersection(p2, l2));
                                                int c = line(intersection(l1, p3), intersection(l2, p4));
                                                int d = line(intersection(l3, p3), intersection(l4, p4));
                                                if (isParallel(a, b) && isParallel(a, c) && !isParallel(a, d)) {
                                                    return false;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean isDiagonal() {
        for (int x00 : points()) {
            for (int x01 : points()) {
                if (x01 == x00) {
                    continue;
                }
                for (int x10 : points()) {
                    if (x10 == x00 || x10 == x01 || line(x10, x00) == line(x10, x01)) {
                        continue;
                    }
                    int x11 = intersection(parallel(line(x00, x01), x10), parallel(line(x00, x10), x01));
                    int x12 = intersection(parallel(line(x00, x11), x01), line(x10, x11));
                    int x21 = intersection(parallel(line(x00, x11), x10), line(x01, x11));
                    int x22 = intersection(parallel(line(x00, x01), x21), parallel(line(x00, x10), x12));
                    if (!plane.flag(line(x00, x11), x22)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public int zigZagNumber(int o, int x, int y) {
        int counter = 0;
        int base = o;
        while (x != base) {
            counter++;
            int ox = line(o, x);
            int yPar = parallel(ox, y);
            y = intersection(yPar, parallel(line(o, y), x));
            int y1 = intersection(parallel(line(o, y), x), yPar);
            o = x;
            x = plane.intersection(parallel(line(o, y), y1), ox);
        }
        return counter + 1;
    }

    public int parallelogram(int o, int x, int y) {
        return intersection(parallel(line(o, x), y), parallel(line(o, y), x));
    }

    public BitSet closure(BitSet base) {
        BitSet additional;
        while ((additional = additional(base)).cardinality() > base.cardinality()) {
            base = additional;
        }
        return base;
    }

    private BitSet additional(BitSet base) {
        BitSet result = (BitSet) base.clone();
        base.stream().forEach(a -> base.stream().filter(b -> b != a).forEach(b -> base.stream()
                .filter(c -> c != b && c != a && plane.line(a, c) != plane.line(b, c))
                .forEach(c -> result.set(parallelogram(a, b, c)))));
        result.or(base);
        return result;
    }

    public Map<Set<Pair>, Integer> getCharacteristics() {
        Map<Set<Pair>, Integer> charToPoint = new HashMap<>();
        for (int o : points()) {
            Set<Pair> pairs = new HashSet<>();
            for (int a : points()) {
                if (o == a) {
                    continue;
                }
                for (int b : points()) {
                    if (o == b || b == a || line(o, b) == line(o, a)) {
                        continue;
                    }
                    Pair p = new Pair(zigZagNumber(o, a, b), zigZagNumber(o, b, a));
                    pairs.add(p);
                }
            }
            charToPoint.put(pairs, o);
        }
        return charToPoint;
    }

    public List<Set<Pair>> vectors() {
        List<Set<Pair>> result = new ArrayList<>();
        for (int p1 : points()) {
            for (int p2 : points()) {
                if (p1 == p2) {
                    continue;
                }
                Pair p = new Pair(p1, p2);
                if (result.stream().anyMatch(s -> s.contains(p))) {
                    continue;
                }
                Set<Pair> vectors = new HashSet<>();
                vectors.add(new Pair(p1, p2));
                Set<Pair> currentLayer = new HashSet<>(vectors);
                while (!currentLayer.isEmpty()) {
                    Set<Pair> nextLayer = new HashSet<>();
                    for (Pair pair : currentLayer) {
                        int line = line(pair.f(), pair.s());
                        for (int beg : notLine(line)) {
                            int end = parallelogram(pair.f(), pair.s(), beg);
                            Pair newPair = new Pair(beg, end);
                            if (!vectors.contains(newPair)) {
                                nextLayer.add(newPair);
                            }
                        }
                    }
                    vectors.addAll(currentLayer);
                    currentLayer = nextLayer;
                }
                result.add(vectors);
            }
        }
        return result;
    }
}
