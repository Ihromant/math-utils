package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

public class HessenbergTest {
    @Test
    public void test() {
        int[][] desargues = {
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {1, 3, 7},
                {1, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9},
                {4, 6, 9}
        };
        FuzzyLiner base = FuzzyLiner.of(desargues, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processP));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        ContradictionUtil.printContradiction(base, op);
    }
}
