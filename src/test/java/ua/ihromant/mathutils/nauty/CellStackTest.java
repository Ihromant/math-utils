package ua.ihromant.mathutils.nauty;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.PartialLiner;

public class CellStackTest {
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
        System.out.println();
    }
}
