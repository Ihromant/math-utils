package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class HallPointTest {
    private static final Iterable<Integer> space = () -> IntStream.range(0, HallPoint.SIZE).iterator();

    private static Iterable<Integer> line(int a, int b) {
        return () -> HallPoint.hull(a, b).stream().iterator();
    }

    @Test
    public void testCorrectness() {
        for (int x : space) {
            for (int y : space) {
                int z = HallPoint.add(x, y);
                assertEquals(x, HallPoint.add(y, z));
                assertEquals(y, HallPoint.add(x, z));
            }
        }
    }

    @Test
    public void testReflexivity() {
        for (int x : space) {
            assertEquals(x, HallPoint.add(x, x));
        }
    }

    @Test
    public void testSymmetric() {
        for (int x : space) {
            for (int y : space) {
                assertEquals(HallPoint.add(x, y), HallPoint.add(y, x));
            }
        }
    }

    @Test
    public void testDistributive() {
        int counter = 0;
        for (int x : space) {
            for (int y : space) {
                for (int z : space) {
                    assertEquals(HallPoint.add(x, HallPoint.add(y, z)),
                            HallPoint.add(HallPoint.add(x, y), HallPoint.add(x, z)));
                    counter++;
                }
            }
        }
        assertEquals(HallPoint.SIZE * HallPoint.SIZE * HallPoint.SIZE, counter);
    }

    @Test
    public void checkLinealIdentity() {
        for (int x : space) {
            for (int y : space) {
                if (!HallPoint.collinear(x, y, x)) {
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
                assertTrue(HallPoint.collinear(x, x, y));
                assertTrue(HallPoint.collinear(x, y, y));
            }
        }
    }

    @Test
    public void checkLinearExchange() {
        for (int x : space) {
            for (int y : space) {
                for (int a : space) {
                    for (int b : space) {
                        if (!HallPoint.collinear(a, x, b) || !HallPoint.collinear(a, y, b) || x == y) {
                            continue;
                        }
                        assertTrue(HallPoint.collinear(x, a, y));
                        assertTrue(HallPoint.collinear(x, b, y));
                    }
                }
            }
        }
    }

    @Test
    public void testSmallHulls() {
        for (int x : space) {
            assertEquals(1, HallPoint.hull(x).cardinality());
            for (int y : space) {
                BitSet line = HallPoint.hull(x, y);
                if (x == y) {
                    assertEquals(1, line.cardinality());
                } else {
                    assertEquals(3, line.cardinality());
                }
                for (int z : space) {
                    assertEquals(line.get(z), HallPoint.collinear(x, z, y));
                }
            }
        }
    }

    @Test
    public void testHyperbolicIndex() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int o : HallPoint.points()) {
            for (int x : HallPoint.points()) {
                if (o == x) {
                    continue;
                }
                for (int y : HallPoint.points()) {
                    if (x == y || o == y || HallPoint.collinear(o, x, y)) {
                        continue;
                    }
                    BitSet xy = HallPoint.hull(x, y);
                    for (int p : (Iterable<Integer>) () -> xy.stream().iterator()) {
                        if (p == x || p == y) {
                            continue;
                        }
                        BitSet ox = HallPoint.hull(o, x);
                        BitSet oy = HallPoint.hull(o, y);
                        int counter = 0;
                        for (int u : (Iterable<Integer>) () -> oy.stream().iterator()) {
                            if (u == o || u == y) {
                                continue;
                            }
                            if (!HallPoint.hull(p, u).intersects(ox)) {
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
    public void testThreePointClosure() {
        Set<BitSet> planes = new HashSet<>();
        for (int x : space) {
            for (int y : space) {
                if (x == y) {
                    continue;
                }
                for (int z : space) {
                    if (HallPoint.collinear(x, z, y)) {
                        continue;
                    }
                    BitSet plane = HallPoint.hull(x, y, z);
                    assertEquals(9, plane.cardinality());
                    planes.add(plane);
                    int a = HallPoint.add(x, z);
                    int b = HallPoint.add(x, y);
                    BitSet firstStep = new BitSet();
                    firstStep.set(a);
                    firstStep.set(b);
                    firstStep.set(x);
                    firstStep.set(y);
                    firstStep.set(z);
                    firstStep = HallPoint.next(firstStep);
                    assertEquals(9, firstStep.cardinality()); //testing of claim 2.4
                }
            }
        }
        assertEquals(1170, planes.size());
        for (BitSet p : planes) {
            assertEquals(Map.of(0, 296L, 1, 729L, 3, 144L, 9, 1L),
                    planes.stream().collect(Collectors.groupingBy(p1 -> {
                        BitSet clone = (BitSet) p.clone();
                        clone.and(p1);
                        return clone.cardinality();
                    }, Collectors.counting())));
        }
    }

    @Test // long-running test
    public void testFourPointClosures() {
        Set<Integer> cardinalities = new HashSet<>(List.of(1, 3, 9, 27, 81));
        for (int x : space) {
            for (int y : space) {
                for (int z : space) {
                    for (int v : space) {
                        assertTrue(cardinalities.contains(HallPoint.hull(x, y, z, v).cardinality()));
                    }
                }
            }
        }
    }

    @Test
    public void testPlayfair() {
        for (int o : space) { // linear form
            for (int x : space) {
                BitSet ox = HallPoint.hull(o, x);
                for (int y : space) {
                    outerB: for (int a : line(x, y)) {
                        if (ox.get(a)) {
                            continue;
                        }
                        for (int b : line(o, y)) {
                            boolean forEach = true;
                            for (int c : line(o, y)) {
                                BitSet ac = HallPoint.hull(a, c);
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
                        if (!HallPoint.collinear(x, a, y) || HallPoint.collinear(o, a, x)) {
                            continue;
                        }
                        for (int b : space) {
                            if (!HallPoint.collinear(o, b, y)) {
                                continue;
                            }
                            boolean forEach = true;
                            for (int c : space) {
                                if (!HallPoint.collinear(o, c, y)) {
                                    continue;
                                }
                                if (c != b) {
                                    forEach = forEach && IntStream.range(0, HallPoint.SIZE).anyMatch(t -> HallPoint.collinear(c, t, a) && HallPoint.collinear(o, t, x));
                                } else {
                                    forEach = forEach && IntStream.range(0, HallPoint.SIZE).noneMatch(t -> HallPoint.collinear(c, t, a) && HallPoint.collinear(o, t, x));
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
    public void testRegularityBreak() {
        int o = HallPoint.parse("(-1,-1,-1,-1)");
        int a = HallPoint.parse("(-1,-1,-1,0)");
        int b = HallPoint.parse("(-1,-1,0,-1)");
        int u = HallPoint.parse("(-1,0,-1,-1)");
        int v = HallPoint.parse("(-1,-1,-1,-1)");
        int x = HallPoint.parse("(-1,-1,-1,0)");
        int y = HallPoint.parse("(0,1,1,-1)");
        int z = HallPoint.parse("(-1,0,0,1)");
        assertTrue(HallPoint.collinear(o, v, u));
        assertTrue(HallPoint.collinear(a, x, v));
        assertTrue(HallPoint.collinear(b, y, u));
        assertTrue(HallPoint.collinear(x, z, y));
        Iterable<Integer> sLine = () -> HallPoint.hull(o, a).stream().iterator();
        Iterable<Integer> tLine = () -> HallPoint.hull(o, b).stream().iterator();
        Iterable<Integer> wLine = () -> HallPoint.hull(o, u).stream().iterator();
        for (int s : sLine) {
            for (int t : tLine) {
                for (int w : wLine) {
                    Iterable<Integer> cLine = () -> HallPoint.hull(s, t).stream().iterator();
                    for (int c : cLine) {
                        assertFalse(HallPoint.collinear(c, z, w));
                    }
                }
            }
        }
    }

    @Test
    public void findFullRegularityBreak() {
        for (int o : space) {
            for (int a : space) {
                if (o == a) {
                    continue;
                }
                for (int b : space) {
                    if (HallPoint.collinear(o, b, a)) {
                        continue;
                    }
                    for (int u : space) {
                        if (HallPoint.hull(o, a, b).get(u)) {
                            continue;
                        }
                        for (int v : line(o, u)) {
                            for (int y : line(u, b)) {
                                for (int x : line(v, a)) {
                                    outer: for (int z : line(x, y)) {
                                        for (int s : line(o, a)) {
                                            for (int t : line(o, b)) {
                                                for (int w : line(o, u)) {
                                                    for (int c : line(s, t)) {
                                                        if (HallPoint.collinear(c, z, w)) {
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

    @Test
    public void printFourPointHull() {
        int x = 0;
        int y = 1;
        int z = 3;
        int v = 27;
        BitSet base = new BitSet(HallPoint.SIZE);
        base.set(x);
        base.set(y);
        base.set(z);
        base.set(v);
        BitSet next = HallPoint.next(base);
        while (next.cardinality() > base.cardinality()) {
            System.out.println(base.cardinality() + " " + base.stream().mapToObj(HallPoint::toString).collect(Collectors.joining(",", "{", "}")));
            base = next;
            next = HallPoint.next(base);
        }
        System.out.println(next.cardinality() + " " + next.stream().mapToObj(HallPoint::toString).collect(Collectors.joining(",", "{", "}")));
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
                                                if (HallPoint.collinear(v, z, c)) {
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

    @Test
    public void testPappusDesargues() {
        for (BitSet l1 : HallPoint.lines()) {
            for (BitSet l2 : HallPoint.lines()) {
                if (!HallPoint.parallel(l1, l2)) {
                    continue;
                }
                int[] pts1 = l1.stream().toArray();
                int[] pts2 = l2.stream().toArray();
                GaloisField.permutations(pts1).forEach(prm1 -> GaloisField.permutations(pts2).forEach(prm2 -> {
                    if (HallPoint.parallel(HallPoint.line(prm1[0], prm2[1]), HallPoint.line(prm1[2], prm2[1]))
                            && HallPoint.parallel(HallPoint.line(prm2[0], prm1[1]), HallPoint.line(prm2[2], prm1[1]))
                            && !HallPoint.parallel(HallPoint.line(prm1[0], prm2[0]), HallPoint.line(prm1[2], prm2[2]))) {
                        System.out.println("Fail Pappus");
                    }
                }));
            }
        }
        System.out.println("Lines");
        for (BitSet l : HallPoint.lines()) {
            System.out.println(l);
        }
        for (BitSet l1 : HallPoint.lines()) {
            for (BitSet l2 : HallPoint.lines()) {
                if (!HallPoint.parallel(l1, l2)) {
                    continue;
                }
                for (BitSet l3 : HallPoint.lines()) {
                    if (!HallPoint.parallel(l1, l3) || !HallPoint.parallel(l2, l3)) {
                        continue;
                    }
                    for (int a1 : l1.stream().toArray()) {
                        for (int b1 : l1.stream().toArray()) {
                            for (int a2 : l2.stream().toArray()) {
                                for (int b2 : l2.stream().toArray()) {
                                    for (int a3 : l3.stream().toArray()) {
                                        for (int b3 : l3.stream().toArray()) {
                                            if (HallPoint.parallel(HallPoint.line(a1, a2), HallPoint.line(b1, b2))
                                                    && HallPoint.parallel(HallPoint.line(a3, a2), HallPoint.line(b3, b2))
                                                    && !HallPoint.parallel(HallPoint.line(a1, a3), HallPoint.line(b1, b3))) {
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
                                                System.out.println("a1a2 " + HallPoint.line(a1, a2));
                                                System.out.println("b1b2 " + HallPoint.line(b1, b2));
                                                System.out.println("a2a3 " + HallPoint.line(a2, a3));
                                                System.out.println("b2b3 " + HallPoint.line(b2, b3));
                                                System.out.println("a1a3 " + HallPoint.line(a1, a3));
                                                System.out.println("b1b3 " + HallPoint.line(b1, b3));
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
        for (int o : HallPoint.points()) {
            for (BitSet l1 : HallPoint.lines()) {
                if (!l1.get(o)) {
                    continue;
                }
                for (BitSet l2 : HallPoint.lines()) {
                    if (!l2.get(o) || l2.equals(l1)) {
                        continue;
                    }
                    for (BitSet l3 : HallPoint.lines()) {
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
                                                if (HallPoint.parallel(HallPoint.line(a1, a2), HallPoint.line(b1, b2))
                                                        && HallPoint.parallel(HallPoint.line(a3, a2), HallPoint.line(b3, b2))
                                                        && !HallPoint.parallel(HallPoint.line(a1, a3), HallPoint.line(b1, b3))) {
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
                                                    System.out.println("a1a2 " + HallPoint.line(a1, a2));
                                                    System.out.println("b1b2 " + HallPoint.line(b1, b2));
                                                    System.out.println("a2a3 " + HallPoint.line(a2, a3));
                                                    System.out.println("b2b3 " + HallPoint.line(b2, b3));
                                                    System.out.println("a1a3 " + HallPoint.line(a1, a3));
                                                    System.out.println("b1b3 " + HallPoint.line(b1, b3));
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
}
