package ua.ihromant.mathutils.plane;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.FuzzySepLiner;
import ua.ihromant.mathutils.Pair;
import ua.ihromant.mathutils.Triple;

import java.util.List;

public class FuzzySepLinerTest {
    @Test
    public void testMoufang() {
        FuzzySepLiner base = new FuzzySepLiner(new int[][]{
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
        FuzzySepLiner next = base.intersectLines();
        System.out.println(next.getD().size() + " " + next.getL().size() + " " + next.getT().size() + " " + (next.getL().size() + next.getT().size()));
        next = enhanceFullFano(next);
        System.out.println(next.getD().size() + " " + next.getL().size() + " " + next.getT().size() + " " + (next.getL().size() + next.getT().size()));
        base = next.subLiner(10);
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
    }

    public FuzzySepLiner enhanceFullFano(FuzzySepLiner liner) {
        boolean incorrect = true;
        while (incorrect) {
            incorrect = false;
            List<Quad> full = liner.quads(3);
            System.out.println("Quads " + full.size());
            for (Quad q : full) {
                int abcd = liner.intersection(new Pair(q.a(), q.b()), new Pair(q.c(), q.d()));
                int acbd = liner.intersection(new Pair(q.a(), q.c()), new Pair(q.b(), q.d()));
                int adbc = liner.intersection(new Pair(q.a(), q.d()), new Pair(q.b(), q.c()));
                if (!liner.collinear(abcd, acbd, adbc)) {
                    incorrect = true;
                    liner.colline(abcd, acbd, adbc);
                }
            }
            liner.update();
            System.out.println("Before " + liner.getPc());
            liner = liner.quotient();
            System.out.println("After " + liner.getPc());
        }
        return liner;
    }
}
