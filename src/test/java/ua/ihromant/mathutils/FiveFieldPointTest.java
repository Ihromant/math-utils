package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FiveFieldPointTest {
    private static final BitSet droppedLines = getDropped();

    private static BitSet getDropped() {
        BitSet result = new BitSet();
        result.set(FiveFieldPoint.fieldCardinality() * FiveFieldPoint.fieldCardinality() + FiveFieldPoint.fieldCardinality()); // infinity
        result.set(0); // horizontal
        result.set(FiveFieldPoint.fieldCardinality() * FiveFieldPoint.fieldCardinality()); // vertical
        return result;
    }

    private static final BitSet droppedPoints = droppedLines.stream()
            .flatMap(l -> StreamSupport.stream(FiveFieldPoint.points(l).spliterator(), false).mapToInt(Integer::intValue))
            .collect(BitSet::new, BitSet::set, BitSet::or);

    @Test
    public void testCorrectness() {
        for (int p : FiveFieldPoint.points()) {
            assertEquals(FiveFieldPoint.fieldCardinality() + 1, FiveFieldPoint.point(p).cardinality());
        }
        for (int l : FiveFieldPoint.lines()) {
            assertEquals(FiveFieldPoint.fieldCardinality() + 1, FiveFieldPoint.line(l).cardinality());
        }
        for (int p1 : FiveFieldPoint.points()) {
            for (int p2 : FiveFieldPoint.points()) {
                if (p1 != p2) {
                    BitSet line = FiveFieldPoint.line(FiveFieldPoint.line(p1, p2));
                    assertTrue(line.get(p1));
                    assertTrue(line.get(p2));
                }
            }
        }
        for (int l1 : FiveFieldPoint.lines()) {
            for (int l2 : FiveFieldPoint.lines()) {
                if (l1 != l2) {
                    BitSet intersection = FiveFieldPoint.point(FiveFieldPoint.intersection(l1, l2));
                    assertTrue(intersection.get(l1));
                    assertTrue(intersection.get(l2));
                }
            }
        }
        for (int p : FiveFieldPoint.points()) {
            for (int l : FiveFieldPoint.lines(p)) {
                assertTrue(FiveFieldPoint.line(l).get(p));
            }
        }
        for (int l : FiveFieldPoint.lines()) {
            for (int p : FiveFieldPoint.points(l)) {
                assertTrue(FiveFieldPoint.point(p).get(l));
            }
        }
    }

    @Test
    public void testDropped() {
        assertTrue(droppedPoints.stream().allMatch(p -> {
            String str = FiveFieldPoint.pointToString(p);
            return str.indexOf('0') >= 0 || str.indexOf('∞') >= 0;
        }));
    }

    @Test
    public void testPlayfair() {
        int max = 0;
        int min = Integer.MAX_VALUE;
        for (int l : FiveFieldPoint.lines(droppedLines)) {
            System.out.println(FiveFieldPoint.lineToString(l, droppedPoints));
            BitSet line = FiveFieldPoint.line(l);
            for (int p : FiveFieldPoint.points(droppedPoints)) {
                if (line.get(p)) {
                    continue;
                }
                int counter = 0;
                for (int parallel : FiveFieldPoint.lines(p)) {
                    if (droppedPoints.get(FiveFieldPoint.intersection(parallel, l))) {
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
