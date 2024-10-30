package ua.ihromant.mathutils.plane;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Pair;
import ua.ihromant.mathutils.SimpleLiner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class FanoMoufangTest {
    @Test
    public void generateFanoMoufang() {
        SimpleLiner base = joinByTwo(new SimpleLiner(10, new int[][]{
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {0, 7, 8, 9},
                {1, 3, 7},
                {1, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9},
                {4, 6, 9}
        }));
        int counter = 0;
        while (true) {
            int prev = base.pointCount();
            base = generateSimpleSteps(base, counter++);
            if (base.pointCount() == prev) {
                base = addTriple(base, counter);
            }
        }
    }

    @Test
    public void generateFanoNotNearMoufang() {
        SimpleLiner base = adjustFano(joinByTwo(new SimpleLiner(12, new int[][]{
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
        })));
        int counter = 0;
        while (true) {
            int prev = base.pointCount();
            base = generateSimpleSteps(base, counter++);
            if (base.pointCount() == prev) {
                base = addTuple(base, counter);
            }
        }
    }

    @Test
    public void generateFanoNotMoufang() {
        SimpleLiner base = joinByTwo(new SimpleLiner(12, new int[][]{
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
        }));
        int counter = 0;
        while (true) {
            int prev = base.pointCount();
            base = generateSimpleSteps(base, counter++);
            if (base.pointCount() == prev) {
                base = addTuple(base, counter);
            }
        }
    }

    private static SimpleLiner addTuple(SimpleLiner base, int counter) {
        System.out.println("Before tuple liner " + counter + " points " + base.pointCount() + " lines " + base.lineCount());
        testCorrectness(base);
        int skip = 0;
        Quad q = quads(base, 1).skip(skip).findAny().orElseThrow();
        int newPc = base.pointCount() + 2;
        List<FixBS> newLines = Arrays.stream(base.lines()).map(bs -> bs.copy(newPc)).collect(Collectors.toList());
        int ab = base.line(q.a, q.b);
        int cd = base.line(q.c, q.d);
        int abcd = base.intersection(ab, cd);
        int ac = base.line(q.a, q.c);
        int bd = base.line(q.b, q.d);
        int acbd = base.intersection(ac, bd);
        int ad = base.line(q.a, q.d);
        int bc = base.line(q.b, q.c);
        int adbc = base.intersection(ad, bc);
        if (abcd >= 0) {
            newLines.get(ac).set(base.pointCount());
            newLines.get(bd).set(base.pointCount());
            newLines.get(ad).set(base.pointCount() + 1);
            newLines.get(bc).set(base.pointCount() + 1);
            newLines.add(of(newPc, abcd, base.pointCount(), base.pointCount() + 1));
        } else {
            newLines.get(ab).set(base.pointCount());
            newLines.get(cd).set(base.pointCount());
            if (acbd >= 0) {
                newLines.get(ad).set(base.pointCount() + 1);
                newLines.get(bc).set(base.pointCount() + 1);
                newLines.add(of(newPc, acbd, base.pointCount(), base.pointCount() + 1));
            } else {
                newLines.get(ac).set(base.pointCount() + 1);
                newLines.get(bd).set(base.pointCount() + 1);
                newLines.add(of(newPc, adbc, base.pointCount(), base.pointCount() + 1));
            }
        }
        return joinByTwo(new SimpleLiner(newPc, newLines.toArray(FixBS[]::new)));
    }

    private static SimpleLiner addTriple(SimpleLiner base, int counter) {
        System.out.println("Before triple liner " + counter + " points " + base.pointCount() + " lines " + base.lineCount());
        testCorrectness(base);
        Quad q = quads(base, 0).findAny().orElseThrow();
        int newPc = base.pointCount() + 3;
        List<FixBS> newLines = Arrays.stream(base.lines()).map(bs -> bs.copy(newPc)).collect(Collectors.toList());
        int ab = base.line(q.a, q.b);
        int cd = base.line(q.c, q.d);
        int ac = base.line(q.a, q.c);
        int bd = base.line(q.b, q.d);
        int ad = base.line(q.a, q.d);
        int bc = base.line(q.b, q.c);
        newLines.get(ab).set(base.pointCount());
        newLines.get(cd).set(base.pointCount());
        newLines.get(ac).set(base.pointCount() + 1);
        newLines.get(bd).set(base.pointCount() + 1);
        newLines.get(ad).set(base.pointCount() + 2);
        newLines.get(bc).set(base.pointCount() + 2);
        newLines.add(of(newPc, base.pointCount(), base.pointCount() + 1, base.pointCount() + 2));
        return joinByTwo(new SimpleLiner(newPc, newLines.toArray(FixBS[]::new)));
    }

    private static SimpleLiner generateSimpleSteps(SimpleLiner base, int counter) {
        System.out.println("Before twos liner " + counter + " points " + base.pointCount() + " lines " + base.lineCount());
        testCorrectness(base);
        checkFano(quads(base, 3), base);
        logLiner(base);
        Set<FixBS> unique = new HashSet<>();
        quads(base, 2).forEach(q -> {
            int ab = base.line(q.a, q.b);
            int cd = base.line(q.c, q.d);
            int abcd = base.intersection(ab, cd);
            int ac = base.line(q.a, q.c);
            int bd = base.line(q.b, q.d);
            int acbd = base.intersection(ac, bd);
            int ad = base.line(q.a, q.d);
            int bc = base.line(q.b, q.c);
            int adbc = base.intersection(ad, bc);
            if (abcd < 0) {
                FixBS bs = new FixBS(base.lineCount());
                bs.set(ab);
                bs.set(cd);
                bs.set(base.line(acbd, adbc));
                if (unique.add(bs)) {
                    logTwoFano(bs, q.a, q.b, q.c, q.d, acbd, adbc);
                }
            } else {
                if (acbd < 0) {
                    FixBS bs = new FixBS(base.lineCount());
                    bs.set(ac);
                    bs.set(bd);
                    bs.set(base.line(abcd, adbc));
                    if (unique.add(bs)) {
                        logTwoFano(bs, q.a, q.c, q.b, q.d, abcd, adbc);
                    }
                } else {
                    FixBS bs = new FixBS(base.lineCount());
                    bs.set(ad);
                    bs.set(bc);
                    bs.set(base.line(abcd, acbd));
                    if (unique.add(bs)) {
                        logTwoFano(bs, q.a, q.d, q.b, q.c, abcd, acbd);
                    }
                }
            }
        });
        List<FixBS> grouped = getGrouped(new ArrayList<>(unique), false);
        int cnt = 0;
        int newPc = base.pointCount() + grouped.size();
        List<FixBS> newLines = Arrays.stream(base.lines()).map(bs -> bs.copy(newPc)).collect(Collectors.toList());
        for (FixBS beam : grouped) {
            int newPt = base.pointCount() + cnt++;
            for (int i = beam.nextSetBit(0); i >= 0; i = beam.nextSetBit(i + 1)) {
                newLines.get(i).set(newPt);
            }
        }
        SimpleLiner pre2Joined = new SimpleLiner(newPc, newLines.toArray(FixBS[]::new));
        SimpleLiner l = joinByTwo(pre2Joined);
        logLiner(l);
        return adjustFano(l);
    }

    private static void logTwoFano(FixBS intersectionTriple, int... pts) {
        System.out.println("Consider points " + Arrays.toString(pts) + ". All lines are intersecting but ["
                + pts[0] + ", " + pts[1] + "], [" + pts[2] + ", " + pts[3] + "], ["
                + pts[4] + ", " + pts[5] + "]. Therefore lines " + intersectionTriple + " should belong to one beam");
    }

    private static List<FixBS> getGrouped(List<FixBS> grouped, boolean line) {
        int sz;
        do {
            sz = grouped.size();
            grouped = mergeBeams(grouped, line);
        } while (sz != grouped.size());
        return grouped;
    }

    private static List<FixBS> mergeBeams(Collection<FixBS> beams, boolean line) {
        List<FixBS> result = new ArrayList<>();
        ex: for (FixBS beam : beams) {
            for (FixBS present : result) {
                if (present.equals(beam)) {
                    continue ex;
                }
                FixBS inter = present.intersection(beam);
                if (inter.equals(beam)) {
                    continue ex;
                }
                if (inter.cardinality() >= 2) {
                    System.out.println("Consider " + (line ? "lines " : "beams ") + present + " and " + beam
                            + ". They are intersecting with " + inter + ". Therefore we are uniting them to " + present.union(beam));
                    present.or(beam);
                    continue ex;
                }
            }
            result.add(beam);
        }
        return result;
    }

    private static SimpleLiner joinByTwo(SimpleLiner preBase) {
        Pair[] notJoined = notJoined(preBase);
        return new SimpleLiner(preBase.pointCount(),
                Stream.concat(Arrays.stream(preBase.lines()).map(l -> l.stream().toArray()),
                        Arrays.stream(notJoined).map(p -> new int[]{p.f(), p.s()})).toArray(int[][]::new));
    }

    private static FixBS of(int v, int... values) {
        FixBS bs = new FixBS(v);
        IntStream.of(values).forEach(bs::set);
        return bs;
    }

    private record Quad(int a, int b, int c, int d) {}

    private static Pair[] notJoined(SimpleLiner liner) {
        List<Pair> notIntersecting = new ArrayList<>();
        for (int a = 0; a < liner.pointCount(); a++) {
            for (int b = a + 1; b < liner.pointCount(); b++) {
                if (liner.line(a, b) < 0) {
                    notIntersecting.add(new Pair(a, b));
                }
            }
        }
        return notIntersecting.toArray(Pair[]::new);
    }

    private static Stream<Quad> quads(SimpleLiner liner, Integer desiredCount) {
        return quads(liner, liner.pointCount(), desiredCount);
    }

    private static Stream<Quad> quads(SimpleLiner liner, int cap, Integer desiredCount) {
        return IntStream.range(0, cap).boxed().flatMap(a -> IntStream.range(a + 1, cap).boxed().flatMap(b -> IntStream.range(b + 1, cap)
                .filter(c -> !liner.collinear(a, b, c)).boxed().mapMulti((c, sink) -> {
                    for (int d = c + 1; d < cap; d++) {
                        if (liner.collinear(a, b, d) || liner.collinear(a, c, d) || liner.collinear(b, c, d)) {
                            continue;
                        }
                        if (desiredCount == null) {
                            sink.accept(new Quad(a, b, c, d));
                            return;
                        }
                        int cnt = 0;
                        if (liner.intersection(liner.line(a, b), liner.line(c, d)) >= 0) {
                            cnt++;
                        }
                        if (liner.intersection(liner.line(a, c), liner.line(b, d)) >= 0) {
                            cnt++;
                        }
                        if (liner.intersection(liner.line(a, d), liner.line(b, c)) >= 0) {
                            cnt++;
                        }
                        if (cnt == desiredCount) {
                            sink.accept(new Quad(a, b, c, d));
                        }
                    }
                })));
    }

    private static void checkFano(Stream<Quad> quads, SimpleLiner liner) {
        List<FixBS> failed = new ArrayList<>();
        quads.forEach(q -> {
            int[] pts = IntStream.of(liner.intersection(liner.line(q.a, q.b), liner.line(q.c, q.d)),
                    liner.intersection(liner.line(q.a, q.c), liner.line(q.b, q.d)),
                    liner.intersection(liner.line(q.a, q.d), liner.line(q.b, q.c))).filter(i -> i >= 0).toArray();
            if (!liner.collinear(pts)) {
                failed.add(of(liner.pointCount(), pts));
            }
        });
        if (!failed.isEmpty()) {
            List<FixBS> grouped = getGrouped(failed, true);
            grouped.forEach(pts -> System.out.println(pts.stream().mapToObj(Integer::toString).collect(
                    Collectors.joining(", ", "newLines.add(of(newPc, ", "));"))));
            fail();
        }
    }

    private static void logLiner(SimpleLiner liner) {
        System.out.println("Current liner is");
        for (int i = 0; i < liner.lineCount(); i++) {
            System.out.println(i + ": " + liner.lines()[i]);
        }
    }

    private static SimpleLiner adjustFano(SimpleLiner liner) {
        int lc = liner.lineCount();
        Set<FixBS> unique = new HashSet<>();
        quads(liner, 3).forEach(q -> {
            int[] pts = IntStream.of(liner.intersection(liner.line(q.a, q.b), liner.line(q.c, q.d)),
                    liner.intersection(liner.line(q.a, q.c), liner.line(q.b, q.d)),
                    liner.intersection(liner.line(q.a, q.d), liner.line(q.b, q.c))).filter(i -> i >= 0).toArray();
            if (!liner.collinear(pts)) {
                FixBS bs = of(liner.pointCount(), pts);
                if (unique.add(bs)) {
                    System.out.println("Consider points " + q.a + ", " + q.b + ", " + q.c + ", " + q.d + ". They form full fano, but intersections "
                    + liner.intersection(liner.line(q.a, q.b), liner.line(q.c, q.d)) + ", " + liner.intersection(liner.line(q.a, q.c), liner.line(q.b, q.d))
                    + ", " + liner.intersection(liner.line(q.a, q.d), liner.line(q.b, q.c)) + " are not collinear. Therefore adding a line "
                    + bs + " to the list");
                }
            }
        });
        List<FixBS> grouped = getGrouped(Stream.concat(Arrays.stream(liner.lines()).map(bs -> bs.copy(liner.pointCount())), unique.stream())
                .sorted(Comparator.comparingInt(FixBS::cardinality).reversed()).toList(), true);
        if (grouped.size() != lc) {
            return adjustFano(new SimpleLiner(liner.pointCount(), grouped.toArray(FixBS[]::new)));
        }
        return liner;
    }

    public static void testCorrectness(SimpleLiner plane) {
        for (int p1 = 0; p1 < plane.pointCount(); p1++) {
            for (int p2 = p1 + 1; p2 < plane.pointCount(); p2++) {
                assertTrue(plane.line(p1, p2) >= 0, p1 + " " + p2);
            }
        }
    }
}
