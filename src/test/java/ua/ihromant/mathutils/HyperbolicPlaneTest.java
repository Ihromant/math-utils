package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class HyperbolicPlaneTest {
    @Test
    public void hyperbolicPlaneExample() {
        HyperbolicPlane p = new HyperbolicPlane(217, new int[]{0,1,37,67,88,92,149}, new int[]{0,15,18,65,78,121,137}, new int[]{0,8,53,79,85,102,107},
                new int[]{0,11,86,100,120,144,190}, new int[]{0,29,64,165,198,205,207}, new int[]{0,31,62,93,124,155,186});
        assertEquals(217, p.pointCount());
        assertEquals(1116, p.lineCount());
        testCorrectness(p, of(7), 36);
        testPlayfairIndex(p, of(29));
        testHyperbolicIndex(p, 2, 5);
        // very long-running test checkPlane(p);
    }

    @Test
    public void checkNotPlane() {
        HyperbolicPlane p = new HyperbolicPlane("00000001111112222223333444455556666",
                "13579bd3478bc3478bc789a789a789a789a",
                "2468ace569ade65a9edbcdecbeddebcedcb");
        assertEquals(15, p.pointCount());
        assertEquals(35, p.lineCount());
        testCorrectness(p, of(3), 7);
        testHyperbolicIndex(p, 0, 0);
        try {
            checkPlane(p); // it's model of 3-dimensional projective space
            fail();
        } catch (AssertionFailedError e) {
            // ok
        }
    }

    @Test
    public void testCyclic() {
        CyclicGroup cg1 = new CyclicGroup(new int[]{11, 11});
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
        testPlayfairIndex(p1, of(18));
        testHyperbolicIndex(p1, 1, 4);

        CyclicGroup cg = new CyclicGroup(new int[]{7, 37});
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
        testPlayfairIndex(p1, of(18));
        testHyperbolicIndex(p1, 1, 4);
    }

    @Test
    public void testPrimePower() {
        HyperbolicPlane p4 = new HyperbolicPlane(109, new int[]{0, 1, 3, 60});
        assertEquals(109, p4.pointCount());
        assertEquals(981, p4.lineCount());
        testCorrectness(p4, of(4), 36);
        testPlayfairIndex(p4, of(32));
        testHyperbolicIndex(p4, 1, 2);

        GaloisField fd1 = new GaloisField(121);
        int x = fd1.solve(new int[]{1, 3, 8}).findAny().orElseThrow();
        HyperbolicPlane p3 = new HyperbolicPlane(fd1.cardinality(), new int[]{0, 1, x, fd1.power(x, 10)});
        assertEquals(121, p3.pointCount());
        assertEquals(1210, p3.lineCount());
        testCorrectness(p3, of(4), 40);
        testPlayfairIndex(p3, of(36));
        testHyperbolicIndex(p3, 0, 2);

        GaloisField fd = new GaloisField(421);
        int c1 = 1;
        int c2 = 4;
        int w = fd.oneCubeRoots().findAny().orElseThrow();
        HyperbolicPlane p = new HyperbolicPlane(fd.cardinality(),
                new int[] {0, c1, fd.mul(c1, w), fd.mul(c1, fd.mul(w, w)), c2, fd.mul(c2, w), fd.mul(c2, fd.mul(w, w))});
        assertEquals(421, p.pointCount());
        assertEquals(4210, p.lineCount());
        testCorrectness(p, of(7), 70);
        testPlayfairIndex(p, of(63));
        testHyperbolicIndex(p, 2, 5);

        HyperbolicPlane p1 = new HyperbolicPlane(433, new int[]{0, 1, 3, 30, 52, 61, 84, 280, 394});
        assertEquals(433, p1.pointCount());
        assertEquals(2598, p1.lineCount());
        testCorrectness(p1, of(9), 54);
        testPlayfairIndex(p1, of(45));
        testHyperbolicIndex(p1, 2, 7);

        HyperbolicPlane p2 = new HyperbolicPlane(449, new int[]{0, 1, 3, 8, 61, 104, 332, 381});
        assertEquals(449, p2.pointCount());
        assertEquals(3592, p2.lineCount());
        testCorrectness(p2, of(8), 64);
        testPlayfairIndex(p2, of(56));
        testHyperbolicIndex(p2, 1, 6);
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
        testPlayfairIndex(p, of(13));
        testHyperbolicIndex(p, 1, 4);

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
        testPlayfairIndex(p1, of(15));
        testHyperbolicIndex(p1, 0, 4);

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
        HyperbolicPlane p1 = new HyperbolicPlane("00000000111111122222223333344445555666778899aabbil",
                "134567ce34578cd34568de468bh679f78ag79b9aabcddecejm",
                "298dfbhkea6g9kf7c9afkg5cgfihdgifchi8ejjcjdfhgfghkn",
                "iaolgmjnmbohnljonblhmjjdlknmeklnekmkinlimimonooloo");
        assertEquals(25, p1.pointCount());
        assertEquals(50, p1.lineCount());
        testCorrectness(p1, of(4), 8);
        testPlayfairIndex(p1, of(4));
        testHyperbolicIndex(p1, 1, 2);
        checkPlane(p1);

        HyperbolicPlane p2 = new HyperbolicPlane("0000000011111112222222333334444555566667778889abil",
                "13457bce34589cd3456ade489eh6acf7bdg79ab9ab9abdecjm",
                "2689gdfka76fekg798fckh5cfgidghichfi8chjjdfgjehfgkn",
                "iloahnjmbmohlnjobngmljjdknmeklnekmlkinmnilmliooooo");
        assertEquals(25, p2.pointCount());
        assertEquals(50, p2.lineCount());
        testCorrectness(p2, of(4), 8);
        testPlayfairIndex(p2, of(4));
        testHyperbolicIndex(p2, 0, 2);

        HyperbolicPlane p3 = new HyperbolicPlane("0000000011111112222222333334444555566667778889abil",
                "13457bde34589ce3456acd489eh6acf7bdg79ab9ab9abcdejm",
                "2689gcfka76fdkg798fehk5cgfidhgicfhi8hcjjfdejgfghkn",
                "iloahmjnbmohnljobngljmjdkmneknleklmkminlniimlooooo");
        assertEquals(25, p3.pointCount());
        assertEquals(50, p3.lineCount());
        testCorrectness(p3, of(4), 8);
        testPlayfairIndex(p3, of(4));
        testHyperbolicIndex(p3, 0, 2);

        HyperbolicPlane p4 = new HyperbolicPlane("000000001111111222222333334444555566667778899abcde",
                "123468cj23479af3458bg456ah57bi78bd89ce9adabacghiff",
                "5ad97fek6be8gdl7ch9em89fcn6gdkfickgjdlhmeekblhijjg",
                "oignbhmlkjhcinmlfjdonmeikoajlonlgmomhnkoijnfolmnok");
        assertEquals(25, p4.pointCount());
        assertEquals(50, p4.lineCount());
        testCorrectness(p4, of(4), 8);
        testPlayfairIndex(p4, of(4));
        testHyperbolicIndex(p4, 0, 2);

        HyperbolicPlane p5 = new HyperbolicPlane("000000001111111222222333333444445555566666777889al",
                "124789bh2589ace39abde47abcf8bcde79cdf78adg89b9cabm",
                "365egifj46fhjgi5gikhf6ihjegjikfggkjehfhekiadcbdcdn",
                "dcaolmnk7bolmnk8olmnj9nolmknolmhmnolilmnojkjheifgo");
        assertEquals(25, p5.pointCount());
        assertEquals(50, p5.lineCount());
        testCorrectness(p5, of(4), 8);
        testPlayfairIndex(p5, of(4));
        testHyperbolicIndex(p5, 0, 2);

        HyperbolicPlane p6 = new HyperbolicPlane("000000001111111222222333333444455566667777889aacee",
                "12459bdf24569cg3458bh4568bi59bh9ac78ab89cgaddbedfl",
                "3786cihjd78haek96kfgja7fgclgkdiimfi9djebkjcjffhmgn",
                "oanegklmifbmljnecolmnjdkhnmlmeojnhnoglmhloiknokoio");
        assertEquals(25, p6.pointCount());
        assertEquals(50, p6.lineCount());
        testCorrectness(p6, of(4), 8);
        testPlayfairIndex(p6, of(4));
        testHyperbolicIndex(p6, 0, 2);

        HyperbolicPlane p7 = new HyperbolicPlane("000000001111111222222233333444455556677889abcdefgh",
                "13456789345678a34567894679c67ad68be9aab9bdecijkijk",
                "2fbcdgiadg9jcfbaehficb5b8eg89ch7adfchdfgefghmlllmn",
                "onklehjmmlikehnjnmgkdloilhkmjfinkgjolomnokijnnmooo");
        assertEquals(25, p7.pointCount());
        assertEquals(50, p7.lineCount());
        testCorrectness(p7, of(4), 8);
        testPlayfairIndex(p7, of(4));
        testHyperbolicIndex(p7, 0, 2);

        HyperbolicPlane p8 = new HyperbolicPlane("0000000011111112222222333333344445555666677778889a",
                "147adgjm4569chi4569bdf45689ae9bcf9ach89abbcdeabicd",
                "258behkn78ebfkl87caekgdb7cfglgkehefgjfildfighegjdj",
                "369cfiloadgjmnonlohimjimjhoknmojlkinoknmhnkomolmln");
        assertEquals(25, p8.pointCount());
        assertEquals(50, p8.lineCount());
        testCorrectness(p8, of(4), 8);
        testPlayfairIndex(p8, of(4));
        testHyperbolicIndex(p8, 0, 2);

        HyperbolicPlane p9 = new HyperbolicPlane("0000000011111112222222333333344445555666677778889a",
                "147adgjm4569cef4569bcd45689ae9bdf9abe89acbcdiabcfd",
                "258behkn78bhkil87jafghhc7jbigcekggfihelhdheglgkfij",
                "369cfiloadgjmnoikoemnlmlfndokolnjmnjomnkinjomlohkm");
        assertEquals(25, p9.pointCount());
        assertEquals(50, p9.lineCount());
        testCorrectness(p9, of(4), 8);
        testPlayfairIndex(p9, of(4));
        testHyperbolicIndex(p9, 0, 2);
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
        testPlayfairIndex(triPoints, of(3));
        testHyperbolicIndex(triPoints, 0, 1);
        checkPlane(triPoints);

        assertEquals(19, otherTriPoints.pointCount());
        assertEquals(57, otherTriPoints.lineCount());
        testCorrectness(otherTriPoints, of(3), 9);
        testPlayfairIndex(otherTriPoints, of(6));
        testHyperbolicIndex(otherTriPoints, 0, 1);

        assertEquals(37, fourPoints.pointCount());
        assertEquals(111, fourPoints.lineCount());
        testCorrectness(fourPoints, of(4), 12);
        testPlayfairIndex(fourPoints, of(8));
        testHyperbolicIndex(fourPoints, 0, 2);

        assertEquals(49, otherFourPoints.pointCount());
        assertEquals(196, otherFourPoints.lineCount());
        testCorrectness(otherFourPoints, of(4), 16);
        testPlayfairIndex(otherFourPoints, of(12));
        testHyperbolicIndex(otherFourPoints, 0, 2);

        assertEquals(41, fivePoints.pointCount());
        assertEquals(82, fivePoints.lineCount());
        testCorrectness(fivePoints, of(5), 10);
        testPlayfairIndex(fivePoints, of(5));
        testHyperbolicIndex(fivePoints, 1, 3);

        assertEquals(41, otherFivePoints.pointCount());
        assertEquals(82, otherFivePoints.lineCount());
        testCorrectness(otherFivePoints, of(5), 10);
        testPlayfairIndex(otherFivePoints, of(5));
        testHyperbolicIndex(otherFivePoints, 1, 3);

        assertEquals(19, triFour.pointCount());
        assertEquals(38, triFour.lineCount());
        testCorrectness(triFour, of(3, 4), 7);
        testPlayfairIndex(triFour, of(3, 4));
        testHyperbolicIndex(triFour, 0, 2);

        assertEquals(91, sixPoints.pointCount());
        assertEquals(273, sixPoints.lineCount());
        testCorrectness(sixPoints, of(6), 18);
        testPlayfairIndex(sixPoints, of(12));
        testHyperbolicIndex(sixPoints, 1, 4);
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
        testPlayfairIndex(fp1, of(10));
        testHyperbolicIndex(fp1, 0, 3);

        assertEquals(61, fp2.pointCount());
        assertEquals(183, fp2.lineCount());
        testCorrectness(fp2, of(5), 15);
        testPlayfairIndex(fp2, of(10));
        testHyperbolicIndex(fp2, 0, 3);

        assertEquals(61, fp3.pointCount());
        assertEquals(183, fp3.lineCount());
        testCorrectness(fp3, of(5), 15);
        testPlayfairIndex(fp3, of(10));
        testHyperbolicIndex(fp3, 0, 3);

        assertEquals(61, fp4.pointCount());
        assertEquals(183, fp4.lineCount());
        testCorrectness(fp4, of(5), 15);
        testPlayfairIndex(fp4, of(10));
        testHyperbolicIndex(fp4, 0, 3);

        assertEquals(61, fp5.pointCount());
        assertEquals(183, fp5.lineCount());
        testCorrectness(fp5, of(5), 15);
        testPlayfairIndex(fp5, of(10));
        testHyperbolicIndex(fp5, 0, 3);

        assertEquals(71, fp6.pointCount());
        assertEquals(213, fp6.lineCount());
        testCorrectness(fp6, of(5, 6), 16);
        testPlayfairIndex(fp6, of(10, 11));
        testHyperbolicIndex(fp6, 0, 4);

        assertEquals(121, fp7.pointCount());
        assertEquals(726, fp7.lineCount());
        testCorrectness(fp7, of(5), 30);
        testPlayfairIndex(fp7, of(25));
        testHyperbolicIndex(fp7, 0, 3);

        assertEquals(81, fp8.pointCount());
        assertEquals(324, fp8.lineCount());
        testCorrectness(fp8, of(5), 20);
        testPlayfairIndex(fp8, of(15));
        testHyperbolicIndex(fp8, 0, 3);
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet();
        IntStream.of(values).forEach(bs::set);
        return bs;
    }

    private void testCorrectness(HyperbolicPlane plane, BitSet perLine, int beamCount) {
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

    private void checkPlane(HyperbolicPlane plane) {
        for (int x : plane.points()) {
            for (int y : plane.points()) {
                for (int z : plane.points()) {
                    if (plane.collinear(x, y, z)) {
                        continue;
                    }
                    assertEquals(plane.pointCount(), plane.hull(x, y, z).cardinality());
                }
            }
        }
    }

    private void testPlayfairIndex(HyperbolicPlane plane, BitSet hyperbolicNumber) {
        for (int l : plane.lines()) {
            BitSet line = plane.line(l);
            for (int p : plane.points()) {
                if (line.get(p)) {
                    continue;
                }
                int counter = 0;
                for (int parallel : plane.lines(p)) {
                    if (plane.intersection(parallel, l) == -1) {
                        counter++;
                    }
                }
                assertTrue(hyperbolicNumber.get(counter));
            }
        }
    }

    private void testHyperbolicIndex(HyperbolicPlane plane, int minIdx, int maxIdx) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int o : plane.points()) {
            for (int x : plane.points()) {
                if (o == x) {
                    continue;
                }
                for (int y : plane.points()) {
                    if (plane.collinear(o, x, y)) {
                        continue;
                    }
                    int xy = plane.line(x, y);
                    for (int p : plane.points(xy)) {
                        if (p == x || p == y) {
                            continue;
                        }
                        int ox = plane.line(o, x);
                        int oy = plane.line(o, y);
                        int counter = 0;
                        for (int u : plane.points(oy)) {
                            if (u == o || u == y) {
                                continue;
                            }
                            if (plane.intersection(plane.line(p, u), ox) == -1) {
                                counter++;
                            }
                        }
                        min = Math.min(min, counter);
                        max = Math.max(max, counter);
                    }
                }
            }
        }
        assertEquals(min, minIdx);
        assertEquals(max, maxIdx);
    }
}
