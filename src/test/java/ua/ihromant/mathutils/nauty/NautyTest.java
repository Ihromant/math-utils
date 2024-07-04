package ua.ihromant.mathutils.nauty;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.AffinePlane;
import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.PartialLiner;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class NautyTest {
    @Test
    public void testRefine() {
        Liner l = new Liner(new GaloisField(2).generatePlane());
        GraphWrapper graph = GraphWrapper.forFull(l);
        Partition base = graph.partition();
        SubPartition alpha = base.subPartition();
        base.refine(graph, alpha, new BitSet());
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
            Partition partBase = partGraph.partition();
            SubPartition partAlpha = partBase.subPartition();
            partBase.refine(partGraph, partAlpha, new BitSet());
            //System.out.println("x");
        }
        base.ort(graph, 0, 0);
        base.ort(graph, 11, 0);
        System.out.println();
    }

    private static String getCanonicalOld(Liner liner) {
        GraphWrapper graph = GraphWrapper.forFull(liner);
        CanonicalConsumer cons = new CanonicalConsumer(graph);
        NautyAlgo.search(graph, cons);
        return cons.toString();
    }

    private static String getCanonicalOld(PartialLiner liner) {
        GraphWrapper graph = GraphWrapper.forPartial(liner);
        CanonicalConsumer cons = new CanonicalConsumer(graph);
        NautyAlgo.search(graph, cons);
        return cons.toString();
    }

    private static String getCanonicalNew(Liner liner) {
        GraphWrapper graph = GraphWrapper.forFull(liner);
        CanonicalConsumerNew cons = new CanonicalConsumerNew(graph);
        NautyAlgoNew.search(graph, cons);
        return cons.toString();
    }

    private static String getCanonicalNew(PartialLiner liner) {
        GraphWrapper graph = GraphWrapper.forPartial(liner);
        CanonicalConsumerNew cons = new CanonicalConsumerNew(graph);
        NautyAlgoNew.search(graph, cons);
        return cons.toString();
    }

    @Test
    public void testSimplest() {
        testSimplest(NautyTest::getCanonicalOld);
        testSimplest(NautyTest::getCanonicalNew);
    }

    public void testSimplest(Function<Liner, String> stringForm) {
        Liner liner = new Liner(new GaloisField(2).generatePlane());
        System.out.println(stringForm.apply(liner));

        Liner byStr = Liner.byStrings(new String[]{
                "0001123",
                "1242534",
                "3654656"
        });
        System.out.println(stringForm.apply(byStr));
        assertEquals(stringForm.apply(liner), stringForm.apply(byStr));

        Liner first9 = Liner.byStrings(new String[]{
                "000011122236",
                "134534534547",
                "268787676858"
        });
        System.out.println(stringForm.apply(first9));
        Liner second9 = new AffinePlane(new Liner(new GaloisField(3).generatePlane()), 0).toLiner();
        System.out.println(stringForm.apply(second9));
        assertEquals(stringForm.apply(first9), stringForm.apply(second9));
    }

    @Test
    public void test13() {
        test13(NautyTest::getCanonicalOld);
        test13(NautyTest::getCanonicalNew);
    }

    public void test13(Function<Liner, String> stringForm) {
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
        assertEquals(stringForm.apply(first13), stringForm.apply(second13));
        assertEquals(stringForm.apply(second13), stringForm.apply(first13));
        assertNotEquals(stringForm.apply(alt13), stringForm.apply(first13));
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
        test15(NautyTest::getCanonicalOld);
        test15(NautyTest::getCanonicalNew);
    }

    public void test15(Function<Liner, String> stringForm) {
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
        assertEquals(stringForm.apply(firstFlat15), stringForm.apply(secondFlat15));
        assertEquals(stringForm.apply(firstSpace15), stringForm.apply(secondSpace15));
        assertEquals(stringForm.apply(firstSpace15), stringForm.apply(thirdSpace15));
        assertNotEquals(stringForm.apply(firstFlat15), stringForm.apply(firstSpace15));
    }

    @Test
    public void testPartials() {
        testPartials(NautyTest::getCanonicalOld);
        testPartials(NautyTest::getCanonicalNew);
    }

    public void testPartials(Function<PartialLiner, String> stringForm) {
        PartialLiner firstPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 3, 4}});
        PartialLiner secondPartial = new PartialLiner(9, new int[][]{{6, 7, 8}, {4, 5, 8}});
        PartialLiner thirdPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {3, 4, 5}});
        PartialLiner fourthPartial = new PartialLiner(9, new int[][]{{0, 3, 6}, {1, 4, 7}});
        PartialLiner fifthPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 3, 4}, {1, 3, 5}});
        PartialLiner sixthPartial = new PartialLiner(9, new int[][]{{0, 1, 2}, {0, 5, 6}, {2, 4, 6}});
        assertEquals(stringForm.apply(firstPartial), stringForm.apply(secondPartial));
        assertNotEquals(stringForm.apply(firstPartial), stringForm.apply(thirdPartial));
        assertEquals(stringForm.apply(thirdPartial), stringForm.apply(fourthPartial));
        assertEquals(stringForm.apply(fifthPartial), stringForm.apply(sixthPartial));
    }

    @Test
    public void testSubPartition() {
        SubPartition part = new SubPartition(new int[][]{{3}, {5}, {1}, {7, 4}, null, {9}, {2, 6, 8}, null, null, {0}});
        assertArrayEquals(new int[]{6, 2, 5, 0, -1, 1, -1, 3, -1, 4}, part.getIdxes());
        assertArrayEquals(new int[]{3, 5, 1, 7, 9, 2, 0, 0, 0, 0}, part.getCellMins());
        assertEquals(7, part.getSize());
        part.replace(3, new int[][]{{7}, {4}});
        assertArrayEquals(new int[]{7, 2, 6, 0, 4, 1, -1, 3, -1, 5}, part.getIdxes());
        assertArrayEquals(new int[]{3, 5, 1, 7, 4, 9, 2, 0, 0, 0}, part.getCellMins());
        assertEquals(8, part.getSize());
        part.addButLargest(new DistinguishResult(new int[][]{{8}, {6}, {2}}, 1, new BitSet()));
        assertArrayEquals(new int[]{7, 2, 9, 0, 4, 1, -1, 3, 8, 5}, part.getIdxes());
        assertArrayEquals(new int[]{3, 5, 1, 7, 4, 9, 2, 0, 8, 2}, part.getCellMins());
        assertEquals(10, part.getSize());
        assertEquals(2, part.remove());
        assertArrayEquals(new int[]{7, 2, -1, 0, 4, 1, -1, 3, 8, 5}, part.getIdxes());
        assertArrayEquals(new int[]{3, 5, 1, 7, 4, 9, 2, 0, 8, 2}, part.getCellMins());
        assertEquals(9, part.getSize());
        assertEquals(-1, part.idxOf(2));
        assertEquals(4, part.idxOf(4));
    }
}
