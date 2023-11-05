package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    public HyperbolicPlane(HyperbolicPlane... planes) {
        int sm = planes[0].pointCount;
        int k = planes.length + 1;
        int v = sm * planes.length + k;
        this.pointCount = v;
        this.lines = new BitSet[v * (v - 1) / k / (k - 1)];
        this.lookup = new int[pointCount][pointCount];
        Arrays.stream(lookup).forEach(arr -> Arrays.fill(arr, -1));
        IntStream.rangeClosed(0, sm).forEach(i -> {
            BitSet line = IntStream.range(0, planes.length).map(j -> i * planes.length + j + 1).collect(BitSet::new, BitSet::set, BitSet::or);
            line.set(0);
            lines[i] = line;
            connect(line, i);
        });
        for (int p = 0; p < planes.length; p++) {
            int first = p + 1;
            HyperbolicPlane plane = planes[p];
            int lineIdx = plane.lineCount() * first + 1;
            for (int l : plane.lines()) {
                BitSet line = plane.line(l);
                int[] pts = line.stream().toArray();
                BitSet newLine = new BitSet();
                newLine.set(first);
                for (int idx : pts) {
                    for (int poss = 0; poss < pts.length; poss++) {
                        int candidate = (idx + 1) * planes.length + poss + 1;
                        if (newLine.stream().allMatch(curr -> line(curr, candidate) == -1)) {
                            newLine.set(candidate);
                            break;
                        }
                    }
                }
                if (newLine.cardinality() != k) {
                    throw new IllegalStateException();
                }
                connect(newLine, lineIdx);
                lines[lineIdx++] = newLine;
            }
        }
        for (int i = 0; i < v; i++) {
            for (int j = i + 1; j < v; j++) {
                if (line(i, j) != -1) {
                    continue;
                }
//                // connect i and j
//                BitSet line = new BitSet();
//                line.set(i);
//                line.set(j);
//                linesList.add(line);
//                fillLookup(pointToLine, i, j, lineIdx);
//                for (int k = j + 1; ; k++) {
//                    if (pointToLine.containsKey(pack(i, k)) || pointToLine.containsKey(pack(j, k))) {
//                        continue;
//                    }
//                    if (k >= v) {
//                        throw new IllegalStateException(String.valueOf(i));
//                    }
//                    if (findTriangle(pointToLine, linesList, i, i, j, k)) {
//                        continue;
//                    }
//                    // connect k to i and j
//                    line.set(k);
//                    fillLookup(pointToLine, i, k, lineIdx);
//                    fillLookup(pointToLine, j, k, lineIdx);
//                    for (int l = k + 1; ; l++) {
//                        if (pointToLine.containsKey(pack(i, l)) || pointToLine.containsKey(pack(j, l)) || pointToLine.containsKey(pack(k, l))) {
//                            continue;
//                        }
//                        if (l >= v) {
//                            throw new IllegalStateException(String.valueOf(i));
//                        }
//                        if (findTriangle(pointToLine, linesList, i, i, j, l) || findTriangle(pointToLine, linesList, i, i, k, l) || findTriangle(pointToLine, linesList, i, j, k, l)) {
//                            continue;
//                        }
//                        // connect l to i, j and k
//                        line.set(l);
//                        fillLookup(pointToLine, i, l, lineIdx);
//                        fillLookup(pointToLine, j, l, lineIdx);
//                        fillLookup(pointToLine, k, l, lineIdx);
//                        break;
//                    }
//                    break;
//                }
//                lineIdx++;
            }
        }
        Map<Integer, Integer> frequencies = Arrays.stream(lines).filter(Objects::nonNull).flatMap(l -> l.stream().boxed())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.mapping(Function.identity(), Collectors.summingInt(i -> 1))));
        this.points = generateBeams();
        this.intersections = generateIntersections();
    }

    private void connect(BitSet line, int idx) {
        line.stream().forEach(p1 -> line.stream().forEach(p2 -> connect(p1, p2, p1 != p2 ? idx : -1)));
    }

    private void connect(int p1, int p2, int line) {
        lookup[p1][p2] = line;
        lookup[p2][p1] = line;
    }

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
        this.lines = Stream.of(base).flatMap(arr -> IntStream.range(0, pointCount).mapToObj(idx -> {
            BitSet res = new BitSet();
            for (int shift : arr) {
                res.set((idx + shift) % pointCount);
            }
            return res;
        })).collect(Collectors.toSet()).toArray(BitSet[]::new);
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
}
