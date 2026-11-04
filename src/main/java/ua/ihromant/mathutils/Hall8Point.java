package ua.ihromant.mathutils;

import ua.ihromant.mathutils.util.FixBS;
import ua.ihromant.mathutils.vector.LinearSpace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class Hall8Point {
    private static final LinearSpace space = LinearSpace.of(3, 8);
    private static final int[] base = IntStream.concat(IntStream.of(1), IntStream.range(0, 7).map(_ -> 0)).toArray();
    private static final int e0 = space.fromCrd(base);

    private static int addByDefinition(int a, int b) {
        int[] x = space.toCrd(a);
        int[] y = space.toCrd(b);
        int cff = (x[1] - y[1]) * (x[2] * y[3] - x[3] * y[2] + x[4] * y[7] - x[7] * y[4])
                + (x[2] - y[2]) * (x[5] * y[7] - x[7] * y[5])
                + (x[3] - y[3]) * (x[6] * y[7] - x[7] * y[6])
                + (x[4] - y[4]) * (x[5] * y[6] - x[6] * y[5]);
        return space.add(a, b, space.neg(space.mul(Math.floorMod(cff, 3), e0)));
    }

    public static Liner toPlane() {
        List<int[]> lines = new ArrayList<>();
        for (int i = 0; i < space.cardinality(); i++) {
            for (int j = i + 1; j < space.cardinality(); j++) {
                int k = addByDefinition(i, j);
                if (k > j) {
                    int[] line = new int[]{i, j, k};
                    lines.add(line);
                }
            }
        }
        return new Liner(lines.toArray(int[][]::new));
    }

    public static FixBS cardSubPlanes(boolean full) {
        int pointCount = space.cardinality();
        FixBS result = new FixBS(pointCount + 1);
        for (int x = 0; x < pointCount; x++) {
            for (int y = x + 1; y < pointCount; y++) {
                for (int z = y + 1; z < pointCount; z++) {
                    int card = hull(x, y, z).cardinality();
                    result.set(card);
                    System.out.println(card);
                    if (!full && card == pointCount) {
                        return result; // it's either plane or has no exchange property
                    }
                }
            }
        }
        result.clear(3);
        return result;
    }

    public static void cardSubSpaces() {
        int pointCount = space.cardinality();
        Set<Integer> cards = new HashSet<>();
        while (true) {
            int x = ThreadLocalRandom.current().nextInt(pointCount);
            int y = ThreadLocalRandom.current().nextInt(pointCount);
            int z = ThreadLocalRandom.current().nextInt(pointCount);
            int w = ThreadLocalRandom.current().nextInt(pointCount);
            int sCard = hull(x, y, z, w).cardinality();
            if (cards.add(sCard)) {
                System.out.println(sCard);
            }
        }
    }

    public static FixBS hull(int... points) {
        FixBS base = new FixBS(space.cardinality());
        for (int point : points) {
            base.set(point);
        }
        FixBS additional = base;
        while (!(additional = additional(base, additional)).isEmpty()) {
            base.or(additional);
        }
        return base;
    }

    public static FixBS additional(FixBS base, FixBS add) {
        FixBS result = new FixBS(space.cardinality());
        for (int x = base.nextSetBit(0); x >= 0; x = base.nextSetBit(x + 1)) {
            for (int y = add.nextSetBit(0); y >= 0; y = add.nextSetBit(y + 1)) {
                if (x == y) {
                    continue;
                }
                result.set(x);
                result.set(y);
                result.set(addByDefinition(x, y));
            }
        }
        result.andNot(base);
        return result;
    }
}
