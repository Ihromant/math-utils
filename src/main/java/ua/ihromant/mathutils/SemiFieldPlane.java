package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SemiFieldPlane {
    private static final int FIELD_SIZE = 27;
    private static final int COUNT = FIELD_SIZE * FIELD_SIZE;
    private static final int[][] RAYS = Stream.concat(
            Stream.of(ray(of(SemiField.ZERO, SemiField.ONE)).toArray()),
            IntStream.range(0, SemiField.SIZE).mapToObj(nf -> ray(of(SemiField.ONE, nf)).toArray())).toArray(int[][]::new);

    private static final BitSet[] LINES = generateLines();

    private static final int[][] LOOKUP = generateLookup();

    private static final BitSet[] POINTS = generateBeams();
    private static final int[][] INTERSECTIONS = generateIntersections();

    private static BitSet[] generateLines() {
        BitSet[] lines = new BitSet[FIELD_SIZE * FIELD_SIZE + FIELD_SIZE];
        for (int i = 0; i < FIELD_SIZE; i++) {
            int start = of(SemiField.ZERO, i);
            for (int j = 0; j < FIELD_SIZE; j++) {
                int lineIdx = i * FIELD_SIZE + j;
                BitSet line = new BitSet();
                line.set(start);
                IntStream.of(RAYS[j + 1]).forEach(p -> line.set(addPoints(p, start)));
                lines[lineIdx] = line;
            }
        }
        for (int i = 0; i < FIELD_SIZE; i++) {
            int lineIdx = FIELD_SIZE * FIELD_SIZE + i;
            BitSet line = new BitSet();
            int start = of(i, SemiField.ZERO);
            line.set(start);
            IntStream.of(RAYS[0]).forEach(p -> line.set(addPoints(p, start)));
            lines[lineIdx] = line;
        }
        return lines;
    }

    private static int[][] generateIntersections() {
        int[][] result = new int[LINES.length][LINES.length];
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
        return () -> IntStream.range(0, LINES.length).boxed().iterator();
    }

    public static Iterable<Integer> lines(BitSet filter) {
        return () -> IntStream.range(0, LINES.length).filter(l -> !filter.get(l)).boxed().iterator();
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

    public static boolean parallel(int l1, int l2) {
        return l1 == l2 || intersection(l1, l2) == -1;
    }

    public static boolean collinear(int... points) {
        if (points.length == 0) {
            return true;
        }
        int first = points[0];
        for (int i = 1; i < points.length; i++) {
            int second = points[i];
            if (first != second) {
                BitSet line = LINES[line(first, second)];
                return Arrays.stream(points).allMatch(line::get);
            }
        }
        return true;
    }

    private static IntStream ray(int point) {
        return IntStream.range(0, SemiField.SIZE).filter(sc -> sc != SemiField.ZERO).map(sc -> mul(point, sc));
    }

    public static int mul(int point, int scalar) {
        return of(SemiField.mul(scalar, x(point)), SemiField.mul(scalar, y(point)));
    }

    public static int pointCount() {
        return COUNT;
    }

    public static int lineCount() {
        return LINES.length;
    }

    private static int addPoints(int p1, int p2) {
        int x1 = x(p1);
        int y1 = y(p1);
        int x2 = x(p2);
        int y2 = y(p2);
        return of(SemiField.add(x1, x2), SemiField.add(y1, y2));
    }

    private static int x(int point) {
        return point / FIELD_SIZE;
    }

    private static int y(int point) {
        return point % FIELD_SIZE;
    }

    public static int of(int x, int y) {
        return x * FIELD_SIZE + y;
    }

    public static String pointToString(int p) {
        return "(" + SemiField.toString(x(p)) + "," + SemiField.toString(y(p)) + ")";
    }

    public static String lineToString(int i) {
        return line(i).stream().mapToObj(SemiFieldPlane::pointToString).collect(Collectors.joining(",", "[", "]"));
    }
}
