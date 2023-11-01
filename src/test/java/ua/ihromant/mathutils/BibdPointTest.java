package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BibdPointTest {
    @Test
    public void testCorrectness() {
        for (int p : BibdPoint.points()) {
            assertEquals(6, BibdPoint.point(p).cardinality());
        }
        for (int l : BibdPoint.lines()) {
            assertEquals(3, BibdPoint.line(l).cardinality());
        }
        for (int p1 : BibdPoint.points()) {
            for (int p2 : BibdPoint.points()) {
                if (p1 != p2) {
                    BitSet line = BibdPoint.line(BibdPoint.line(p1, p2));
                    assertTrue(line.get(p1));
                    assertTrue(line.get(p2));
                }
            }
        }
        for (int p : BibdPoint.points()) {
            for (int l : BibdPoint.lines(p)) {
                assertTrue(BibdPoint.line(l).get(p));
            }
        }
        for (int l : BibdPoint.lines()) {
            for (int p : BibdPoint.points(l)) {
                assertTrue(BibdPoint.point(p).get(l));
            }
        }
    }

    @Test
    public void testHyperbolicity() {
        int max = 0;
        int min = Integer.MAX_VALUE;
        for (int l : BibdPoint.lines()) {
            BitSet line = BibdPoint.line(l);
            for (int p : BibdPoint.points()) {
                if (line.get(p)) {
                    continue;
                }
                int counter = 0;
                for (int parallel : BibdPoint.lines(p)) {
                    if (BibdPoint.intersection(parallel, l) == -1) {
                        counter++;
                    }
                }
                //assertEquals(1, counter);
                max = Math.max(max, counter);
                min = Math.min(min, counter);
            }
        }
        System.out.println(min + " " + max);
    }
}
