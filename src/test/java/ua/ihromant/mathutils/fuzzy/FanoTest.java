package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Combinatorics;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class FanoTest {
    @Test
    public void testFano() {
        int[][] antiMoufang = {
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
        };
        FuzzyLiner base = FuzzyLiner.of(antiMoufang, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processFullFano,
                ContradictionUtil::processP1, ContradictionUtil::processPS, ContradictionUtil::processD2, ContradictionUtil::processD1S));
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, false, op);
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = op.apply(base);
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, true, op);
        base.printChars();
        ContradictionUtil.multipleByContradiction(base, true, op, l -> {
            try {
                l.printChars();
                System.out.println("Found partial");
                l = ContradictionUtil.singleByContradiction(l, false, op);
                l.printChars();
                ContradictionUtil.multipleByContradiction(l, false, op, l1 -> {
                    l1.printChars();
                    System.out.println("Found example");
                });
            } catch (IllegalArgumentException e) {
                System.out.println("Exception partial");
                // ok
            }
        });
    }

    @Test
    public void testFano1() {
        int[][] antiMoufang = {
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {1, 3, 7},
                {1, 4, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9},
                {4, 6, 9}
        };
        FuzzyLiner base = FuzzyLiner.of(antiMoufang, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processFullFano,
                ContradictionUtil::processP1, ContradictionUtil::processPS, ContradictionUtil::processD2, ContradictionUtil::processD1S));
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, false, op);
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = op.apply(base);
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, true, op);
        base.printChars();
        ContradictionUtil.multipleByContradiction(base, true, op, l -> {
            try {
                l.printChars();
                System.out.println("Found partial");
                l = ContradictionUtil.singleByContradiction(l, false, op);
                l.printChars();
                ContradictionUtil.multipleByContradiction(l, false, op, l1 -> {
                    l1.printChars();
                    System.out.println("Found example");
                });
            } catch (IllegalArgumentException e) {
                System.out.println("Exception partial");
                // ok
            }
        });
    }

    @Test
    public void testNearMoufang() {
        int[][] antiMoufang = {
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
        FuzzyLiner base = FuzzyLiner.of(antiMoufang, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processFullFano));
        base.printChars();
        ContradictionUtil.printContradiction(base, op);
    }

    @Test
    public void moufangFanoNotDesargues() {
        int[][] antiDesargues = {
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
        FuzzyLiner base = FuzzyLiner.of(antiDesargues, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processFullFano, ContradictionUtil::processD1));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        ContradictionUtil.printContradiction(base, op);
    }

    @Test
    public void fanoNoPentagon() {
        int[][] antiPentagon = {
                {0, 1, 5},
                {2, 4, 5},
                {1, 2, 6},
                {0, 3, 6},
                {1, 4, 7},
                {2, 3, 7},
                {0, 2, 8},
                {3, 4, 8},
                {0, 4, 9},
                {1, 3, 9},
                {5, 6, 7, 8}
        };
        FuzzyLiner base = FuzzyLiner.of(antiPentagon, Stream.concat(Stream.of(new Triple(7, 8, 9)),
                Combinatorics.choices(5, 3).map(ch -> new Triple(ch[0], ch[1], ch[2]))).toArray(Triple[]::new));
        base.printChars();
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processFullFano));
        ContradictionUtil.printContradiction(base, op);
        base = base.intersectLines();
        base.printChars();
        ContradictionUtil.printContradiction(base, op);
    }
}
