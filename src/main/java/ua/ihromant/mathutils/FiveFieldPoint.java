package ua.ihromant.mathutils;

import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FiveFieldPoint {
    private static final int SIZE = 5;
    private static final int[] FIELD = IntStream.range(0, SIZE).toArray();
    private static final int COUNT = FIELD.length * FIELD.length + FIELD.length + 1;

    private static final int[][] RAYS = Stream.concat(
            Stream.of(IntStream.of(FIELD).skip(1)
                    .map(y -> fromCrd(0, y)).toArray()),
            IntStream.of(FIELD).mapToObj(y -> IntStream.of(FIELD).skip(1)
                    .map(cf -> mulPoint(fromCrd(1, y), cf)).toArray())).toArray(int[][]::new);

    private static final BitSet[] LINES = generateLines();

    private static final int[][] LOOKUP = generateLookup();

    private static final BitSet[] POINTS = generateBeams();
    private static final int[][] INTERSECTIONS = generateIntersections();

    private static BitSet[] generateLines() {
        BitSet[] lines = new BitSet[COUNT];
        for (int i = 0; i < FIELD.length; i++) {
            int start = fromCrd(0, FIELD[i]);
            for (int j = 0; j < FIELD.length; j++) {
                int lineIdx = i * FIELD.length + j;
                BitSet line = new BitSet();
                line.set(start);
                IntStream.of(RAYS[j + 1]).forEach(p -> line.set(addPoints(p, start)));
                line.set(FIELD.length * FIELD.length + j);
                lines[lineIdx] = line;
            }
        }
        for (int i = 0; i < FIELD.length; i++) {
            int lineIdx = FIELD.length * FIELD.length + i;
            BitSet line = new BitSet();
            int start = fromCrd(FIELD[i], 0);
            line.set(start);
            IntStream.of(RAYS[0]).forEach(p -> line.set(addPoints(p, start)));
            line.set(FIELD.length * FIELD.length + FIELD.length); // 90
            lines[lineIdx] = line;
        }
        BitSet infinity = new BitSet();
        for (int i = 0; i <= FIELD.length; i++) {
            infinity.set(FIELD.length * FIELD.length + i);
        }
        lines[FIELD.length * FIELD.length + FIELD.length] = infinity;
        return lines;
    }

    private static int[][] generateIntersections() {
        int[][] result = new int[COUNT][COUNT];
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

    private static int[][] generateLookup() {
        int[][] result = new int[COUNT][COUNT];
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

    private static BitSet[] generateBeams() {
        BitSet[] result = new BitSet[COUNT];
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

    public static BitSet line(int line) {
        return LINES[line];
    }

    public static int line(int p1, int p2) {
        return LOOKUP[p1][p2];
    }

    public static Iterable<Integer> lines() {
        return () -> IntStream.range(0, COUNT).boxed().iterator();
    }

    public static Iterable<Integer> lines(BitSet filter) {
        return () -> IntStream.range(0, COUNT).filter(l -> !filter.get(l)).boxed().iterator();
    }

    public static Iterable<Integer> lines(int point) {
        return () -> POINTS[point].stream().boxed().iterator();
    }

    public static BitSet point(int point) {
        return POINTS[point];
    }

    public static int intersection(int l1, int l2) {
        return INTERSECTIONS[l1][l2];
    }

    public static Iterable<Integer> points() {
        return () -> IntStream.range(0, COUNT).boxed().iterator();
    }

    public static Iterable<Integer> points(BitSet filter) {
        return () -> IntStream.range(0, COUNT).filter(p -> !filter.get(p)).boxed().iterator();
    }

    public static Iterable<Integer> points(int line) {
        return () -> LINES[line].stream().boxed().iterator();
    }

    private static int mulPoint(int point, int cff) {
        int x = mulField(x(point), cff);
        int y = mulField(y(point), cff);
        return fromCrd(x, y);
    }

    private static int addPoints(int p1, int p2) {
        int x1 = x(p1);
        int y1 = y(p1);
        int x2 = x(p2);
        int y2 = y(p2);
        return fromCrd(addField(x1, x2), addField(y1, y2));
    }

    public static int fieldCardinality() {
        return FIELD.length;
    }

    public static int planeCardinality() {
        return COUNT;
    }

    public static int fromCrd(int x, int y) {
        return x * FIELD.length + y;
    }

    private static int x(int point) {
        return point / FIELD.length;
    }

    private static int y(int point) {
        return point % FIELD.length;
    }

    private static int mulField(int a, int b) {
        return (a * b) % FIELD.length;
    }

    private static int addField(int a, int b) {
        return (a + b) % FIELD.length;
    }

    private static String simpleToString(int point) {
        return "(" + x(point) + "," + y(point) + ")";
    }

    public static String pointToString(int point) {
        if (point < FIELD.length * FIELD.length) {
            return simpleToString(point);
        }
        if (point < FIELD.length * FIELD.length + FIELD.length) {
            return "∞" + simpleToString(fromCrd(1, FIELD[point - FIELD.length * FIELD.length]));
        }
        return "∞" + simpleToString(fromCrd(0, 1));
    }

    public static String lineToString(int i) {
        return line(i).stream().mapToObj(FiveFieldPoint::pointToString).collect(Collectors.joining(",", "[", "]"));
    }

    public static String lineToString(int i, BitSet filter) {
        return line(i).stream().filter(p -> !filter.get(p)).mapToObj(FiveFieldPoint::pointToString).collect(Collectors.joining(",", "[", "]"));
    }
}

