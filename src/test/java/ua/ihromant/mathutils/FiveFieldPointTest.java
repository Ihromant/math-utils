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

    @Test
    public void testHyperbolicIndex() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int o : FiveFieldPoint.points(droppedPoints)) {
            for (int x : FiveFieldPoint.points(droppedPoints)) {
                if (o == x) {
                    continue;
                }
                for (int y : FiveFieldPoint.points(droppedPoints)) {
                    if (FiveFieldPoint.collinear(o, x, y)) {
                        continue;
                    }
                    int xy = FiveFieldPoint.line(x, y);
                    for (int p : FiveFieldPoint.points(xy)) {
                        if (droppedPoints.get(p) || p == x || p == y) {
                            continue;
                        }
                        int ox = FiveFieldPoint.line(o, x);
                        int oy = FiveFieldPoint.line(o, y);
                        int counter = 0;
                        for (int u : FiveFieldPoint.points(oy)) {
                            if (droppedPoints.get(u) || u == o || u == y) {
                                continue;
                            }
                            if (droppedPoints.get(FiveFieldPoint.intersection(FiveFieldPoint.line(p, u), ox))) {
                                counter++;
                            }
                        }
                        min = Math.min(min, counter);
                        max = Math.max(max, counter);
                    }
                }
            }
        }
        assertEquals(0, min);
        assertEquals(2, max);
    }

    @Test
    public void generatePlane() {
        HyperbolicPlane p1 = new HyperbolicPlane(new String[]{
                "231500240117613536020",
                "34aa1248924dc588776b4",
                "5gce3e9fb76ed99ba98c5",
                "6hjgdfaig8bhffcdbiah7",
                "kjkiijdkkgekghejfjhic"});
        HyperbolicPlane p2 = new HyperbolicPlane(new String[] {
                "40153200a306131252410",
                "7226b658bc39476784591",
                "a937d8gaee4a88dfcc9f7",
                "dd5ehbihfi6ce9gjdgbhb",
                "ieahjijkgkfjjgkkfhkic"});
        HyperbolicPlane p3 = new HyperbolicPlane(new String[]{
                "208035740051013234112",
                "31924eca6865925667d49",
                "a3a48gdh7b8ac77cbbf6b",
                "egd5cheiaefbf89gdfj9i",
                "figdjjikjkichhkkhgkej"});
        HyperbolicPlane p4 = new HyperbolicPlane(new String[]{
                "422501071330034042181",
                "d43612589a66ae6258597",
                "f758367gac7cbhb99cdbb",
                "gaba4ffigf9edjihcdeec",
                "hegh8kjkjidgkkjikjifh"
        });
        HyperbolicPlane p5 = new HyperbolicPlane(new String[] {
                "345678901012340012012",
                "6789abcab234565123345",
                "789abcddecdefg7678456",
                "cdefghieffghijh89a9ab",
                "efghijkjkghijkkijkbcd"
        });
        HyperbolicPlane p = new HyperbolicPlane(p1, p2, p3, p4, p5);
    }
}
