package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.BitSet;
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
        BitSet next = next(base);
        while (next.cardinality() > base.cardinality()) {
            base = next;
            next = next(base);
        }
        return base;
    }

    public BitSet next(BitSet base) {
        BitSet next = (BitSet) base.clone();
        base.stream().forEach(x -> base.stream().filter(y -> x != y).forEach(y -> next.or(lines[line(x, y)])));
        return next;
    }
}