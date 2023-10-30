package ua.ihromant.mathutils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class VeblenPointTest {
    @Test
    public void testCorrectness() {
        assertEquals(91, VeblenPoint.POINTS.size());
        assertEquals(91, VeblenPoint.LINES.size());
        VeblenPoint.LINES.forEach(l -> assertEquals(10, l.size()));
        assertEquals(91, VeblenPoint.LOOKUP.size());
        VeblenPoint.LOOKUP.forEach((p1, map) -> {
            assertEquals(91, map.size());
            map.forEach((p2, line) -> {
                assertEquals(p1.equals(p2) ? 1 : 10, line.size());
                assertTrue(line.contains(p1));
                assertTrue(line.contains(p2));
            });
        });
        assertEquals(91, VeblenPoint.BEAMS.size());
        VeblenPoint.BEAMS.forEach((p, beam) -> {
            assertEquals(10, beam.size());
            beam.forEach(line -> assertTrue(line.contains(p)));
        });
        for (Set<VeblenPoint> l1 : VeblenPoint.LINES) {
            for (Set<VeblenPoint> l2 : VeblenPoint.LINES) {
                if (l1.equals(l2)) {
                    assertEquals(10, l1.stream().filter(l2::contains).count());
                } else {
                    assertEquals(1, l1.stream().filter(l2::contains).count());
                }
            }
        }
    }

    @Test
    public void testParallelTransitivity() {
        exit: for (Set<VeblenPoint> droppedLine : VeblenPoint.LINES) {
            System.out.println(droppedLine);
            for (VeblenPoint a : VeblenPoint.POINTS) {
                if (droppedLine.contains(a)) {
                    continue;
                }
                for (VeblenPoint b : VeblenPoint.POINTS) {
                    if (droppedLine.contains(b) || a.equals(b)) {
                        continue;
                    }
                    Set<VeblenPoint> abLine = VeblenPoint.line(a, b);
                    for (VeblenPoint u : VeblenPoint.POINTS) {
                        if (droppedLine.contains(u) || abLine.contains(u)) {
                            continue;
                        }
                        for (VeblenPoint v : VeblenPoint.POINTS) {
                            if (droppedLine.contains(v) || abLine.contains(v) || u.equals(v)) {
                                continue;
                            }
                            for (VeblenPoint x : VeblenPoint.POINTS) {
                                if (droppedLine.contains(x) || abLine.contains(x) || u.equals(x) || v.equals(x)) {
                                    continue;
                                }
                                for (VeblenPoint y : VeblenPoint.POINTS) {
                                    if (droppedLine.contains(y) || abLine.contains(y) || u.equals(y) || v.equals(y) || x.equals(y)) {
                                        continue;
                                    }
                                    Set<VeblenPoint> xyLine = VeblenPoint.line(x, y);
                                    Set<VeblenPoint> uvLine = VeblenPoint.line(u, v);
                                    if (droppedLine.contains(intersection(abLine, xyLine))
                                        && droppedLine.contains(intersection(abLine, uvLine))
                                        && !droppedLine.contains(intersection(uvLine, xyLine))) {
                                        continue exit;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("Successful: " + droppedLine);
        }
        fail();
    }

    @Test
    public void testPlayfair() {
        int max = 0;
        int min = Integer.MAX_VALUE;
        for (Set<VeblenPoint> dropped : VeblenPoint.LINES) {
            for (Set<VeblenPoint> line : VeblenPoint.LINES) {
                if (line.equals(dropped)) {
                    continue;
                }
                for (VeblenPoint p : VeblenPoint.POINTS) {
                    if (dropped.contains(p) || line.contains(p)) {
                        continue;
                    }
                    int counter = 0;
                    for (Set<VeblenPoint> parallel : VeblenPoint.BEAMS.get(p)) {
                        if (dropped.contains(intersection(parallel, line))) {
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
    public void decomposeParallelity() {
        Set<VeblenPoint> dropped = VeblenPoint.line(VeblenPoint.parse("A1"), VeblenPoint.parse("A3"));
        VeblenPoint a = VeblenPoint.parse("A2");
        VeblenPoint b = VeblenPoint.parse("A5");
        VeblenPoint x = VeblenPoint.parse("A12");
        VeblenPoint y = VeblenPoint.parse("B3");
        VeblenPoint u = VeblenPoint.parse("A4");
        VeblenPoint v = VeblenPoint.parse("A6");
        VeblenPoint abxy = intersection(VeblenPoint.line(a, b), VeblenPoint.line(x, y));
        VeblenPoint abuv = intersection(VeblenPoint.line(a, b), VeblenPoint.line(u, v));
        VeblenPoint xyuv = intersection(VeblenPoint.line(x, y), VeblenPoint.line(u, v));
        assertTrue(dropped.contains(abuv));
        assertTrue(dropped.contains(abxy));
        assertFalse(dropped.contains(xyuv));
        System.out.println(abxy + " " +  abuv + " " + xyuv);
    }

    @Test
    public void testDoubling() {
        exit: for (Set<VeblenPoint> droppedLine : VeblenPoint.LINES) {
            System.out.println(droppedLine);
            for (VeblenPoint a : VeblenPoint.POINTS) {
                if (droppedLine.contains(a)) {
                    continue;
                }
                for (VeblenPoint b : VeblenPoint.POINTS) {
                    if (droppedLine.contains(b) || a.equals(b)) {
                        continue;
                    }
                    Set<VeblenPoint> abLine = VeblenPoint.line(a, b);
                    for (VeblenPoint c : abLine) {
                        if (droppedLine.contains(c) || a.equals(c) || b.equals(c)) {
                            continue;
                        }
                        assertSame(abLine, VeblenPoint.line(a, c));
                        for (VeblenPoint u : VeblenPoint.POINTS) {
                            if (droppedLine.contains(u) || abLine.contains(u)) {
                                continue;
                            }
                            for (VeblenPoint v : VeblenPoint.POINTS) {
                                if (droppedLine.contains(v) || abLine.contains(v) || u.equals(v)) {
                                    continue;
                                }
                                for (VeblenPoint x : VeblenPoint.POINTS) {
                                    if (droppedLine.contains(x) || abLine.contains(x) || u.equals(x) || v.equals(x)) {
                                        continue;
                                    }
                                    for (VeblenPoint y : VeblenPoint.POINTS) {
                                        if (droppedLine.contains(y) || abLine.contains(y) || u.equals(y) || v.equals(y) || x.equals(y)) {
                                            continue;
                                        }
                                        Set<VeblenPoint> xyLine = VeblenPoint.line(x, y);
                                        Set<VeblenPoint> uvLine = VeblenPoint.line(u, v);
                                        Set<VeblenPoint> axLine = VeblenPoint.line(a, x);
                                        Set<VeblenPoint> byLine = VeblenPoint.line(b, y);
                                        Set<VeblenPoint> xbLine = VeblenPoint.line(x, b);
                                        Set<VeblenPoint> ycLine = VeblenPoint.line(y, c);
                                        Set<VeblenPoint> auLine = VeblenPoint.line(a, u);
                                        Set<VeblenPoint> bvLine = VeblenPoint.line(b, v);
                                        Set<VeblenPoint> ubLine = VeblenPoint.line(u, b);
                                        Set<VeblenPoint> vcLine = VeblenPoint.line(v, c);
                                        if (parallel(xyLine, abLine, droppedLine)
                                                && parallel(abLine, uvLine, droppedLine)) {
                                            // [A1, A3, A9, G0, F0, E0, D0, C0, B0, A0]A2A5A12B3A4A6
                                            assertTrue(parallel(xyLine, uvLine, droppedLine));
                                            if (parallel(axLine, byLine, droppedLine)
                                                && parallel(xbLine, ycLine, droppedLine)
                                                && parallel(auLine, bvLine, droppedLine)
                                                && !parallel(ubLine, vcLine, droppedLine)) {
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
            System.out.println("Successful: " + droppedLine);
        }
        fail();
    }

    public boolean parallel(Set<VeblenPoint> first, Set<VeblenPoint> second, Set<VeblenPoint> dropped) {
        return first.equals(second) || dropped.contains(intersection(first, second));
    }

    //@Test
    public void testSmallDesargue() {
        for (VeblenPoint o : VeblenPoint.POINTS) {
            System.out.println(o);
            exit: for (Set<VeblenPoint> l0 : VeblenPoint.BEAMS.get(o)) {
                for (Set<VeblenPoint> l1 : VeblenPoint.BEAMS.get(o)) {
                    if (l1.equals(l0)) {
                        continue;
                    }
                    for (Set<VeblenPoint> l2 : VeblenPoint.BEAMS.get(o)) {
                        if (l2.equals(l0) || l2.equals(l1)) {
                            continue;
                        }
                        for (Set<VeblenPoint> l3 : VeblenPoint.BEAMS.get(o)) {
                            if (l3.equals(l0) || l3.equals(l1) || l3.equals(l2)) {
                                continue;
                            }
                            for (VeblenPoint x : l0) {
                                if (x.equals(o)) {
                                    continue;
                                }
                                for (VeblenPoint y : l0) {
                                    if (y.equals(x) || y.equals(o)) {
                                        continue;
                                    }
                                    assertTrue(VeblenPoint.collinear(o, x, y));
                                    for (VeblenPoint a1 : l1) {
                                        if (a1.equals(o)) {
                                            continue;
                                        }
                                        for (VeblenPoint a2 : l1) {
                                            if (a2.equals(a1) || a2.equals(o)) {
                                                continue;
                                            }
                                            VeblenPoint b1 = intersection(VeblenPoint.line(x, a1), l2);
                                            VeblenPoint b2 = intersection(VeblenPoint.line(x, a2), l2);
                                            VeblenPoint c1 = intersection(VeblenPoint.line(y, a1), l3);
                                            VeblenPoint c2 = intersection(VeblenPoint.line(y, a2), l3);
                                            VeblenPoint toCheck = intersection(VeblenPoint.line(b1, c1), VeblenPoint.line(b2, c2));
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
        for (VeblenPoint o : VeblenPoint.POINTS) {
            System.out.println(o);
            for (Set<VeblenPoint> l1 : VeblenPoint.BEAMS.get(o)) {
                for (Set<VeblenPoint> l2 : VeblenPoint.BEAMS.get(o)) {
                    if (l2.equals(l1)) {
                        continue;
                    }
                    for (Set<VeblenPoint> l3 : VeblenPoint.BEAMS.get(o)) {
                        if (l3.equals(l1) || l3.equals(l2)) {
                            continue;
                        }
                        for (VeblenPoint a1 : l1) {
                            if (a1.equals(o)) {
                                continue;
                            }
                            for (VeblenPoint a2 : l1) {
                                if (a2.equals(o) || a1.equals(a2)) {
                                    continue;
                                }
                                for (VeblenPoint b1 : l2) {
                                    if (b1.equals(o)) {
                                        continue;
                                    }
                                    for (VeblenPoint b2 : l2) {
                                        if (b2.equals(o) || b1.equals(b2)) {
                                            continue;
                                        }
                                        for (VeblenPoint c1 : l3) {
                                            if (c1.equals(o)) {
                                                continue;
                                            }
                                            for (VeblenPoint c2 : l3) {
                                                if (c2.equals(o) || c1.equals(c2)) {
                                                    continue;
                                                }
                                                VeblenPoint i1 = intersection(VeblenPoint.line(a1, b1), VeblenPoint.line(a2, b2));
                                                VeblenPoint i2 = intersection(VeblenPoint.line(a1, c1), VeblenPoint.line(a2, c2));
                                                VeblenPoint i3 = intersection(VeblenPoint.line(c1, b1), VeblenPoint.line(c2, b2));
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
        for (Set<VeblenPoint> l1 : VeblenPoint.LINES) {
            for (Set<VeblenPoint> l2 : VeblenPoint.LINES) {
                for (VeblenPoint a1 : l1) {
                    for (VeblenPoint b1 : l1) {
                        if (a1.equals(b1)) {
                            continue;
                        }
                        for (VeblenPoint c1 : l1) {
                            for (VeblenPoint a2 : l2) {
                                if (a2.equals(a1) || a2.equals(b1) || a2.equals(c1)) {
                                    continue;
                                }
                                for (VeblenPoint b2 : l2) {
                                    if (b2.equals(a1) || b2.equals(b1) || b2.equals(c1) || b2.equals(a2)) {
                                        continue;
                                    }
                                    for (VeblenPoint c2 : l2) {
                                        if (c2.equals(a1) || c2.equals(b1) || c2.equals(c1) || c2.equals(a2) || c2.equals(b2)) {
                                            continue;
                                        }
                                        VeblenPoint i1 = intersection(VeblenPoint.line(a1, c2), VeblenPoint.line(c1, a2));
                                        VeblenPoint i2 = intersection(VeblenPoint.line(a1, b2), VeblenPoint.line(b1, a2));
                                        if (i1.equals(i2)) {
                                            continue;
                                        }
                                        VeblenPoint i3 = intersection(VeblenPoint.line(b1, c2), VeblenPoint.line(c1, b2));
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

    private static VeblenPoint intersection(Set<VeblenPoint> l1, Set<VeblenPoint> l2) {
        return l1.stream().filter(l2::contains).findAny().orElseThrow();
    }
}
