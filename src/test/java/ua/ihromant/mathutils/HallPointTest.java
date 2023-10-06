package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class HallPointTest {
    @Test
    public void testCorrectness() {
        IntStream.range(0, HallPoint.SIZE).forEach(x -> IntStream.range(0, HallPoint.SIZE).forEach(y -> {
            int z = HallPoint.add(x, y);
            assertEquals(x, HallPoint.add(y, z));
            assertEquals(y, HallPoint.add(x, z));
        }));
    }

    @Test
    public void testReflexivity() {
        IntStream.range(0, HallPoint.SIZE).forEach(i -> assertEquals(i, HallPoint.add(i, i)));
    }

    @Test
    public void testSymmetric() {
        IntStream.range(0, HallPoint.SIZE).forEach(i -> IntStream.range(0, HallPoint.SIZE).forEach(j ->
                assertEquals(HallPoint.add(i, j), HallPoint.add(j, i))));
    }

    @Test
    public void testDistributive() {
        IntStream.range(0, HallPoint.SIZE).forEach(x -> IntStream.range(0, HallPoint.SIZE).forEach(y ->
                IntStream.range(0, HallPoint.SIZE).forEach(z ->
                        assertEquals(HallPoint.add(x, HallPoint.add(y, z)),
                                HallPoint.add(HallPoint.add(x, y), HallPoint.add(x, z))))));
    }

    @Test
    public void checkLinealIdentity() {
        IntStream.range(0, HallPoint.SIZE).forEach(x -> IntStream.range(0, HallPoint.SIZE).forEach(y -> {
            if (HallPoint.collinear(x, y, x)) {
                assertEquals(x, y);
            }
        }));
    }

    @Test
    public void checkLinealReflexivity() {
        IntStream.range(0, HallPoint.SIZE).forEach(x -> IntStream.range(0, HallPoint.SIZE).forEach(y -> {
            assertTrue(HallPoint.collinear(x, x, y));
            assertTrue(HallPoint.collinear(x, y, y));
        }));
    }

    @Test
    public void checkLinearExchange() {
        IntStream.range(0, HallPoint.SIZE).forEach(x -> IntStream.range(0, HallPoint.SIZE).forEach(y ->
                IntStream.range(0, HallPoint.SIZE).forEach(a -> IntStream.range(0, HallPoint.SIZE).forEach(b -> {
                    if (HallPoint.collinear(a, x, b) && HallPoint.collinear(a, y, b) && x != y) {
                        assertTrue(HallPoint.collinear(x, a, y));
                        assertTrue(HallPoint.collinear(x, b, y));
                    }
                }))));
    }

    @Test
    public void testSingleClosure() {
        IntStream.range(0, HallPoint.SIZE).forEach(x -> assertEquals(1, HallPoint.closure(x).cardinality()));
    }

    @Test
    public void testTwoPointClosure() {
        IntStream.range(0, HallPoint.SIZE).forEach(x -> IntStream.range(0, HallPoint.SIZE).filter(y -> x != y)
                .forEach(y -> assertEquals(3, HallPoint.closure(x, y).cardinality())));
    }

    @Test
    public void testThreePointClosure() {
        Set<BitSet> planes = new HashSet<>();
        IntStream.range(0, HallPoint.SIZE).forEach(x -> IntStream.range(0, HallPoint.SIZE).filter(y -> x != y)
                .forEach(y -> IntStream.range(0, HallPoint.SIZE).filter(z -> !HallPoint.collinear(x, z, y))
                        .forEach(z -> {
                            BitSet plane = HallPoint.closure(x, y, z);
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
                        })));
        assertEquals(1170, planes.size());
        planes.forEach(p1 -> {
            int counter = 0;
            for (BitSet p2 : planes) {
                BitSet clone = (BitSet) p1.clone();
                if (!clone.intersects(p2)) {
                    counter++;
                }
            }
            assertEquals(296, counter);
        });
    }

    //@Test long-running test
    public void testFourPointClosure() {
        Set<Integer> cardinalities = new HashSet<>(List.of(1, 3, 9, 27, 81));
        IntStream.range(0, HallPoint.SIZE).forEach(x -> IntStream.range(0, HallPoint.SIZE).forEach(y ->
                IntStream.range(0, HallPoint.SIZE).forEach(z -> IntStream.range(0, HallPoint.SIZE).forEach(v ->
                        assertTrue(cardinalities.contains(HallPoint.closure(x, y, z, v).cardinality()), x + " " + y + " " + z + " " + v)))));
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
        Iterable<Integer> sLine = () -> HallPoint.closure(o, a).stream().iterator();
        Iterable<Integer> tLine = () -> HallPoint.closure(o, b).stream().iterator();
        Iterable<Integer> wLine = () -> HallPoint.closure(o, u).stream().iterator();
        for (int s : sLine) {
            for (int t : tLine) {
                for (int w : wLine) {
                    Iterable<Integer> cLine = () -> HallPoint.closure(s, t).stream().iterator();
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
        Iterable<Integer> sLine = () -> HallPoint.closure(o, a).stream().iterator();
        Iterable<Integer> tLine = () -> HallPoint.closure(o, y).stream().iterator();
        Iterable<Integer> vLine = () -> HallPoint.closure(o, u).stream().iterator();
        for (int s : sLine) {
            for (int t : tLine) {
                for (int v : vLine) {
                    Iterable<Integer> cLine = () -> HallPoint.closure(s, t).stream().iterator();
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
                        if (HallPoint.closure(o, a, b).get(u)) {
                            continue;
                        }
                        Iterable<Integer> vLine = () -> HallPoint.closure(o, u).stream().iterator();
                        Iterable<Integer> yLine = () -> HallPoint.closure(u, b).stream().iterator();
                        for (int v : vLine) {
                            for (int y : yLine) {
                                Iterable<Integer> xLine = () -> HallPoint.closure(v, a).stream().iterator();
                                for (int x : xLine) {
                                    Iterable<Integer> zLine = () -> HallPoint.closure(x, y).stream().iterator();
                                    outer: for (int z : zLine) {
                                        Iterable<Integer> sLine = () -> HallPoint.closure(o, a).stream().iterator();
                                        Iterable<Integer> tLine = () -> HallPoint.closure(o, b).stream().iterator();
                                        Iterable<Integer> wLine = () -> HallPoint.closure(o, u).stream().iterator();
                                        for (int s : sLine) {
                                            for (int t : tLine) {
                                                for (int w : wLine) {
                                                    Iterable<Integer> cLine = () -> HallPoint.closure(s, t).stream().iterator();
                                                    for (int c : cLine) {
                                                        if (HallPoint.collinear(c, z, w)) {
                                                            continue outer;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        fail(o + " " + a + " " + b + " " + u + " " + v + " " + x + " " + y + " " + z);
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
