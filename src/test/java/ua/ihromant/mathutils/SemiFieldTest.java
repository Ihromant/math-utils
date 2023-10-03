package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

public class SemiFieldTest {
    private static final int NON_ZERO = SemiField.SIZE - 1;

    static {
        Map<Set<SemiFieldPoint>, List<SemiFieldPoint>> lineToPoint = SemiFieldPoint.points()
                .collect(Collectors.groupingBy(p -> p.line().keySet()));
//        System.out.println(lineToPoint.size());
//        lineToPoint.forEach((key, value) -> System.out.println(key.size() + " " + value.size() + " " + value + " " + key));
    }

    private static List<Set<SemiFieldPoint>> LINES = Stream.concat(
            Stream.of(SemiFieldPoint.of(SemiField.ZERO, SemiField.ONE).line().keySet()),
            IntStream.range(0, SemiField.SIZE).filter(i -> i != SemiField.ZERO)
                    .mapToObj(nf -> SemiFieldPoint.of(SemiField.ONE, nf).line().keySet())).toList();

    private static final List<Triple> TRIPLES = StreamSupport.stream(distinct().spliterator(), false).toList();

    @Test
    public void testPappus() {
        for (int i = 0; i < LINES.size(); i++) {
            for (int j = 0; j < LINES.size(); j++) {
                if (i == j) {
                    continue;
                }
                Set<SemiFieldPoint> line1 = LINES.get(i);
                Set<SemiFieldPoint> line2 = LINES.get(j);
                for (int k = 0; k < TRIPLES.size(); k++) {
                    for (int l = 0; l < TRIPLES.size(); l++) {
                        Triple t1 = TRIPLES.get(k);
                        Triple t2 = TRIPLES.get(l);
//                        SemiFieldPoint a1 = line1[t1.a()];
//                        SemiFieldPoint b1 = line1[t1.b()];
//                        SemiFieldPoint c1 = line1[t1.c()];
//                        SemiFieldPoint a2 = line2[t2.a()];
//                        SemiFieldPoint b2 = line2[t2.b()];
//                        SemiFieldPoint c2 = line2[t2.c()];
//                        SemiFieldPoint a1b2 = b2.sub(a1);
//                        SemiFieldPoint b1c2 = c2.sub(b1);
//                        SemiFieldPoint b1a2 = a2.sub(b1);
//                        SemiFieldPoint c1b2 = b2.sub(c1);
//                        SemiFieldPoint a1a2 = a2.sub(a1);
//                        SemiFieldPoint c1c2 = c2.sub(c1);
//                        if (a1b2.parallel(b1c2) && b1a2.parallel(c1b2) && !a1a2.parallel(c1c2)) {
//                            System.out.println("a1: " + a1 + ", " + "b1: " + b1 + ", " +
//                                    "c1: " + c1 + ", " + "a2: " + a2 + ", " +
//                                    "b2: " + b2 + ", " + "c2: " + c2 + ", " +
//                                    "a1b2: " + a1b2 + ", " + "b1c2: " + b1c2 + ", " +
//                                    "b1a2: " + b1a2 + ", " + "c1b2: " + c1b2 + ", " +
//                                    "a1a2: " + a1a2 + ", " + "c1c2: " + c1c2);
//                        }
                    }
                }
            }
        }
    }

    @Test
    public void testLine() {
        int x1 = 9;
        int y1 = 21;
        int x2 = 2;
        int y2 = 2;
        assertEquals("-i-j", SemiField.toString(x1));
        assertEquals("1-j", SemiField.toString(y1));
        assertEquals("-1-i+j", SemiField.toString(x2));
        assertEquals("-1-i+j", SemiField.toString(y2));

        assertFalse(SemiFieldPoint.of(x1, y1).line().keySet().containsAll(SemiFieldPoint.of(x2, y2).line().keySet()));
        Set<SemiFieldPoint> l = new HashSet<>(SemiFieldPoint.of(x2, y2).line().keySet());
        l.removeAll(SemiFieldPoint.of(x1, y1).line().keySet());
        System.out.println(l);
    }

    @Test
    public void testTriples() {
        assertEquals(26 * 25 * 24, StreamSupport.stream(distinct().spliterator(), false)
                .peek(System.out::println).collect(Collectors.toSet()).size());
    }

    @Test
    public void testUniquePoints() {
        assertEquals(LINES.size() * NON_ZERO, LINES.stream().flatMap(Set::stream).collect(Collectors.toSet()).size());
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
                    int a = curr / NON_ZERO / NON_ZERO;
                    int b = curr / NON_ZERO % NON_ZERO;
                    int c = curr % NON_ZERO;
                    if (c != b && b != a && c != a) {
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
}
