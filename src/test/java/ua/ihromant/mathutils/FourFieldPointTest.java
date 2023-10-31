package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FourFieldPointTest {
    @Test
    public void testCorrectness() {
        for (int p : FourFieldPoint.points()) {
            assertEquals(FourFieldPoint.fieldCardinality() + 1, FourFieldPoint.point(p).cardinality());
        }
        for (int l : FourFieldPoint.lines()) {
            assertEquals(FourFieldPoint.fieldCardinality() + 1, FourFieldPoint.line(l).cardinality());
        }
        for (int p1 : FourFieldPoint.points()) {
            for (int p2 : FourFieldPoint.points()) {
                if (p1 != p2) {
                    BitSet line = FourFieldPoint.line(FourFieldPoint.line(p1, p2));
                    assertTrue(line.get(p1));
                    assertTrue(line.get(p2));
                }
            }
        }
        for (int l1 : FourFieldPoint.lines()) {
            for (int l2 : FourFieldPoint.lines()) {
                if (l1 != l2) {
                    BitSet intersection = FourFieldPoint.point(FourFieldPoint.intersection(l1, l2));
                    assertTrue(intersection.get(l1));
                    assertTrue(intersection.get(l2));
                }
            }
        }
        for (int p : FourFieldPoint.points()) {
            for (int l : FourFieldPoint.lines(p)) {
                assertTrue(FourFieldPoint.line(l).get(p));
            }
        }
        for (int l : FourFieldPoint.lines()) {
            for (int p : FourFieldPoint.points(l)) {
                assertTrue(FourFieldPoint.point(p).get(l));
            }
        }
    }
}
