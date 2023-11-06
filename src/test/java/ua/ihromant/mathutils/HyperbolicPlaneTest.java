package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class HyperbolicPlaneTest {
    @Test
    public void hyperbolicPlaneExample() {
        HyperbolicPlane p2 = new HyperbolicPlane(new String[]{
                "00000001111112222223333444455566678",
                "13579bd3469ac34578b678a58ab78979c9a",
                "2468ace578bde96aecdbcded9cebecaeddb"
        });
        assertEquals(15, p2.pointCount());
        assertEquals(35, p2.lineCount());
        testCorrectness(p2, of(3), 7);
        assertEquals(of(4), p2.playfairIndex());
        assertEquals(of(1), p2.hyperbolicIndex());
        checkPlane(p2, p2.pointCount(), p2.pointCount());

        HyperbolicPlane p1 = new HyperbolicPlane(new String[]{
                "0000000001111111122222222333333334444455556666777788899aabbcgko",
                "14567ghij4567cdef456789ab456789ab59adf8bce9bcf8ade9decfdfcedhlp",
                "289abklmnba89lknmefdchgjijighfecd6klhilkgjnmhjmngiajgihigjheimq",
                "3cdefopqrghijrqopqrponmklporqklmn7romnqpnmqoklrplkbopporqqrfjnr"});
        assertEquals(28, p1.pointCount());
        assertEquals(63, p1.lineCount());
        testCorrectness(p1, of(4), 9);
        assertEquals(of(5), p1.playfairIndex());
        assertEquals(of(2), p1.hyperbolicIndex());
        checkPlane(p1, p1.pointCount(), p1.pointCount());


        HyperbolicPlane p = new HyperbolicPlane(217, new int[]{0,1,37,67,88,92,149}, new int[]{0,15,18,65,78,121,137}, new int[]{0,8,53,79,85,102,107},
                new int[]{0,11,86,100,120,144,190}, new int[]{0,29,64,165,198,205,207}, new int[]{0,31,62,93,124,155,186});
        assertEquals(217, p.pointCount());
        assertEquals(1116, p.lineCount());
        testCorrectness(p, of(7), 36);
        assertEquals(of(29), p.playfairIndex());
        assertEquals(of(2, 3, 4, 5), p.hyperbolicIndex());
        checkPlane(p, p.pointCount(), p.pointCount());
    }

    @Test
    public void checkNotPlane() {
        HyperbolicPlane p = new HyperbolicPlane("00000001111112222223333444455556666",
                "13579bd3478bc3478bc789a789a789a789a",
                "2468ace569ade65a9edbcdecbeddebcedcb");
        assertEquals(15, p.pointCount());
        assertEquals(35, p.lineCount());
        testCorrectness(p, of(3), 7);
        assertEquals(of(0, 0), p.hyperbolicIndex());
        checkPlane(p, 7, 7); // it's model of 3-dimensional projective space
        checkSpace(p, p.pointCount(), p.pointCount());

        HyperbolicPlane p3 = new HyperbolicPlane("00000001111112222223333444455556666",
                "13579bd3478bc3478bc789a789a789a789a",
                "2468ace569ade65a9edbcdecbededcbdebc");
        assertEquals(15, p3.pointCount());
        assertEquals(35, p3.lineCount());
        testCorrectness(p3, of(3), 7);
        assertEquals(of(0, 1), p3.hyperbolicIndex());
        checkPlane(p3, 7, p.pointCount()); // it's plane with no exchange property

        HyperbolicPlane p1 = new HyperbolicPlane(31, new int[]{0, 1, 12}, new int[]{0, 2, 24},
                new int[]{0, 3, 8}, new int[]{0, 4, 17}, new int[]{0, 6, 16});
        testCorrectness(p1, of(3), 15);
        assertEquals(of(0), p1.hyperbolicIndex());
        checkPlane(p1, 7, 7); // 4-dimensional projective space
        checkSpace(p1, 15, 15); // 4-dimensional projective space
    }

    @Test
    public void testCyclic() {
        CyclicGroup cg1 = new CyclicGroup(11, 11);
        int[][][] cycles = new int[][][]{
                {{0, 0}, {0, 3}, {0, 4}, {1, 1}, {1, 7}, {4, 6}},
                {{0, 0}, {0, 2}, {2, 5}, {4, 7}, {6, 4}, {8, 0}},
                {{0, 0}, {1, 5}, {2, 0}, {4, 1}, {6, 0}, {7, 2}},
                {{0, 0}, {1, 0}, {3, 9}, {4, 8}, {6, 1}, {9, 5}}
        };
        BitSet[] lines = Arrays.stream(cycles).flatMap(base -> IntStream.range(0, cg1.cardinality()).mapToObj(idx -> {
            BitSet res = new BitSet();
            for (int[] numb : base) {
                res.set(cg1.add(cg1.fromArr(numb), idx));
            }
            return res;
        })).collect(Collectors.toSet()).toArray(BitSet[]::new);
        HyperbolicPlane p1 = new HyperbolicPlane(lines);
        assertEquals(121, p1.pointCount());
        assertEquals(484, p1.lineCount());
        testCorrectness(p1, of(6), 24);
        assertEquals(of(18), p1.playfairIndex());
        assertEquals(of(1, 2, 3, 4), p1.hyperbolicIndex());

        CyclicGroup cg2 = new CyclicGroup(7, 5, 5);

        cycles = new int[][][] {
                {{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {3, 0, 0}, {4, 0, 0}, {5, 0, 0}, {6, 0, 0}},
                {{0, 0, 0}, {1, 1, 3}, {1, 4, 2}, {2, 2, 2}, {2, 3, 3}, {4, 2, 0}, {4, 3, 0}},
                {{0, 0, 0}, {1, 3, 4}, {1, 2, 1}, {2, 2, 2}, {2, 3, 2}, {4, 0, 2}, {4, 0, 3}},
                {{0, 0, 0}, {1, 1, 2}, {1, 4, 3}, {2, 1, 4}, {2, 4, 4}, {4, 0, 1}, {4, 0, 4}},
                {{0, 0, 0}, {1, 3, 1}, {1, 2, 4}, {2, 4, 1}, {2, 1, 4}, {4, 1, 0}, {4, 4, 0}}
        };
        lines = Arrays.stream(cycles).flatMap(base -> IntStream.range(0, cg2.cardinality()).mapToObj(idx -> {
            BitSet res = new BitSet();
            for (int[] numb : base) {
                res.set(cg2.add(cg2.fromArr(numb), idx));
            }
            return res;
        })).collect(Collectors.toSet()).toArray(BitSet[]::new);
        p1 = new HyperbolicPlane(lines);
        assertEquals(175, p1.pointCount());
        assertEquals(725, p1.lineCount());
        testCorrectness(p1, of(7), 29); // this fails, example is broken
        assertEquals(of(22), p1.playfairIndex());
        assertEquals(of(1, 2, 3, 4), p1.hyperbolicIndex());


        CyclicGroup cg = new CyclicGroup(7, 37);
        int[][] microBase = new int[][] {{1, 1}, {2, 10}, {4, 26}};
        cycles = Stream.concat(Stream.<int[][]>of(new int[][]{{0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0}, {6, 0}}),
                Arrays.stream(microBase).flatMap(arr -> Stream.of(
                        new int[][]{{0, 0}, cg.arrMul(arr, new int[]{0, 1}), cg.arrMul(arr, new int[]{0, 6}), cg.arrMul(arr, new int[]{1, 4}),
                                cg.arrMul(arr, new int[]{2, 19}), cg.arrMul(arr, new int[]{3, 25}), cg.arrMul(arr, new int[]{6, 25})},
                        new int[][]{{0, 0}, cg.arrMul(arr, new int[]{0, 4}), cg.arrMul(arr, new int[]{1, 25}), cg.arrMul(arr, new int[]{1, 34}),
                                cg.arrMul(arr, new int[]{2, 24}), cg.arrMul(arr, new int[]{2, 35}), cg.arrMul(arr, new int[]{4, 10})}))).toArray(int[][][]::new);
        lines = Arrays.stream(cycles).flatMap(base -> IntStream.range(0, cg.cardinality()).mapToObj(idx -> {
            BitSet res = new BitSet();
            for (int[] numb : base) {
                res.set(cg.add(cg.fromArr(numb), idx));
            }
            return res;
        })).collect(Collectors.toSet()).toArray(BitSet[]::new);
        p1 = new HyperbolicPlane(lines);
        assertEquals(259, p1.pointCount());
        assertEquals(1591, p1.lineCount());
        testCorrectness(p1, of(7), 43); // this fails, example is broken
        assertEquals(of(18), p1.playfairIndex());
        assertEquals(of(1, 2, 3, 4), p1.hyperbolicIndex());
    }

    @Test
    public void testPrimePower() {
        HyperbolicPlane p4 = new HyperbolicPlane(109, new int[]{0, 1, 3, 60});
        assertEquals(109, p4.pointCount());
        assertEquals(981, p4.lineCount());
        testCorrectness(p4, of(4), 36);
        assertEquals(of(32), p4.playfairIndex());
        assertEquals(of(1, 2), p4.hyperbolicIndex());

        GaloisField fd1 = new GaloisField(121);
        int x = fd1.solve(new int[]{1, 3, 8}).findAny().orElseThrow();
        HyperbolicPlane p3 = new HyperbolicPlane(fd1.cardinality(), new int[]{0, 1, x, fd1.power(x, 10)});
        assertEquals(121, p3.pointCount());
        assertEquals(1210, p3.lineCount());
        testCorrectness(p3, of(4), 40);
        assertEquals(of(36), p3.playfairIndex());
        assertEquals(of(0, 1, 2), p3.hyperbolicIndex());

        GaloisField fd = new GaloisField(421);
        int c1 = 1;
        int c2 = 4;
        int w = fd.oneCubeRoots().findAny().orElseThrow();
        HyperbolicPlane p = new HyperbolicPlane(fd.cardinality(),
                new int[] {0, c1, fd.mul(c1, w), fd.mul(c1, fd.mul(w, w)), c2, fd.mul(c2, w), fd.mul(c2, fd.mul(w, w))});
        assertEquals(421, p.pointCount());
        assertEquals(4210, p.lineCount());
        testCorrectness(p, of(7), 70);
        assertEquals(of(63), p.playfairIndex());
        assertEquals(of(2, 3, 4, 5), p.hyperbolicIndex());

        HyperbolicPlane p1 = new HyperbolicPlane(433, new int[]{0, 1, 3, 30, 52, 61, 84, 280, 394});
        assertEquals(433, p1.pointCount());
        assertEquals(2598, p1.lineCount());
        testCorrectness(p1, of(9), 54);
        assertEquals(of(45), p1.playfairIndex());
        assertEquals(of(2, 3, 4, 5, 6, 7), p1.hyperbolicIndex());

        HyperbolicPlane p2 = new HyperbolicPlane(449, new int[]{0, 1, 3, 8, 61, 104, 332, 381});
        assertEquals(449, p2.pointCount());
        assertEquals(3592, p2.lineCount());
        testCorrectness(p2, of(8), 64);
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

    @Test
    public void nonStandard() {
        CyclicGroup cg = new CyclicGroup(13, 5);
        HyperbolicPlane p7 = new HyperbolicPlane(Stream.concat(Stream.of(
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
                        result.set(cg.add(i, cg.fromArr(idx, 0)));
                    }
                    return result;
                })), IntStream.range(0, 13).mapToObj(idx -> {
            BitSet result = new BitSet();
            for (int i = 0; i < 5; i++) {
                result.set(cg.fromArr(idx, i));
            }
            result.set(cg.cardinality());
            return result;
        })).toArray(BitSet[]::new));
        assertEquals(cg.cardinality() + 1, p7.pointCount());
        assertEquals(143, p7.lineCount());
        testCorrectness(p7, of(6), 13);
        assertEquals(of(7), p7.playfairIndex());
        assertEquals(of(0, 1, 2, 3, 4), p7.hyperbolicIndex());
        checkPlane(p7, p7.pointCount(), p7.pointCount());

        CyclicGroup cg1 = new CyclicGroup(19, 4);
        HyperbolicPlane p9 = new HyperbolicPlane(Stream.of(
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
                        result.set(cg1.add(i, cg1.fromArr(idx, 0)));
                    }
                    return result;
                })).toArray(BitSet[]::new));
        assertEquals(cg1.cardinality(), p9.pointCount());
        assertEquals(190, p9.lineCount());
        testCorrectness(p9, of(6), 15);
        assertEquals(of(9), p9.playfairIndex());
        assertEquals(of(0, 1, 2, 3, 4), p9.hyperbolicIndex());
        checkPlane(p9, p9.pointCount(), p9.pointCount());

        int count = 96;
        HyperbolicPlane p = new HyperbolicPlane(Stream.of(new int[]{0, 16, 32, 48, 64, 80}, new int[]{1, 17, 33, 49, 65, 81},
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
        testCorrectness(p, of(6), 19);
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
        HyperbolicPlane p1 = new HyperbolicPlane(lines);
        assertEquals(count1, p1.pointCount());
        assertEquals(371, p1.lineCount());
        testCorrectness(p1, of(6), 21);
        assertEquals(of(15), p1.playfairIndex());
        assertEquals(of(0, 1, 2, 3, 4), p1.hyperbolicIndex());

        int count2 = 111;
        int[][] blocks = new int[][]{new int[]{0, 3, 9, 21, 51, 1}, new int[]{0, 15, 58, 85, 32, 92},
                new int[]{15, 99, 40, 103, 59, 23}, new int[]{27, 81, 49, 34, 38, 110}, new int[]{30, 69, 79, 25, 5, 20},
                new int[]{39, 72, 58, 55, 17, 98}, new int[]{78, 102, 4, 22, 32, 101}};
        BitSet[] lines1 = Stream.concat(Arrays.stream(blocks).flatMap(arr -> IntStream.range(0, count2 / 3).mapToObj(idx -> {
            BitSet result = new BitSet();
            for (int i : arr) {
                result.set((i + 3 * idx) % count1);
            }
            return result;
        })), Arrays.stream(blocks).flatMap(arr -> IntStream.range(0, count2 / 3).mapToObj(idx -> {
            BitSet result = new BitSet();
            for (int i : arr) {
                switch (i % 3) {
                    case 0 -> result.set(((i / 3 + idx) % 37) * 3);
                    case 1 -> result.set(((i / 3 + 10 * idx) % 37) * 3 + 1);
                    case 2 -> result.set(((i / 3 + 26 * idx) % 37) * 3 + 2);
                }
            }
            return result;
        }))).collect(Collectors.toSet()).toArray(BitSet[]::new);
        System.out.println(lines1.length);
    }

    @Test
    public void test25PointPlanes() {
        String[] batch =
            """
                    00000000111111122222223333344445555666778899aabbil
                    134567ce34578cd34568de468bh679f78ag79b9aabcddecejm
                    298dfbhkea6g9kf7c9afkg5cgfihdgifchi8ejjcjdfhgfghkn
                    iaolgmjnmbohnljonblhmjjdlknmeklnekmkinlimimonooloo
                    0000000011111112222222333334444555566667778889abil
                    13457bce34589cd3456ade489eh6acf7bdg79ab9ab9abdecjm
                    2689gdfka76fekg798fckh5cfgidghichfi8chjjdfgjehfgkn
                    iloahnjmbmohlnjobngmljjdknmeklnekmlkinmnilmliooooo
                    0000000011111112222222333334444555566667778889abil
                    13457bde34589ce3456acd489eh6acf7bdg79ab9ab9abcdejm
                    2689gcfka76fdkg798fehk5cgfidhgicfhi8hcjjfdejgfghkn
                    iloahmjnbmohnljobngljmjdkmneknleklmkminlniimlooooo
                    0000000011111112222222333334444555566667778889abil
                    13457aef3458bcf34569dg489dh69ef7acg79bc9acabdcdejm
                    2689bcjha769djg7b8aejh5cbfkdagkebhk8gieihdifefghkn
                    ilodgknmemohklnocnfkmljgminhnilflimkjnmljnmjlooooo
                    00000000111111122222223333344445555666778899abcfil
                    134567cd34578de34568ce468ag67bh789f79aab9bacdedgjm
                    298abhgkba69fhk79bgakf5cdfiedgicehi8djejjcbfghehkn
                    ieolfmjnmcognjlondlhmjjhlknmfklngkmkinilmiolmnoooo
                    000000001111111222222333334444555566667778899abcde
                    123468cj23479af3458bg456ah57bi78bd89ce9adabacghiff
                    5ad97fek6be8gdl7ch9em89fcn6gdkfickgjdlhmeekblhijjg
                    oignbhmlkjhcinmlfjdonmeikoajlonlgmomhnkoijnfolmnok
                    000000001111111222222333333444445555566666777889al
                    124789bh2589ace39abde47abcf8bcde79cdf78adg89b9cabm
                    365egifj46fhjgi5gikhf6ihjegjikfggkjehfhekiadcbdcdn
                    dcaolmnk7bolmnk8olmnj9nolmknolmhmnolilmnojkjheifgo
                    000000001111111222222333333444455566667777889aacee
                    12459bdf24569cg3458bh4568bi59bh9ac78ab89cgaddbedfl
                    3786cihjd78haek96kfgja7fgclgkdiimfi9djebkjcjffhmgn
                    oanegklmifbmljnecolmnjdkhnmlmeojnhnoglmhloiknokoio
                    000000001111111222222233333444455556677889abcdefgh
                    13456789345678a34567894679c67ad68be9aab9bdecijkijk
                    2fbcdgiadg9jcfbaehficb5b8eg89ch7adfchdfgefghmlllmn
                    onklehjmmlikehnjnmgkdloilhkmjfinkgjolomnokijnnmooo
                    000000001111111222222233333444455566577889abccdfgh
                    13456789345678a345678b467ad679e69c9b89aabfghdeeijk
                    2jadebchekbdc9f9ciaedg5f8bg8gbf7afcihjdiemllikjlmn
                    onmkgfliilnmhgjljmhnfkokmchnidhlegojjkokonnmnmlooo
                    0000000011111112222222333334444555566778899acdefgh
                    1345678b3456789345678a467bc679d68ae9daebcabbkijijk
                    2iace9fgdjbgcah9cibhdf5a8fg8bfh79gfcfdgehedclmllmn
                    ojmdhnklekniflmlekmjgnokmhnniglljhmojokoiikjmnnooo
                    0000000011111112222222333334444555566778899accdfgh
                    1345678a345678b34567894679e67ac68bd9fagbfabbdeeijk
                    2j9ebcmickal9dibdielaj5a8df8beg79chcgdhehgfhjiklmn
                    olhgdfnkhmfnegjgfnhmckomkininjljlkmokoiojnmlnmlooo
                    0000000011111112222222333334444555566677899accdfgh
                    134578ae34568bc345679d467ad78be689c7898ababbdeeijk
                    2f6b9cjg9g7dakh8ahbeif5lbigl9ifamjffhcgdegfhikjlmn
                    omkdhlnienimfljjclgnmkonckhmdjhenkgjiokoolnmnmlooo
                    0000000011111112222222333334444555566677899accdfgh
                    1345789c34568ad34567be467ad78be689c7898ababbdeeijk
                    2h6beajg9f7bckh8agd9if5lbifl9jgamiffhcgdegfhjiklmn
                    omidnflkenjglmikclmhnjoncjgmdkhenkhkjoioonmlnmlooo
                    0000000011111112222222333334444555566677889abcdeil
                    1345689b345679a34578ab467ad789e689c7bc9daeghffghjm
                    2cg7afdi8dhfbjcf6eg9kc59ebhacbfdbag8ehcfdgijkijkkn
                    lnkjohemknigomejinhomdmligoljhoklfonjmkmimnnnllloo
                    0000000011111112222222333334444555566677889abcdeil
                    134568ab345679b345789a467ae789c689d7be9cadfghfghjm
                    2cf7g9dk8dgafich6ebfcj59dbgaebhcbaf8dfegchkijijkkn
                    lnjihoemjnkohmeiknogdmmlkfoligojlhonimjmkmnnnllloo
                    0000000011111112222222333333344445555666677778889a
                    147adgjm4569chi4569bdf45689ae9bcf9ach89abbcdeabicd
                    258behkn78ebfkl87caekgdb7cfglgkehefgjfildfighegjdj
                    369cfiloadgjmnonlohimjimjhoknmojlkinoknmhnkomolmln
                    0000000011111112222222333333344445555666677778889a
                    147adgjm4569cef4569bcd45689ae9bdf9abe89acbcdiabcfd
                    258behkn78bhkil87jafghhc7jbigcekggfihelhdheglgkfij
                    369cfiloadgjmnoikoemnlmlfndokolnjmnjomnkinjomlohkm
                    """.split("\n");
        for (int i = 0; i < batch.length / 4; i++) {
            HyperbolicPlane p = new HyperbolicPlane(batch[i * 4], batch[i * 4 + 1], batch[i * 4 + 2], batch[i * 4 + 3]);
            assertEquals(25, p.pointCount());
            assertEquals(50, p.lineCount());
            testCorrectness(p, of(4), 8);
            assertEquals(of(4), p.playfairIndex());
            assertEquals(i == 0 ? of(1, 2) : of(0, 1, 2), p.hyperbolicIndex()); // first is hyperaffine
            checkPlane(p, p.pointCount(), p.pointCount());
        }
    }

    @Test
    public void testClosure() {
        HyperbolicPlane p = new HyperbolicPlane("00000011111222223334445556", "13579b3469a3467867868a7897", "2468ac578bc95acbbacc9bbac9");
        // 0, 1, 3, 10
        BitSet set = p.hull(1, 3, 10);
        assertEquals(13, set.cardinality());
    }

    @Test
    public void testPlanesCorrectness() {
        HyperbolicPlane triPoints = new HyperbolicPlane(new int[]{0, 2, 7}, new int[]{0, 1, 4});
        HyperbolicPlane otherTriPoints = new HyperbolicPlane(new int[]{0, 8, 10}, new int[]{0, 1, 6}, new int[]{0, 3, 7});
        HyperbolicPlane fourPoints = new HyperbolicPlane(new int[]{0, 18, 27, 33}, new int[]{0, 7, 24, 36}, new int[]{0, 3, 5, 26});
        HyperbolicPlane otherFourPoints = new HyperbolicPlane(new int[]{0, 33, 34, 39}, new int[]{0, 17, 25, 28}, new int[]{0, 2, 9, 22}, new int[]{0, 19, 23, 37});
        HyperbolicPlane fivePoints = new HyperbolicPlane(new int[]{0, 19, 24, 33, 39}, new int[]{0, 1, 4, 11, 29});
        HyperbolicPlane otherFivePoints = new HyperbolicPlane(new int[]{0, 16, 17, 31, 35}, new int[]{0, 3, 11, 32, 39});
        HyperbolicPlane triFour = new HyperbolicPlane(new int[]{0, 9, 13}, new int[]{0, 1, 3, 8});
        HyperbolicPlane sixPoints = new HyperbolicPlane(new int[]{0, 1, 3, 7, 25, 38}, new int[]{0, 16, 21, 36, 48, 62}, new int[]{0, 30, 40, 63, 74, 82});
        assertEquals(13, triPoints.pointCount());
        assertEquals(26, triPoints.lineCount());
        testCorrectness(triPoints, of(3), 6);
        assertEquals(of(3), triPoints.playfairIndex());
        assertEquals(of(0, 1), triPoints.hyperbolicIndex());
        checkPlane(triPoints, triPoints.pointCount(), triPoints.pointCount());

        assertEquals(19, otherTriPoints.pointCount());
        assertEquals(57, otherTriPoints.lineCount());
        testCorrectness(otherTriPoints, of(3), 9);
        assertEquals(of(6), otherTriPoints.playfairIndex());
        assertEquals(of(0, 1), otherTriPoints.hyperbolicIndex());

        assertEquals(37, fourPoints.pointCount());
        assertEquals(111, fourPoints.lineCount());
        testCorrectness(fourPoints, of(4), 12);
        assertEquals(of(8), fourPoints.playfairIndex());
        assertEquals(of(0, 1, 2), fourPoints.hyperbolicIndex());

        assertEquals(49, otherFourPoints.pointCount());
        assertEquals(196, otherFourPoints.lineCount());
        testCorrectness(otherFourPoints, of(4), 16);
        assertEquals(of(12), otherFourPoints.playfairIndex());
        assertEquals(of(0, 1, 2), otherFourPoints.hyperbolicIndex());

        assertEquals(41, fivePoints.pointCount());
        assertEquals(82, fivePoints.lineCount());
        testCorrectness(fivePoints, of(5), 10);
        assertEquals(of(5), fivePoints.playfairIndex());
        assertEquals(of(1, 2, 3), fivePoints.hyperbolicIndex());

        assertEquals(41, otherFivePoints.pointCount());
        assertEquals(82, otherFivePoints.lineCount());
        testCorrectness(otherFivePoints, of(5), 10);
        assertEquals(of(5), otherFivePoints.playfairIndex());
        assertEquals(of(1, 2, 3), otherFivePoints.hyperbolicIndex());

        assertEquals(19, triFour.pointCount());
        assertEquals(38, triFour.lineCount());
        testCorrectness(triFour, of(3, 4), 7);
        assertEquals(of(3, 4), triFour.playfairIndex());
        assertEquals(of(0, 1, 2), triFour.hyperbolicIndex());

        assertEquals(91, sixPoints.pointCount());
        assertEquals(273, sixPoints.lineCount());
        testCorrectness(sixPoints, of(6), 18);
        assertEquals(of(12), sixPoints.playfairIndex());
        assertEquals(of(1, 2, 3, 4), sixPoints.hyperbolicIndex());

        HyperbolicPlane p3 = new HyperbolicPlane(39, new int[]{0, 1, 3}, new int[]{0, 4, 18},
                new int[]{0, 5, 27}, new int[]{0, 6, 16}, new int[]{0, 7, 15}, new int[]{0, 9, 20}, new int[]{0, 13, 26});
        testCorrectness(p3, of(3), 19);
        assertEquals(of(1), p3.hyperbolicIndex());
        checkPlane(p3, p3.pointCount(), p3.pointCount());

        HyperbolicPlane p2 = new HyperbolicPlane(37, new int[]{0, 1, 3}, new int[]{0, 4, 26},
                new int[]{0, 5, 14}, new int[]{0, 6, 25}, new int[]{0, 7, 17}, new int[]{0, 8, 21});
        testCorrectness(p2, of(3), 18);
        assertEquals(of(1), p2.hyperbolicIndex());
        checkPlane(p2, p2.pointCount(), p2.pointCount());
    }

    @Test
    public void testFivePointPlanes() {
        HyperbolicPlane fp1 = new HyperbolicPlane(new int[]{0, 17, 18, 21, 45}, new int[]{0, 2, 9, 38, 48}, new int[]{0, 5, 11, 19, 31});
        HyperbolicPlane fp2 = new HyperbolicPlane(new int[]{0, 34, 36, 39, 48}, new int[]{0, 1, 7, 30, 51}, new int[]{0, 18, 26, 42, 46});
        HyperbolicPlane fp3 = new HyperbolicPlane(new int[]{0, 17, 18, 24, 50}, new int[]{0, 2, 10, 14, 23}, new int[]{0, 3, 19, 34, 39});
        HyperbolicPlane fp4 = new HyperbolicPlane(new int[]{0, 17, 18, 33, 57}, new int[]{0, 2, 9, 38, 51}, new int[]{0, 20, 26, 31, 34});
        HyperbolicPlane fp5 = new HyperbolicPlane(new int[]{0, 16, 52, 57, 58}, new int[]{0, 12, 23, 30, 40}, new int[]{0, 14, 22, 46, 48});
        HyperbolicPlane fp6 = new HyperbolicPlane(new int[]{0, 13, 19, 21, 43, 53}, new int[]{0, 1, 12, 17, 26}, new int[]{0, 3, 7, 36, 51});
        HyperbolicPlane fp7 = new HyperbolicPlane(new int[]{0, 14, 26, 51, 60}, new int[]{0, 15, 31, 55, 59}, new int[]{0, 10, 23, 52, 58},
                new int[]{0, 3, 36, 56, 57}, new int[]{0, 7, 18, 45, 50}, new int[]{0, 8, 30, 47, 49});
        HyperbolicPlane fp8 = new HyperbolicPlane(new int[]{0, 1, 5, 12, 26}, new int[]{0, 2, 10, 40, 64}, new int[]{0, 3, 18, 47, 53}, new int[]{0, 9, 32, 48, 68});

        assertEquals(61, fp1.pointCount());
        assertEquals(183, fp1.lineCount());
        testCorrectness(fp1, of(5), 15);
        assertEquals(of(10), fp1.playfairIndex());
        assertEquals(of(0, 1, 2, 3), fp1.hyperbolicIndex());

        assertEquals(61, fp2.pointCount());
        assertEquals(183, fp2.lineCount());
        testCorrectness(fp2, of(5), 15);
        assertEquals(of(10), fp2.playfairIndex());
        assertEquals(of(0, 1, 2, 3), fp2.hyperbolicIndex());

        assertEquals(61, fp3.pointCount());
        assertEquals(183, fp3.lineCount());
        testCorrectness(fp3, of(5), 15);
        assertEquals(of(10), fp3.playfairIndex());
        assertEquals(of(0, 1, 2, 3), fp3.hyperbolicIndex());

        assertEquals(61, fp4.pointCount());
        assertEquals(183, fp4.lineCount());
        testCorrectness(fp4, of(5), 15);
        assertEquals(of(10), fp4.playfairIndex());
        assertEquals(of(0, 1, 2, 3), fp4.hyperbolicIndex());

        assertEquals(61, fp5.pointCount());
        assertEquals(183, fp5.lineCount());
        testCorrectness(fp5, of(5), 15);
        assertEquals(of(10), fp5.playfairIndex());
        assertEquals(of(0, 1, 2, 3), fp5.hyperbolicIndex());

        assertEquals(71, fp6.pointCount());
        assertEquals(213, fp6.lineCount());
        testCorrectness(fp6, of(5, 6), 16);
        assertEquals(of(10, 11), fp6.playfairIndex());
        assertEquals(of(0, 1, 2, 3, 4), fp6.hyperbolicIndex());

        assertEquals(121, fp7.pointCount());
        assertEquals(726, fp7.lineCount());
        testCorrectness(fp7, of(5), 30);
        assertEquals(of(25), fp7.playfairIndex());
        assertEquals(of(0, 1, 2, 3), fp7.hyperbolicIndex());

        assertEquals(81, fp8.pointCount());
        assertEquals(324, fp8.lineCount());
        testCorrectness(fp8, of(5), 20);
        assertEquals(of(15), fp8.playfairIndex());
        assertEquals(of(0, 1, 2, 3), fp8.hyperbolicIndex());
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet();
        IntStream.of(values).forEach(bs::set);
        return bs;
    }

    public static void testCorrectness(HyperbolicPlane plane, BitSet perLine, int beamCount) {
        for (int p : plane.points()) {
            assertEquals(beamCount, plane.point(p).cardinality());
        }
        for (int l : plane.lines()) {
            assertTrue(perLine.get(plane.line(l).cardinality()));
        }
        for (int p1 : plane.points()) {
            for (int p2 : plane.points()) {
                if (p1 != p2) {
                    BitSet line = plane.line(plane.line(p1, p2));
                    assertTrue(line.get(p1));
                    assertTrue(line.get(p2));
                }
            }
        }
        for (int p : plane.points()) {
            for (int l : plane.lines(p)) {
                assertTrue(plane.line(l).get(p));
            }
        }
        for (int l : plane.lines()) {
            for (int p : plane.points(l)) {
                assertTrue(plane.point(p).get(l));
            }
        }
        if (perLine.cardinality() == 1) { // Theorem 8.3.1
            assertEquals(beamCount * (perLine.stream().findAny().orElseThrow() - 1), plane.pointCount() - 1);
            assertEquals(plane.pointCount() * beamCount, plane.lineCount() * perLine.stream().findAny().orElseThrow());
        }
    }

    public static void checkPlane(HyperbolicPlane plane, int minPlane, int maxPlane) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x : plane.points()) {
            for (int y : plane.points()) {
                if (x >= y) {
                    continue;
                }
                for (int z : plane.points()) {
                    if (y >= z || plane.collinear(x, y, z)) {
                        continue;
                    }
                    int pCard = plane.hull(x, y, z).cardinality();
                    min = Math.min(min, pCard);
                    max = Math.max(max, pCard);
                }
            }
        }
        assertEquals(minPlane, min);
        assertEquals(maxPlane, max);
    }

    public static void checkSpace(HyperbolicPlane plane, int minSpace, int maxSpace) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x : plane.points()) {
            for (int y : plane.points()) {
                if (x >= y) {
                    continue;
                }
                for (int z : plane.points()) {
                    if (y >= z || plane.collinear(x, y, z)) {
                        continue;
                    }
                    BitSet hull = plane.hull(x, y, z);
                    for (int w : plane.points()) {
                        if (w >= z || hull.get(w)) {
                            continue;
                        }
                        int sCard = plane.hull(x, y, z, w).cardinality();
                        min = Math.min(min, sCard);
                        max = Math.max(max, sCard);
                    }
                }
            }
        }
        assertEquals(minSpace, min);
        assertEquals(maxSpace, max);
    }
}
