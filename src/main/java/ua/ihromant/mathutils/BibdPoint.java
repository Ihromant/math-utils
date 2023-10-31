package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BibdPoint {
    private static final int COUNT = 13;
    private static final int[] first = {0, 2, 7};
    private static final int[] second = {0, 1, 4};
    private static final BitSet[] LINES = Stream.concat(IntStream.range(0, COUNT).mapToObj(i -> {
        BitSet res = new BitSet();
        res.set((i + first[0]) % COUNT);
        res.set((i + first[1]) % COUNT);
        res.set((i + first[2]) % COUNT);
        return res;
    }), IntStream.range(0, COUNT).mapToObj(i -> {
        BitSet res = new BitSet();
        res.set((i + second[0]) % COUNT);
        res.set((i + second[1]) % COUNT);
        res.set((i + second[2]) % COUNT);
        return res;
    })).toArray(BitSet[]::new);

    private static final int[][] LOOKUP = generateLookup();

    private static final BitSet[] POINTS = generateBeams();
    private static final int[][] INTERSECTIONS = generateIntersections();

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

    public static BitSet line(int line) {
        return LINES[line];
    }

    public static int line(int p1, int p2) {
        return LOOKUP[p1][p2];
    }

    public static Iterable<Integer> lines() {
        return () -> IntStream.range(0, LINES.length).boxed().iterator();
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

    public static String lineToString(int line) {
        return LINES[line].toString();
    }
}
