package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

public class SemiFieldTest {
    private static final List<List<SemiFieldPoint>> LINES = Stream.concat(
            Stream.of(line(SemiFieldPoint.of(SemiField.ZERO, SemiField.ONE)).toList()),
            IntStream.range(0, SemiField.SIZE).mapToObj(nf -> line(SemiFieldPoint.of(SemiField.ONE, nf)).toList())).toList();

    private static final Map<SemiFieldPoint, Integer> POINT_TO_LINE = IntStream.range(0, LINES.size())
            .boxed().flatMap(line -> IntStream.range(0, LINES.get(line).size())
                    .filter(pos -> pos != SemiField.ZERO)
                    .mapToObj(pos -> new LinePos(line, pos)))
            .collect(Collectors.toMap(lp -> LINES.get(lp.line()).get(lp.pos()), LinePos::line));

    private record LinePos(int line, int pos) {

    }

    private static Stream<SemiFieldPoint> line(SemiFieldPoint point) {
        return IntStream.range(0, SemiField.SIZE).mapToObj(point::mul);
    }

    private static final List<Triple> TRIPLES = StreamSupport.stream(distinct().spliterator(), false).toList();

    //@Test uncomment to generate
    public void testPappus() {
        for (int i = 0; i < LINES.size(); i++) {
            for (int j = 0; j < LINES.size(); j++) {
                if (i == j) {
                    continue;
                }
                List<SemiFieldPoint> line1 = LINES.get(i);
                List<SemiFieldPoint> line2 = LINES.get(j);
                for (int k = 0; k < TRIPLES.size(); k++) {
                    for (int l = 0; l < TRIPLES.size(); l++) {
                        Triple t1 = TRIPLES.get(k);
                        Triple t2 = TRIPLES.get(l);
                        SemiFieldPoint a1 = line1.get(t1.a());
                        SemiFieldPoint b1 = line1.get(t1.b());
                        SemiFieldPoint c1 = line1.get(t1.c());
                        SemiFieldPoint a2 = line2.get(t2.a());
                        SemiFieldPoint b2 = line2.get(t2.b());
                        SemiFieldPoint c2 = line2.get(t2.c());
                        SemiFieldPoint a1b2 = b2.sub(a1);
                        SemiFieldPoint b1c2 = c2.sub(b1);
                        SemiFieldPoint b1a2 = a2.sub(b1);
                        SemiFieldPoint c1b2 = b2.sub(c1);
                        SemiFieldPoint a1a2 = a2.sub(a1);
                        SemiFieldPoint c1c2 = c2.sub(c1);
                        if (parallel(a1b2, b1c2) && parallel(b1a2, c1b2) && !parallel(a1a2, c1c2)) {
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

    private static boolean parallel(SemiFieldPoint p1, SemiFieldPoint p2) {
        return POINT_TO_LINE.get(p1).intValue() == POINT_TO_LINE.get(p2).intValue();
    }

    @Test
    public void testParallelReflexive() {
        assertNotNull(SemiFieldPoint.parse("(0,0)"));
        assertTrue(SemiFieldPoint.nonZeroPoints().noneMatch(p -> "(0,0)".equals(p.toString())));
        SemiFieldPoint.nonZeroPoints().forEach(p -> parallel(p, p));
    }

    @Test
    public void testParallelSymmetric() {
        SemiFieldPoint.nonZeroPoints().forEach(p1 -> SemiFieldPoint.nonZeroPoints().forEach(p2 ->
                assertEquals(parallel(p1, p2), parallel(p2, p1))));
    }

    @Test
    public void testParallelTransitive() {
        SemiFieldPoint.nonZeroPoints().forEach(p1 -> SemiFieldPoint.nonZeroPoints().forEach(p2 ->
                SemiFieldPoint.nonZeroPoints().forEach(p3 -> {
                    if (parallel(p1, p2) && parallel(p2, p3)) {
                        assertTrue(parallel(p1, p3));
                    }
                })));
    }

    @Test
    public void testMultipliersReflexive() {
        SemiFieldPoint.nonZeroPoints().forEach(p -> assertEquals(SemiField.ONE, p.multiplier(p)));
    }

    //@Test
    public void testMultipliersSymmetric() {
        SemiFieldPoint.nonZeroPoints().forEach(p1 -> SemiFieldPoint.nonZeroPoints().forEach(p2 ->
                assertFalse(p1.multiplier(p2) != null ^ p2.multiplier(p1) != null,
                        "p1: " + p1 + ", p2: " + p2
                                + ", p1 to p2 multiplier: "
                                + Optional.ofNullable(p1.multiplier(p2)).map(SemiField::toString).orElse(null)
                                + ", p2 to p1 multiplier: "
                                + Optional.ofNullable(p2.multiplier(p1)).map(SemiField::toString).orElse(null))));
    }

    //@Test
    public void testMultipliersTransitive() {
        SemiFieldPoint.nonZeroPoints().forEach(p1 -> SemiFieldPoint.nonZeroPoints().forEach(p2 ->
                SemiFieldPoint.nonZeroPoints().forEach(p3 -> {
                    if (p1.multiplier(p2) != null && p2.multiplier(p3) != null) {
                        assertNotNull(p1.multiplier(p3));
                    }
                })));
    }

    @Test
    public void testLine() {
        for (List<SemiFieldPoint> l : LINES) {
            Set<SemiFieldPoint> line = new HashSet<>(l);
            assertEquals(SemiField.SIZE, line.size());
            for (Triple t : TRIPLES) {
                SemiFieldPoint a = l.get(t.a());
                SemiFieldPoint b = l.get(t.b());
                SemiFieldPoint c = l.get(t.c());
                assertTrue(parallel(b, a));
                assertTrue(parallel(c, a));
                assertTrue(parallel(c, b));
                assertTrue(parallel(a.sub(b), a.sub(c)));
                assertTrue(parallel(a.sub(b), b.sub(c)));
                assertTrue(parallel(a.sub(c), b.sub(c)));
            }
        }
    }

    @Test
    public void testDiscoveredCase() {
        SemiFieldPoint a1 = SemiFieldPoint.parse("(0,j)");
        SemiFieldPoint b1 = SemiFieldPoint.parse("(0,-1+j)");
        SemiFieldPoint c1 = SemiFieldPoint.parse("(0,-1-i)");
        SemiFieldPoint a2 = SemiFieldPoint.parse("(i-j,-1+j)");
        SemiFieldPoint b2 = SemiFieldPoint.parse("(i+j,-1-i)");
        SemiFieldPoint c2 = SemiFieldPoint.parse("(1-i-j,-j)");
        SemiFieldPoint a1b2 = SemiFieldPoint.parse("(i+j,-1-i-j)");
        SemiFieldPoint b1c2 = SemiFieldPoint.parse("(1-i-j,1+j)");
        SemiFieldPoint b1a2 = SemiFieldPoint.parse("(i-j,0)");
        SemiFieldPoint c1b2 = SemiFieldPoint.parse("(i+j,0)");
        SemiFieldPoint a1a2 = SemiFieldPoint.parse("(i-j,-1)");
        SemiFieldPoint c1c2 = SemiFieldPoint.parse("(1-i-j,1+i-j)");
        assertEquals(a1.sub(b2).neg(), a1b2);
        assertEquals(b1.sub(c2).neg(), b1c2);
        assertEquals(b1.sub(a2).neg(), b1a2);
        assertEquals(c1.sub(b2).neg(), c1b2);
        assertEquals(a1.sub(a2).neg(), a1a2);
        assertEquals(c1.sub(c2).neg(), c1c2);
        assertTrue(parallel(a1, b1));
//        assertEquals("1+i", SemiField.toString(a1.multiplier(b1)));
//        assertEquals("-1+i-j", SemiField.toString(b1.multiplier(a1)));
        assertTrue(parallel(a1, c1));
//        assertEquals("1-j", SemiField.toString(a1.multiplier(c1)));
//        assertEquals("-i", SemiField.toString(c1.multiplier(a1)));
        assertTrue(parallel(c1, b1));
//        assertEquals("i-j", SemiField.toString(c1.multiplier(b1)));
//        assertEquals("1-i", SemiField.toString(b1.multiplier(c1)));
        assertTrue(parallel(a2, b2));
//        assertEquals("i-j", SemiField.toString(a2.multiplier(b2)));
//        assertEquals("1-i", SemiField.toString(b2.multiplier(a2)));
        assertTrue(parallel(a2, c2));
//        assertEquals("i-j", SemiField.toString(a2.multiplier(c2)));
//        assertEquals("1-i", SemiField.toString(c2.multiplier(a2)));
        assertTrue(parallel(b2, c2));
//        assertEquals("i-j", SemiField.toString(b2.multiplier(c2)));
//        assertEquals("1-i", SemiField.toString(c2.multiplier(b2)));
        assertTrue(parallel(a1b2, b1c2));
//        assertEquals("i-j", SemiField.toString(a1b2.multiplier(b1c2)));
//        assertEquals("1-i", SemiField.toString(b1c2.multiplier(a1b2)));
        assertTrue(parallel(b1a2, c1b2));
//        assertEquals("i-j", SemiField.toString(b1a2.multiplier(c1b2)));
//        assertEquals("1-i", SemiField.toString(c1b2.multiplier(b1a2)));
        assertFalse(parallel(a1a2, c1c2)); // Parallel Pappus doesn't hold
//        assertNull(a1a2.multiplier(c1c2));
//        assertNull(c1c2.multiplier(a1a2));
    }

    @Test
    public void testTriples() {
        assertEquals(26 * 25 * 24, StreamSupport.stream(distinct().spliterator(), false)
                .peek(System.out::println).collect(Collectors.toSet()).size());
    }

    @Test
    public void testUniquePoints() {
        assertEquals(LINES.size(), SemiField.SIZE + 1);
        assertEquals(LINES.stream().mapToInt(List::size).sum(), SemiField.SIZE * SemiField.SIZE + SemiField.SIZE);
        assertEquals(SemiField.SIZE * SemiField.SIZE, LINES.stream().flatMap(List::stream).collect(Collectors.toSet()).size());
    }

    @Test
    public void testZero() {
        assertEquals(13, SemiField.ZERO);
    }

    @Test // a + b = b + a
    public void testCommutativeAddition() {
        for (int a = 0; a < SemiField.SIZE; a++) {
            for (int b = 0; b < SemiField.SIZE; b++) {
                assertEquals(SemiField.add(a, b), SemiField.add(b, a));
            }
        }
    }

    @Test // (a + b) + c = a + (b + c)
    public void testAssociativeAddition() {
        for (int a = 0; a < SemiField.SIZE; a++) {
            for (int b = 0; b < SemiField.SIZE; b++) {
                for (int c = 0; c < SemiField.SIZE; c++) {
                    assertEquals(SemiField.add(a, SemiField.add(b, c)), SemiField.add(SemiField.add(a, b), c));
                }
            }
        }
    }

    @Test // (a * b) * c != (a * b) * c
    public void testAssociativeMultiplication() {
        int counter = 0;
        for (int a = 0; a < SemiField.SIZE; a++) {
            for (int b = 0; b < SemiField.SIZE; b++) {
                for (int c = 0; c < SemiField.SIZE; c++) {
                    int ABc = SemiField.mul(SemiField.mul(a, b), c);
                    int aBC = SemiField.mul(a, SemiField.mul(b, c));
                    if (ABc != aBC) {
                        counter++;
                    }
                }
            }
        }
        assertEquals(10368, counter); // 2 ^ 7 * 3 ^ 4
    }

    @Test // a * b = b * a
    public void testCommutativeMultiplication() {
        for (int a = 0; a < SemiField.SIZE; a++) {
            for (int b = 0; b < SemiField.SIZE; b++) {
                assertEquals(SemiField.mul(a, b), SemiField.mul(b, a));
            }
        }
    }

    @Test // (a + b) * c = a * c + b * c
    public void testRightDistributiveMultiplication() {
        for (int a = 0; a < SemiField.SIZE; a++) {
            for (int b = 0; b < SemiField.SIZE; b++) {
                for (int c = 0; c < SemiField.SIZE; c++) {
                    assertEquals(SemiField.mul(SemiField.add(a, b), c),
                            SemiField.add(SemiField.mul(a, c), SemiField.mul(b, c)));
                }
            }
        }
    }

    @Test // a * (b + c) = a * b + a * c
    public void testLeftDistributiveMultiplication() {
        for (int a = 0; a < SemiField.SIZE; a++) {
            for (int b = 0; b < SemiField.SIZE; b++) {
                for (int c = 0; c < SemiField.SIZE; c++) {
                    assertEquals(SemiField.mul(a, SemiField.add(b, c)),
                            SemiField.add(SemiField.mul(a, b), SemiField.mul(a, c)));
                }
            }
        }
    }

    private record Triple(int a, int b, int c) {

    }

    private static Iterable<Triple> distinct() {
        return () -> new Iterator<>() {
            private int idx = calculateNext(0);

            private int calculateNext(int curr) {
                while (true) {
                    int a = curr / SemiField.SIZE / SemiField.SIZE;
                    int b = curr / SemiField.SIZE % SemiField.SIZE;
                    int c = curr % SemiField.SIZE;
                    if (c != b && b != a && c != a
                            && a != SemiField.ZERO && b != SemiField.ZERO && c != SemiField.ZERO) {
                        break;
                    } else {
                        curr++;
                    }
                }
                return curr;
            }

            @Override
            public boolean hasNext() {
                return idx < SemiField.SIZE * SemiField.SIZE * SemiField.SIZE;
            }

            @Override
            public Triple next() {
                int res = idx;
                idx = calculateNext(idx + 1);
                return new Triple(res / SemiField.SIZE / SemiField.SIZE, res / SemiField.SIZE % SemiField.SIZE, res % SemiField.SIZE);
            }
        };
    }
}
