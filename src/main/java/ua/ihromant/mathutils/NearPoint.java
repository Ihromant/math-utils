package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record NearPoint(NearField x, NearField y) {
    private static final int COUNT = 91;
    private static final NearField[] FIELD = NearField.values();

    private static final List<List<NearPoint>> RAYS = Stream.concat(
            Stream.of(Arrays.stream(NearField.values()).skip(1)
                    .map(nf -> new NearPoint(NearField.ZERO, nf)).collect(Collectors.toList())),
            Arrays.stream(NearField.values()).map(nf -> Arrays.stream(NearField.values()).skip(1)
                    .map(cf -> cf.mul(new NearPoint(NearField.PL_1, nf))).collect(Collectors.toList()))).toList();

    private static final BitSet[] LINES = generateLines();

    private static final int[][] LOOKUP = generateLookup();

    private static final BitSet[] POINTS = generateBeams();
    private static final int[][] INTERSECTIONS = generateIntersections();

    private static BitSet[] generateLines() {
        BitSet[] lines = new BitSet[COUNT];
        for (int i = 0; i < FIELD.length; i++) {
            NearPoint start = new NearPoint(NearField.ZERO, FIELD[i]);
            for (int j = 0; j < FIELD.length; j++) {
                int lineIdx = i * FIELD.length + j;
                BitSet line = new BitSet();
                line.set(start.idx());
                RAYS.get(j + 1).forEach(p -> line.set(p.add(start).idx()));
                line.set(FIELD.length * FIELD.length + j);
                lines[lineIdx] = line;
            }
        }
        for (int i = 0; i < FIELD.length; i++) {
            int lineIdx = FIELD.length * FIELD.length + i;
            BitSet line = new BitSet();
            NearPoint start = new NearPoint(FIELD[i], NearField.ZERO);
            line.set(start.idx());
            RAYS.get(0).forEach(p -> line.set(p.add(start).idx()));
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

    public static Iterable<Integer> points(int line) {
        return () -> LINES[line].stream().boxed().iterator();
    }

    public static boolean collinear(int... points) {
        if (points.length == 0) {
            return true;
        }
        int first = points[0];
        for (int i = 1; i < points.length; i++) {
            int second = points[i];
            if (first != second) {
                BitSet line = LINES[LOOKUP[first][second]];
                return Arrays.stream(points).allMatch(line::get);
            }
        }
        return true;
    }

    public NearPoint sub(NearPoint that) {
        return new NearPoint(this.x.sub(that.x), this.y.sub(that.y));
    }

    public NearPoint add(NearPoint that) {
        return new NearPoint(this.x.add(that.x), this.y.add(that.y));
    }

    public boolean parallel(NearPoint that) {
        return Arrays.stream(NearField.values()).skip(1).anyMatch(nf -> this.equals(nf.mul(that)));
    }

    public static boolean parallel(int first, int second, int droppedLine) {
        return first == second || LINES[droppedLine].get(NearPoint.intersection(first, second));
    }

    private int idx() {
        return x.ordinal() * FIELD.length + y.ordinal();
    }

    private static NearPoint byIdx(int idx) {
        return new NearPoint(FIELD[idx / FIELD.length], FIELD[idx % FIELD.length]);
    }

    public static String pointToString(int idx) {
        if (idx < FIELD.length * FIELD.length) {
            return byIdx(idx).toString();
        }
        if (idx < FIELD.length * FIELD.length + FIELD.length) {
            return "∞" + new NearPoint(NearField.PL_1, FIELD[idx - FIELD.length * FIELD.length]);
        }
        return "∞" + new NearPoint(NearField.ZERO, NearField.PL_1);
    }

    public static String lineToString(int i) {
        return line(i).stream().mapToObj(NearPoint::pointToString).collect(Collectors.joining(",", "[", "]"));
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
