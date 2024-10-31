package ua.ihromant.mathutils.plane;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.FuzzyLiner;
import ua.ihromant.mathutils.Pair;

import java.util.List;

public class FuzzyLinerTest {
    @Test
    public void testFanoNearMoufang() {
        FuzzyLiner base = new FuzzyLiner(new int[][]{
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {0, 7, 8, 10, 11},
                {1, 3, 7},
                {1, 4, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9, 10},
                {4, 6, 9, 11}
        });
        base.update();
        enhanceFullFano(base);
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        Quad q2;
        while ((q2 = base.quad(2)) != null) {
            int newPt = base.getPc();
            System.out.println("Adding point " + newPt + " to almost full quad " + q2);
            base = base.addPoint();
            Pair ab = new Pair(q2.a(), q2.b());
            Pair cd = new Pair(q2.c(), q2.d());
            int abcd = base.intersection(ab, cd);
            Pair ac = new Pair(q2.a(), q2.c());
            Pair bd = new Pair(q2.b(), q2.d());
            int acbd = base.intersection(ac, bd);
            Pair ad = new Pair(q2.a(), q2.d());
            Pair bc = new Pair(q2.b(), q2.c());
            int adbc = base.intersection(ad, bc);
            if (abcd < 0) {
                base.colline(q2.a(), q2.b(), newPt);
                base.colline(q2.c(), q2.d(), newPt);
                base.colline(acbd, adbc, newPt);
            } else {
                if (acbd < 0) {
                    base.colline(q2.a(), q2.c(), newPt);
                    base.colline(q2.b(), q2.d(), newPt);
                    base.colline(abcd, adbc, newPt);
                } else {
                    base.colline(q2.a(), q2.d(), newPt);
                    base.colline(q2.b(), q2.c(), newPt);
                    base.colline(abcd, acbd, newPt);
                }
            }
            base.update();
        }
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
    }

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
        Quad q;
        while ((q = base.quad(2)) != null) {
            int newPt = base.getPc();
            System.out.println("Adding point " + newPt + " to almost full quad " + q);
            base = base.addPoint();
            Pair ab = new Pair(q.a(), q.b());
            Pair cd = new Pair(q.c(), q.d());
            int abcd = base.intersection(ab, cd);
            Pair ac = new Pair(q.a(), q.c());
            Pair bd = new Pair(q.b(), q.d());
            int acbd = base.intersection(ac, bd);
            Pair ad = new Pair(q.a(), q.d());
            Pair bc = new Pair(q.b(), q.c());
            int adbc = base.intersection(ad, bc);
            if (abcd < 0) {
                base.colline(q.a(), q.b(), newPt);
                base.colline(q.c(), q.d(), newPt);
                base.colline(acbd, adbc, newPt);
            } else {
                if (acbd < 0) {
                    base.colline(q.a(), q.c(), newPt);
                    base.colline(q.b(), q.d(), newPt);
                    base.colline(abcd, adbc, newPt);
                } else {
                    base.colline(q.a(), q.d(), newPt);
                    base.colline(q.b(), q.c(), newPt);
                    base.colline(abcd, acbd, newPt);
                }
            }
            base.update();
        }
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
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
            if (incorrect) {
                fl.update();
            }
        }
    }
}
