package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TriNearPointTest {
    @Test
    public void testCorrectness() {
        assertEquals(91, TriNearPoint.POINTS.length);
        assertEquals(91, TriNearPoint.LINES.size());
        TriNearPoint.LINES.forEach((k, v) -> assertEquals(10, v.size()));
        assertEquals(91, TriNearPoint.LOOKUP.size());
        TriNearPoint.LOOKUP.forEach((p1, map) -> {
            assertEquals(91, map.size());
            map.forEach((p2, line) -> {
                assertEquals(p1.equals(p2) ? 1 : 10, line.size());
                assertTrue(line.contains(p1));
                assertTrue(line.contains(p2));
            });
        });
        assertEquals(91, TriNearPoint.BEAMS.size());
        TriNearPoint.BEAMS.forEach((p, beam) -> {
            assertEquals(10, beam.size());
            beam.forEach(line -> assertTrue(line.contains(p)));
        });
        for (Set<TriNearPoint> l1 : TriNearPoint.LINES.values()) {
            for (Set<TriNearPoint> l2 : TriNearPoint.LINES.values()) {
                if (l1.equals(l2)) {
                    assertEquals(10, l1.stream().filter(l2::contains).count());
                } else {
                    assertEquals(1, l1.stream().filter(l2::contains).count());
                }
            }
        }
    }

    @Test
    public void testSmallDesargue() {
        for (TriNearPoint o : TriNearPoint.POINTS) {
            System.out.println(o);
            for (Set<TriNearPoint> l0 : TriNearPoint.BEAMS.get(o)) {
                for (Set<TriNearPoint> l1 : TriNearPoint.BEAMS.get(o)) {
                    if (l1.equals(l0)) {
                        continue;
                    }
                    for (Set<TriNearPoint> l2 : TriNearPoint.BEAMS.get(o)) {
                        if (l2.equals(l0) || l2.equals(l1)) {
                            continue;
                        }
                        for (Set<TriNearPoint> l3 : TriNearPoint.BEAMS.get(o)) {
                            if (l3.equals(l0) || l3.equals(l1) || l3.equals(l2)) {
                                continue;
                            }
                            for (TriNearPoint x : l0) {
                                if (x.equals(o)) {
                                    continue;
                                }
                                for (TriNearPoint y : l0) {
                                    if (y.equals(x) || y.equals(o)) {
                                        continue;
                                    }
                                    assertTrue(TriNearPoint.collinear(o, x, y));
                                    for (TriNearPoint a1 : l1) {
                                        if (a1.equals(o)) {
                                            continue;
                                        }
                                        for (TriNearPoint a2 : l1) {
                                            if (a2.equals(a1) || a2.equals(o)) {
                                                continue;
                                            }
                                            TriNearPoint b1 = intersection(TriNearPoint.line(x, a1), l2);
                                            TriNearPoint b2 = intersection(TriNearPoint.line(x, a2), l2);
                                            TriNearPoint c1 = intersection(TriNearPoint.line(y, a1), l3);
                                            TriNearPoint c2 = intersection(TriNearPoint.line(y, a2), l3);
                                            TriNearPoint toCheck = intersection(TriNearPoint.line(b1, c1), TriNearPoint.line(b2, c2));
                                            assertTrue(TriNearPoint.collinear(o, x, y, toCheck));
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
    public void testDesargues() {
        for (TriNearPoint o : TriNearPoint.POINTS) {
            System.out.println(o);
            for (Set<TriNearPoint> l1 : TriNearPoint.BEAMS.get(o)) {
                for (Set<TriNearPoint> l2 : TriNearPoint.BEAMS.get(o)) {
                    if (l2.equals(l1)) {
                        continue;
                    }
                    for (Set<TriNearPoint> l3 : TriNearPoint.BEAMS.get(o)) {
                        if (l3.equals(l1) || l3.equals(l2)) {
                            continue;
                        }
                        for (TriNearPoint a1 : l1) {
                            if (a1.equals(o)) {
                                continue;
                            }
                            for (TriNearPoint a2 : l1) {
                                if (a2.equals(o) || a1.equals(a2)) {
                                    continue;
                                }
                                for (TriNearPoint b1 : l2) {
                                    if (b1.equals(o)) {
                                        continue;
                                    }
                                    for (TriNearPoint b2 : l2) {
                                        if (b2.equals(o) || b1.equals(b2)) {
                                            continue;
                                        }
                                        for (TriNearPoint c1 : l3) {
                                            if (c1.equals(o)) {
                                                continue;
                                            }
                                            for (TriNearPoint c2 : l3) {
                                                if (c2.equals(o) || c1.equals(c2)) {
                                                    continue;
                                                }
                                                TriNearPoint i1 = intersection(TriNearPoint.line(a1, b1), TriNearPoint.line(a2, b2));
                                                TriNearPoint i2 = intersection(TriNearPoint.line(a1, c1), TriNearPoint.line(a2, c2));
                                                TriNearPoint i3 = intersection(TriNearPoint.line(c1, b1), TriNearPoint.line(c2, b2));
                                                assertTrue(TriNearPoint.collinear(i1, i2, i3));
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

    private static TriNearPoint intersection(Set<TriNearPoint> l1, Set<TriNearPoint> l2) {
        return l1.stream().filter(l2::contains).findAny().orElseThrow();
    }
}
