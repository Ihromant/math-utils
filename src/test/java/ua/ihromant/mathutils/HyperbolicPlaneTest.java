package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.BurnsideGroup;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.SemiDirectProduct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HyperbolicPlaneTest {
    @Test
    public void test151_156() {
        int v = 156;
        int k = 6;
        int[][][] diffSets = new int[][][]{
                {
                        {0, 31, 49, 74, 102, 132},
                        {0, 60, 87, 100, 110, 122},
                        {0, 75, 76, 95, 140, 142},
                        {0, 79, 86, 115, 118, 123},
                        {0, 84, 88, 99, 105, 147},
                        {0, 26, 52, 78, 104, 130}
                },
                {
                        {0, 31, 58, 72, 97, 127},
                        {0, 51, 54, 99, 121, 122},
                        {0, 63, 95, 106, 110, 116},
                        {0, 74, 76, 118, 138, 151},
                        {0, 83, 91, 100, 107, 119},
                        {0, 26, 52, 78, 104, 130}
                }
        };
        Map<Map<Integer, Integer>, Liner> lnrz = new HashMap<>();
        for (int[][] diffSet : diffSets) {
            IntStream.range(0, 1 << (diffSet.length - (v % k == 0 ? 2 : 1))).forEach(comb -> {
                int[][] ds = IntStream.range(0, diffSet.length)
                        .mapToObj(i -> ((1 << i) & comb) == 0 ? diffSet[i]
                                : IntStream.concat(IntStream.of(0), IntStream.range(1, k).map(idx -> k - idx).map(idx -> v - diffSet[i][idx])).toArray())
                        .toArray(int[][]::new);
                Liner p = Liner.byDiffFamily(v, ds);
                lnrz.putIfAbsent(p.hyperbolicFreq(), p);
                //System.out.println(p.hyperbolicFreq() + " " + p.cardSubPlanes(false) + " " + Arrays.deepToString(ds));
            });
        }
        System.out.println(lnrz.size() + " " + lnrz);
        int v1 = 151;
        int[][][] diffSets1 = new int[][][]{
                {
                        {0, 30, 41, 67, 94, 122},
                        {0, 60, 66, 100, 108, 112},
                        {0, 61, 62, 71, 78, 136},
                        {0, 68, 82, 113, 118, 131},
                        {0, 72, 95, 97, 116, 119}
                }
        };
        Map<Map<Integer, Integer>, Liner> lnrz1 = new HashMap<>();
        for (int[][] diffSet : diffSets1) {
            IntStream.range(0, 1 << (diffSet.length - (v1 % k == 0 ? 2 : 1))).forEach(comb -> {
                int[][] ds = IntStream.range(0, diffSet.length)
                        .mapToObj(i -> ((1 << i) & comb) == 0 ? diffSet[i]
                                : IntStream.concat(IntStream.of(0), IntStream.range(1, k).map(idx -> k - idx).map(idx -> v1 - diffSet[i][idx])).toArray())
                        .toArray(int[][]::new);
                Liner p = Liner.byDiffFamily(v1, ds);
                lnrz1.putIfAbsent(p.hyperbolicFreq(), p);
                //System.out.println(p.hyperbolicFreq() + " " + p.cardSubPlanes(false) + " " + Arrays.deepToString(ds));
            });
        }
        System.out.println(lnrz1.size() + " " + lnrz1);
    }

    @Test
    public void test126() {
        int v = 126;
        int k = 6;
        int[][] diffSet = new int[][]{
                {0, 26, 38, 56, 81, 103},
                {0, 40, 48, 68, 99, 102},
                {0, 41, 57, 93, 94, 107},
                {0, 80, 82, 87, 91, 97},
                {0, 21, 42, 63, 84, 105}
        };
        IntStream.range(0, 1 << (diffSet.length - (v % k == 0 ? 2 : 1))).forEach(comb -> {
            int[][] ds = IntStream.range(0, diffSet.length)
                    .mapToObj(i -> ((1 << i) & comb) == 0 ? diffSet[i]
                            : IntStream.concat(IntStream.of(0), IntStream.range(1, k).map(idx -> k - idx).map(idx -> v - diffSet[i][idx])).toArray())
                    .toArray(int[][]::new);
            Liner p = Liner.byDiffFamily(v, ds);
            System.out.println(p.hyperbolicFreq() + " " + Arrays.deepToString(ds));
        });
    }

    @Test
    public void testLarge() {
        CyclicProduct c1 = new CyclicProduct(43, 7);
        Liner p1 = Liner.byDiffFamily(c1, new int[][]{
                {0, c1.fromArr(1, 1), c1.fromArr(37, 2), c1.fromArr(36, 4), c1.fromArr(42, 1), c1.fromArr(6, 2), c1.fromArr(7, 4)},
                {0, c1.fromArr(3, 2), c1.fromArr(25, 4), c1.fromArr(22, 1), c1.fromArr(40, 2), c1.fromArr(18, 4), c1.fromArr(21, 1)},
                {0, c1.fromArr(9, 4), c1.fromArr(32, 1), c1.fromArr(23, 2), c1.fromArr(34, 4), c1.fromArr(11, 1), c1.fromArr(20, 2)},
                {0, c1.fromArr(27, 1), c1.fromArr(10, 2), c1.fromArr(26, 4), c1.fromArr(16, 1), c1.fromArr(33, 2), c1.fromArr(17, 4)},
                {0, c1.fromArr(38, 2), c1.fromArr(30, 4), c1.fromArr(35, 1), c1.fromArr(5, 2), c1.fromArr(13, 4), c1.fromArr(8, 1)},
                {0, c1.fromArr(28, 4), c1.fromArr(4, 1), c1.fromArr(19, 2), c1.fromArr(15, 4), c1.fromArr(39, 1), c1.fromArr(24, 2)},
                {0, c1.fromArr(41, 1), c1.fromArr(12, 2), c1.fromArr(14, 4), c1.fromArr(2, 1), c1.fromArr(31, 2), c1.fromArr(29, 4)},
                {0, c1.fromArr(0, 1), c1.fromArr(0, 2), c1.fromArr(0, 3), c1.fromArr(0, 4), c1.fromArr(0, 5), c1.fromArr(0, 6)}
        });
        assertEquals(of(2, 3, 4, 5), p1.hyperbolicIndex());

        CyclicProduct cg = new CyclicProduct(31, 7);
        Liner p = Liner.byDiffFamily(cg, new int[][]{
                {0, cg.fromArr(1, 1), cg.fromArr(26, 4), cg.fromArr(25, 2), cg.fromArr(30, 1), cg.fromArr(5, 4), cg.fromArr(6, 2)},
                {0, cg.fromArr(3, 2), cg.fromArr(16, 1), cg.fromArr(13, 4), cg.fromArr(28, 2), cg.fromArr(15, 1), cg.fromArr(18, 4)},
                {0, cg.fromArr(9, 4), cg.fromArr(17, 2), cg.fromArr(8, 1), cg.fromArr(22, 4), cg.fromArr(14, 2), cg.fromArr(23, 1)},
                {0, cg.fromArr(27, 1), cg.fromArr(20, 4), cg.fromArr(24, 2), cg.fromArr(4, 1), cg.fromArr(11, 4), cg.fromArr(7, 2)},
                {0, cg.fromArr(19, 2), cg.fromArr(29, 1), cg.fromArr(10, 4), cg.fromArr(12, 2), cg.fromArr(2, 1), cg.fromArr(21, 4)},
                {0, cg.fromArr(0, 1), cg.fromArr(0, 2), cg.fromArr(0, 3), cg.fromArr(0, 4), cg.fromArr(0, 5), cg.fromArr(0, 6)}
        });
        assertEquals(of(2, 3, 4, 5), p.hyperbolicIndex());
    }

    @Test
    public void testRecursive() {
        Liner base = Liner.byStrings("00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95abcbcac9babc9");
        BitSet[] constructed1 = Stream.concat(Stream.concat(IntStream.range(0, base.lineCount()).boxed().flatMap(l -> IntStream.range(0, 3).mapToObj(sk -> {
                    BitSet result = new BitSet();
                    int[] line = base.line(l);
                    for (int i = 0; i < 3; i++) {
                        result.set(2 * line[i] + (sk == i ? 0 : 1));
                    }
                    return result;
                })),
                IntStream.range(0, base.pointCount()).mapToObj(x -> {
                    BitSet result = new BitSet();
                    result.set(2 * x);
                    result.set(2 * x + 1);
                    result.set(2 * base.pointCount());
                    return result;
                })), IntStream.range(0, base.lineCount()).mapToObj(base::line).map(bs -> of(Arrays.stream(bs).map(i -> 2 * i).toArray()))).toArray(BitSet[]::new);
        Liner infty = new Liner(constructed1);
        assertEquals(2 * base.pointCount() + 1, infty.pointCount());
        assertEquals(117, infty.lineCount());
        assertEquals(of(0, 1), infty.hyperbolicIndex());
        assertEquals(of(7, 13, 27), infty.cardSubPlanes(true));

        Liner affine = Liner.byStrings("000011122236", "134534534547", "268787676858");
        BitSet[] constructed = Stream.concat(IntStream.range(0, affine.pointCount()).boxed().flatMap(x ->
                        IntStream.range(x + 1, affine.pointCount()).boxed().flatMap(y -> IntStream.range(0, 3).mapToObj(i -> {
                            BitSet result = new BitSet();
                            result.set(3 * x + i);
                            result.set(3 * y + i);
                            result.set(3 * quasiOp(affine, x, y) + ((i + 1) % 3));
                            return result;
                        }))),
                IntStream.range(0, affine.pointCount()).mapToObj(x -> {
                    BitSet result = new BitSet();
                    result.set(3 * x);
                    result.set(3 * x + 1);
                    result.set(3 * x + 2);
                    return result;
                })).toArray(BitSet[]::new);
        Liner bose = new Liner(constructed);
        assertEquals(3 * affine.pointCount(), bose.pointCount());
        assertEquals(9 * 13, bose.lineCount());
        assertEquals(of(1), bose.hyperbolicIndex());
        assertEquals(of(9, 27), bose.cardSubPlanes(true));
    }

    private static int quasiOp(Liner pl, int x, int y) {
        return Arrays.stream(pl.line(pl.line(x, y))).filter(p -> p != x && p != y).findAny().orElseThrow();
    }

    @Test
    public void hyperbolicPlaneExample() {
        Liner p2 = Liner.byStrings("00000001111112222223333444455566678",
                "13579bd3469ac34578b678a58ab78979c9a",
                "2468ace578bde96aecdbcded9cebecaeddb");
        assertEquals(15, p2.pointCount());
        assertEquals(35, p2.lineCount());
        assertEquals(60, Automorphisms.autCountOld(p2));
        assertEquals(of(4), p2.playfairIndex());
        assertEquals(of(1), p2.hyperbolicIndex());
        assertEquals(of(p2.pointCount()), p2.cardSubPlanes(true));

        Liner p3 = new Liner(19, new int[][]{{0, 1, 2}, {0, 3, 4}, {0, 5, 6}, {0, 7, 8}, {0, 9, 10}, {0, 11, 12},
                {0, 13, 14}, {0, 15, 16}, {0, 17, 18}, {1, 3, 5}, {1, 4, 7}, {1, 6, 9}, {1, 8, 11}, {1, 10, 13}, {1, 12, 15},
                {1, 14, 17}, {1, 16, 18}, {2, 3, 8}, {2, 4, 17}, {2, 5, 14}, {2, 6, 16}, {2, 7, 9}, {2, 10, 11}, {2, 12, 18},
                {2, 13, 15}, {3, 6, 7}, {3, 9, 12}, {3, 10, 17}, {3, 11, 16}, {3, 13, 18}, {3, 14, 15}, {4, 5, 13}, {4, 6, 11},
                {4, 8, 16}, {4, 9, 18}, {4, 10, 15}, {4, 12, 14}, {5, 7, 12}, {5, 8, 18}, {5, 9, 17}, {5, 10, 16}, {5, 11, 15},
                {6, 8, 14}, {6, 10, 18}, {6, 12, 13}, {6, 15, 17}, {7, 10, 14}, {7, 11, 17}, {7, 13, 16}, {7, 15, 18}, {8, 9, 15},
                {8, 10, 12}, {8, 13, 17}, {9, 11, 13}, {9, 14, 16}, {11, 14, 18}, {12, 16, 17}});
        assertEquals(of(1), p3.hyperbolicIndex());
        assertEquals(1, Automorphisms.autCountOld(p3));

        Liner p1 = Liner.byStrings("0000000001111111122222222333333334444455556666777788899aabbcgko",
                "14567ghij4567cdef456789ab456789ab59adf8bce9bcf8ade9decfdfcedhlp",
                "289abklmnba89lknmefdchgjijighfecd6klhilkgjnmhjmngiajgihigjheimq",
                "3cdefopqrghijrqopqrponmklporqklmn7romnqpnmqoklrplkbopporqqrfjnr");
        assertEquals(28, p1.pointCount());
        assertEquals(63, p1.lineCount());
        assertEquals(of(5), p1.playfairIndex());
        assertEquals(of(2), p1.hyperbolicIndex());
        assertEquals(of(p1.pointCount()), p1.cardSubPlanes(true));

        Liner p = Liner.byDiffFamily(217, new int[]{0,1,37,67,88,92,149}, new int[]{0,15,18,65,78,121,137}, new int[]{0,8,53,79,85,102,107},
                new int[]{0,11,86,100,120,144,190}, new int[]{0,29,64,165,198,205,207}, new int[]{0,31,62,93,124,155,186});
        assertEquals(217, p.pointCount());
        assertEquals(1116, p.lineCount());
        assertEquals(of(29), p.playfairIndex());
        assertEquals(of(2, 3, 4, 5), p.hyperbolicIndex());
        assertEquals(of(p.pointCount()), p.cardSubPlanes(false)); // it's a plane, but long-running, change to true to check
    }

    @Test
    public void checkNotPlane() {
        Liner p = Liner.byStrings("00000001111112222223333444455556666",
                "13579bd3478bc3478bc789a789a789a789a",
                "2468ace569ade65a9edbcdecbeddebcedcb");
        assertEquals(15, p.pointCount());
        assertEquals(35, p.lineCount());
        assertEquals(of(0), p.hyperbolicIndex());
        assertEquals(of(7), p.cardSubPlanes(true)); // it's model of 3-dimensional projective space
        assertEquals(of(p.pointCount()), p.cardSubSpaces(true));

        Liner p3 = Liner.byStrings("00000001111112222223333444455556666",
                "13579bd3478bc3478bc789a789a789a789a",
                "2468ace569ade65a9edbcdecbededcbdebc");
        assertEquals(15, p3.pointCount());
        assertEquals(35, p3.lineCount());
        assertEquals(of(0, 1), p3.hyperbolicIndex());
        assertEquals(of(7, p3.pointCount()), p3.cardSubPlanes(true)); // it's plane with no exchange property

        Liner p1 = Liner.byDiffFamily(31, new int[]{0, 1, 12}, new int[]{0, 2, 24},
                new int[]{0, 3, 8}, new int[]{0, 4, 17}, new int[]{0, 6, 16});
        assertEquals(of(0), p1.hyperbolicIndex());
        assertEquals(of(7), p1.cardSubPlanes(true)); // 4-dimensional projective space
        assertEquals(of(15), p1.cardSubSpaces(true)); // 4-dimensional projective space

        Liner p2 = Liner.byDiffFamily(63, new int[][]{{0, 7, 26}, {0, 13, 35}, {0, 1, 6},
                {0, 16, 33}, {0, 11, 25}, {0, 2, 12}, {0, 9, 27}, {0, 3, 32}, {0, 15, 23}, {0, 4, 24}, {0, 21, 42}});
        assertEquals(of(0, 1), p2.hyperbolicIndex());
        assertEquals(of(7, 15), p2.cardSubPlanes(true));
        assertEquals(of(15, 63), p2.cardSubSpaces(true));
    }

    @Test
    public void testCyclic() {
        CyclicProduct cg3 = new CyclicProduct(5, 5);

        Liner p3 = Liner.byDiffFamily(cg3, new int[][]{{0, 9, 23, 24}, {0, 12, 14, 17}});
        assertEquals(25, p3.pointCount());
        assertEquals(50, p3.lineCount());
        assertEquals(of(4), p3.playfairIndex());
        assertEquals(of(0, 1, 2), p3.hyperbolicIndex());

        CyclicProduct cg1 = new CyclicProduct(11, 11);
        int[][] cycles = new int[][]{
                {cg1.fromArr(0, 0), cg1.fromArr(0, 3), cg1.fromArr(0, 4), cg1.fromArr(1, 1), cg1.fromArr(1, 7), cg1.fromArr(4, 6)},
                {cg1.fromArr(0, 0), cg1.fromArr(0, 2), cg1.fromArr(2, 5), cg1.fromArr(4, 7), cg1.fromArr(6, 4), cg1.fromArr(8, 0)},
                {cg1.fromArr(0, 0), cg1.fromArr(1, 5), cg1.fromArr(2, 0), cg1.fromArr(4, 1), cg1.fromArr(6, 0), cg1.fromArr(7, 2)},
                {cg1.fromArr(0, 0), cg1.fromArr(1, 0), cg1.fromArr(3, 9), cg1.fromArr(4, 8), cg1.fromArr(6, 1), cg1.fromArr(9, 5)}
        };
        Liner p1 = Liner.byDiffFamily(cg1, cycles);
        assertEquals(121, p1.pointCount());
        assertEquals(484, p1.lineCount());
        assertEquals(of(18), p1.playfairIndex());
        assertEquals(of(1, 2, 3, 4), p1.hyperbolicIndex());

        CyclicProduct cg2 = new CyclicProduct(7, 5, 5);
        int[][] cycles1 = new int[][] {
                {0, cg2.fromArr(1, 1, 3), cg2.fromArr(1, 4, 2), cg2.fromArr(2, 2, 2), cg2.fromArr(2, 3, 3), cg2.fromArr(4, 2, 0), cg2.fromArr(4, 3, 0)},
                {0, cg2.fromArr(1, 3, 4), cg2.fromArr(1, 2, 1), cg2.fromArr(2, 2, 3), cg2.fromArr(2, 3, 2), cg2.fromArr(4, 0, 2), cg2.fromArr(4, 0, 3)},
                {0, cg2.fromArr(1, 1, 2), cg2.fromArr(1, 4, 3), cg2.fromArr(2, 1, 1), cg2.fromArr(2, 4, 4), cg2.fromArr(4, 0, 1), cg2.fromArr(4, 0, 4)},
                {0, cg2.fromArr(1, 3, 1), cg2.fromArr(1, 2, 4), cg2.fromArr(2, 4, 1), cg2.fromArr(2, 1, 4), cg2.fromArr(4, 1, 0), cg2.fromArr(4, 4, 0)},
                {0, cg2.fromArr(1, 0, 0), cg2.fromArr(2, 0, 0), cg2.fromArr(3, 0, 0), cg2.fromArr(4, 0, 0), cg2.fromArr(5, 0, 0), cg2.fromArr(6, 0, 0)}
        };
        Liner p = Liner.byDiffFamily(cg2, cycles1);
        assertEquals(175, p.pointCount());
        assertEquals(725, p.lineCount());
        assertEquals(of(22), p.playfairIndex());
        assertEquals(of(2, 3, 4, 5), p.hyperbolicIndex());
        assertEquals(of(p.pointCount()), p.cardSubPlanes(false)); // it's a plane, but long-running, change to true to check

        CyclicGroup left = new CyclicGroup(7);
        CyclicGroup right = new CyclicGroup(41);
        CyclicProduct cg = new CyclicProduct(7, 41);
        int[][] bases = new int[][]{{0, 9}, {0, 32}, {1, 3}, {1, 38}, {2, 1}, {2, 40}, {4, 14}, {4, 27}};
        BitSet[] lines = Stream.concat(IntStream.of(1, 37, 16, 18, 10).boxed().flatMap(t -> {
            int[][] shifted = Arrays.stream(bases).map(pair -> {
                int[] result = pair.clone();
                result[1] = right.mul(result[1], t);
                return result;
            }).toArray(int[][]::new);
            return left.elements().boxed().flatMap(l -> right.elements().mapToObj(r -> {
                BitSet result = new BitSet();
                for (int[] pair : shifted) {
                    result.set(cg.fromArr(left.op(l, pair[0]), right.op(r, pair[1])));
                }
                return result;
            }));
        }), right.elements().mapToObj(r -> {
            BitSet result = new BitSet();
            left.elements().forEach(l -> result.set(cg.fromArr(l, r)));
            result.set(cg.order());
            return result;
        })).toArray(BitSet[]::new);
        Liner p6 = new Liner(lines);
        assertEquals(of(2, 3, 4, 5, 6), p6.hyperbolicIndex());

        Liner triFour = Liner.byDiffFamily(new int[]{0, 9, 13}, new int[]{0, 1, 3, 8});

        assertEquals(19, triFour.pointCount());
        assertEquals(38, triFour.lineCount());
        assertEquals(of(3, 4), triFour.playfairIndex());
        assertEquals(of(0, 1, 2), triFour.hyperbolicIndex());

        Liner fp6 = Liner.byDiffFamily(new int[]{0, 13, 19, 21, 43, 53}, new int[]{0, 1, 12, 17, 26}, new int[]{0, 3, 7, 36, 51});

        assertEquals(71, fp6.pointCount());
        assertEquals(213, fp6.lineCount());
        assertEquals(of(10, 11), fp6.playfairIndex());
        assertEquals(of(0, 1, 2, 3, 4), fp6.hyperbolicIndex());
    }

    @Test
    public void testPrimePower() {
        Liner p4 = Liner.byGaloisPower(109, new int[]{0, 1, 3, 60});
        assertEquals(109, p4.pointCount());
        assertEquals(981, p4.lineCount());
        assertEquals(of(32), p4.playfairIndex());
        assertEquals(of(1, 2), p4.hyperbolicIndex());

        GaloisField fd1 = new GaloisField(121);
        int x = fd1.solve(new int[]{1, 3, 8}).findAny().orElseThrow();
        Liner p3 = Liner.byGaloisPower(fd1.cardinality(), new int[]{0, 1, x, fd1.power(x, 10)});
        assertEquals(121, p3.pointCount());
        assertEquals(1210, p3.lineCount());
        assertEquals(of(36), p3.playfairIndex());
        assertEquals(of(0, 1, 2), p3.hyperbolicIndex());

        GaloisField fd2 = new GaloisField(169);
        int x1 = fd2.solve(new int[]{1, 9, 6}).findAny().orElseThrow();
        Liner p5 = Liner.byGaloisPower(fd2.cardinality(), new int[]{0, 1, x1, fd2.power(x1, 3), fd2.power(x1, 8), fd2.power(x1, 51), fd2.power(x1, 58)});
        assertEquals(169, p5.pointCount());
        assertEquals(676, p5.lineCount());
        assertEquals(of(21), p5.playfairIndex());
        assertEquals(of(1, 2, 3, 4, 5), p5.hyperbolicIndex());

        GaloisField fd = new GaloisField(421);
        int c1 = 1;
        int c2 = 4;
        int w = fd.oneRoots(3).findAny().orElseThrow();
        Liner p = Liner.byGaloisPower(fd.cardinality(),
                new int[] {0, c1, fd.mul(c1, w), fd.mul(c1, fd.mul(w, w)), c2, fd.mul(c2, w), fd.mul(c2, fd.mul(w, w))});
        assertEquals(421, p.pointCount());
        assertEquals(4210, p.lineCount());
        assertEquals(of(63), p.playfairIndex());
        assertEquals(of(2, 3, 4, 5), p.hyperbolicIndex());

        Liner p1 = Liner.byGaloisPower(433, new int[]{0, 1, 3, 30, 52, 61, 84, 280, 394});
        assertEquals(433, p1.pointCount());
        assertEquals(2598, p1.lineCount());
        assertEquals(of(45), p1.playfairIndex());
        assertEquals(of(2, 3, 4, 5, 6, 7), p1.hyperbolicIndex());

        Liner p2 = Liner.byGaloisPower(449, new int[]{0, 1, 3, 8, 61, 104, 332, 381});
        assertEquals(449, p2.pointCount());
        assertEquals(3592, p2.lineCount());
        assertEquals(of(56), p2.playfairIndex());
        assertEquals(of(1, 2, 3, 4, 5, 6), p2.hyperbolicIndex());
    }

    // ((k^2+(a-1)*k-a+1)*(k^2+(a-1)*k - a)) - k^4 + k^3 - (2a-1)k^3 + (2a-1)k^2 - (a^2 - 2a + 1)k^2 + (a^2-2a+1)k
    @Test
    public void findValuesForPlayfairIndex() {
        for (int pl = 2; pl <= 10; pl++) {
            int p = pl * (pl - 1);
            for (int k = 3; k <= p; k++) {
                int v = k * k - k + pl * k - pl + 1;
                if (p % k == 0) {
                    System.out.println("pl=" + pl + ",k=" + k + ",v=" + v);
                }
            }
        }
    }

    private int[] apply(UnaryOperator<int[]> auth, int[] base, int times) {
        int[] result = base;
        for (int i = 0; i < times; i++) {
            result = auth.apply(result);
        }
        return result;
    }

    @Test
    public void nonStandard() {
        CyclicProduct cg = new CyclicProduct(13, 5);
        Liner p7 = new Liner(Stream.concat(Stream.of(
                        new int[]{cg.fromArr(2, 0), cg.fromArr(5, 0), cg.fromArr(4, 1), cg.fromArr(9, 1), cg.fromArr(0, 3), cg.fromArr(6, 3)},
                        new int[]{cg.fromArr(6, 1), cg.fromArr(2, 1), cg.fromArr(12, 2), cg.fromArr(1, 2), cg.fromArr(0, 3), cg.fromArr(5, 3)},
                        new int[]{cg.fromArr(5, 2), cg.fromArr(6, 2), cg.fromArr(10, 0), cg.fromArr(3, 0), cg.fromArr(0, 3), cg.fromArr(2, 3)},
                        new int[]{cg.fromArr(1, 0), cg.fromArr(2, 0), cg.fromArr(6, 0), cg.fromArr(12, 2), cg.fromArr(5, 4), cg.fromArr(8, 4)},
                        new int[]{cg.fromArr(3, 1), cg.fromArr(6, 1), cg.fromArr(5, 1), cg.fromArr(10, 0), cg.fromArr(2, 4), cg.fromArr(11, 4)},
                        new int[]{cg.fromArr(9, 2), cg.fromArr(5, 2), cg.fromArr(2, 2), cg.fromArr(4, 1), cg.fromArr(6, 4), cg.fromArr(7, 4)},
                        new int[]{cg.fromArr(7, 0), cg.fromArr(9, 0), cg.fromArr(10, 1), cg.fromArr(1, 2), cg.fromArr(3, 3), cg.fromArr(4, 4)},
                        new int[]{cg.fromArr(8, 1), cg.fromArr(1, 1), cg.fromArr(4, 2), cg.fromArr(3, 0), cg.fromArr(9, 3), cg.fromArr(12, 4)},
                        new int[]{cg.fromArr(11, 2), cg.fromArr(3, 2), cg.fromArr(12, 0), cg.fromArr(9, 1), cg.fromArr(1, 3), cg.fromArr(10, 4)},
                        new int[]{cg.fromArr(2, 3), cg.fromArr(6, 3), cg.fromArr(5, 3), cg.fromArr(4, 4), cg.fromArr(12, 4), cg.fromArr(10, 4)})
                .flatMap(arr -> IntStream.range(0, 13).mapToObj(idx -> {
                    BitSet result = new BitSet();
                    for (int i : arr) {
                        result.set(cg.op(i, cg.fromArr(idx, 0)));
                    }
                    return result;
                })), IntStream.range(0, 13).mapToObj(idx -> {
            BitSet result = new BitSet();
            for (int i = 0; i < 5; i++) {
                result.set(cg.fromArr(idx, i));
            }
            result.set(cg.order());
            return result;
        })).toArray(BitSet[]::new));
        assertEquals(cg.order() + 1, p7.pointCount());
        assertEquals(143, p7.lineCount());
        assertEquals(of(7), p7.playfairIndex());
        assertEquals(of(0, 1, 2, 3, 4), p7.hyperbolicIndex());

        CyclicProduct cg1 = new CyclicProduct(19, 4);
        Liner p9 = new Liner(Stream.of(
                        new int[]{cg1.fromArr(0, 0), cg1.fromArr(0, 1), cg1.fromArr(1, 1), cg1.fromArr(3, 1), cg1.fromArr(14, 1), cg1.fromArr(10, 3)},
                        new int[]{cg1.fromArr(0, 0), cg1.fromArr(0, 2), cg1.fromArr(7, 2), cg1.fromArr(2, 2), cg1.fromArr(3, 2), cg1.fromArr(13, 1)},
                        new int[]{cg1.fromArr(0, 0), cg1.fromArr(0, 3), cg1.fromArr(11, 3), cg1.fromArr(14, 3), cg1.fromArr(2, 3), cg1.fromArr(15, 2)},
                        new int[]{cg1.fromArr(1, 0), cg1.fromArr(3, 0), cg1.fromArr(0, 1), cg1.fromArr(7, 1), cg1.fromArr(0, 2), cg1.fromArr(2, 3)},
                        new int[]{cg1.fromArr(7, 0), cg1.fromArr(2, 0), cg1.fromArr(0, 2), cg1.fromArr(11, 2), cg1.fromArr(0, 3), cg1.fromArr(14, 1)},
                        new int[]{cg1.fromArr(11, 0), cg1.fromArr(14, 0), cg1.fromArr(0, 3), cg1.fromArr(1, 3), cg1.fromArr(0, 1), cg1.fromArr(3, 2)},
                        new int[]{cg1.fromArr(1, 0), cg1.fromArr(7, 0), cg1.fromArr(11, 0), cg1.fromArr(3, 1), cg1.fromArr(2, 2), cg1.fromArr(14, 3)},
                        new int[]{cg1.fromArr(3, 0), cg1.fromArr(2, 0), cg1.fromArr(14, 0), cg1.fromArr(12, 1), cg1.fromArr(8, 2), cg1.fromArr(18, 3)},
                        new int[]{cg1.fromArr(7, 1), cg1.fromArr(16, 1), cg1.fromArr(11, 2), cg1.fromArr(17, 2), cg1.fromArr(1, 3), cg1.fromArr(5, 3)},
                        new int[]{cg1.fromArr(13, 1), cg1.fromArr(17, 1), cg1.fromArr(15, 2), cg1.fromArr(5, 2), cg1.fromArr(10, 3), cg1.fromArr(16, 3)})
                .flatMap(arr -> IntStream.range(0, 19).mapToObj(idx -> {
                    BitSet result = new BitSet();
                    for (int i : arr) {
                        result.set(cg1.op(i, cg1.fromArr(idx, 0)));
                    }
                    return result;
                })).toArray(BitSet[]::new));
        assertEquals(cg1.order(), p9.pointCount());
        assertEquals(190, p9.lineCount());
        assertEquals(of(9), p9.playfairIndex());
        assertEquals(of(0, 1, 2, 3, 4), p9.hyperbolicIndex());

        int count = 96;
        Liner p = new Liner(Stream.of(new int[]{0, 16, 32, 48, 64, 80}, new int[]{1, 17, 33, 49, 65, 81},
                new int[]{0, 2, 6, 26, 56, 1}, new int[]{0, 8, 22, 35, 73, 77},
                new int[]{0, 10, 38, 3, 49, 85}, new int[]{0, 18, 52, 9, 15, 81},
                new int[]{0, 12, 17, 19, 37, 45}, new int[]{0, 36, 23, 57, 67, 79})
                .flatMap(arr -> IntStream.range(0, count / 2).mapToObj(idx -> {
                    BitSet bs = new BitSet();
                    for (int i : arr) {
                        bs.set((i + 2 * idx) % count);
                    }
                    return bs;
                })).collect(Collectors.toSet()).toArray(BitSet[]::new));
        assertEquals(count, p.pointCount());
        assertEquals(304, p.lineCount());
        assertEquals(of(13), p.playfairIndex());
        assertEquals(of(1, 2, 3, 4), p.hyperbolicIndex());

        int count1 = 106;
        BitSet[] lines = Stream.of(new int[]{0, 2, 6, 22, 76, 1}, new int[]{0, 26, 60, 47, 71, 103},
                        new int[]{0, 10, 38, 50, 73, 79}, new int[]{0, 8, 57, 61, 75, 95},
                        new int[]{0, 14, 58, 17, 33, 97}, new int[]{0, 5, 15, 51, 59, 99}, new int[]{0, 18, 42, 25, 27, 55})
                .flatMap(arr -> IntStream.range(0, count1 / 2).mapToObj(idx -> {
                    BitSet result = new BitSet();
                    for (int i : arr) {
                        result.set((i + 2 * idx) % count1);
                    }
                    return result;
                })).toArray(BitSet[]::new);
        Liner p1 = new Liner(lines);
        assertEquals(count1, p1.pointCount());
        assertEquals(371, p1.lineCount());
        assertEquals(of(15), p1.playfairIndex());
        assertEquals(of(0, 1, 2, 3, 4), p1.hyperbolicIndex());

        SemiDirectProduct semi = new SemiDirectProduct(new CyclicGroup(37), new CyclicGroup(3));
        BitSet[] lines2 = Stream.concat(Stream.of(new int[]{0, semi.fromAB(1, 0), semi.fromAB(3, 0),
                        semi.fromAB(7, 0), semi.fromAB(17, 0), semi.fromAB(0, 1)},
                new int[]{0, semi.fromAB(5, 0), semi.fromAB(19, 1), semi.fromAB(28, 1),
                        semi.fromAB(10, 2), semi.fromAB(30, 2)}).flatMap(arr -> IntStream.range(0, semi.order()).mapToObj(i -> {
            BitSet res = new BitSet();
            Arrays.stream(arr).forEach(el -> res.set(semi.op(i, el)));
            return res;
        })), Stream.of(new int[]{5, 33}, new int[]{9, 27}, new int[]{10, 23}, new int[]{13, 24}, new int[]{26, 34})
                .flatMap(arr -> IntStream.range(0, 37).mapToObj(i -> {
                    BitSet res = new BitSet();
                    res.set(semi.op(i, arr[0]));
                    res.set(semi.op(i, arr[1]));
                    res.set(semi.op(semi.fromAB(i, 1), arr[0]));
                    res.set(semi.op(semi.fromAB(i, 1), arr[1]));
                    res.set(semi.op(semi.fromAB(i, 2), arr[0]));
                    res.set(semi.op(semi.fromAB(i, 2), arr[1]));
                    return res;
                }))).toArray(BitSet[]::new);
        Liner p2 = new Liner(lines2);
        assertEquals(semi.order(), p2.pointCount());
        assertEquals(407, p2.lineCount());
        assertEquals(of(16), p2.playfairIndex());
        assertEquals(of(1, 2, 3, 4), p2.hyperbolicIndex());

        CyclicProduct gp = new CyclicProduct(5, 5, 5);
        int[][] bs = new int[][] {
                {gp.fromArr(0, 0, 1), gp.fromArr(0, 0, 4), gp.fromArr(1, 2, 2), gp.fromArr(1, 3, 3), gp.fromArr(4, 2, 1), gp.fromArr(4, 3, 4)},
                {gp.fromArr(0, 0, 2), gp.fromArr(0, 0, 3), gp.fromArr(1, 4, 4), gp.fromArr(1, 1, 1), gp.fromArr(4, 4, 2), gp.fromArr(4, 1, 3)},
                {gp.fromArr(0, 4, 3), gp.fromArr(0, 1, 2), gp.fromArr(2, 2, 0), gp.fromArr(2, 3, 0), gp.fromArr(3, 3, 2), gp.fromArr(3, 2, 3)},
                {gp.fromArr(0, 3, 1), gp.fromArr(0, 2, 4), gp.fromArr(2, 4, 0), gp.fromArr(2, 1, 0), gp.fromArr(3, 1, 4), gp.fromArr(3, 4, 1)}
        };
        BitSet[] ls = Stream.concat(Arrays.stream(bs).flatMap(arr -> IntStream.range(0, gp.order()).mapToObj(idx -> {
            BitSet result = new BitSet();
            for (int i : arr) {
                result.set(gp.op(i, idx));
            }
            return result;
        })), IntStream.range(0, 25).mapToObj(idx -> {
            BitSet result = new BitSet();
            for (int i = 0; i < 5; i++) {
                result.set(gp.op(i * 25, idx));
            }
            result.set(gp.order());
            return result;
        })).toArray(BitSet[]::new);
        Liner p126 = new Liner(ls);
        assertEquals(of(1, 2, 3, 4), p126.hyperbolicIndex());

        CyclicGroup c3 = new CyclicGroup(3);
        CyclicGroup c45 = new CyclicGroup(45);
        CyclicProduct pr135 = new CyclicProduct(3, 45);
        int[][][] base1 = new int[][][] {
                {{0, 0}, {0, 3}, {0, 15}, {0, 35}, {2, 6}, {2, 10}},
                {{0, 0}, {0, 22}, {1, 11}, {1, 30}, {2, 1}, {2, 18}},
                {{0, 0}, {0, 5}, {1, 18}, {1, 41}, {2, 13}, {2, 42}},
                {{0, 0}, {0, 11}, {0, 17}, {2, 4}, {2, 5}, {2, 28}},
                {{0, 0}, {0, 1}, {1, 0}, {1, 16}, {2, 0}, {2, 31}}
        };
        int[][] base2 = new int[][]{{0, 0}, {0, 9}, {0, 18}, {0, 27}, {0, 36}};
        UnaryOperator<int[]> alpha135 = arr -> new int[]{c3.op(arr[0], 1), c45.mul(arr[1], 16)};
        UnaryOperator<int[]> beta135 = arr -> new int[]{arr[0], c45.op(arr[1], 1)};
        BitSet[] blocks = Stream.concat(Arrays.stream(base1).flatMap(arr -> c3.elements().boxed().flatMap(a -> c45.elements().mapToObj(b -> {
            BitSet result = new BitSet();
            for (int[] base : arr) {
                result.set(pr135.fromArr(apply(beta135, apply(alpha135, base, a), b)));
            }
            return result;
        }))), c3.elements().boxed().flatMap(a -> c45.elements().mapToObj(b -> {
            BitSet result = new BitSet();
            for (int[] base : base2) {
                result.set(pr135.fromArr(apply(beta135, apply(alpha135, base, a), b)));
            }
            result.set(pr135.order());
            return result;
        }))).collect(Collectors.toSet()).toArray(BitSet[]::new);
        Liner p4 = new Liner(blocks);
        assertEquals(of(21), p4.playfairIndex());
        assertEquals(of(0, 1, 2, 3, 4), p4.hyperbolicIndex());

        CyclicGroup c4 = new CyclicGroup(4);
        CyclicGroup c35 = new CyclicGroup(35);
        CyclicProduct pr140 = new CyclicProduct(4, 35);
        int[][][] base4 = new int[][][]{
                {{0, 0}, {0, 16}, {0, 24}, {1, 24}, {2, 15}, {2, 25}},
                {{0, 0}, {0, 3}, {0, 26}, {1, 13}, {1, 33}, {3, 34}},
                {{0, 0}, {0, 13}, {0, 18}, {1, 15}, {2, 7}, {3, 0}},
                {{0, 0}, {0, 2}, {1, 14}, {1, 23}, {3, 26}, {3, 32}},
                {{0, 0}, {0, 4}, {1, 29}, {2, 6}, {3, 9}, {3, 20}},
                {{0, 0}, {0, 1}, {2, 12}, {3, 2}, {3, 4}, {3, 19}}
        };
        int[][] base5 = new int[][]{{0, 0}, {0, 7}, {0, 14}, {0, 21}, {0, 28}};
        UnaryOperator<int[]> alpha105 = arr -> new int[]{arr[0] == c3.order() ? arr[0] : c3.op(arr[0], 1), c35.mul(arr[1], 16)};
        UnaryOperator<int[]> beta105 = arr -> new int[]{arr[0], c35.op(arr[1], 1)};
        BitSet[] blocks2 = Stream.concat(Arrays.stream(base4).flatMap(arr -> c3.elements().boxed().flatMap(a -> c35.elements().mapToObj(b -> {
            BitSet result = new BitSet();
            for (int[] base : arr) {
                result.set(pr140.fromArr(apply(beta105, apply(alpha105, base, a), b)));
            }
            return result;
        }))), c4.elements().boxed().flatMap(a -> c35.elements().mapToObj(b -> {
            BitSet result = new BitSet();
            for (int[] base : base5) {
                result.set(pr140.fromArr(apply(beta105, apply(alpha105, new int[]{base[0] + a, base[1]}, a), b)));
            }
            result.set(pr140.order());
            return result;
        }))).collect(Collectors.toSet()).toArray(BitSet[]::new);
        Liner p6 = new Liner(blocks2);
        assertEquals(of(22), p6.playfairIndex());
        assertEquals(of(1, 2, 3, 4), p6.hyperbolicIndex());

        SemiDirectProduct semi1 = new SemiDirectProduct(new CyclicGroup(57), new CyclicGroup(3));
        BitSet[] lines3 = Stream.concat(Stream.of(new int[]{0, 19, 39, 41, semi1.fromAB(14, 1), semi1.fromAB(38, 2)},
                        new int[]{0, 21, 44, 48, semi1.fromAB(26, 1), semi1.fromAB(11, 2)},
                        new int[]{0, 1, 43, semi1.fromAB(8, 2), semi1.fromAB(15, 2), semi1.fromAB(44, 2)},
                        new int[]{0, 3, 31, semi1.fromAB(23, 1), semi1.fromAB(43, 1), semi1.fromAB(36, 2)},
                        new int[]{0, 40, 50, semi1.fromAB(11, 1), semi1.fromAB(25, 2), semi1.fromAB(34, 2)})
                .flatMap(arr -> IntStream.range(0, semi1.order()).mapToObj(i -> {
                    BitSet result = new BitSet();
                    Arrays.stream(arr).forEach(el -> result.set(semi1.op(i, el)));
                    return result;
                })), Stream.of(new int[]{0, 12}, new int[]{37, 42})
                .flatMap(arr -> IntStream.range(0, 57).mapToObj(i -> {
                    BitSet res = new BitSet();
                    res.set(semi1.op(i, arr[0]));
                    res.set(semi1.op(i, arr[1]));
                    res.set(semi1.op(semi1.fromAB(i, 1), arr[0]));
                    res.set(semi1.op(semi1.fromAB(i, 1), arr[1]));
                    res.set(semi1.op(semi1.fromAB(i, 2), arr[0]));
                    res.set(semi1.op(semi1.fromAB(i, 2), arr[1]));
                    return res;
                }))).toArray(BitSet[]::new);
        Liner p3 = new Liner(lines3);
        assertEquals(semi1.order(), p3.pointCount());
        assertEquals(969, p3.lineCount());
        assertEquals(of(28), p3.playfairIndex());
        assertEquals(of(1, 2, 3, 4), p3.hyperbolicIndex());

        CyclicGroup c49 = new CyclicGroup(49);
        CyclicProduct pr196 = new CyclicProduct(4, 49);
        int[][][] base6 = new int[][][]{
                {{0, 0}, {0, 1}, {1, 0}, {1, 30}, {2, 0}, {2, 18}},
                {{0, 8}, {0, 19}, {1, 44}, {1, 31}, {2, 46}, {2, 48}},
                {{0, 0}, {0, 2}, {0, 12}, {0, 45}, {1, 3}, {3, 11}},
                {{0, 0}, {0, 3}, {0, 8}, {1, 5}, {1, 17}, {3, 39}},
                {{0, 0}, {0, 9}, {0, 36}, {1, 24}, {1, 44}, {3, 37}},
                {{0, 0}, {0, 15}, {1, 34}, {1, 41}, {2, 47}, {3, 18}},
                {{0, 0}, {0, 7}, {0, 31}, {1, 13}, {2, 35}, {3, 41}},
                {{0, 0}, {0, 14}, {1, 32}, {2, 10}, {3, 22}, {3, 44}},
                {{0, 0}, {0, 23}, {1, 21}, {1, 39}, {3, 19}, {3, 25}},
                {{0, 0}, {1, 33}, {3, 0}, {3, 5}, {3, 29}, {3, 47}}
        };
        UnaryOperator<int[]> alpha147 = arr -> new int[]{arr[0] == c3.order() ? arr[0] : c3.op(arr[0], 1), c49.mul(arr[1], 30)};
        UnaryOperator<int[]> beta147 = arr -> new int[]{arr[0], c49.op(arr[1], 1)};
        BitSet[] blocks3 = Arrays.stream(base6).flatMap(arr -> c3.elements().boxed().flatMap(a -> c49.elements().mapToObj(b -> {
            BitSet result = new BitSet();
            for (int[] base : arr) {
                result.set(pr196.fromArr(apply(beta147, apply(alpha147, base, a), b)));
            }
            return result;
        }))).collect(Collectors.toSet()).toArray(BitSet[]::new);
        Liner p8 = new Liner(blocks3);
        assertEquals(of(33), p8.playfairIndex());
        assertEquals(of(0, 1, 2, 3, 4), p8.hyperbolicIndex());

        CyclicGroup c67 = new CyclicGroup(67);
        CyclicProduct pr201 = new CyclicProduct(3, 67);
        int[][][] base3 = new int[][][] {
                {{1, 3}, {1, 20}, {1, 44}, {2, 36}, {2, 39}, {2, 59}},
                {{0, 0}, {1, 0}, {1, 30}, {1, 38}, {1, 66}, {2, 0}},
                {{0, 0}, {0, 1}, {2, 4}, {2, 9}, {2, 34}, {2, 62}},
                {{1, 0}, {1, 2}, {1, 15}, {2, 8}, {2, 27}, {2, 49}},
                {{0, 0}, {0, 3}, {0, 22}, {1, 54}, {2, 13}, {2, 40}},
                {{0, 0}, {0, 36}, {0, 40}, {1, 31}, {1, 34}, {2, 5}},
                {{0, 0}, {0, 50}, {0, 55}, {1, 6}, {1, 24}, {2, 26}},
                {{0, 0}, {0, 2}, {1, 3}, {1, 14}, {1, 35}, {2, 25}}
        };
        UnaryOperator<int[]> alpha201 = arr -> new int[]{arr[0], c67.mul(arr[1], 29)};
        UnaryOperator<int[]> beta201 = arr -> new int[]{arr[0], c67.op(arr[1], 1)};
        BitSet[] blocks1 = Arrays.stream(base3).flatMap(arr -> c3.elements().boxed().flatMap(a -> c67.elements().mapToObj(b -> {
            BitSet result = new BitSet();
            for (int[] base : arr) {
                result.set(pr201.fromArr(apply(beta201, apply(alpha201, base, a), b)));
            }
            return result;
        }))).collect(Collectors.toSet()).toArray(BitSet[]::new);
        Liner p5 = new Liner(blocks1);
        assertEquals(of(34), p5.playfairIndex());
        assertEquals(of(1, 2, 3, 4), p5.hyperbolicIndex());

        CyclicGroup c97 = new CyclicGroup(97);
        CyclicProduct pr291 = new CyclicProduct(3, 97);
        int[][][] base7 = new int[][][] {
                {{0, 1}, {0, 35}, {0, 61}, {1, 38}, {1, 69}, {1, 87}},
                {{0, 0}, {1, 0}, {2, 0}, {2, 1}, {2, 35}, {2, 61}},
                {{0, 0}, {0, 20}, {2, 73}, {2, 77}, {2, 80}, {2, 89}},
                {{1, 0}, {1, 50}, {1, 57}, {2, 56}, {2, 79}, {2, 84}},
                {{0, 0}, {0, 42}, {0, 53}, {1, 66}, {2, 15}, {2, 56}},
                {{0, 0}, {0, 13}, {0, 18}, {1, 40}, {1, 93}, {2, 49}},
                {{0, 0}, {0, 45}, {0, 46}, {0, 73}, {1, 19}, {2, 23}},
                {{0, 0}, {0, 43}, {1, 35}, {1, 57}, {1, 76}, {2, 27}},
                {{0, 0}, {0, 22}, {1, 21}, {1, 32}, {2, 17}, {2, 65}},
                {{0, 0}, {1, 38}, {1, 39}, {1, 48}, {1, 65}, {2, 90}},
                {{0, 0}, {1, 23}, {1, 46}, {2, 9}, {2, 26}, {2, 51}}
        };
        UnaryOperator<int[]> alpha291 = arr -> new int[]{arr[0], c97.mul(arr[1], 35)};
        UnaryOperator<int[]> beta291 = arr -> new int[]{arr[0], c97.op(arr[1], 1)};
        BitSet[] blocks4 = Arrays.stream(base7).flatMap(arr -> c3.elements().boxed().flatMap(a -> c97.elements().mapToObj(b -> {
            BitSet result = new BitSet();
            for (int[] base : arr) {
                result.set(pr291.fromArr(apply(beta291, apply(alpha291, base, a), b)));
            }
            return result;
        }))).collect(Collectors.toSet()).toArray(BitSet[]::new);
        Liner p10 = new Liner(blocks4);
        assertEquals(of(52), p10.playfairIndex());
        assertEquals(of(1, 2, 3, 4), p10.hyperbolicIndex());
    }

    @Test
    public void testRotational() {
        CyclicProduct gp = new CyclicProduct(3, 6);
        int[][][] base19 = new int[][][] {
                {{0, 0}, {1, 0}, {2, 0}},
                {{0, 0}, {1, 2}, {2, 4}},
                {{0, 0}, {0, 1}, {1, 5}},
                {{0, 0}, {0, 2}, {1, 3}}
        };
        BitSet[] lines19 = Stream.concat(Arrays.stream(base19).flatMap(arr -> {
            int[] bl = Arrays.stream(arr).mapToInt(gp::fromArr).toArray();
            return IntStream.range(0, gp.order()).mapToObj(s -> {
               BitSet result = new BitSet();
               for (int i : bl) {
                   result.set(gp.op(i, s));
               }
               return result;
            });
        }), IntStream.range(0, gp.order()).mapToObj(s -> {
            BitSet result = new BitSet();
            result.set(gp.order());
            result.set(gp.op(gp.fromArr(0, 0), s));
            result.set(gp.op(gp.fromArr(0, 3), s));
            return result;
        })).collect(Collectors.toSet()).toArray(BitSet[]::new);
        Liner pl19 = new Liner(lines19);
        assertEquals(of(0, 1), pl19.hyperbolicIndex());

        SemiDirectProduct sdp24 = new SemiDirectProduct(new CyclicGroup(12), new CyclicGroup(2), 7);
        int[][][] base24 = new int[][][] {
                {{0, 0}, {6, 0}, {0, 1}, {6, 1}},
                {{0, 0}, {1, 0}, {3, 1}, {10, 1}},
                {{0, 0}, {2, 0}, {5, 0}, {1, 1}}
        };
        BitSet[] lines24 = Stream.concat(Arrays.stream(base24).flatMap(arr -> {
            int[] bl = Arrays.stream(arr).mapToInt(pair -> sdp24.op(sdp24.fromAB(0, pair[1]), sdp24.fromAB(pair[0], 0))).toArray();
            return IntStream.range(0, sdp24.order()).mapToObj(s -> {
                BitSet result = new BitSet();
                for (int i : bl) {
                    result.set(sdp24.op(i, s));
                }
                return result;
            });
        }), IntStream.range(0, sdp24.order()).mapToObj(s -> {
            BitSet result = new BitSet();
            result.set(sdp24.order());
            result.set(sdp24.op(sdp24.fromAB(0, 0), s));
            result.set(sdp24.op(sdp24.fromAB(4, 0), s));
            result.set(sdp24.op(sdp24.fromAB(8, 0), s));
            return result;
        })).collect(Collectors.toSet()).toArray(BitSet[]::new);
        Liner pl25 = new Liner(lines24);
        assertEquals(of(1, 2), pl25.hyperbolicIndex());

        SemiDirectProduct sdp42 = new SemiDirectProduct(new CyclicGroup(14), new CyclicGroup(3), 11);
        int[][][] base42 = new int[][][] {
                {{0, 0}, {0, 1}, {0, 2}},
                {{0, 0}, {2, 1}, {6, 2}},
                {{0, 0}, {1, 0}, {4, 1}},
                {{0, 0}, {2, 0}, {9, 1}},
                {{0, 0}, {3, 0}, {13, 1}},
                {{0, 0}, {4, 0}, {12, 1}},
                {{0, 0}, {5, 0}, {6, 1}},
                {{0, 0}, {6, 0}, {11, 1}}
        };
        BitSet[] lines42 = Stream.concat(Arrays.stream(base42).flatMap(arr -> {
            int[] bl = Arrays.stream(arr).mapToInt(pair -> sdp42.op(sdp42.fromAB(0, pair[1]), sdp42.fromAB(pair[0], 0))).toArray();
            return IntStream.range(0, sdp42.order()).mapToObj(s -> {
                BitSet result = new BitSet();
                for (int i : bl) {
                    result.set(sdp42.op(i, s));
                }
                return result;
            });
        }), IntStream.range(0, sdp42.order()).mapToObj(s -> {
            BitSet result = new BitSet();
            result.set(sdp42.order());
            result.set(sdp42.op(sdp42.fromAB(0, 0), s));
            result.set(sdp42.op(sdp42.fromAB(7, 0), s));
            return result;
        })).collect(Collectors.toSet()).toArray(BitSet[]::new);
        Liner pl43 = new Liner(lines42);
        assertEquals(of(43), pl43.cardSubPlanes(true));
        assertEquals(of(0, 1), pl43.hyperbolicIndex());
    }

    @Test
    public void testOvals() {
        int q = 19;
        GaloisField fd = new GaloisField(q);
        Liner pl = new Liner(fd.generatePlane());
        BitSet set = new BitSet();
        IntStream.range(0, q).forEach(i -> set.set(i * q + fd.mul(i, i)));
        set.set(q * q + q);
        int[] oval = set.stream().toArray();
        Map<Integer, int[]> tangents = new HashMap<>();
        for (int p : oval) {
            int t = -1;
            q: for (int l : pl.lines(p)) {
                for (int p1 : oval) {
                    if (p != p1 && pl.flag(l, p1)) {
                        continue q;
                    }
                }
                t = l;
            }
            tangents.put(p, pl.line(t));
        }
        BitSet pts = new BitSet();
        pts.set(0, q * q + q + 1);
        tangents.values().forEach(t -> Arrays.stream(t).forEach(i -> pts.set(i, false)));
        Liner hyp = pl.subPlane(pts.stream().toArray());
        System.out.println(hyp.isRegular());
        System.out.println(hyp.pointCount());
        System.out.println(hyp.cardSubPlanes(true));
        BitSet bs = new BitSet();
        System.out.println(hyp.hull(0, 1, 3).cardinality());
        for (int p1 : hyp.points(hyp.line(0, 1))) {
            for (int p2 : hyp.points(hyp.line(0, 3))) {
                if (p1 == p2) {
                    continue;
                }
                Arrays.stream(hyp.line(hyp.line(p1, p2))).forEach(bs::set);
            }
        }
        System.out.println(bs.cardinality());
        System.out.println(hyp.hyperbolicIndex());
        System.out.println(hyp.playfairIndex());
    }

    @Test
    public void testOvoids() {
        int q = 7;
        GaloisField fd = new GaloisField(q);
        int a = fd.elements().filter(o -> fd.elements().noneMatch(x -> fd.add(fd.mul(x, x), x, o) == 0)).findAny().orElseThrow();
        Liner sp = new Liner(fd.generateSpace());
        BitSet set = new BitSet();
        IntStream.range(0, q).forEach(s -> IntStream.range(0, q).forEach(t -> set.set(
                q * q * fd.add(fd.mul(s, s), fd.mul(s, t), fd.mul(a, t, t)) + t * q + s)));
        set.set(q * q * q);
        int[] ovoid = set.stream().toArray();
        Map<Integer, List<int[]>> tangents = new HashMap<>();
        for (int p : ovoid) {
            q: for (int l : sp.lines(p)) {
                for (int p1 : ovoid) {
                    if (p != p1 && sp.flag(l, p1)) {
                        continue q;
                    }
                }
                tangents.computeIfAbsent(p, p1 -> new ArrayList<>()).add(sp.line(l));
            }
        }
        BitSet pts = new BitSet();
        pts.set(0, q * q * q + q * q + q + 1);
        System.out.println(pts.cardinality());
        for (int x = 0; x < sp.pointCount(); x++) {
            for (int y = x + 1; y < sp.pointCount(); y++) {
                for (int z = y + 1; z < sp.pointCount(); z++) {
                    if (sp.line(x, y) == sp.line(y, z)) {
                        continue;
                    }
                    BitSet bs = sp.hull(x, y, z);
                    bs.and(set);
                }
            }
        }
        tangents.forEach((k, v) -> System.out.println(k + " " + v.size()));
        tangents.values().forEach(l -> l.forEach(t -> Arrays.stream(t).forEach(i -> pts.set(i, false))));
        System.out.println(pts.cardinality());
    }

    @Test
    public void testRecursivePlane() {
        int q = 7;
        GaloisField fd = new GaloisField(q);
        Liner aff = new Liner(fd.generatePlane()).subPlane(IntStream.range(0, q * q).toArray());
        Liner subPl = Liner.byStrings("0001123", "1242534", "3654656");
        //HyperbolicPlane subPl = new HyperbolicPlane("00000011111222223334445556", "13579b3469a3467867868a7897", "2468ac578bc95acbbacc9bbac9");
        Set<BitSet> lines = new LinkedHashSet<>();
        for (int l = 0; l < aff.lineCount(); l++) {
//            List<Integer> pts = new ArrayList<>(Arrays.asList(aff.line(l).stream().boxed().toArray(Integer[]::new)));
//            Collections.shuffle(pts);
//            Integer[] line = pts.toArray(Integer[]::new);
            int[] line = aff.line(l);
            for (int i = 0; i < q; i++) {
                for (int j = i + 1; j < q; j++) {
                    int[] sl = subPl.line(subPl.line(i, j));
                    int[] nl = new int[sl.length];
                    for (int k = 0; k < nl.length; k++) {
                        nl[k] = line[sl[k]];
                    }
                    lines.add(of(nl));
                }
            }
        }
        Liner res = new Liner(lines.toArray(BitSet[]::new));
        System.out.println(res.cardSubPlanesFreq());
    }

    @Test
    public void testDirectProduct() {
        Liner tr11 = Liner.byStrings("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a");
        Liner tr7 = Liner.byStrings("0", "1", "2", "3", "4", "5", "6");
        Liner pl91 = Liner.byDiffFamily(91, new int[][]{{0, 8, 29, 51, 54, 61, 63}, {0, 11, 16, 17, 31, 35, 58}, {0, 13, 26, 39, 52, 65, 78}});
        Liner tr5 = Liner.byStrings("0", "1", "2", "3", "4");
        Liner tr1 = Liner.byStrings("0", "1", "2", "3");
        Liner prj4 = Liner.byStrings("0000111223345", "1246257364789", "385a46b57689a", "9c7ba8cb9cabc");
        Liner uni = Liner.byStrings("0000000001111111122222222333333334444455556666777788899aabbcgko",
                "14567ghij4567cdef456789ab456789ab59adf8bce9bcf8ade9decfdfcedhlp",
                "289abklmnba89lknmefdchgjijighfecd6klhilkgjnmhjmngiajgihigjheimq",
                "3cdefopqrghijrqopqrponmklporqklmn7romnqpnmqoklrplkbopporqqrfjnr");
        Liner tr = Liner.byStrings("0", "1", "2");
        Liner prj = Liner.byStrings("0001123", "1242534", "3654656");
        Liner aff = Liner.byStrings("000011122236", "134534534547", "268787676858");
        Liner p13 = Liner.byStrings("00000011111222223334445556", "13579b3469a3467867868a7897", "2468ac578bc95acbbacc9bbac9");
        Liner p13a = Liner.byStrings("00000011111222223334445556", "13579b3469a3467867868a7897", "2468ac578bc95abcbcac9babc9");
        Liner p15aff = Liner.byStrings("00000001111112222223333444455566678", "13579bd3469ac34578b678a58ab78979c9a", "2468ace578bde96aecdbcded9cebecaeddb");
        Liner prod = pl91.directProduct(tr7);
        System.out.println(prod.cardSubPlanes(false));
        //System.out.println(prod.cardSubSpaces(false));
    }

    @Test
    public void generateArc() {
        int q = 16;
        int n = 8;
        int k = (q + 1) * (n - 1) + 1;
        GaloisField fd = new GaloisField(q);
        Liner pl = new Liner(fd.generatePlane());
        BitSet pts = generatePts(fd, q, n, k);
//        System.out.println(pts);
//        List<BitSet> lines = new ArrayList<>();
//        for (int l : pl.lines()) {
//            BitSet line = of(pl.line(l).stream().filter(p -> pts.get(p)).toArray());
//            if (line.cardinality() > 1) {
//                lines.add(line);
//            }
//        }
//        System.out.println(lines);
        Liner arc = pl.subPlane(pts.stream().toArray());
        assertEquals(k, arc.pointCount());
        System.out.println(arc.isRegular());
        System.out.println(arc.hyperbolicIndex());
    }

    private static BitSet generatePts(GaloisField fd, int q, int n, int k) {
        for (int a = 1; a < q; a++) {
            for (int b = 1; b < q; b++) {
                for (int c = 0; c < q; c++) {
                    BitSet pts = new BitSet();
                    for (int i = 0; i < q * q; i++) {
                        int x = i / q;
                        int y = i % q;
                        int qf = fd.add(fd.mul(a, x, x), fd.mul(c, x, y), fd.mul(b, y, y));
                        if (qf < n) {
                            pts.set(i);
                        }
                    }
                    if (pts.cardinality() == k) {
                        return pts;
                    }
                }
            }
        }
        throw new IllegalArgumentException();
    }

    @Test
    public void testBurnside() {
        BurnsideGroup bg = new BurnsideGroup();
        BitSet[] lines = IntStream.range(0, bg.order()).boxed().flatMap(x -> IntStream.range(x + 1, bg.order()).mapToObj(y -> {
            BitSet result = new BitSet();
            result.set(x);
            result.set(y);
            result.set(bg.op(bg.op(x, bg.inv(y)), x));
            return result;
        })).collect(Collectors.toSet()).toArray(BitSet[]::new);
        Liner bp = new Liner(lines);
        assertEquals(of(1), bp.hyperbolicIndex());
        assertEquals(of(9), bp.cardSubPlanes(true));
        assertEquals(of(bp.pointCount()), bp.cardSubSpaces(true));
    }

    private int operator(GaloisField fd, int x, int a, int b, int alpha) {
        return fd.add(fd.mul(a, a, x), b);
    }

    @Test
    public void generateNetto() {
        int q = 79; // prime number 12x + 7
        GaloisField fd = new GaloisField(q);
        int eps = fd.oneRoots(6).filter(i -> fd.add(fd.mul(i, i), fd.neg(i), 1) == 0).findFirst().orElseThrow();
        int alpha = IntStream.range(2, fd.cardinality()).filter(i -> fd.expOrder(i) == fd.cardinality() - 1).findAny().orElseThrow();
        Set<BitSet> lines = new HashSet<>();
        for (int a = 1; a < fd.cardinality(); a++) {
            for (int b = 0; b < fd.cardinality(); b++) {
                BitSet bs = new BitSet();
                bs.set(operator(fd, 0, a, b, alpha));
                bs.set(operator(fd, 1, a, b, alpha));
                bs.set(operator(fd, eps, a, b, alpha));
                lines.add(bs);
            }
        }
        Liner pl = new Liner(lines.toArray(BitSet[]::new));
        System.out.println(pl.hyperbolicIndex());
        System.out.println(pl.cardSubPlanes(true));
    }

    @Test
    public void generateDerived() {
        int ord = 7;
        List<BitSet> lines = new ArrayList<>();
        int max = (1 << ord) - 1;
        for (int i = 0; i < max; i++) {
            for (int j = i + 1; j < max; j++) {
                for (int k = j + 1; k < max; k++) {
                    if (((i ^ j) ^ (k ^ max)) == 0) {
                        lines.add(of(i, j, k));
                    }
                }
            }
        }
        Liner sp = new Liner(lines.toArray(BitSet[]::new));
        BitSet[][] planes = new BitSet[max][max / 6];
        for (int curr = 0; curr < max; curr++) {
            beg: for (int idx = findFirstNull(planes[curr]); idx < max / 6; idx++) {
                for (int x = curr + 1; x < max; x++) {
                    for (int y = x + 1; y < max; y++) {
                        if (sp.line(x, y) == sp.line(y, curr)) {
                            continue;
                        }
                        BitSet pl = sp.hull(x, y, curr);
                        if (Arrays.stream(planes).flatMap(arr -> Arrays.stream(arr).filter(Objects::nonNull)).noneMatch(p -> {
                            BitSet bs = (BitSet) pl.clone();
                            bs.and(p);
                            return bs.cardinality() != 1;
                        })) {
                            for (int p : pl.stream().toArray()) {
                                int pIdx = findFirstNull(planes[p]);
                                planes[p][pIdx] = pl;
                            }
                            continue beg;
                        }
                    }
                }
                throw new IllegalStateException(curr + " " + idx + " " + Arrays.deepToString(planes));
            }
        }
        System.out.println(Arrays.deepToString(planes));
    }

    private int findFirstNull(Object[] arr) {
        return IntStream.range(0, arr.length).filter(i -> arr[i] == null).findFirst().orElse(arr.length);
    }

    @Test
    public void testResidue() {
        int q = 3;
        GaloisField fd = new GaloisField(q);
        Liner sp = new Liner(fd.generateSpace());
        Liner res = new Liner(sp.pointResidue(0));
        assertEquals(q * q + q + 1, res.pointCount());
        System.out.println(Arrays.toString(TaoPoint.toPlane().pointResidue(0)));
        Liner hallResidue = new Liner(HallPoint.toPlane().pointResidue(0));
        System.out.println(hallResidue.cardSubPlanesFreq());
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet();
        IntStream.of(values).forEach(bs::set);
        return bs;
    }
}
