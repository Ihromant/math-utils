package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HyperbolicPlaneTest {
    @Test
    public void testPlanesCorrectness() {
        HyperbolicPlane triPoints = new HyperbolicPlane(new int[]{0, 2, 7}, new int[]{0, 1, 4});
        HyperbolicPlane otherTriPoints = new HyperbolicPlane(new int[]{0, 8, 10}, new int[]{0, 1, 6}, new int[]{0, 3, 7});
        HyperbolicPlane fourPoints = new HyperbolicPlane(new int[]{0, 18, 27, 33}, new int[]{0, 7, 24, 36}, new int[]{0, 3, 5, 26});
        HyperbolicPlane otherFourPoints = new HyperbolicPlane(new int[]{0, 33, 34, 39}, new int[]{0, 17, 25, 28}, new int[]{0, 2, 9, 22}, new int[]{0, 19, 23, 37});
        HyperbolicPlane fivePoints = new HyperbolicPlane(new int[]{0, 19, 24, 33, 39}, new int[]{0, 1, 4, 11, 29});
        HyperbolicPlane otherFivePoints = new HyperbolicPlane(new int[]{0, 17, 18, 21, 45}, new int[]{0, 2, 9, 38, 48}, new int[]{0, 5, 11, 19, 31});
        HyperbolicPlane triFour = new HyperbolicPlane(new int[]{0, 9, 13}, new int[]{0, 1, 3, 8});
        assertEquals(13, triPoints.pointCount());
        assertEquals(26, triPoints.lineCount());
        testCorrectness(triPoints, of(3), 6);
        testPlayfairIndex(triPoints, of(3));
        testHyperbolicIndex(triPoints, 0, 1);

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

        assertEquals(61, otherFivePoints.pointCount());
        assertEquals(183, otherFivePoints.lineCount());
        testCorrectness(otherFivePoints, of(5), 15);
        testPlayfairIndex(otherFivePoints, of(10));
        testHyperbolicIndex(otherFivePoints, 0, 3);

        assertEquals(19, triFour.pointCount());
        assertEquals(38, triFour.lineCount());
        testCorrectness(triFour, of(3, 4), 7);
        testPlayfairIndex(triFour, of(3, 4));
        testHyperbolicIndex(triFour, 0, 2);
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
//        for (int x : plane.points()) {
//            for (int y : plane.points()) {
//                for (int z : plane.points()) {
//                    if (plane.collinear(x, y, z)) {
//                        continue;
//                    }
//                    assertEquals(plane.pointCount(), plane.hull(x, y, z).cardinality());
//                }
//            }
//        }
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
