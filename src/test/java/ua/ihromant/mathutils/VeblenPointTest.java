package ua.ihromant.mathutils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.*;

public class VeblenPointTest {
    @Test
    public void testCorrectness() {
        for (int p : VeblenPoint.points()) {
            assertEquals(10, VeblenPoint.point(p).cardinality());
        }
        for (int l : VeblenPoint.lines()) {
            assertEquals(10, VeblenPoint.line(l).cardinality());
        }
        for (int p1 : VeblenPoint.points()) {
            for (int p2 : VeblenPoint.points()) {
                if (p1 != p2) {
                    BitSet line = VeblenPoint.line(VeblenPoint.line(p1, p2));
                    assertTrue(line.get(p1));
                    assertTrue(line.get(p2));
                }
            }
        }
        for (int l1 : VeblenPoint.lines()) {
            for (int l2 : VeblenPoint.lines()) {
                if (l1 != l2) {
                    BitSet intersection = VeblenPoint.point(VeblenPoint.intersection(l1, l2));
                    assertTrue(intersection.get(l1));
                    assertTrue(intersection.get(l2));
                }
            }
        }
        for (int p : VeblenPoint.points()) {
            for (int l : VeblenPoint.lines(p)) {
                assertTrue(VeblenPoint.line(l).get(p));
            }
        }
        for (int l : VeblenPoint.lines()) {
            for (int p : VeblenPoint.points(l)) {
                assertTrue(VeblenPoint.point(p).get(l));
            }
        }
    }

    @Test
    public void testParallelTransitivity() {
        for (int dl : VeblenPoint.lines()) {
            BitSet droppedLine = VeblenPoint.line(dl);
            System.out.println(droppedLine);
            for (int a : VeblenPoint.points()) {
                if (droppedLine.get(a)) {
                    continue;
                }
                for (int b : VeblenPoint.points()) {
                    if (droppedLine.get(b) || a == b) {
                        continue;
                    }
                    int ab = VeblenPoint.line(a, b);
                    BitSet abLine = VeblenPoint.line(ab);
                    for (int u : VeblenPoint.points()) {
                        if (droppedLine.get(u) || abLine.get(u)) {
                            continue;
                        }
                        for (int v : VeblenPoint.points()) {
                            if (droppedLine.get(v) || abLine.get(v) || u == v) {
                                continue;
                            }
                            for (int x : VeblenPoint.points()) {
                                if (droppedLine.get(x) || abLine.get(x) || u == x || v == x) {
                                    continue;
                                }
                                for (int y : VeblenPoint.points()) {
                                    if (droppedLine.get(y) || abLine.get(y) || u == y || v == y || x == y) {
                                        continue;
                                    }
                                    int xyLine = VeblenPoint.line(x, y);
                                    int uvLine = VeblenPoint.line(u, v);
                                    if (parallel(ab, xyLine, droppedLine)
                                        && parallel(ab, uvLine, droppedLine)
                                        && !parallel(uvLine, xyLine, droppedLine)) {
                                        fail();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("Successful: " + droppedLine);
        }
    }

    @Test
    public void testPlayfair() {
        int max = 0;
        int min = Integer.MAX_VALUE;
        for (int dr : VeblenPoint.lines()) {
            for (int l : VeblenPoint.lines()) {
                if (l == dr) {
                    continue;
                }
                BitSet dropped = VeblenPoint.line(dr);
                BitSet line = VeblenPoint.line(l);
                for (int p : VeblenPoint.points()) {
                    if (dropped.get(p) || line.get(p)) {
                        continue;
                    }
                    int counter = 0;
                    for (int parallel : VeblenPoint.lines(p)) {
                        if (dropped.get(VeblenPoint.intersection(parallel, l))) {
                            counter++;
                        }
                    }
                    assertEquals(1, counter);
                    max = Math.max(max, counter);
                    min = Math.min(min, counter);
                }
            }
        }
        System.out.println(min + " " + max);
    }

    @Test
    public void testDoubling() {
        exit: for (int dl : VeblenPoint.lines()) {
            BitSet droppedLine = VeblenPoint.line(dl);
            System.out.println(VeblenPoint.lineToString(dl));
            for (int a : VeblenPoint.points()) {
                if (droppedLine.get(a)) {
                    continue;
                }
                for (int b : VeblenPoint.points()) {
                    if (droppedLine.get(b) || a == b) {
                        continue;
                    }
                    int ab = VeblenPoint.line(a, b);
                    BitSet abLine = VeblenPoint.line(ab);
                    for (int c : VeblenPoint.points(ab)) {
                        if (droppedLine.get(c) || a == c || b == c) {
                            continue;
                        }
                        assertEquals(ab, VeblenPoint.line(a, c));
                        for (int u : VeblenPoint.points()) {
                            if (droppedLine.get(u) || abLine.get(u)) {
                                continue;
                            }
                            for (int v : VeblenPoint.points()) {
                                if (droppedLine.get(v) || abLine.get(v) || u == v) {
                                    continue;
                                }
                                for (int x : VeblenPoint.points()) {
                                    if (droppedLine.get(x) || abLine.get(x) || u == x || v == x) {
                                        continue;
                                    }
                                    for (int y : VeblenPoint.points()) {
                                        if (droppedLine.get(y) || abLine.get(y) || u == y || v == y || x == y) {
                                            continue;
                                        }
                                        int xyLine = VeblenPoint.line(x, y);
                                        int uvLine = VeblenPoint.line(u, v);
                                        int axLine = VeblenPoint.line(a, x);
                                        int byLine = VeblenPoint.line(b, y);
                                        int xbLine = VeblenPoint.line(x, b);
                                        int ycLine = VeblenPoint.line(y, c);
                                        int auLine = VeblenPoint.line(a, u);
                                        int bvLine = VeblenPoint.line(b, v);
                                        int ubLine = VeblenPoint.line(u, b);
                                        int vcLine = VeblenPoint.line(v, c);
                                        if (parallel(xyLine, ab, droppedLine)
                                                && parallel(ab, uvLine, droppedLine)) {
                                            assertTrue(parallel(xyLine, uvLine, droppedLine));
                                            if (parallel(axLine, byLine, droppedLine)
                                                && parallel(xbLine, ycLine, droppedLine)
                                                && parallel(auLine, bvLine, droppedLine)
                                                && !parallel(ubLine, vcLine, droppedLine)) {
                                                System.out.println("a=" + VeblenPoint.pointToString(a) + ",b=" + VeblenPoint.pointToString(b)
                                                        + ",c=" + VeblenPoint.pointToString(c) + ",x=" + VeblenPoint.pointToString(x)
                                                        + ",y=" + VeblenPoint.pointToString(y) + ",u=" + VeblenPoint.pointToString(u)
                                                        + ",v=" + VeblenPoint.pointToString(v) + ",ab=" + VeblenPoint.lineToString(ab)
                                                        + ",xy=" + VeblenPoint.lineToString(xyLine) + ",uv=" + VeblenPoint.lineToString(uvLine)
                                                        + ",ax=" + VeblenPoint.lineToString(axLine) + ",by=" + VeblenPoint.lineToString(byLine)
                                                        + ",bx=" + VeblenPoint.lineToString(xbLine) + ",cy=" + VeblenPoint.lineToString(ycLine)
                                                        + ",au=" + VeblenPoint.lineToString(auLine) + ",bv=" + VeblenPoint.lineToString(bvLine)
                                                        + ",ub=" + VeblenPoint.lineToString(ubLine) + ",vc=" + VeblenPoint.lineToString(vcLine)
                                                        + ",intersection ub and vc =" + VeblenPoint.pointToString(VeblenPoint.intersection(ubLine, vcLine)));
                                                continue exit;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("Doubling: " + VeblenPoint.lineToString(dl));
            return;
        }
        fail();
    }

    public boolean parallel(int first, int second, BitSet dropped) {
        return first == second || dropped.get(VeblenPoint.intersection(first, second));
    }

    //@Test
    public void testSmallDesargue() {
        for (int o : VeblenPoint.points()) {
            System.out.println(o);
            exit: for (int l0 : VeblenPoint.lines(o)) {
                for (int l1 : VeblenPoint.lines(o)) {
                    if (l1 == l0) {
                        continue;
                    }
                    for (int l2 : VeblenPoint.lines(o)) {
                        if (l2 == l0 || l2 == l1) {
                            continue;
                        }
                        for (int l3 : VeblenPoint.lines(o)) {
                            if (l3 == l0 || l3 == l1 || l3 == l2) {
                                continue;
                            }
                            for (int x : VeblenPoint.points(l0)) {
                                if (x == o) {
                                    continue;
                                }
                                for (int y : VeblenPoint.points(l0)) {
                                    if (y == x || y == o) {
                                        continue;
                                    }
                                    assertTrue(VeblenPoint.collinear(o, x, y));
                                    for (int a1 : VeblenPoint.points(l1)) {
                                        if (a1 == o) {
                                            continue;
                                        }
                                        for (int a2 : VeblenPoint.points(l1)) {
                                            if (a2 == a1 || a2 == o) {
                                                continue;
                                            }
                                            int b1 = VeblenPoint.intersection(VeblenPoint.line(x, a1), l2);
                                            int b2 = VeblenPoint.intersection(VeblenPoint.line(x, a2), l2);
                                            int c1 = VeblenPoint.intersection(VeblenPoint.line(y, a1), l3);
                                            int c2 = VeblenPoint.intersection(VeblenPoint.line(y, a2), l3);
                                            int toCheck = VeblenPoint.intersection(VeblenPoint.line(b1, c1), VeblenPoint.line(b2, c2));
                                            if (!VeblenPoint.collinear(o, x, y, toCheck)) {
                                                continue exit;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return;
            }
        }
        Assertions.fail();
    }

    //@Test
    public void testDesargues() {
        for (int o : VeblenPoint.points()) {
            System.out.println(o);
            for (int l1 : VeblenPoint.lines(o)) {
                for (int l2 : VeblenPoint.lines(o)) {
                    if (l2 == l1) {
                        continue;
                    }
                    for (int l3 : VeblenPoint.lines(o)) {
                        if (l3 == l1 || l3 == l2) {
                            continue;
                        }
                        for (int a1 : VeblenPoint.points(l1)) {
                            if (a1 == o) {
                                continue;
                            }
                            for (int a2 : VeblenPoint.points(l1)) {
                                if (a2 == o || a1 == a2) {
                                    continue;
                                }
                                for (int b1 : VeblenPoint.points(l2)) {
                                    if (b1 == o) {
                                        continue;
                                    }
                                    for (int b2 : VeblenPoint.points(l2)) {
                                        if (b2 == o || b1 ==b2) {
                                            continue;
                                        }
                                        for (int c1 : VeblenPoint.points(l3)) {
                                            if (c1 == o) {
                                                continue;
                                            }
                                            for (int c2 : VeblenPoint.points(l3)) {
                                                if (c2 == o || c1 == c2) {
                                                    continue;
                                                }
                                                int i1 = VeblenPoint.intersection(VeblenPoint.line(a1, b1), VeblenPoint.line(a2, b2));
                                                int i2 = VeblenPoint.intersection(VeblenPoint.line(a1, c1), VeblenPoint.line(a2, c2));
                                                int i3 = VeblenPoint.intersection(VeblenPoint.line(c1, b1), VeblenPoint.line(c2, b2));
                                                assertTrue(VeblenPoint.collinear(i1, i2, i3), o + "" + a1 + a2 + b1 + b2 + c1 + c2 + i1 + i2 + i3);
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
    }

    //@Test
    public void testPascalian() {
        for (int l1 : VeblenPoint.lines()) {
            for (int l2 : VeblenPoint.lines()) {
                for (int a1 : VeblenPoint.points(l1)) {
                    for (int b1 : VeblenPoint.points(l1)) {
                        if (a1 == b1) {
                            continue;
                        }
                        for (int c1 : VeblenPoint.points(l1)) {
                            for (int a2 : VeblenPoint.points(l2)) {
                                if (a2 == a1 || a2 == b1 || a2 == c1) {
                                    continue;
                                }
                                for (int b2 : VeblenPoint.points(l2)) {
                                    if (b2 == a1 || b2 == b1 || b2 == c1 || b2 == a2) {
                                        continue;
                                    }
                                    for (int c2 : VeblenPoint.points(l2)) {
                                        if (c2 == a1 || c2 == b1 || c2 == c1 || c2 == a2 || c2 == b2) {
                                            continue;
                                        }
                                        int i1 = VeblenPoint.intersection(VeblenPoint.line(a1, c2), VeblenPoint.line(c1, a2));
                                        int i2 = VeblenPoint.intersection(VeblenPoint.line(a1, b2), VeblenPoint.line(b1, a2));
                                        if (i1 == i2) {
                                            continue;
                                        }
                                        int i3 = VeblenPoint.intersection(VeblenPoint.line(b1, c2), VeblenPoint.line(c1, b2));
                                        assertTrue(VeblenPoint.collinear(a1, b1, c1));
                                        assertTrue(VeblenPoint.collinear(a2, b2, c2));
                                        assertTrue(VeblenPoint.collinear(a1, c2, i1));
                                        assertTrue(VeblenPoint.collinear(a2, c1, i1));
                                        assertTrue(VeblenPoint.collinear(a1, b2, i2));
                                        assertTrue(VeblenPoint.collinear(a2, b1, i2));
                                        assertTrue(VeblenPoint.collinear(b1, c2, i3));
                                        assertTrue(VeblenPoint.collinear(b2, c1, i3));
                                        assertTrue(VeblenPoint.collinear(i1, i2, i3),
                                                a1 + " " + b1 + " " + c1 + " "
                                                        + a2 + " " + b2 + " " + c2 + " "
                                                        + i1 + " " + i2 + " " + i3);
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
