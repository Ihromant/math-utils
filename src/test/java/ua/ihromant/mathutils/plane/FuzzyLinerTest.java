package ua.ihromant.mathutils.plane;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.FuzzyLiner;

public class FuzzyLinerTest {
    @Test
    public void testFanoNotMoufang() {
        FuzzyLiner base = new FuzzyLiner(new int[][]{
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {0, 7, 8, 10, 11},
                {1, 3, 7},
                {1, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9, 10},
                {4, 6, 9, 11}
        });
        base.update();
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
    }
}
