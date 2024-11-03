package ua.ihromant.mathutils.plane;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.FuzzyLiner;
import ua.ihromant.mathutils.Triple;

public class FuzzySepLinerTest {
    @Test
    public void testMoufang() {
        FuzzyLiner base = new FuzzyLiner(new int[][]{
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {0, 7, 8},
                {1, 3, 7},
                {1, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9},
                {4, 6, 9}
        }, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(0, 7, 9)});
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
    }
}
