package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HyperbolicPlaneTest {
    // Quadruple[i=19, j=24, k=33, l=39]={2, 5, 6, 8, 9, 14, 15, 17, 19, 20} Quadruple[i=1, j=4, k=11, l=29]={1, 3, 4, 7, 10, 11, 12, 13, 16, 18}
    @Test
    public void testPlanesCorrectness() {
        HyperbolicPlane triPoints = new HyperbolicPlane(new int[]{0, 2, 7}, new int[]{0, 1, 4});
        HyperbolicPlane fourPoints = new HyperbolicPlane(new int[]{0, 18, 27, 33}, new int[]{0, 7, 24, 36}, new int[]{0, 3, 5, 26});
        HyperbolicPlane fivePoints = new HyperbolicPlane(new int[]{0, 19, 24, 33, 39}, new int[]{0, 1, 4, 11, 29});
        testCorrectness(triPoints, 3, 6);
        testCorrectness(fourPoints, 4, 12);
        testHyperbolicity(triPoints, 3);
        testHyperbolicity(fourPoints, 8);
        testCorrectness(fivePoints, 5, 10);
        testHyperbolicity(fivePoints, 5);
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
}
