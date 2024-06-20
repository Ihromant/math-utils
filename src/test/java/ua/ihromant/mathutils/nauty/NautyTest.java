package ua.ihromant.mathutils.nauty;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.AffinePlane;
import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.PartialLiner;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class NautyTest {
    @Test
    public void testRefine() {
        Liner l = new Liner(new GaloisField(2).generatePlane());
        GraphWrapper graph = GraphWrapper.forFull(l);
        CellStack base = graph.partition();
        CellStack alpha = new CellStack(base);
        base.refine(graph, alpha);
        PartialLiner[] partials = new PartialLiner[]{
                new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 3, 4}}),
                new PartialLiner(9, new int[][]{{6, 7, 8}, {4, 5, 8}}),
                new PartialLiner(9, new int[][]{{0, 1, 2}, {3, 4, 5}}),
                new PartialLiner(9, new int[][]{{0, 3, 6}, {1, 4, 7}}),
                new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 3, 4}, {1, 3, 5}}),
                new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 5, 6}, {2, 4, 6}})
        };
        for (PartialLiner part : partials) {
            GraphWrapper partGraph = GraphWrapper.forPartial(part);
            CellStack partBase = partGraph.partition();
            CellStack partAlpha = new CellStack(partBase);
            partBase.refine(partGraph, partAlpha);
            //System.out.println("x");
        }
        CellStack next = base.ort(graph, 0);
        next = next.ort(graph, 11);
        System.out.println();
    }

    private static String getCanonical(Liner liner) {
        GraphWrapper graph = GraphWrapper.forFull(liner);
        CanonicalConsumer cons = new CanonicalConsumer(graph);
        NautyAlgo.search(graph, cons);
        return cons.toString();
    }

    private static String getCanonical(PartialLiner liner) {
        GraphWrapper graph = GraphWrapper.forPartial(liner);
        CanonicalConsumer cons = new CanonicalConsumer(graph);
        NautyAlgo.search(graph, cons);
        return cons.toString();
    }

    @Test
    public void testSimplest() {
        Liner liner = new Liner(new GaloisField(2).generatePlane());
        System.out.println(getCanonical(liner));

        Liner byStr = Liner.byStrings(new String[]{
                "0001123",
                "1242534",
                "3654656"
        });
        System.out.println(getCanonical(byStr));
        assertEquals(getCanonical(liner), getCanonical(byStr));

        Liner first9 = Liner.byStrings(new String[]{
                "000011122236",
                "134534534547",
                "268787676858"
        });
        System.out.println(getCanonical(first9));
        Liner second9 = new AffinePlane(new Liner(new GaloisField(3).generatePlane()), 0).toLiner();
        System.out.println(getCanonical(second9));
        assertEquals(getCanonical(first9), getCanonical(second9));
    }

    @Test
    public void test13() {
        Liner first13 = Liner.byStrings(new String[]{
                "00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95abcbcac9babc9"
        });
        Liner alt13 = Liner.byStrings(new String[]{
                "00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95acbbacc9bbac9"
        });
        Liner second13 = Liner.byDiffFamily(new int[]{0, 6, 8}, new int[]{0, 9, 10});
        assertEquals(getCanonical(first13), getCanonical(second13));
        assertEquals(getCanonical(second13), getCanonical(first13));
        assertNotEquals(getCanonical(alt13), getCanonical(first13));
        testAutomorphisms(first13, 39);
        testAutomorphisms(alt13, 6);
    }

    private static void testAutomorphisms(Liner first13, int autCount) {
        int[][] firstLines = first13.lines();
        PartialLiner part = new PartialLiner(firstLines);
        int[][] auths = part.automorphisms();
        assertEquals(autCount, auths.length);
        for (int[] aut : auths) {
            int[][] newLines = new int[firstLines.length][];
            for (int i = 0; i < firstLines.length; i++) {
                int[] line = firstLines[i];
                int lineIdx = aut[i + part.pointCount()] - part.pointCount();
                int[] newLine = line.clone();
                for (int j = 0; j < newLine.length; j++) {
                    newLine[j] = aut[newLine[j]];
                }
                Arrays.sort(newLine);
                newLines[lineIdx] = newLine;
            }
            assertArrayEquals(firstLines, newLines);
        }
    }

    @Test
    public void test15() {
        Liner firstFlat15 = Liner.byStrings(new String[] {
                "00000001111112222223333444455566678",
                "13579bd3469ac34578b678a58ab78979c9a",
                "2468ace578bde96aecdbcded9cebecaeddb"
        });
        Liner firstSpace15 = Liner.byStrings(new String[]{
                "00000001111112222223333444455556666",
                "13579bd3478bc3478bc789a789a789a789a",
                "2468ace569ade65a9edbcdecbeddebcedcb"
        });
        Liner secondFlat15 = Liner.byDiffFamily(15, new int[]{0, 6, 8}, new int[]{0, 1, 4}, new int[]{0, 5, 10});
        Liner secondSpace15 = Liner.byDiffFamily(15, new int[]{0, 2, 8}, new int[]{0, 1, 4}, new int[]{0, 5, 10});
        Liner thirdSpace15 = new Liner(new GaloisField(2).generateSpace());
        assertEquals(getCanonical(firstFlat15), getCanonical(secondFlat15));
        assertEquals(getCanonical(firstSpace15), getCanonical(secondSpace15));
        assertEquals(getCanonical(firstSpace15), getCanonical(thirdSpace15));
        assertNotEquals(getCanonical(firstFlat15), getCanonical(firstSpace15));
    }

    @Test
    public void testPartials() {
        PartialLiner firstPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 3, 4}});
        PartialLiner secondPartial = new PartialLiner(9, new int[][]{{6, 7, 8}, {4, 5, 8}});
        PartialLiner thirdPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {3, 4, 5}});
        PartialLiner fourthPartial = new PartialLiner(9, new int[][]{{0, 3, 6}, {1, 4, 7}});
        PartialLiner fifthPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 3, 4}, {1, 3, 5}});
        PartialLiner sixthPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 5, 6}, {2, 4, 6}});
        assertEquals(getCanonical(firstPartial), getCanonical(secondPartial));
        assertNotEquals(getCanonical(firstPartial), getCanonical(thirdPartial));
        assertEquals(getCanonical(thirdPartial), getCanonical(fourthPartial));
        assertEquals(getCanonical(fifthPartial), getCanonical(sixthPartial));
    }
}
