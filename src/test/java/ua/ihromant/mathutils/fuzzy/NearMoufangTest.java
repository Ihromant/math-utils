package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

public class NearMoufangTest {
    @Test
    public void test() {
        int[][] nearMoufang = new int[][]{
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {0, 7, 8},
                {1, 3, 7},
                {1, 4, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9},
                {4, 6, 9}
        };
        FuzzyLiner base = FuzzyLiner.of(nearMoufang, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processP1, ContradictionUtil::processPS));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = op.apply(base);
        base.printChars();
    }
}
