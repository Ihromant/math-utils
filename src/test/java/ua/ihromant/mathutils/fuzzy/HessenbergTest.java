package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
        LinerHistory initial = FuzzyLiner.of(desargues, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        Map<Rel, Update> updates = new HashMap<>(initial.updates());
        FuzzyLiner base = initial.liner();
        Function<FuzzyLiner, LinerHistory> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processP));
        base.printChars();
        LinerHistory afterIntersect = base.intersectLines();
        updates.putAll(afterIntersect.updates());
        base = afterIntersect.liner();
        base.printChars();
        LinerHistory afterPappus = op.apply(base);
        base = afterPappus.liner();
        base.printChars();
    }
}
