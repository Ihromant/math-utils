package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

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
        for (int x : space) {
            for (int y : space) {
                for (int z : space) {
                    BitSet plane = TaoPoint.hull(x, y, z);
                    if (plane.cardinality() == TaoPoint.SIZE) {
                        return;
                    }
                }
            }
        }
        fail();
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
}