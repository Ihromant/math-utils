package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record VeblenPoint(String l, int cff) {
    private static final int COUNT = 91;
    private static final int SHIFT = 13;

    private static final BitSet[] base = new BitSet[]{
            Stream.of("A0", "A1", "A3", "A9", "B0", "C0", "D0", "E0", "F0", "G0")
                    .mapToInt(VeblenPoint::parse).collect(BitSet::new, BitSet::set, BitSet::or),
            Stream.of("A0", "B1", "B8", "D3", "D11", "E2", "E5", "E6", "G7", "G9")
                    .mapToInt(VeblenPoint::parse).collect(BitSet::new, BitSet::set, BitSet::or),
            Stream.of("A0", "C1", "C8", "E7", "E9", "F3", "F11", "G2", "G5", "G6")
                    .mapToInt(VeblenPoint::parse).collect(BitSet::new, BitSet::set, BitSet::or),
            Stream.of("A0", "B7", "B9", "D1", "D8", "F2", "F5", "F6", "G3", "G11")
                    .mapToInt(VeblenPoint::parse).collect(BitSet::new, BitSet::set, BitSet::or),
            Stream.of("A0", "B2", "B5", "B6", "C3", "C11", "E1", "E8", "F7", "F9")
                    .mapToInt(VeblenPoint::parse).collect(BitSet::new, BitSet::set, BitSet::or),
            Stream.of("A0", "C7", "C9", "D2", "D5", "D6", "E3", "E11", "F1", "F8")
                    .mapToInt(VeblenPoint::parse).collect(BitSet::new, BitSet::set, BitSet::or),
            Stream.of("A0", "B3", "B11", "C2", "C5", "C6", "D7", "D9", "G1", "G8")
                    .mapToInt(VeblenPoint::parse).collect(BitSet::new, BitSet::set, BitSet::or)
    };

    private static final BitSet[] LINES = IntStream.range(0, COUNT).mapToObj(idx -> base[idx / SHIFT].stream()
            .map(p -> next(p, idx % SHIFT)).collect(BitSet::new, BitSet::set, BitSet::or)).toArray(BitSet[]::new);

    private static final int[][] LOOKUP = generateLookup();

    private static final BitSet[] POINTS = generateBeams();
    private static final int[][] INTERSECTIONS = generateIntersections();

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

    public static int parse(String from) {
        char letter = from.toUpperCase().charAt(0);
        int val = Integer.parseInt(from.substring(1));
        return from(letter - 'A', val);
    }

    private static int from(int letter, int idx) {
        return letter * SHIFT + idx;
    }

    public static int next(int base, int add) {
        int letter = base / SHIFT;
        return from(letter, (base + add) % SHIFT);
    }

    public static String pointToString(int i) {
        return String.valueOf((char) ('A' + i / SHIFT)) + (i % SHIFT);
    }

    public static String lineToString(int i) {
        return line(i).stream().mapToObj(VeblenPoint::pointToString).collect(Collectors.joining(",", "[", "]"));
    }
}
