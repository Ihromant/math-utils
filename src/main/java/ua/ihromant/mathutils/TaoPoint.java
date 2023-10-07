package ua.ihromant.mathutils;

import java.util.BitSet;
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

    public static BitSet hull(int... points) {
        BitSet base = new BitSet(SIZE);
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
}
