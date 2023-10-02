package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

public class NearFieldTest {
    private static final int NON_ZERO = NearField.values().length - 1;

    private static List<List<NearPoint>> LINES = Stream.concat(
                    Stream.of(Arrays.stream(NearField.values()).skip(1)
                            .map(nf -> new NearPoint(NearField.ZERO, nf)).collect(Collectors.toList())),
                    Arrays.stream(NearField.values()).map(nf -> Arrays.stream(NearField.values()).skip(1)
                            .map(cf -> cf.mul(new NearPoint(NearField.PL_1, nf))).collect(Collectors.toList())))
            .peek(System.out::println).toList();

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
                for (int k = 0; k < TRIPLES.size(); k++) {
                    for (int l = 0; l < TRIPLES.size(); l++) {
                        Triple t1 = TRIPLES.get(k);
                        Triple t2 = TRIPLES.get(l);
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
}
