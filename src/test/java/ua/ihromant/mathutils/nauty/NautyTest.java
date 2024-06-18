package ua.ihromant.mathutils.nauty;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.AffinePlane;
import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.PartialLiner;

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
        System.out.println(cons.count());
        return cons.toString();
    }

    @Test
    public void nautyTest() {
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
    }
}
