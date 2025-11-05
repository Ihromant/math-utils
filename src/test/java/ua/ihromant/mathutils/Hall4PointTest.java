package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Hall4PointTest {
    private final GaloisField fd = new GaloisField(4);
    private final int lc = fd.cardinality();
    private final int cube = lc * lc * lc;

    @Test
    public void testHall() {
        int[][] rays = Stream.of(Stream.of(fd.elements().skip(1)
                        .map(cf -> mulSpace(fromArray(0, 0, 1), cf)).toArray()),
                fd.elements().mapToObj(z -> fd.elements().skip(1)
                        .map(cf -> mulSpace(fromArray(0, 1, z), cf)).toArray()),
                fd.elements().boxed().flatMap(y -> fd.elements().mapToObj(z -> fd.elements().skip(1)
                        .map(cf -> mulSpace(fromArray(1, y, z), cf)).toArray()))).flatMap(Function.identity()).toArray(int[][]::new);
        BitSet[] cubeLines = new BitSet[lc * lc * lc * lc + lc * lc * lc + lc * lc];
        int fromIdx = 0;
        for (int y = 0; y < lc; y++) {
            for (int z = 0; z < lc; z++) {
                int start = fromArray(0, y, z);
                for (int yDir = 0; yDir < lc; yDir++) {
                    for (int zDir = 0; zDir < lc; zDir++) {
                        int lineIdx = fromIdx + ((y * lc + z) * lc + yDir) * lc + zDir;
                        BitSet line = new BitSet();
                        line.set(start);
                        IntStream.of(rays[1 + lc + yDir * lc + zDir]).forEach(p -> line.set(addSpace(p, start)));
                        cubeLines[lineIdx] = line;
                    }
                }
            }
        }
        fromIdx = fromIdx + lc * lc * lc * lc;
        for (int x = 0; x < lc; x++) {
            for (int z = 0; z < lc; z++) {
                int start = fromArray(x, 0, z);
                for (int zDir = 0; zDir < lc; zDir++) {
                    int lineIdx = fromIdx + (x * lc + z) * lc + zDir;
                    BitSet line = new BitSet();
                    line.set(start);
                    IntStream.of(rays[1 + zDir]).forEach(p -> line.set(addSpace(p, start)));
                    cubeLines[lineIdx] = line;
                }
            }
        }
        fromIdx = fromIdx + lc * lc * lc;
        for (int x = 0; x < lc; x++) {
            for (int y = 0; y < lc; y++) {
                int start = fromArray(x, y, 0);
                int lineIdx = fromIdx + x * lc + y;
                BitSet line = new BitSet();
                line.set(start);
                IntStream.of(rays[0]).forEach(p -> line.set(addSpace(p, start)));
                cubeLines[lineIdx] = line;
            }
        }
        Liner cubeLiner = new Liner(cubeLines);
        assertEquals(cube, cubeLiner.pointCount());
        BitSet[] linez = Stream.of(
                IntStream.range(0, lc).boxed().flatMap(t -> Arrays.stream(cubeLines).map(l -> {
                    BitSet result = new BitSet();
                    l.stream().forEach(i -> result.set(i * lc + t));
                    return result;
                })),
                IntStream.range(0, cube).mapToObj(i -> {
                    BitSet result = new BitSet();
                    for (int j = 0; j < fd.cardinality(); j++) {
                        result.set(i * lc + j);
                    }
                    return result;
                }),
                IntStream.range(0, cube).boxed().flatMap(x -> IntStream.range(0, cube).filter(v -> Arrays.stream(toArray(v)).anyMatch(i -> i > 1))
                        .mapToObj(v -> {
                            BitSet result = new BitSet();
                            int[] xArr = toArray(x);
                            int[] vArr = toArray(v);
                            for (int t = 0; t < fd.cardinality(); t++) {
                                int sum = fromArray(linear(xArr, t, vArr));
                                result.set(sum * fd.cardinality() + t);
                            }
                            return result;
                        })),
                IntStream.range(0, cube).boxed().flatMap(x -> IntStream.range(1, 8).map(v -> fromArray(new int[]{v / 4, v / 2 % 2, v % 2}))
                        .mapToObj(v -> {
                            BitSet result = new BitSet();
                            int[] xArr = toArray(x);
                            int[] vArr = toArray(v);
                            for (int t = 0; t < fd.cardinality(); t++) {
                                int sum = fromArray(linear(xArr, t, vArr));
                                result.set(sum * fd.cardinality() + conjugate(t));
                            }
                            return result;
                        }))
        ).flatMap(Function.identity()).toArray(BitSet[]::new);
        Liner pl = new Liner(linez);
        assertEquals(cube * fd.cardinality(), pl.pointCount());
        assertEquals(cube * 85, pl.lineCount());
    }

    @Test
    public void testHall1() {
        int[][] sphere = Stream.of(fd.elements().boxed().flatMap(y -> fd.elements().mapToObj(z -> new int[]{1, y, z})),
                fd.elements().mapToObj(z -> new int[]{0, 1, z}), Stream.of(new int[]{0, 0, 1})).flatMap(Function.identity()).toArray(int[][]::new);
        BitSet[] horizontal = Arrays.stream(sphere).flatMap(v -> IntStream.range(0, lc).boxed().flatMap(b -> IntStream.range(0, cube).mapToObj(x -> {
            BitSet result = new BitSet();
            for (int t = 0; t < lc; t++) {
                result.set(addSpace(x, mulSpace(fromArray(v), t)) * lc + b);
            }
            return result;
        }))).collect(Collectors.toSet()).toArray(BitSet[]::new);
        BitSet[] lines = Stream.of(
                IntStream.range(0, cube).mapToObj(i -> {
                    BitSet result = new BitSet();
                    for (int j = 0; j < lc; j++) {
                        result.set(i * lc + j);
                    }
                    return result;
                }),
                Arrays.stream(horizontal),
                Arrays.stream(sphere).filter(v -> Arrays.stream(v).anyMatch(i -> i > 1)).flatMap(v -> IntStream.range(1, lc).boxed()
                        .flatMap(a -> IntStream.range(0, cube).mapToObj(x -> {
                            BitSet result = new BitSet();
                            for (int t = 0; t < lc; t++) {
                                result.set(addSpace(x, mulSpace(fromArray(v), t)) * lc + fd.mul(a, t));
                            }
                            return result;
                        }))),
                Arrays.stream(sphere).filter(v -> Arrays.stream(v).allMatch(i -> i <= 1)).flatMap(v -> IntStream.range(1, lc).boxed()
                        .flatMap(a -> IntStream.range(0, cube).mapToObj(x -> {
                            BitSet result = new BitSet();
                            for (int t = 0; t < lc; t++) {
                                result.set(addSpace(x, mulSpace(fromArray(v), t)) * lc + fd.mul(a, conjugate(t)));
                            }
                            return result;
                        })))
        ).flatMap(Function.identity()).toArray(BitSet[]::new);
        Liner pl = new Liner(lines);
        assertEquals(cube * lc, pl.pointCount());
        assertEquals(cube * 85, pl.lineCount());
        assertEquals(FixBS.of(pl.pointCount() + 1, 16), pl.cardSubPlanes(true));
        assertEquals(of(64, 256), checkSpace(pl));
    }

    public static BitSet checkSpace(Liner plane) {
        BitSet result = new BitSet();
        for (int x = 0; x < plane.pointCount(); x++) {
            for (int y = x + 1; y < plane.pointCount(); y++) {
                for (int z = y + 1; z < plane.pointCount(); z++) {
                    if (plane.collinear(x, y, z)) {
                        continue;
                    }
                    FixBS hull = plane.hull(x, y, z);
                    for (int w = z + 1; w < plane.pointCount(); w++) {
                        if (hull.get(w)) {
                            continue;
                        }
                        int sCard = plane.hull(x, y, z, w).cardinality();
                        int card = result.cardinality();
                        result.set(sCard);
                        int newCard = result.cardinality();
                        if (newCard != card) {
                            System.out.println(result);
                            if (newCard > 1) {
                                return result;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private int addSpace(int p1, int p2) {
        int x1 = p1 / lc / lc;
        int y1 = p1 / lc % lc;
        int z1 = p1 % lc;
        int x2 = p2 / lc / lc;
        int y2 = p2 / lc % lc;
        int z2 = p2 % lc;
        return fromArray(fd.add(x1, x2), fd.add(y1, y2), fd.add(z1, z2));
    }

    private int mulSpace(int point, int cff) {
        int x = fd.mul(point / lc / lc, cff);
        int y = fd.mul(point / lc % lc, cff);
        int z = fd.mul(point % lc, cff);
        return fromArray(new int[]{x, y, z});
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet(values[values.length - 1] + 1);
        IntStream.of(values).forEach(bs::set);
        return bs;
    }

    private int[] toArray(int idx) {
        return new int[]{idx / fd.cardinality() / fd.cardinality(), idx / fd.cardinality() % fd.cardinality(), idx % fd.cardinality()};
    }

    private int fromArray(int... arr) {
        return (arr[0] * fd.cardinality() + arr[1]) * fd.cardinality() + arr[2];
    }

    private int[] linear(int[] arr1, int t, int[] arr2) {
        int[] result = new int[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            result[i] = fd.add(arr1[i], fd.mul(t, arr2[i]));
        }
        return result;
    }

    private int conjugate(int par) {
        if (par < 2) {
            return par;
        }
        if (par == 2) {
            return 3;
        }
        if (par == 3) {
            return 2;
        }
        throw new IllegalArgumentException();
    }
}
