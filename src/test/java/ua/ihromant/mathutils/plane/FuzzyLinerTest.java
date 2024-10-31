package ua.ihromant.mathutils.plane;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.FuzzyLiner;
import ua.ihromant.mathutils.Pair;

import java.util.List;

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
        enhanceFullFano(base);
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        System.out.println(base.quad(2));
    }

    private static void enhanceFullFano(FuzzyLiner fl) {
        boolean incorrect = true;
        while (incorrect) {
            incorrect = false;
            List<Quad> full = fl.quads(3);
            for (Quad q : full) {
                int abcd = fl.intersection(new Pair(q.a(), q.b()), new Pair(q.c(), q.d()));
                int acbd = fl.intersection(new Pair(q.a(), q.c()), new Pair(q.b(), q.d()));
                int adbc = fl.intersection(new Pair(q.a(), q.d()), new Pair(q.b(), q.c()));
                if (!fl.collinear(abcd, acbd, adbc)) {
                    incorrect = true;
                    fl.colline(abcd, acbd, adbc);
                }
            }
            fl.update();
        }
    }
}
