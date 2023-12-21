package ua.ihromant.mathutils;

import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupProduct;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Function;
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
        for (int l = 0; l < lines.length; l++) {
            BitSet line = lines[l];
            for (int p1 = line.nextSetBit(0); p1 >= 0; p1 = line.nextSetBit(p1 + 1)) {
                for (int p2 = line.nextSetBit(p1 + 1); p2 >= 0; p2 = line.nextSetBit(p2 + 1)) {
                    result[p1][p2] = l;
                    result[p2][p1] = l;
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
        for (int p = 0; p < pointCount; p++) {
            BitSet beam = points[p];
            for (int l1 = beam.nextSetBit(0); l1 >= 0; l1 = beam.nextSetBit(l1 + 1)) {
                for (int l2 = beam.nextSetBit(l1 + 1); l2 >= 0; l2 = beam.nextSetBit(l2 + 1)) {
                    result[l1][l2] = p;
                    result[l2][l1] = p;
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
        for (int x = first.nextSetBit(0); x >= 0; x = first.nextSetBit(x + 1)) {
            for (int y = second.nextSetBit(0); y >= 0; y = second.nextSetBit(y + 1)) {
                if (x == y) {
                    continue;
                }
                result.or(lines[line(x, y)]);
            }
        }
        BitSet removal = new BitSet();
        removal.or(first);
        removal.or(second);
        result.xor(removal);
        return result;
    }

    public HyperbolicPlane subPlane(int[] pointArray) {
        return new HyperbolicPlane(Arrays.stream(lines).map(l -> l.stream()
                        .map(p -> Arrays.binarySearch(pointArray, p)).filter(p -> p >= 0).collect(BitSet::new, BitSet::set, BitSet::or))
                .filter(bs -> bs.cardinality() > 1).toArray(BitSet[]::new));
    }

    public BitSet hyperbolicIndex() {
        int maximum = Arrays.stream(lines).mapToInt(BitSet::cardinality).max().orElseThrow() - 1;
        BitSet result = new BitSet();
        for (int o = 0; o < pointCount; o++) {
            for (int x = 0; x < pointCount; x++) {
                if (o == x) {
                    continue;
                }
                for (int y = 0; y < pointCount; y++) {
                    if (collinear(o, x, y)) {
                        continue;
                    }
                    BitSet xy = lines[line(x, y)];
                    for (int p = xy.nextSetBit(0); p >= 0; p = xy.nextSetBit(p + 1)) {
                        if (p == x || p == y) {
                            continue;
                        }
                        int ox = line(o, x);
                        BitSet oy = lines[line(o, y)];
                        int counter = 0;
                        for (int u = oy.nextSetBit(0); u >= 0; u = oy.nextSetBit(u + 1)) {
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
                BitSet beam = points[p];
                for (int par = beam.nextSetBit(0); par >= 0; par = beam.nextSetBit(par + 1)) {
                    if (intersection(par, l) == -1) {
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
        for (int x = 0; x < pointCount; x++) {
            for (int y = x + 1; y < pointCount; y++) {
                for (int z = y + 1; z < pointCount; z++) {
                    if (line(x, y) == line(y, z)) {
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

    public BitSet cardSubSpaces(boolean full) {
        BitSet result = new BitSet();
        for (int x = 0; x < pointCount; x++) {
            for (int y = x + 1; y < pointCount; y++) {
                for (int z = y + 1; z < pointCount; z++) {
                    if (line(x, y) == line(y, z)) {
                        continue;
                    }
                    BitSet hull = hull(x, y, z);
                    for (int w = z + 1; w < pointCount; w++) {
                        if (hull.get(w)) {
                            continue;
                        }
                        int sCard = hull(x, y, z, w).cardinality();
                        result.set(sCard);
                        if (!full && sCard == pointCount) {
                            return result;
                        }
                    }
                }
            }
        }
        return result;
    }

    public HyperbolicPlane directProduct(HyperbolicPlane that) {
        GroupProduct cg = new GroupProduct(this.pointCount(), that.pointCount());
        BitSet[] lines = Stream.of(IntStream.range(0, this.lineCount()).boxed().flatMap(l1 -> IntStream.range(0, that.pointCount()).mapToObj(p2 -> {
                    BitSet result = new BitSet();
                    for (int p1 : this.points(l1)) {
                        result.set(cg.fromArr(p1, p2));
                    }
                    return result;
                })),
                IntStream.range(0, that.lineCount()).boxed().flatMap(l2 -> IntStream.range(0, this.pointCount()).mapToObj(p1 -> {
                    BitSet result = new BitSet();
                    for (int p2 : that.points(l2)) {
                        result.set(cg.fromArr(p1, p2));
                    }
                    return result;
                })),
                IntStream.range(0, this.lineCount()).boxed().flatMap(l1 -> IntStream.range(0, that.lineCount()).boxed().flatMap(l2 -> {
                    int[] arr1 = this.line(l1).stream().toArray();
                    int[] arr2 = that.line(l2).stream().toArray();
                    return GaloisField.permutations(new int[]{0, 1, 2})//.filter(perm -> parity(perm) % 2 == 0)
                            .map(perm -> {
                        BitSet result = new BitSet();
                        for (int i = 0; i < 3; i++) {
                            result.set(cg.fromArr(arr1[i], arr2[perm[i]]));
                        }
                        return result;
                    });
                }))).flatMap(Function.identity()).toArray(BitSet[]::new);
        return new HyperbolicPlane(lines);
    }

    private static int parity(int[] perm) {
        int result = 0;
        for (int i = 0; i < perm.length; i++) {
            for (int j = i + 1; j < perm.length; j++) {
                if (perm[i] > perm[j]) {
                    result++;
                }
            }
        }
        return result;
    }
}
