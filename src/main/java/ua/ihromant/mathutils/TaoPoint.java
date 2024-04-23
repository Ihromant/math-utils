package ua.ihromant.mathutils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TaoPoint {
    private static final int FIELD_SIZE = 3;
    public static final int SIZE = 27;

    private static int[] toCoordinates(int p) {
        return new int[]{
                (p / FIELD_SIZE / FIELD_SIZE) - 1,
                ((p / FIELD_SIZE) % FIELD_SIZE) - 1,
                (p % FIELD_SIZE) - 1
        };
    }

    private static int fromCoordinates(int[] crd) {
        return ((crd[0] + 1) * FIELD_SIZE + crd[1] + 1) * FIELD_SIZE + crd[2] + 1;
    }

    public static boolean collinear(int begin, int point, int end) {
        return begin == point || point == end || add(begin, end) == point;
    }

    public static int add(int a, int b) {
        int[] xCor = toCoordinates(a);
        int[] yCor = toCoordinates(b);
        return fromCoordinates(new int[]{addCrd(-xCor[0], -yCor[0]), addCrd(-xCor[1], -yCor[1]),
                addCrd(evenFunction(addCrd(xCor[0], -yCor[0]), addCrd(xCor[1], -yCor[1])), addCrd(-xCor[2], -yCor[2]))});
    }

    private static int addCrd(int x, int y) {
        int res = x + y;
        if (res == 2) {
            return -1;
        }
        if (res == -2) {
            return 1;
        }
        return res;
    }

    private static int evenFunction(int x, int y) {
        if (x != 0) {
            return 0;
        }
        if (y == 0) {
            return 0;
        }
        return 1;
    }

    public static BitSet line(int a, int b) {
        if (a == b) {
            throw new IllegalArgumentException();
        }
        BitSet result = new BitSet();
        result.set(a);
        result.set(b);
        result.set(add(a, b));
        return result;
    }

    public static boolean parallel(BitSet fst, BitSet snd) {
        boolean fstMatch = fst.stream().allMatch(a -> {
            BitSet cl = (BitSet) snd.clone();
            cl.set(a);
            BitSet hull = hull(cl);
            return fst.stream().allMatch(hull::get);
        });
        boolean sndMatch = snd.stream().allMatch(b -> {
            BitSet cl = (BitSet) fst.clone();
            cl.set(b);
            BitSet hull = hull(cl);
            return snd.stream().allMatch(hull::get);
        });
        return !fst.equals(snd) && fstMatch && sndMatch;
    }

    public static int lineCount() {
        return SIZE * (SIZE - 1) / 6;
    }

    public static Iterable<BitSet> lines() {
        return () -> IntStream.range(0, SIZE).boxed().flatMap(a -> IntStream.range(a + 1, SIZE)
                .filter(b -> add(a, b) > b).mapToObj(b -> line(a, b))).iterator();
    }

    public static BitSet hull(BitSet pts) {
        BitSet next = next(pts);
        while (next.cardinality() > pts.cardinality()) {
            pts = next;
            next = next(pts);
        }
        return pts;
    }

    public static BitSet hull(int... points) {
        BitSet base = new BitSet(SIZE);
        for (int point : points) {
            base.set(point);
        }
        return hull(base);
    }

    public static BitSet next(BitSet base) {
        BitSet next = (BitSet) base.clone();
        base.stream().forEach(x -> base.stream().forEach(y -> next.set(add(x, y))));
        return next;
    }

    public static int parse(String point) {
        point = point.substring(point.indexOf('(') + 1, point.indexOf(')'));
        String[] parts = point.split(",");
        return fromCoordinates(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])});
    }

    public static String toString(int p) {
        int[] crd = toCoordinates(p);
        return IntStream.of(crd).mapToObj(Integer::toString).collect(Collectors.joining(",", "(", ")"));
    }

    public static Iterable<Integer> points() {
        return () -> IntStream.range(0, SIZE).iterator();
    }

    public static Liner toPlane() {
        List<BitSet> lines = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            for (int j = i + 1; j < SIZE; j++) {
                int k = add(i, j);
                if (k > j) {
                    BitSet line = new BitSet();
                    line.set(i);
                    line.set(j);
                    line.set(k);
                    lines.add(line);
                }
            }
        }
        return new Liner(lines.toArray(BitSet[]::new));
    }
}
