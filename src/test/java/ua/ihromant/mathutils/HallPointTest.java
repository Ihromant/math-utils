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

    //@Test long-running test
    public void testFourPointClosure() {
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
        int z = HallPoint.parse("(-1,0,0,1)");;
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

    // @Test fails
    public void testSmallRegularityBreak() {
        int o = HallPoint.parse("(-1,-1,-1,-1)");
        int a = HallPoint.parse("(-1,-1,-1,0)");
        int u = HallPoint.parse("(-1,0,-1,-1)");
        int x = HallPoint.parse("(-1,-1,-1,0)");
        int y = HallPoint.parse("(0,1,1,-1)");
        int z = HallPoint.parse("(-1,0,0,1)");;
        assertTrue(HallPoint.collinear(o, x, a));
        assertTrue(HallPoint.collinear(x, z, y));
        Iterable<Integer> sLine = () -> HallPoint.hull(o, a).stream().iterator();
        Iterable<Integer> tLine = () -> HallPoint.hull(o, y).stream().iterator();
        Iterable<Integer> vLine = () -> HallPoint.hull(o, u).stream().iterator();
        for (int s : sLine) {
            for (int t : tLine) {
                for (int v : vLine) {
                    Iterable<Integer> cLine = () -> HallPoint.hull(s, t).stream().iterator();
                    for (int c : cLine) {
                        assertFalse(HallPoint.collinear(c, z, v));
                    }
                }
            }
        }
    }

    @Test
    public void findFullRegularityBreak() {
        Iterable<Integer> oSet = () -> IntStream.range(0, HallPoint.SIZE).iterator();
        Iterable<Integer> aSet = () -> IntStream.range(0, HallPoint.SIZE).iterator();
        Iterable<Integer> bSet = () -> IntStream.range(0, HallPoint.SIZE).iterator();
        Iterable<Integer> uSet = () -> IntStream.range(0, HallPoint.SIZE).iterator();
        for (int o : oSet) {
            for (int a : aSet) {
                if (o == a) {
                    continue;
                }
                for (int b : bSet) {
                    if (HallPoint.collinear(o, b, a)) {
                        continue;
                    }
                    for (int u : uSet) {
                        if (HallPoint.hull(o, a, b).get(u)) {
                            continue;
                        }
                        Iterable<Integer> vLine = () -> HallPoint.hull(o, u).stream().iterator();
                        Iterable<Integer> yLine = () -> HallPoint.hull(u, b).stream().iterator();
                        for (int v : vLine) {
                            for (int y : yLine) {
                                Iterable<Integer> xLine = () -> HallPoint.hull(v, a).stream().iterator();
                                for (int x : xLine) {
                                    Iterable<Integer> zLine = () -> HallPoint.hull(x, y).stream().iterator();
                                    outer: for (int z : zLine) {
                                        Iterable<Integer> sLine = () -> HallPoint.hull(o, a).stream().iterator();
                                        Iterable<Integer> tLine = () -> HallPoint.hull(o, b).stream().iterator();
                                        Iterable<Integer> wLine = () -> HallPoint.hull(o, u).stream().iterator();
                                        for (int s : sLine) {
                                            for (int t : tLine) {
                                                for (int w : wLine) {
                                                    Iterable<Integer> cLine = () -> HallPoint.hull(s, t).stream().iterator();
                                                    for (int c : cLine) {
                                                        if (HallPoint.collinear(c, z, w)) {
                                                            continue outer;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        System.out.println(o + " " + a + " " + b + " " + u + " " + v + " " + x + " " + y + " " + z); // fail
                                        return;
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
    public void printFourPointClosure() {
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

    @Test
    public void findRegularityBreak() {
        long time = System.currentTimeMillis();
        for (int o = 0; o < HallPoint.SIZE; o++) {
            for (int a = 0; a < HallPoint.SIZE; a++) {
//                if (o == a) {
//                    continue;
//                }
                for (int u = 0; u < HallPoint.SIZE; u++) {
//                    if (u == a || u == o) {
//                        continue;
//                    }
                    outer: for (int y = 0; y < HallPoint.SIZE; y++) {
                        System.out.println(o + " " + a + " " + u + " " + y);
//                        if (y == o || y == a || y == u) {
//                            continue;
//                        }
                        int x = HallPoint.add(o, a);
                        int z = HallPoint.add(y, x);
                        for (int v = 0; v < HallPoint.SIZE; v++) {
                            for (int s = 0; s < HallPoint.SIZE; s++) {
                                for (int t = 0; t < HallPoint.SIZE; t++) {
                                    for (int c = 0; c < HallPoint.SIZE; c++) {
                                        if (HallPoint.collinear(o, v, u) && HallPoint.collinear(o, s, a) && HallPoint.collinear(o, t, y)
                                                && HallPoint.collinear(s, c, t) && HallPoint.collinear(v, z, c)) {
                                            continue outer;
                                        }
                                    }
                                }
                            }
                        }
                        System.out.println(o + " " + a + " " + u + " " + y + " spent " + (System.currentTimeMillis() - time));
                        return;
                    }
                }
            }
        }
        System.out.println("Not found " + (System.currentTimeMillis() - time));
    }
}
