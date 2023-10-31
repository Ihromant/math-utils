package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

public class NearFieldTest {
    private static final int NON_ZERO = NearField.values().length - 1;

    private static final NearPoint[] POINTS = Arrays.stream(NearField.values())
            .flatMap(i -> Arrays.stream(NearField.values()).map(j -> new NearPoint(i, j))).toArray(NearPoint[]::new);

    private static final List<List<NearPoint>> LINES = Stream.concat(
                    Stream.of(Arrays.stream(NearField.values()).skip(1)
                            .map(nf -> new NearPoint(NearField.ZERO, nf)).collect(Collectors.toList())),
                    Arrays.stream(NearField.values()).map(nf -> Arrays.stream(NearField.values()).skip(1)
                            .map(cf -> cf.mul(new NearPoint(NearField.PL_1, nf))).collect(Collectors.toList()))).toList();

    private static final List<Triple> TRIPLES = StreamSupport.stream(distinct().spliterator(), false).toList();

    @Test
    public void testPappus() {
        for (int i = 0; i < LINES.size(); i++) {
            for (int j = 0; j < LINES.size(); j++) {
                if (i == j) {
                    continue;
                }
                List<NearPoint> line1 = LINES.get(i);
                List<NearPoint> line2 = LINES.get(j);
                for (Triple t1 : TRIPLES) {
                    for (Triple t2 : TRIPLES) {
                        NearPoint a1 = line1.get(t1.a());
                        NearPoint b1 = line1.get(t1.b());
                        NearPoint c1 = line1.get(t1.c());
                        NearPoint a2 = line2.get(t2.a());
                        NearPoint b2 = line2.get(t2.b());
                        NearPoint c2 = line2.get(t2.c());
                        NearPoint a1b2 = b2.sub(a1);
                        NearPoint b1c2 = c2.sub(b1);
                        NearPoint b1a2 = a2.sub(b1);
                        NearPoint c1b2 = b2.sub(c1);
                        NearPoint a1a2 = a2.sub(a1);
                        NearPoint c1c2 = c2.sub(c1);
                        if (a1b2.parallel(b1c2) && b1a2.parallel(c1b2) && !a1a2.parallel(c1c2)) {
                            System.out.println("a1: " + a1 + ", " + "b1: " + b1 + ", " +
                                    "c1: " + c1 + ", " + "a2: " + a2 + ", " +
                                    "b2: " + b2 + ", " + "c2: " + c2 + ", " +
                                    "a1b2: " + a1b2 + ", " + "b1c2: " + b1c2 + ", " +
                                    "b1a2: " + b1a2 + ", " + "c1b2: " + c1b2 + ", " +
                                    "a1a2: " + a1a2 + ", " + "c1c2: " + c1c2);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testParallelity() {
        for (NearPoint p1 : POINTS) {
            assertTrue(p1.parallel(p1));
            for (NearPoint p2 : POINTS) {
                assertEquals(p1.parallel(p2), p2.parallel(p1));
                for (NearPoint p3 : POINTS) {
                    if (p1.parallel(p2) && p2.parallel(p3)) {
                        assertTrue(p1.parallel(p3));
                    }
                }
            }
        }
    }

    @Test
    public void testThales() {
        for (NearPoint a1 : POINTS) {
            for (NearPoint a2 : POINTS) {
                if (a1.equals(a2)) {
                    continue;
                }
                List<NearPoint> a2a1Line = shift(a1, line(a2.sub(a1)));
                for (NearPoint b1 : POINTS) {
                    if (b1.equals(a1) || b1.equals(a2)) {
                        continue;
                    }
                    for (NearPoint b2 : POINTS) {
                        NearPoint b2b1 = b2.sub(b1);
                        if (b2.equals(b1) || b2.equals(a1) || b2.equals(a2) || !b2b1.parallel(a2.sub(a1))) {
                            continue;
                        }
                        List<NearPoint> b2b1Line = shift(b1, line(b2.sub(b1)));
                        if (!intersection(a2a1Line, b2b1Line).isEmpty()) {
                            continue;
                        }
                        for (NearPoint c1 : POINTS) {
                            if (c1.equals(b2) || c1.equals(b1) || c1.equals(a1) || c1.equals(a2)) {
                                continue;
                            }
                            for (NearPoint c2 : POINTS) {
                                if (c1.equals(c2) || c2.equals(b2) || c2.equals(b1) || c2.equals(a1) || c2.equals(a2) || !b2b1.parallel(c2.sub(c1))) {
                                    continue;
                                }
                                List<NearPoint> c2c1Line = shift(c1, line(c2.sub(c1)));
                                if (!intersection(c2c1Line, a2a1Line).isEmpty() || !intersection(c2c1Line, b2b1Line).isEmpty()) {
                                    continue;
                                }
                                if (b1.sub(a1).parallel(b2.sub(a2)) && c1.sub(b1).parallel(c2.sub(b2))) {
                                    if (!c1.sub(a1).parallel(c2.sub(a2))) {
                                        List<NearPoint> b1a1Line = shift(a1, line(b1.sub(a1)));
                                        List<NearPoint> b2a2Line = shift(a2, line(b2.sub(a2)));
                                        List<NearPoint> c1b1Line = shift(b1, line(c1.sub(b1)));
                                        List<NearPoint> c2b2Line = shift(b2, line(c2.sub(b2)));
                                        List<NearPoint> c1a1Line = shift(a1, line(c1.sub(a1)));
                                        List<NearPoint> c2a2Line = shift(a2, line(c2.sub(a2)));
                                        assertTrue(intersection(a2a1Line, b2b1Line).isEmpty());
                                        assertTrue(intersection(a2a1Line, c2c1Line).isEmpty());
                                        assertTrue(intersection(c2c1Line, b2b1Line).isEmpty());
                                        assertTrue(intersection(b1a1Line, b2a2Line).isEmpty());
                                        assertTrue(intersection(c1b1Line, c2b2Line).isEmpty());
                                        assertFalse(intersection(c1a1Line, c2a2Line).isEmpty());
                                        System.out.println("a1=" + a1 + ",a2=" + a2 + ",b1=" + b1 + ",b2=" + b2 + ",c1=" + c1 + "c2=" + c2
                                                + ",a1a2=" + a2a1Line + ",b1b2=" + b2b1Line
                                                + ",c1c2=" + c2c1Line + ",a1b1=" + b1a1Line
                                                + ",a2b2=" + b2a2Line + ",c1b1=" + c1b1Line
                                                + ",c2b2=" + c2b2Line + ",c1a1=" + c1a1Line
                                                + ",c2a2=" + c2a2Line);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testTriples() {
        assertEquals(56, StreamSupport.stream(distinct().spliterator(), false)
                .peek(System.out::println).collect(Collectors.toSet()).size());
    }

    @Test
    public void testUniquePoints() {
        assertEquals(LINES.size() * NON_ZERO, LINES.stream().flatMap(List::stream).collect(Collectors.toSet()).size());
    }

    @Test // a + b = b + a
    public void testCommutativeAddition() {
        for (NearField a : NearField.values()) {
            for (NearField b : NearField.values()) {
                assertSame(a.add(b), b.add(a));
            }
        }
    }

    @Test // (a + b) + c = a + (b + c)
    public void testAssociativeAddition() {
        for (NearField a : NearField.values()) {
            for (NearField b : NearField.values()) {
                for (NearField c : NearField.values()) {
                    assertSame(a.add(b.add(c)), a.add(b).add(c));
                }
            }
        }
    }

    @Test // (a * b) * c = (a * b) * c
    public void testAssociativeMultiplication() {
        for (NearField a : NearField.values()) {
            for (NearField b : NearField.values()) {
                for (NearField c : NearField.values()) {
                    assertSame(a.mul(b.mul(c)), a.mul(b).mul(c));
                }
            }
        }
    }

    @Test // a * b = b * a or -b * a
    public void testCommutativeMultiplication() {
        for (NearField a : NearField.values()) {
            for (NearField b : NearField.values()) {
                if (a.ordinal() < 3 || b.ordinal() < 3 || (a.ordinal() + 1) / 2 == (b.ordinal() + 1) / 2) {
                    assertSame(a.mul(b), b.mul(a));
                } else {
                    assertSame(a.mul(b), b.mul(a).neg());
                }
            }
        }
    }

    @Test // (a + b) * c = a * c + b * c
    public void testRightDistributiveMultiplication() {
        for (NearField a : NearField.values()) {
            for (NearField b : NearField.values()) {
                for (NearField c : NearField.values()) {
                    assertSame(a.add(b).mul(c), a.mul(c).add(b.mul(c)));
                }
            }
        }
    }

    @Test // a * (b + c) != a * b + a * c
    public void testLeftDistributiveMultiplication() {
        int counter = 0;
        for (NearField a : NearField.values()) {
            for (NearField b : NearField.values()) {
                for (NearField c : NearField.values()) {
                    if (a.mul(b.add(c)) != a.mul(b).add(a.mul(c))) {
                        counter++;
                        //System.out.println(a + " " + b + " " + c);
                    }
                }
            }
        }
        assertEquals(6 * 6 * 8, counter);
    }

    private record Triple(int a, int b, int c) {

    }

    private static Iterable<Triple> distinct() {
        return () -> new Iterator<>() {
            private int idx = calculateNext(0);

            private int calculateNext(int curr) {
                while (true) {
                    int a = curr / NON_ZERO / NON_ZERO;
                    int b = curr / NON_ZERO % NON_ZERO;
                    int c = curr % NON_ZERO;
                    if (c < b && b < a) {
                        break;
                    } else {
                        curr++;
                    }
                }
                return curr;
            }

            @Override
            public boolean hasNext() {
                return idx < NON_ZERO * NON_ZERO * NON_ZERO;
            }

            @Override
            public Triple next() {
                int res = idx;
                idx = calculateNext(idx + 1);
                return new Triple(res / NON_ZERO / NON_ZERO, res / NON_ZERO % NON_ZERO, res % NON_ZERO);
            }
        };
    }

    @Test
    public void testProjectiveCorrectness() {
        for (int l : NearPoint.lines()) {
            assertEquals(10, NearPoint.line(l).cardinality());
        }
        for (int p : NearPoint.points()) {
            assertEquals(10, NearPoint.point(p).cardinality());
        }
        for (int p1 : NearPoint.points()) {
            for (int p2 : NearPoint.points()) {
                if (p1 != p2) {
                    BitSet line = NearPoint.line(NearPoint.line(p1, p2));
                    assertTrue(line.get(p1));
                    assertTrue(line.get(p2));
                }
            }
        }
        for (int l1 : NearPoint.lines()) {
            for (int l2 : NearPoint.lines()) {
                if (l1 != l2) {
                    BitSet intersection = NearPoint.point(NearPoint.intersection(l1, l2));
                    assertTrue(intersection.get(l1));
                    assertTrue(intersection.get(l2));
                }
            }
        }
        for (int p : NearPoint.points()) {
            for (int l : NearPoint.lines(p)) {
                assertTrue(NearPoint.line(l).get(p));
            }
        }
        for (int l : NearPoint.lines()) {
            for (int p : NearPoint.points(l)) {
                assertTrue(NearPoint.point(p).get(l));
            }
        }
    }

    @Test
    public void testPlayfair() {
        int max = 0;
        int min = Integer.MAX_VALUE;
        for (int dr : NearPoint.lines()) {
            for (int l : NearPoint.lines()) {
                if (l == dr) {
                    continue;
                }
                BitSet dropped = NearPoint.line(dr);
                BitSet line = NearPoint.line(l);
                for (int p : NearPoint.points()) {
                    if (dropped.get(p) || line.get(p)) {
                        continue;
                    }
                    int counter = 0;
                    for (int parallel : NearPoint.lines(p)) {
                        if (dropped.get(NearPoint.intersection(parallel, l))) {
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
        exit: for (int dl : NearPoint.lines()) {
            BitSet droppedLine = NearPoint.line(dl);
            System.out.println(NearPoint.lineToString(dl));
            for (int a : NearPoint.points()) {
                if (droppedLine.get(a)) {
                    continue;
                }
                for (int b : NearPoint.points()) {
                    if (droppedLine.get(b) || a == b) {
                        continue;
                    }
                    int ab = NearPoint.line(a, b);
                    BitSet abLine = NearPoint.line(ab);
                    for (int c : NearPoint.points(ab)) {
                        if (droppedLine.get(c) || a == c || b == c) {
                            continue;
                        }
                        assertEquals(ab, NearPoint.line(a, c));
                        for (int u : NearPoint.points()) {
                            if (droppedLine.get(u) || abLine.get(u)) {
                                continue;
                            }
                            for (int v : NearPoint.points()) {
                                if (droppedLine.get(v) || abLine.get(v) || u == v) {
                                    continue;
                                }
                                for (int x : NearPoint.points()) {
                                    if (droppedLine.get(x) || abLine.get(x) || u == x || v == x) {
                                        continue;
                                    }
                                    for (int y : NearPoint.points()) {
                                        if (droppedLine.get(y) || abLine.get(y) || u == y || v == y || x == y) {
                                            continue;
                                        }
                                        int xyLine = NearPoint.line(x, y);
                                        int uvLine = NearPoint.line(u, v);
                                        int axLine = NearPoint.line(a, x);
                                        int byLine = NearPoint.line(b, y);
                                        int xbLine = NearPoint.line(x, b);
                                        int ycLine = NearPoint.line(y, c);
                                        int auLine = NearPoint.line(a, u);
                                        int bvLine = NearPoint.line(b, v);
                                        int ubLine = NearPoint.line(u, b);
                                        int vcLine = NearPoint.line(v, c);
                                        if (NearPoint.parallel(xyLine, ab, dl)
                                                && NearPoint.parallel(ab, uvLine, dl)) {
                                            assertTrue(NearPoint.parallel(xyLine, uvLine, dl), NearPoint.lineToString(xyLine)
                                                    + " " + NearPoint.lineToString(ab) + " " + NearPoint.lineToString(uvLine) + " " + NearPoint.lineToString(dl));
                                            if (NearPoint.parallel(axLine, byLine, dl)
                                                    && NearPoint.parallel(xbLine, ycLine, dl)
                                                    && NearPoint.parallel(auLine, bvLine, dl)
                                                    && !NearPoint.parallel(ubLine, vcLine, dl)) {
                                                System.out.println("a=" + NearPoint.pointToString(a) + ",b=" + NearPoint.pointToString(b)
                                                        + ",c=" + NearPoint.pointToString(c) + ",x=" + NearPoint.pointToString(x)
                                                        + ",y=" + NearPoint.pointToString(y) + ",u=" + NearPoint.pointToString(u)
                                                        + ",v=" + NearPoint.pointToString(v) + ",ab=" + NearPoint.lineToString(ab)
                                                        + ",xy=" + NearPoint.lineToString(xyLine) + ",uv=" + NearPoint.lineToString(uvLine)
                                                        + ",ax=" + NearPoint.lineToString(axLine) + ",by=" + NearPoint.lineToString(byLine)
                                                        + ",bx=" + NearPoint.lineToString(xbLine) + ",cy=" + NearPoint.lineToString(ycLine)
                                                        + ",au=" + NearPoint.lineToString(auLine) + ",bv=" + NearPoint.lineToString(bvLine)
                                                        + ",ub=" + NearPoint.lineToString(ubLine) + ",vc=" + NearPoint.lineToString(vcLine)
                                                        + ",intersection ub and vc =" + NearPoint.pointToString(NearPoint.intersection(ubLine, vcLine)));
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
            System.out.println("Doubling: " + NearPoint.lineToString(dl));
            return;
        }
        fail();
    }

    private static List<NearPoint> line(NearPoint from) {
        return LINES.stream().filter(l -> l.contains(from)).findAny().orElseThrow();
    }

    private static List<NearPoint> shift(NearPoint v, List<NearPoint> line) {
        return line.stream().map(p -> p.add(v)).collect(Collectors.toList());
    }

    private static List<NearPoint> intersection(List<NearPoint> l1, List<NearPoint> l2) {
        return l1.stream().filter(l2::contains).collect(Collectors.toList());
    }
}
