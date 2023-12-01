package ua.ihromant.mathutils;

import ua.ihromant.mathutils.group.Group;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class HyperbolicPlane {
    private final int pointCount;
    private final BitSet[] lines;
    private final int[][] lookup;
    private final BitSet[] points;
    private final int[][] intersections;

    public HyperbolicPlane(BitSet[] lines) {
        this.pointCount = Arrays.stream(lines).collect(Collector.of(BitSet::new, BitSet::or, (b1, b2) -> {b1.or(b2); return b1;})).cardinality();
        this.lines = lines;
        this.lookup = generateLookup();
        this.points = generateBeams();
        this.intersections = generateIntersections();
    }

    public HyperbolicPlane(int v, int[] base) {
        this.pointCount = v;
        int k = base.length;
        int t = (v - 1) / k / (k - 1);
        int m = (v - 1) / 2 / t;
        GaloisField pf = new GaloisField(v);
        int prim = pf.primitives().findAny().orElseThrow();
        this.lines = IntStream.range(0, t).boxed().flatMap(idx -> {
            int[] block = Arrays.stream(base).map(j -> pf.mul(j, pf.power(prim, idx * m))).toArray();
            return pf.elements().mapToObj(i -> {
                BitSet res = new BitSet();
                for (int shift : block) {
                    res.set(pf.add(i, shift));
                }
                return res;
            });
        }).toArray(BitSet[]::new);
        this.lookup = generateLookup();
        this.points = generateBeams();
        this.intersections = generateIntersections();
    }

    public HyperbolicPlane(int[]... base) {
        this.pointCount = Arrays.stream(base).mapToInt(arr -> arr.length * (arr.length - 1)).sum() + 1;
        this.lines = Stream.of(base).flatMap(arr -> IntStream.range(0, pointCount).mapToObj(idx -> {
            BitSet res = new BitSet();
            for (int shift : arr) {
                res.set((idx + shift) % pointCount);
            }
            return res;
        })).toArray(BitSet[]::new);
        this.lookup = generateLookup();
        this.points = generateBeams();
        this.intersections = generateIntersections();
    }

    public HyperbolicPlane(int pointCount, int[]... base) {
        this.pointCount = pointCount;
        int k = base[0].length; // assuming that difference set is correct
        this.lines = Stream.concat(Arrays.stream(base, 0, pointCount % k == 0 ? base.length - 1 : base.length)
                .flatMap(arr -> IntStream.range(0, pointCount).mapToObj(idx -> {
                    BitSet res = new BitSet();
                    for (int shift : arr) {
                        res.set((idx + shift) % pointCount);
                    }
                    return res;
                })), pointCount % k == 0 ? IntStream.range(0, pointCount / k).mapToObj(idx -> {
            BitSet res = new BitSet();
            for (int shift : base[base.length - 1]) {
                res.set((idx + shift) % pointCount);
            }
            return res;
        }) : Stream.of()).toArray(BitSet[]::new);
        this.lookup = generateLookup();
        this.points = generateBeams();
        this.intersections = generateIntersections();
    }

    public HyperbolicPlane(Group g, int[]... base) {
        this.pointCount = g.order();
        int k = base[0].length; // assuming that difference set is correct
        this.lines = Stream.concat(Arrays.stream(base, 0, pointCount % k == 0 ? base.length - 1 : base.length)
                .flatMap(arr -> g.elements().mapToObj(el -> {
                    BitSet res = new BitSet();
                    for (int shift : arr) {
                        res.set(g.op(el, shift));
                    }
                    return res;
                })), pointCount % k == 0 ? g.elements().mapToObj(idx -> {
            BitSet res = new BitSet();
            for (int shift : base[base.length - 1]) {
                res.set(g.op(idx, shift));
            }
            return res;
        }).distinct() : Stream.of()).toArray(BitSet[]::new);
        this.lookup = generateLookup();
        this.points = generateBeams();
        this.intersections = generateIntersections();
    }

    public HyperbolicPlane(String... design) {
        this.pointCount = Arrays.stream(design).flatMap(s -> s.chars().boxed()).collect(Collectors.toSet()).size();
        this.lines = IntStream.range(0, design[0].length()).mapToObj(idx -> {
            BitSet res = new BitSet();
            for (String s : design) {
                res.set(Character.digit(s.charAt(idx), 36));
            }
            return res;
        }).toArray((BitSet[]::new));
        this.lookup = generateLookup();
        this.points = generateBeams();
        this.intersections = generateIntersections();
    }

    private int[][] generateLookup() {
        int[][] result = new int[pointCount][pointCount];
        Arrays.stream(result).forEach(l -> Arrays.fill(l, -1));
        for (int line : lines()) {
            for (int p1 : points(line)) {
                int[] map = result[p1];
                for (int p2 : points(line)) {
                    if (p1 == p2) {
                        map[p2] = -1;
                    } else {
                        map[p2] = line;
                    }
                }
            }
        }
        return result;
    }

    private BitSet[] generateBeams() {
        BitSet[] result = new BitSet[pointCount];
        for (int p1 : points()) {
            BitSet beam = new BitSet();
            result[p1] = beam;
            for (int p2 : points()) {
                if (p1 == p2) {
                    continue;
                }
                beam.set(line(p1, p2));
            }
        }
        return result;
    }

    private int[][] generateIntersections() {
        int[][] result = new int[lines.length][lines.length];
        Arrays.stream(result).forEach(arr -> Arrays.fill(arr, -1));
        for (int point : points()) {
            for (int l1 : lines(point)) {
                int[] map = result[l1];
                for (int l2 : lines(point)) {
                    if (l1 == l2) {
                        map[l2] = -1;
                    } else {
                        map[l2] = point;
                    }
                }
            }
        }
        return result;
    }

    public int pointCount() {
        return pointCount;
    }

    public int lineCount() {
        return lines.length;
    }

    public BitSet line(int line) {
        return lines[line];
    }

    public int line(int p1, int p2) {
        return lookup[p1][p2];
    }

    public Iterable<Integer> lines() {
        return () -> IntStream.range(0, lines.length).boxed().iterator();
    }

    public Iterable<Integer> lines(int point) {
        return () -> points[point].stream().boxed().iterator();
    }

    public BitSet point(int point) {
        return points[point];
    }

    public int intersection(int l1, int l2) {
        return intersections[l1][l2];
    }

    public Iterable<Integer> points() {
        return () -> IntStream.range(0, pointCount).boxed().iterator();
    }

    public Iterable<Integer> points(int line) {
        return () -> lines[line].stream().boxed().iterator();
    }

    public boolean collinear(int... points) {
        if (points.length == 0) {
            return true;
        }
        int first = points[0];
        for (int i = 1; i < points.length; i++) {
            int second = points[i];
            if (first != second) {
                BitSet line = lines[line(first, second)];
                return Arrays.stream(points).allMatch(line::get);
            }
        }
        return true;
    }

    public String lineToString(int line) {
        return lines[line].toString();
    }

    public BitSet hull(int... points) {
        BitSet base = new BitSet();
        for (int point : points) {
            base.set(point);
        }
        BitSet additional = base;
        while (!(additional = additional(base, additional)).isEmpty()) {
            base.or(additional);
        }
        return base;
    }

    public BitSet additional(BitSet first, BitSet second) {
        BitSet result = new BitSet();
        first.stream().forEach(x -> second.stream().filter(y -> x != y).forEach(y -> result.or(lines[line(x, y)])));
        BitSet removal = new BitSet();
        removal.or(first);
        removal.or(second);
        result.xor(removal);
        return result;
    }

    public Set<BitSet> differences() {
        return Arrays.stream(lines).map(line -> {
            BitSet result = new BitSet();
            line.stream().forEach(a -> line.stream().filter(b -> a != b).forEach(b -> {
                int diff = Math.abs(a - b);
                result.set(Math.min(diff, pointCount - diff));
            }));
            return result;
//        }).map(diff -> {
//            BitSet result = new BitSet();
//            result.set(0);
//            while (!diff.isEmpty()) {
//                result.set(diff.stream().iterator().next() + result.length() - 1);
//                result.stream().flatMap(a -> result.stream()
//                        .filter(b -> a < b).map(b -> b - a)).forEach(idx -> {
//                            diff.set(idx, false);
//                            diff.set(pointCount - idx, false);
//                });
//            }
//            return result;
        }).collect(Collectors.toSet());
    }

    public HyperbolicPlane subPlane(int[] pointArray) {
        return new HyperbolicPlane(Arrays.stream(lines).map(l -> l.stream()
                        .map(p -> Arrays.binarySearch(pointArray, p)).filter(p -> p >= 0).collect(BitSet::new, BitSet::set, BitSet::or))
                .filter(bs -> bs.cardinality() > 1).toArray(BitSet[]::new));
    }

    public BitSet hyperbolicIndex() {
        int maximum = lines[0].cardinality() - 1; // uncomment below when testing PBD
        // int maximum = Arrays.stream(lines).mapToInt(BitSet::cardinality).max().orElseThrow() - 1;
        BitSet result = new BitSet();
        for (int o : points()) {
            for (int x : points()) {
                if (o == x) {
                    continue;
                }
                for (int y : points()) {
                    if (collinear(o, x, y)) {
                        continue;
                    }
                    int xy = line(x, y);
                    for (int p : points(xy)) {
                        if (p == x || p == y) {
                            continue;
                        }
                        int ox = line(o, x);
                        int oy = line(o, y);
                        int counter = 0;
                        for (int u : points(oy)) {
                            if (u == o || u == y) {
                                continue;
                            }
                            if (intersection(line(p, u), ox) == -1) {
                                counter++;
                            }
                        }
                        result.set(counter);
                    }
                    if (result.cardinality() == maximum) {
                        return result;
                    }
                }
            }
        }
        return result;
    }

    public BitSet playfairIndex() {
        BitSet result = new BitSet();
        for (int l : lines()) {
            BitSet line = line(l);
            for (int p : points()) {
                if (line.get(p)) {
                    continue;
                }
                int counter = 0;
                for (int parallel : lines(p)) {
                    if (intersection(parallel, l) == -1) {
                        counter++;
                    }
                }
                result.set(counter);
            }
        }
        return result;
    }

    public BitSet cardSubPlanes(boolean full) {
        BitSet result = new BitSet();
        for (int x : points()) {
            for (int y : points()) {
                if (x >= y) {
                    continue;
                }
                for (int z : points()) {
                    if (y >= z || line(x, y) == line(y, z)) {
                        continue;
                    }
                    int card = hull(x, y, z).cardinality();
                    result.set(card);
                    if (!full && card == pointCount) {
                        return result; // it's either plane or has no exchange property
                    }
                }
            }
        }
        return result;
    }
}
