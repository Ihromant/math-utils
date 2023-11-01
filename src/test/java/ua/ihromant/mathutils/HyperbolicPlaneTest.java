package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HyperbolicPlaneTest {
    @Test
    public void testPlanesCorrectness() {
        HyperbolicPlane triPoints = new HyperbolicPlane(new int[]{0, 2, 7}, new int[]{0, 1, 4});
        HyperbolicPlane fourPoints = new HyperbolicPlane(new int[]{0, 18, 27, 33}, new int[]{0, 7, 24, 36}, new int[]{0, 3, 5, 26});
        HyperbolicPlane fivePoints = new HyperbolicPlane(new int[]{0, 19, 24, 33, 39}, new int[]{0, 1, 4, 11, 29});
        HyperbolicPlane otherFivePoints = new HyperbolicPlane(new int[]{0, 17, 18, 21, 45}, new int[]{0, 2, 9, 38, 48}, new int[]{0, 5, 11, 19, 31});
        assertEquals(13, triPoints.pointCount());
        assertEquals(26, triPoints.lineCount());
        testCorrectness(triPoints, 3, 6);
        testHyperbolicity(triPoints, 3);
        testOurHyperbolicity(triPoints, 0, 1);
        assertEquals(37, fourPoints.pointCount());
        assertEquals(111, fourPoints.lineCount());
        testCorrectness(fourPoints, 4, 12);
        testHyperbolicity(fourPoints, 8);
        testOurHyperbolicity(fourPoints, 0, 2);
        assertEquals(41, fivePoints.pointCount());
        assertEquals(82, fivePoints.lineCount());
        testCorrectness(fivePoints, 5, 10);
        testHyperbolicity(fivePoints, 5);
        testOurHyperbolicity(fivePoints, 1, 3);
        assertEquals(61, otherFivePoints.pointCount());
        assertEquals(183, otherFivePoints.lineCount());
        testCorrectness(otherFivePoints, 5, 15);
        testHyperbolicity(otherFivePoints, 10);
        testOurHyperbolicity(otherFivePoints, 0, 3);
    }

    private void testCorrectness(HyperbolicPlane plane, int perLine, int beamCount) {
        for (int p : plane.points()) {
            assertEquals(beamCount, plane.point(p).cardinality());
        }
        for (int l : plane.lines()) {
            assertEquals(perLine, plane.line(l).cardinality());
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
    }

    private void testHyperbolicity(HyperbolicPlane plane, int hyperbolicNumber) {
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
                assertEquals(hyperbolicNumber, counter);
            }
        }
    }

    private void testOurHyperbolicity(HyperbolicPlane plane, int minHyperbolicNumber, int maxHyperbolicNumber) {
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
        assertEquals(min, minHyperbolicNumber);
        assertEquals(max, maxHyperbolicNumber);
    }
}
