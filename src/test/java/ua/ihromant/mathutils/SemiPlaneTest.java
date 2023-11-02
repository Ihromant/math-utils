package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.*;

public class SemiPlaneTest {
    @Test
    public void testSemiPlane() {
        assertEquals(729, SemiFieldPlane.pointCount());
        assertEquals(756, SemiFieldPlane.lineCount());
        testCorrectness(27, 28);
        System.out.println("Playfair");
        testPlayfairIndex(1);
        System.out.println("Begin");
        testHyperbolicIndex(1);
        // very long-running test checkPlane(p);
    }

    @Test
    public void testDoubling() {
        for (int a : SemiFieldPlane.points()) {
            for (int b : SemiFieldPlane.points()) {
                if (a == b) {
                    continue;
                }
                int ab = SemiFieldPlane.line(a, b);
                BitSet abLine = SemiFieldPlane.line(ab);
                for (int c : SemiFieldPlane.points(ab)) {
                    if (a == c || b == c) {
                        continue;
                    }
                    assertEquals(ab, SemiFieldPlane.line(a, c));
                    for (int u : SemiFieldPlane.points()) {
                        if (abLine.get(u)) {
                            continue;
                        }
                        for (int v : SemiFieldPlane.points()) {
                            if (abLine.get(v) || u == v) {
                                continue;
                            }
                            for (int x : SemiFieldPlane.points()) {
                                if (abLine.get(x) || u == x || v == x) {
                                    continue;
                                }
                                for (int y : SemiFieldPlane.points()) {
                                    if (abLine.get(y) || u == y || v == y || x == y) {
                                        continue;
                                    }
                                    int xyLine = SemiFieldPlane.line(x, y);
                                    int uvLine = SemiFieldPlane.line(u, v);
                                    int axLine = SemiFieldPlane.line(a, x);
                                    int byLine = SemiFieldPlane.line(b, y);
                                    int xbLine = SemiFieldPlane.line(x, b);
                                    int ycLine = SemiFieldPlane.line(y, c);
                                    int auLine = SemiFieldPlane.line(a, u);
                                    int bvLine = SemiFieldPlane.line(b, v);
                                    int ubLine = SemiFieldPlane.line(u, b);
                                    int vcLine = SemiFieldPlane.line(v, c);
                                    if (SemiFieldPlane.parallel(xyLine, ab) && SemiFieldPlane.parallel(ab, uvLine)) {
                                        assertTrue(SemiFieldPlane.parallel(xyLine, uvLine));
                                        if (SemiFieldPlane.parallel(axLine, byLine) && SemiFieldPlane.parallel(xbLine, ycLine)
                                                && SemiFieldPlane.parallel(auLine, bvLine)
                                                && !SemiFieldPlane.parallel(ubLine, vcLine)) {
                                            fail("a=" + SemiFieldPlane.pointToString(a) + ",b=" + SemiFieldPlane.pointToString(b)
                                                    + ",c=" + SemiFieldPlane.pointToString(c) + ",x=" + SemiFieldPlane.pointToString(x)
                                                    + ",y=" + SemiFieldPlane.pointToString(y) + ",u=" + SemiFieldPlane.pointToString(u)
                                                    + ",v=" + SemiFieldPlane.pointToString(v) + ",ab=" + SemiFieldPlane.lineToString(ab)
                                                    + ",xy=" + SemiFieldPlane.lineToString(xyLine) + ",uv=" + SemiFieldPlane.lineToString(uvLine)
                                                    + ",ax=" + SemiFieldPlane.lineToString(axLine) + ",by=" + SemiFieldPlane.lineToString(byLine)
                                                    + ",bx=" + SemiFieldPlane.lineToString(xbLine) + ",cy=" + SemiFieldPlane.lineToString(ycLine)
                                                    + ",au=" + SemiFieldPlane.lineToString(auLine) + ",bv=" + SemiFieldPlane.lineToString(bvLine)
                                                    + ",ub=" + SemiFieldPlane.lineToString(ubLine) + ",vc=" + SemiFieldPlane.lineToString(vcLine)
                                                    + ",intersection ub and vc =" + SemiFieldPlane.pointToString(SemiFieldPlane.intersection(ubLine, vcLine)));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Doubling: ");
    }

    @Test
    public void testPappus() {
        int base = SemiFieldPlane.of(SemiField.ZERO, SemiField.ZERO);
        for (int l1 : SemiFieldPlane.lines(base)) {
            for (int l2 : SemiFieldPlane.lines(base)) {
                if (l1 == l2) {
                    continue;
                }
                for (int a1 : SemiFieldPlane.points(l1)) {
                    if (a1 == base) {
                        continue;
                    }
                    for (int b1 : SemiFieldPlane.points(l1)) {
                        if (b1 == base || a1 >= b1) {
                            continue;
                        }
                        for (int c1 : SemiFieldPlane.points(l1)) {
                            if (c1 == base || b1 >= c1) {
                                continue;
                            }
                            for (int a2 : SemiFieldPlane.points(l2)) {
                                if (a2 == base) {
                                    continue;
                                }
                                for (int b2 : SemiFieldPlane.points(l2)) {
                                    if (b2 == base || a2 >= b2) {
                                        continue;
                                    }
                                    for (int c2 : SemiFieldPlane.points(l2)) {
                                        if (c2 == base || b2 >= c2) {
                                            continue;
                                        }
                                        int a1b2 = SemiFieldPlane.line(b2, a1);
                                        int b1c2 = SemiFieldPlane.line(c2, b1);
                                        int b1a2 = SemiFieldPlane.line(a2, b1);
                                        int c1b2 = SemiFieldPlane.line(b2, c1);
                                        int a1a2 = SemiFieldPlane.line(a2, a1);
                                        int c1c2 = SemiFieldPlane.line(c2, c1);
                                        if (SemiFieldPlane.parallel(a1b2, b1c2) && SemiFieldPlane.parallel(b1a2, c1b2)
                                                && !SemiFieldPlane.parallel(a1a2, c1c2)) {
                                            assertEquals(-1, SemiFieldPlane.intersection(a1b2, b1c2));
                                            assertEquals(-1, SemiFieldPlane.intersection(b1a2, c1b2));
                                            fail("a1: " + SemiFieldPlane.pointToString(a1) + ", " + "b1: " + SemiFieldPlane.pointToString(b1) + ", " +
                                                    "c1: " + SemiFieldPlane.pointToString(c1) + ", " + "a2: " + SemiFieldPlane.pointToString(a2) + ", " +
                                                    "b2: " + SemiFieldPlane.pointToString(b2) + ", " + "c2: " + SemiFieldPlane.pointToString(c2) + ", " +
                                                    "a1b2: " + SemiFieldPlane.lineToString(a1b2) + ", " + "b1c2: " + SemiFieldPlane.lineToString(b1c2) + ", " +
                                                    "b1a2: " + SemiFieldPlane.lineToString(b1a2) + ", " + "c1b2: " + SemiFieldPlane.lineToString(c1b2) + ", " +
                                                    "a1a2: " + SemiFieldPlane.lineToString(a1a2) + ", " + "c1c2: " + SemiFieldPlane.lineToString(c1c2)
                                                    + ", inter: " + SemiFieldPlane.pointToString(SemiFieldPlane.intersection(a1a2, c1c2)));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void testCorrectness(int perLine, int beamCount) {
        for (int p : SemiFieldPlane.points()) {
            assertEquals(beamCount, SemiFieldPlane.point(p).cardinality());
        }
        for (int l : SemiFieldPlane.lines()) {
            assertEquals(perLine, SemiFieldPlane.line(l).cardinality());
        }
        for (int p1 : SemiFieldPlane.points()) {
            for (int p2 : SemiFieldPlane.points()) {
                if (p1 != p2) {
                    BitSet line = SemiFieldPlane.line(SemiFieldPlane.line(p1, p2));
                    assertTrue(line.get(p1));
                    assertTrue(line.get(p2));
                }
            }
        }
        for (int p : SemiFieldPlane.points()) {
            for (int l : SemiFieldPlane.lines(p)) {
                assertTrue(SemiFieldPlane.line(l).get(p));
            }
        }
        for (int l : SemiFieldPlane.lines()) {
            for (int p : SemiFieldPlane.points(l)) {
                assertTrue(SemiFieldPlane.point(p).get(l));
            }
        }
        assertEquals(beamCount * (perLine - 1), SemiFieldPlane.pointCount() - 1);
        assertEquals(SemiFieldPlane.pointCount() * beamCount, SemiFieldPlane.lineCount() * perLine);
    }

//    private void checkPlane(HyperbolicPlane plane) {
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
//    }

    private void testPlayfairIndex(int hyperbolicNumber) {
        for (int l : SemiFieldPlane.lines()) {
            BitSet line = SemiFieldPlane.line(l);
            for (int p : SemiFieldPlane.points()) {
                if (line.get(p)) {
                    continue;
                }
                int counter = 0;
                for (int parallel : SemiFieldPlane.lines(p)) {
                    if (SemiFieldPlane.intersection(parallel, l) == -1) {
                        counter++;
                    }
                }
                assertEquals(hyperbolicNumber, counter);
            }
        }
    }

    private void testHyperbolicIndex(int idx) {
        for (int o : SemiFieldPlane.points()) {
            for (int x : SemiFieldPlane.points()) {
                if (o == x) {
                    continue;
                }
                for (int y : SemiFieldPlane.points()) {
                    if (SemiFieldPlane.collinear(o, x, y)) {
                        continue;
                    }
                    int xy = SemiFieldPlane.line(x, y);
                    for (int p : SemiFieldPlane.points(xy)) {
                        if (p == x || p == y) {
                            continue;
                        }
                        int ox = SemiFieldPlane.line(o, x);
                        int oy = SemiFieldPlane.line(o, y);
                        int counter = 0;
                        for (int u : SemiFieldPlane.points(oy)) {
                            if (u == o || u == y) {
                                continue;
                            }
                            if (SemiFieldPlane.intersection(SemiFieldPlane.line(p, u), ox) == -1) {
                                counter++;
                            }
                        }
                        assertEquals(idx, counter);
                    }
                }
            }
        }
    }
}
