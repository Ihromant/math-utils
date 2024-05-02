package ua.ihromant.mathutils;

import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupProduct;
import ua.ihromant.mathutils.group.PermutationGroup;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Liner {
    private final int pointCount;
    private final int[][] lines;
    private final boolean[][] flags;
    private final int[][] lookup;
    private final int[][] beams;
    private final int[][] intersections;

    public Liner(int pointCount, int[][] lines) {
        this.pointCount = pointCount;
        this.lines = lines;
        this.flags = generateFlags();
        this.lookup = generateLookup();
        this.beams = generateBeams();
        this.intersections = generateIntersections();
    }

    public Liner(int pointCount, BitSet[] lines) {
        this.pointCount = pointCount;
        this.lines = Arrays.stream(lines).map(bs -> bs.stream().toArray()).toArray(int[][]::new);
        this.flags = generateFlags();
        this.lookup = generateLookup();
        this.beams = generateBeams();
        this.intersections = generateIntersections();
    }

    public Liner(BitSet[] lines) {
        this(Arrays.stream(lines).collect(Collector.of(BitSet::new, BitSet::or, (b1, b2) -> {b1.or(b2); return b1;})).cardinality(), lines);
    }

    public static Liner byGaloisPower(int pointCount, int[] base) {
        int k = base.length;
        int t = (pointCount - 1) / k / (k - 1);
        int m = (pointCount - 1) / 2 / t;
        GaloisField pf = new GaloisField(pointCount);
        int prim = pf.primitives().findAny().orElseThrow();
        BitSet[] lines = IntStream.range(0, t).boxed().flatMap(idx -> {
            int[] block = Arrays.stream(base).map(j -> pf.mul(j, pf.power(prim, idx * m))).toArray();
            return pf.elements().mapToObj(i -> {
                BitSet res = new BitSet();
                for (int shift : block) {
                    res.set(pf.add(i, shift));
                }
                return res;
            });
        }).toArray(BitSet[]::new);
        return new Liner(pointCount, lines);
    }

    public static Liner byDiffFamily(int[]... base) {
        int pointCount = Arrays.stream(base).mapToInt(arr -> arr.length * (arr.length - 1)).sum() + 1;
        BitSet[] lines = Stream.of(base).flatMap(arr -> IntStream.range(0, pointCount).mapToObj(idx -> {
            BitSet res = new BitSet();
            for (int shift : arr) {
                res.set((idx + shift) % pointCount);
            }
            return res;
        })).toArray(BitSet[]::new);
        return new Liner(pointCount, lines);
    }

    public static Liner byDiffFamily(int pointCount, int[]... base) {
        int k = base[0].length; // assuming that difference set is correct
        BitSet[] lines = Stream.concat(Arrays.stream(base, 0, pointCount % k == 0 ? base.length - 1 : base.length)
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
        return new Liner(pointCount, lines);
    }

    public static Liner byDiffFamily(Group g, int[]... base) {
        int pointCount = g.order();
        int k = base[0].length; // assuming that difference set is correct
        BitSet[] lines = Stream.concat(Arrays.stream(base, 0, pointCount % k == 0 ? base.length - 1 : base.length)
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
        return new Liner(pointCount, lines);
    }

    public static Liner byStrings(String... design) {
        int pointCount = Arrays.stream(design).flatMap(s -> s.chars().boxed()).collect(Collectors.toSet()).size();
        BitSet[] lines = IntStream.range(0, design[0].length()).mapToObj(idx -> {
            BitSet res = new BitSet();
            for (String s : design) {
                res.set(Character.digit(s.charAt(idx), 36));
            }
            return res;
        }).toArray((BitSet[]::new));
        return new Liner(pointCount, lines);
    }

    private boolean[][] generateFlags() {
        boolean[][] flags = new boolean[lines.length][pointCount];
        for (int i = 0; i < lines.length; i++) {
            for (int pt : lines[i]) {
                flags[i][pt] = true;
            }
        }
        return flags;
    }

    private int[][] generateLookup() {
        int[][] result = new int[pointCount][pointCount];
        Arrays.stream(result).forEach(l -> Arrays.fill(l, -1));
        for (int l = 0; l < lines.length; l++) {
            int[] line = lines[l];
            for (int i = 0; i < line.length; i++) {
                int p1 = line[i];
                for (int j = i + 1; j < line.length; j++) {
                    int p2 = line[j];
                    result[p1][p2] = l;
                    result[p2][p1] = l;
                }
            }
        }
        return result;
    }

    private int[][] generateBeams() {
        int[][] result = new int[pointCount][];
        for (int p1 = 0; p1 < pointCount; p1++) {
            BitSet beam = new BitSet();
            for (int p2 = 0; p2 < pointCount; p2++) {
                if (p1 == p2) {
                    continue;
                }
                int line = line(p1, p2);
                if (line >= 0) {
                    beam.set(line);
                }
            }
            result[p1] = beam.stream().toArray();
        }
        return result;
    }

    private int[][] generateIntersections() {
        int[][] result = new int[lines.length][lines.length];
        Arrays.stream(result).forEach(arr -> Arrays.fill(arr, -1));
        for (int p = 0; p < pointCount; p++) {
            int[] beam = beams[p];
            for (int i = 0; i < beam.length; i++) {
                int l1 = beam[i];
                for (int j = i + 1; j < beam.length; j++) {
                    int l2 = beam[j];
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

    public boolean flag(int line, int point) {
        return flags[line][point];
    }

    public int[] line(int line) {
        return lines[line];
    }

    public int line(int p1, int p2) {
        return lookup[p1][p2];
    }

    public int[] lines(int point) {
        return beams[point];
    }

    public int[] point(int point) {
        return beams[point];
    }

    public int intersection(int l1, int l2) {
        return intersections[l1][l2];
    }

    public int[] points(int line) {
        return lines[line];
    }

    public boolean collinear(int... points) {
        if (points.length == 0) {
            return true;
        }
        int first = points[0];
        for (int i = 1; i < points.length; i++) {
            int second = points[i];
            if (first != second) {
                boolean[] fgs = flags[line(first, second)];
                return Arrays.stream(points, i + 1, points.length).allMatch(p -> fgs[p]);
            }
        }
        return true;
    }

    public String lineToString(int line) {
        return Arrays.toString(lines[line]);
    }

    public BitSet hull(int... points) {
        BitSet base = new BitSet(pointCount);
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
                Arrays.stream(lines[line(x, y)]).forEach(result::set);
            }
        }
        BitSet removal = new BitSet();
        removal.or(first);
        removal.or(second);
        result.xor(removal);
        return result;
    }

    public Liner subPlane(int[] pointArray) {
        return new Liner(pointArray.length, Arrays.stream(lines).map(l -> Arrays.stream(l)
                        .map(p -> Arrays.binarySearch(pointArray, p)).filter(p -> p >= 0).toArray())
                .filter(bs -> bs.length > 1).toArray(int[][]::new));
    }

    public BitSet hyperbolicIndex() {
        int maximum = Arrays.stream(lines).mapToInt(arr -> arr.length).max().orElseThrow() - 1;
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
                    for (int p : lines[line(x, y)]) {
                        if (p == x || p == y) {
                            continue;
                        }
                        int ox = line(o, x);
                        int counter = 0;
                        for (int u : lines[line(o, y)]) {
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

    public Map<Integer, Integer> hyperbolicFreq() {
        Map<Integer, Integer> result = new HashMap<>();
        for (int o = 0; o < pointCount; o++) {
            for (int x = 0; x < pointCount; x++) {
                if (o == x) {
                    continue;
                }
                for (int y = 0; y < pointCount; y++) {
                    if (collinear(o, x, y)) {
                        continue;
                    }
                    for (int p : lines[line(x, y)]) {
                        if (p == x || p == y) {
                            continue;
                        }
                        int ox = line(o, x);
                        int counter = 0;
                        for (int u : lines[line(o, y)]) {
                            if (u == o || u == y) {
                                continue;
                            }
                            if (intersection(line(p, u), ox) == -1) {
                                counter++;
                            }
                        }
                        result.compute(counter, (k, v) -> v == null ? 1 : v + 1);
                    }
                }
            }
        }
        return result;
    }

    public BitSet playfairIndex() {
        BitSet result = new BitSet();
        for (int l = 0; l < lines.length; l++) {
            for (int p = 0; p < pointCount; p++) {
                if (flag(l, p)) {
                    continue;
                }
                int counter = 0;
                for (int par : beams[p]) {
                    if (intersection(par, l) == -1) {
                        counter++;
                    }
                }
                result.set(counter);
            }
        }
        return result;
    }

    public Map<Integer, Integer> cardSubPlanesFreq() {
        Map<Integer, Integer> result = new TreeMap<>();
        for (int x = 0; x < pointCount; x++) {
            for (int y = x + 1; y < pointCount; y++) {
                for (int z = y + 1; z < pointCount; z++) {
                    if (line(x, y) == line(y, z)) {
                        continue;
                    }
                    int card = hull(x, y, z).cardinality();
                    result.compute(card, (k, v) -> v == null ? 1 : v + 1);
                }
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

    public Liner directProduct(Liner that) {
        int length = this.lines[0].length;
        if (Arrays.stream(this.lines).anyMatch(l -> l.length != length)) {
            throw new IllegalStateException("Not all lines of length " + length);
        }
        if (Arrays.stream(that.lines).anyMatch(l -> l.length != length)) {
            throw new IllegalArgumentException("Not all lines of length " + length);
        }
        Liner aff = new Liner(new GaloisField(length).generatePlane()).subPlane(IntStream.range(0, length * length).toArray());
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
                    int[] arr1 = this.line(l1);
                    int[] arr2 = that.line(l2);
                    return IntStream.range(0, aff.lineCount()).mapToObj(aff::line).filter(l -> {
                        int fst = l[0];
                        return Arrays.stream(l, 1, l.length).skip(1).allMatch(p -> p / length != fst / length && p % length != fst % length);
                    }).map(l -> {
                        BitSet result = new BitSet();
                        for (int i = 0; i < length; i++) {
                            result.set(cg.fromArr(arr1[l[i] / length], arr2[l[i] % length]));
                        }
                        return result;
                    });
                }))).flatMap(Function.identity()).toArray(BitSet[]::new);
        return new Liner(lines);
    }

    public boolean isRegular() {
        for (int i = 0; i < lines.length; i++) {
            for (int j = i + 1; j < lines.length; j++) {
                if (intersection(i, j) < 0) {
                    continue;
                }
                BitSet pts = new BitSet();
                for (int p1 : points(i)) {
                    for (int p2 : points(j)) {
                        if (p1 == p2) {
                            continue;
                        }
                        Arrays.stream(lines[line(p1, p2)]).forEach(pts::set);
                    }
                }
                if (pts.cardinality() != pointCount) {
                    System.out.println(pts.cardinality() + " " + pointCount);
                    return false;
                }
            }
        }
        return true;
    }

    public BitSet[] pointResidue(int p) {
        Set<BitSet> sets = new HashSet<>();
        BitSet points = new BitSet();
        for (int i = 0; i < lineCount(); i++) {
            if (flag(i, p)) {
                points.set(i);
            }
        }
        for (int i = 0; i < pointCount; i++) {
            if (i == p) {
                continue;
            }
            for (int j = i + 1; j < pointCount; j++) {
                if (j == p || line(i, p) == line(j, p)) {
                    continue;
                }
                BitSet hull = hull(p, i, j);
                BitSet line = new BitSet();
                for (int pt = points.nextSetBit(0); pt >= 0; pt = points.nextSetBit(pt + 1)) {
                    int[] l = lines[pt];
                    if (Arrays.stream(l).allMatch(hull::get)) {
                        line.set(pt);
                    }
                }
                sets.add(line);
            }
        }
        int[] pointArray = points.stream().toArray();
        return sets.stream().map(l -> l.stream()
                        .map(pt -> Arrays.binarySearch(pointArray, pt)).filter(pt -> pt >= 0).collect(BitSet::new, BitSet::set, BitSet::or))
                .filter(bs -> bs.cardinality() > 1).toArray(BitSet[]::new);
    }

    public PermutationGroup automorphisms() {
        return new PermutationGroup(Automorphisms.autArray(this));
    }
}
