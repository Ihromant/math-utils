package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class TaoPointTest {
    private static final Iterable<Integer> space = () -> IntStream.range(0, TaoPoint.SIZE).iterator();

    private static Iterable<Integer> line(int a, int b) {
        return () -> TaoPoint.hull(a, b).stream().iterator();
    }

    @Test
    public void testCorrectness() {
        for (int x : space) {
            for (int y : space) {
                int z = TaoPoint.add(x, y);
                assertEquals(x, TaoPoint.add(y, z));
                assertEquals(y, TaoPoint.add(x, z));
            }
        }
    }

    @Test
    public void testReflexivity() {
        for (int x : space) {
            assertEquals(x, TaoPoint.add(x, x));
        }
    }

    @Test
    public void testSymmetric() {
        for (int x : space) {
            for (int y : space) {
                assertEquals(TaoPoint.add(x, y), TaoPoint.add(y, x));
            }
        }
    }

    @Test
    public void testFailsDistributive() { // difference to Hall Triple System
        int counter = 0;
        for (int x : space) {
            for (int y : space) {
                for (int z : space) {
                    if (TaoPoint.add(x, TaoPoint.add(y, z)) != TaoPoint.add(TaoPoint.add(x, y), TaoPoint.add(x, z))) {
                        counter++;
                    }
                }
            }
        }
        assertEquals(11664, counter);
    }

    @Test
    public void checkLinealIdentity() {
        for (int x : space) {
            for (int y : space) {
                if (!TaoPoint.collinear(x, y, x)) {
                    continue;
                }
                assertEquals(x, y);
            }
        }
    }

    @Test
    public void checkLinealReflexivity() {
        for (int x : space) {
            for (int y : space) {
                assertTrue(TaoPoint.collinear(x, x, y));
                assertTrue(TaoPoint.collinear(x, y, y));
            }
        }
    }

    @Test
    public void checkLinearExchange() {
        for (int x : space) {
            for (int y : space) {
                for (int a : space) {
                    for (int b : space) {
                        if (!TaoPoint.collinear(a, x, b) || !TaoPoint.collinear(a, y, b) || x == y) {
                            continue;
                        }
                        assertTrue(TaoPoint.collinear(x, a, y));
                        assertTrue(TaoPoint.collinear(x, b, y));
                    }
                }
            }
        }
    }

    @Test
    public void testPlayfair() {
        for (int o : space) { // linear form
            for (int x : space) {
                BitSet ox = TaoPoint.hull(o, x);
                for (int y : space) {
                    outerB: for (int a : line(x, y)) {
                        if (ox.get(a)) {
                            continue;
                        }
                        for (int b : line(o, y)) {
                            boolean forEach = true;
                            for (int c : line(o, y)) {
                                BitSet ac = TaoPoint.hull(a, c);
                                forEach = forEach && (b != c == ac.intersects(ox));
                            }
                            if (forEach) {
                                break outerB;
                            }
                        }
                        fail();
                    }
                }
            }
        }
        for (int o : space) { // collinear form
            for (int x : space) {
                for (int y : space) {
                    outerB: for (int a : space) {
                        if (!TaoPoint.collinear(x, a, y) || TaoPoint.collinear(o, a, x)) {
                            continue;
                        }
                        for (int b : space) {
                            if (!TaoPoint.collinear(o, b, y)) {
                                continue;
                            }
                            boolean forEach = true;
                            for (int c : space) {
                                if (!TaoPoint.collinear(o, c, y)) {
                                    continue;
                                }
                                if (c != b) {
                                    forEach = forEach && IntStream.range(0, TaoPoint.SIZE).anyMatch(t -> TaoPoint.collinear(c, t, a) && TaoPoint.collinear(o, t, x));
                                } else {
                                    forEach = forEach && IntStream.range(0, TaoPoint.SIZE).noneMatch(t -> TaoPoint.collinear(c, t, a) && TaoPoint.collinear(o, t, x));
                                }
                            }
                            if (forEach) {
                                break outerB;
                            }
                        }
                        fail();
                    }
                }
            }
        }
    }

    @Test
    public void testSmallHulls() {
        for (int x : space) {
            assertEquals(1, TaoPoint.hull(x).cardinality());
            for (int y : space) {
                BitSet line = TaoPoint.hull(x, y);
                if (x == y) {
                    assertEquals(1, line.cardinality());
                } else {
                    assertEquals(3, line.cardinality());
                }
                for (int z : space) {
                    assertEquals(line.get(z), TaoPoint.collinear(x, z, y));
                }
            }
        }
    }

    @Test
    public void testThreePointClosure() {
        Map<Integer, Integer> frequencies = new HashMap<>();
        for (int x : space) {
            for (int y : space) {
                for (int z : space) {
                    BitSet hull = TaoPoint.hull(x, y, z);
                    frequencies.compute(hull.cardinality(), (k, v) -> v == null ? 1 : v + 1);
                }
            }
        }
        assertEquals(Map.of(1, 27, 3, 2808, 9, 5184, 27, 11664), frequencies);
        assertEquals(TaoPoint.SIZE * TaoPoint.SIZE * TaoPoint.SIZE, frequencies.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    public void printHull() {
        int x = TaoPoint.parse("(-1,-1,-1)");
        int y = TaoPoint.parse("(-1,0,-1)");
        int z = TaoPoint.parse("(0,-1,-1)");
        BitSet base = new BitSet(TaoPoint.SIZE);
        base.set(x);
        base.set(y);
        base.set(z);
        BitSet next = TaoPoint.next(base);
        while (next.cardinality() > base.cardinality()) {
            System.out.println(base.cardinality() + " " + base.stream().mapToObj(TaoPoint::toString).collect(Collectors.joining(",", "{", "}")));
            base = next;
            next = TaoPoint.next(base);
        }
        System.out.println(next.cardinality() + " " + next.stream().mapToObj(TaoPoint::toString).collect(Collectors.joining(",", "{", "}")));
    }

    @Test
    public void testRegularityBreak() {
        int o = TaoPoint.parse("(-1,-1,-1)");
        int a = TaoPoint.parse("(-1,-1,-1)");
        int b = TaoPoint.parse("(-1,0,-1)");
        int u = TaoPoint.parse("(0,-1,-1)");
        int v = TaoPoint.parse("(-1,-1,-1)");
        int x = TaoPoint.parse("(-1,-1,-1)");
        int y = TaoPoint.parse("(1,1,-1)");
        int z = TaoPoint.parse("(0,0,-1)");
        assertTrue(TaoPoint.collinear(o, v, u));
        assertTrue(TaoPoint.collinear(a, x, v));
        assertTrue(TaoPoint.collinear(b, y, u));
        assertTrue(TaoPoint.collinear(x, z, y));
        Iterable<Integer> sLine = () -> TaoPoint.hull(o, a).stream().iterator();
        Iterable<Integer> tLine = () -> TaoPoint.hull(o, b).stream().iterator();
        Iterable<Integer> wLine = () -> TaoPoint.hull(o, u).stream().iterator();
        for (int s : sLine) {
            for (int t : tLine) {
                for (int w : wLine) {
                    Iterable<Integer> cLine = () -> TaoPoint.hull(s, t).stream().iterator();
                    for (int c : cLine) {
                        assertFalse(TaoPoint.collinear(c, z, w));
                    }
                }
            }
        }
    }

    @Test
    public void testSubparallelity() {
        BitSet l1 = TaoPoint.hull(0, 3, 7);
        BitSet l2 = TaoPoint.hull(9, 10, 11);
        assertEquals(3, l1.cardinality());
        assertEquals(3, l2.cardinality());
        System.out.println(l1.stream().mapToObj(TaoPoint::toString).collect(Collectors.joining(", ")));
        System.out.println(l2.stream().mapToObj(TaoPoint::toString).collect(Collectors.joining(", ")));
        System.out.println(TaoPoint.hull(0, 3, 7, 9));
        System.out.println(TaoPoint.hull(0, 3, 7, 10));
        System.out.println(TaoPoint.hull(0, 3, 7, 11));
        System.out.println(TaoPoint.hull(9, 10, 11, 0).stream().mapToObj(TaoPoint::toString).collect(Collectors.joining(", ")));
        System.out.println(TaoPoint.hull(9, 10, 11, 3).stream().mapToObj(TaoPoint::toString).collect(Collectors.joining(", ")));
        System.out.println(TaoPoint.hull(9, 10, 11, 7).stream().mapToObj(TaoPoint::toString).collect(Collectors.joining(", ")));
    }

    @Test
    public void findSubparallelitySymmetrityBreak() {
        for (int a = 0; a < TaoPoint.SIZE; a++) {
            for (int b = a + 1; b < TaoPoint.SIZE; b++) {
                BitSet l1 = TaoPoint.hull(a, b);
                for (int c = 0; c < TaoPoint.SIZE; c++) {
                    for (int d = c + 1; d < TaoPoint.SIZE; d++) {
                        BitSet l2 = TaoPoint.hull(c, d);
                        if (l1.equals(l2)) {
                            continue;
                        }
                        boolean l1Subl2 = l1.stream().allMatch(p1 -> {
                            BitSet un = (BitSet) l2.clone();
                            un.set(p1);
                            BitSet hull = TaoPoint.hull(un.stream().toArray());
                            return l1.stream().allMatch(hull::get);
                        });
                        boolean l2Subl1 = l2.stream().allMatch(p2 -> {
                            BitSet un = (BitSet) l1.clone();
                            un.set(p2);
                            BitSet hull = TaoPoint.hull(un.stream().toArray());
                            return l2.stream().allMatch(hull::get);
                        });
                        if (!l1Subl2 && l2Subl1) {
                            System.out.println(l1 + " " + l2);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testPappusDesargues() {
        for (BitSet l1 : TaoPoint.lines()) {
            for (BitSet l2 : TaoPoint.lines()) {
                if (!TaoPoint.parallel(l1, l2)) {
                    continue;
                }
                int[] pts1 = l1.stream().toArray();
                int[] pts2 = l2.stream().toArray();
                Combinatorics.permutations(pts1).forEach(prm1 -> Combinatorics.permutations(pts2).forEach(prm2 -> {
                    if (TaoPoint.parallel(TaoPoint.line(prm1[0], prm2[1]), TaoPoint.line(prm1[2], prm2[1]))
                            && TaoPoint.parallel(TaoPoint.line(prm2[0], prm1[1]), TaoPoint.line(prm2[2], prm1[1]))
                            && !TaoPoint.parallel(TaoPoint.line(prm1[0], prm2[0]), TaoPoint.line(prm1[2], prm2[2]))) {
                        System.out.println("Fail Pappus");
                    }
                }));
            }
        }
        System.out.println("Lines");
        for (BitSet l : TaoPoint.lines()) {
            System.out.println(l);
        }
        for (BitSet l1 : TaoPoint.lines()) {
            for (BitSet l2 : TaoPoint.lines()) {
                if (!TaoPoint.parallel(l1, l2)) {
                    continue;
                }
                for (BitSet l3 : TaoPoint.lines()) {
                    if (!TaoPoint.parallel(l1, l3) || !TaoPoint.parallel(l2, l3)) {
                        continue;
                    }
                    for (int a1 : l1.stream().toArray()) {
                        for (int b1 : l1.stream().toArray()) {
                            for (int a2 : l2.stream().toArray()) {
                                for (int b2 : l2.stream().toArray()) {
                                    for (int a3 : l3.stream().toArray()) {
                                        for (int b3 : l3.stream().toArray()) {
                                            if (TaoPoint.parallel(TaoPoint.line(a1, a2), TaoPoint.line(b1, b2))
                                                    && TaoPoint.parallel(TaoPoint.line(a3, a2), TaoPoint.line(b3, b2))
                                                    && !TaoPoint.parallel(TaoPoint.line(a1, a3), TaoPoint.line(b1, b3))) {
                                                System.out.println("Fail Par Desargues");
                                                System.out.println("l1 " + l1);
                                                System.out.println("l2 " + l2);
                                                System.out.println("l3 " + l3);
                                                System.out.println("a1 " + a1);
                                                System.out.println("a2 " + a2);
                                                System.out.println("a3 " + a3);
                                                System.out.println("b1 " + b1);
                                                System.out.println("b2 " + b2);
                                                System.out.println("b3 " + b3);
                                                System.out.println("a1a2 " + TaoPoint.line(a1, a2));
                                                System.out.println("b1b2 " + TaoPoint.line(b1, b2));
                                                System.out.println("a2a3 " + TaoPoint.line(a2, a3));
                                                System.out.println("b2b3 " + TaoPoint.line(b2, b3));
                                                System.out.println("a1a3 " + TaoPoint.line(a1, a3));
                                                System.out.println("b1b3 " + TaoPoint.line(b1, b3));
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
        for (int o : TaoPoint.points()) {
            for (BitSet l1 : TaoPoint.lines()) {
                if (!l1.get(o)) {
                    continue;
                }
                for (BitSet l2 : TaoPoint.lines()) {
                    if (!l2.get(o) || l2.equals(l1)) {
                        continue;
                    }
                    for (BitSet l3 : TaoPoint.lines()) {
                        if (!l3.get(o) || l3.equals(l2) || l3.equals(l1)) {
                            continue;
                        }
                        for (int a1 : l1.stream().toArray()) {
                            if (a1 == o) {
                                continue;
                            }
                            for (int b1 : l1.stream().toArray()) {
                                if (b1 == o) {
                                    continue;
                                }
                                for (int a2 : l2.stream().toArray()) {
                                    if (a2 == o) {
                                        continue;
                                    }
                                    for (int b2 : l2.stream().toArray()) {
                                        if (b2 == o) {
                                            continue;
                                        }
                                        for (int a3 : l3.stream().toArray()) {
                                            if (a3 == o) {
                                                continue;
                                            }
                                            for (int b3 : l3.stream().toArray()) {
                                                if (b3 == o) {
                                                    continue;
                                                }
                                                if (TaoPoint.parallel(TaoPoint.line(a1, a2), TaoPoint.line(b1, b2))
                                                        && TaoPoint.parallel(TaoPoint.line(a3, a2), TaoPoint.line(b3, b2))
                                                        && !TaoPoint.parallel(TaoPoint.line(a1, a3), TaoPoint.line(b1, b3))) {
                                                    System.out.println("Fail Conc Desargues");
                                                    System.out.println("l1 " + l1);
                                                    System.out.println("l2 " + l2);
                                                    System.out.println("l3 " + l3);
                                                    System.out.println("a1 " + a1);
                                                    System.out.println("a2 " + a2);
                                                    System.out.println("a3 " + a3);
                                                    System.out.println("b1 " + b1);
                                                    System.out.println("b2 " + b2);
                                                    System.out.println("b3 " + b3);
                                                    System.out.println("a1a2 " + TaoPoint.line(a1, a2));
                                                    System.out.println("b1b2 " + TaoPoint.line(b1, b2));
                                                    System.out.println("a2a3 " + TaoPoint.line(a2, a3));
                                                    System.out.println("b2b3 " + TaoPoint.line(b2, b3));
                                                    System.out.println("a1a3 " + TaoPoint.line(a1, a3));
                                                    System.out.println("b1b3 " + TaoPoint.line(b1, b3));
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
    }

    @Test
    public void testHyperbolicIndex() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int o : TaoPoint.points()) {
            for (int x : TaoPoint.points()) {
                if (o == x) {
                    continue;
                }
                for (int y : TaoPoint.points()) {
                    if (y == x || o == y || TaoPoint.collinear(o, x, y)) {
                        continue;
                    }
                    BitSet xy = TaoPoint.hull(x, y);
                    for (int p : (Iterable<Integer>) () -> xy.stream().iterator()) {
                        if (p == x || p == y) {
                            continue;
                        }
                        BitSet ox = TaoPoint.hull(o, x);
                        BitSet oy = TaoPoint.hull(o, y);
                        int counter = 0;
                        for (int u : (Iterable<Integer>) () -> oy.stream().iterator()) {
                            if (u == o || u == y) {
                                continue;
                            }
                            if (!TaoPoint.hull(p, u).intersects(ox)) {
                                counter++;
                            }
                        }
                        min = Math.min(min, counter);
                        max = Math.max(max, counter);
                    }
                }
            }
        }
        assertEquals(1, min);
        assertEquals(1, max);
    }

    @Test
    public void findFullRegularityBreak() {
        for (int o : space) {
            for (int a : space) {
                for (int b : space) {
                    for (int u : space) {
                        for (int v : line(o, u)) {
                            for (int y : line(u, b)) {
                                for (int x : line(v, a)) {
                                    outer: for (int z : line(x, y)) {
                                        for (int s : line(o, a)) {
                                            for (int t : line(o, b)) {
                                                for (int w : line(o, u)) {
                                                    for (int c : line(s, t)) {
                                                        if (TaoPoint.collinear(c, z, w)) {
                                                            continue outer;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        System.out.println("Fails with: " + o + " " + a + " " + b + " " + u + " " + v + " " + x + " " + y + " " + z); // fail
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        fail();
    }

    @Test // long-running test
    public void testSmallRegularity() {
        long time = System.currentTimeMillis();
        for (int o : space) {
            System.out.println(o);
            for (int a : space) {
                for (int u : space) {
                    for (int y : space) {
                        for (int x : line(o, a)) {
                            outer: for (int z : line(y, x)) {
                                for (int v : line(o, u)) {
                                    for (int s : line(o, a)) {
                                        for (int t : line(o, y)) {
                                            for (int c : line(s, t)) {
                                                if (TaoPoint.collinear(v, z, c)) {
                                                    continue outer;
                                                }
                                            }
                                        }
                                    }
                                }
                                fail(o + " " + a + " " + u + " " + y + " " + x + " " + z);
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Satisfies. Time elapsed: " + (System.currentTimeMillis() - time));
    }
}
