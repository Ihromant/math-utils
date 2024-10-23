package ua.ihromant.mathutils.plane;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Pair;
import ua.ihromant.mathutils.SimpleLiner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FanoMoufangTest {
    @Test
    public void generateFanoNotMoufang() {
        SimpleLiner base = new SimpleLiner(10, new int[][]{
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
        });
        int counter = 0;
        int prev = 0;
        while (true) {
            int tmp = base.pointCount();
            base = generateNext(base, prev, counter++);
            prev = tmp;
        }
    }

    private static SimpleLiner generateNext(SimpleLiner preBase, int prevPts, int counter) {
        Pair[] notJoined = notJoined(preBase);
        SimpleLiner base = new SimpleLiner(preBase.pointCount(),
                Stream.concat(Arrays.stream(preBase.lines()).map(l -> l.stream().toArray()),
                        Arrays.stream(notJoined).map(p -> new int[]{p.f(), p.s()})).toArray(int[][]::new));
        System.out.println("Checking liner " + counter + " prev pts " + prevPts);
        //testCorrectness(base);
        System.out.println("Base pts: " + base.pointCount() + ", lines: " + base.lineCount());
        assertTrue(checkFano(quads(base, prevPts, null), base));
        List<int[]> twosLines = Arrays.stream(base.lines()).map(bs -> bs.stream().toArray()).collect(Collectors.toList());
        List<Quad> twos = quads(base, base.pointCount(), 2).toList();
        System.out.println("Twos: " + twos.size());
        Set<FixBS> processed = new HashSet<>();
        twos.forEach(q -> {
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
                if (!processed.add(of(base.pointCount() + twos.size(), q.a, q.b, q.c, q.d, acbd, adbc))) {
                    return;
                }
                join2Fano(processed.size() - 1 + base.pointCount(), base.line(acbd, adbc), ab, cd, twosLines);
            } else {
                if (acbd < 0) {
                    if (!processed.add(of(base.pointCount() + twos.size(), q.a, q.b, q.c, q.d, abcd, adbc))) {
                        return;
                    }
                    join2Fano(processed.size() - 1 + base.pointCount(), base.line(abcd, adbc), ac, bd, twosLines);
                } else {
                    if (!processed.add(of(base.pointCount() + twos.size(), q.a, q.b, q.c, q.d, abcd, acbd))) {
                        return;
                    }
                    join2Fano(processed.size() - 1 + base.pointCount(), base.line(abcd, acbd), ad, bc, twosLines);
                }
            }
        });
        SimpleLiner twosBaseJoined = new SimpleLiner(base.pointCount() + processed.size(), twosLines.toArray(int[][]::new));
        Pair[] twosNotJoined = notJoined(twosBaseJoined);
        SimpleLiner twosJoined = new SimpleLiner(twosBaseJoined.pointCount(),
                Stream.concat(Arrays.stream(twosBaseJoined.lines()).map(l -> l.stream().toArray()),
                        Arrays.stream(twosNotJoined).map(p -> new int[]{p.f(), p.s()})).toArray(int[][]::new));
        System.out.println("Twos joined pts: " + twosJoined.pointCount() + ", lines: " + base.lineCount());
        Pair[] notInt = notIntersecting(twosJoined);
        Map<Pair, Integer> idxes = IntStream.range(0, notInt.length).boxed().collect(Collectors.toMap(i -> notInt[i], Function.identity()));
        System.out.println("Not inter: " + notInt.length);
        List<int[]> newLines = Arrays.stream(twosJoined.lines()).map(bs -> bs.stream().toArray()).collect(Collectors.toList());
        Map<Integer, List<List<Pair>>> desiredOneFlags = new HashMap<>();
        quads(twosJoined, twosJoined.pointCount(), 1).forEach(q -> {
            int ab = twosJoined.line(q.a, q.b);
            int cd = twosJoined.line(q.c, q.d);
            int abcd = twosJoined.intersection(ab, cd);
            int ac = twosJoined.line(q.a, q.c);
            int bd = twosJoined.line(q.b, q.d);
            int acbd = twosJoined.intersection(ac, bd);
            int ad = twosJoined.line(q.a, q.d);
            int bc = twosJoined.line(q.b, q.c);
            int adbc = twosJoined.intersection(ad, bc);
            if (abcd >= 0) {
                int acbdIdx = idxes.get(new Pair(ac, bd)) + twosJoined.pointCount();
                int adbcIdx = idxes.get(new Pair(ad, bc)) + twosJoined.pointCount();
                appendToLine(newLines, ac, acbdIdx);
                appendToLine(newLines, bd, acbdIdx);
                appendToLine(newLines, ad, adbcIdx);
                appendToLine(newLines, bc, adbcIdx);
                desiredOneFlags.computeIfAbsent(abcd, k -> new ArrayList<>()).add(List.of(new Pair(ac, bd), new Pair(ad, bc)));
            } else {
                int abcdIdx = idxes.get(new Pair(ab, cd)) + twosJoined.pointCount();
                appendToLine(newLines, ab, abcdIdx);
                appendToLine(newLines, cd, abcdIdx);
                if (acbd >= 0) {
                    int adbcIdx = idxes.get(new Pair(ad, bc)) + twosJoined.pointCount();
                    appendToLine(newLines, ad, adbcIdx);
                    appendToLine(newLines, bc, adbcIdx);
                    desiredOneFlags.computeIfAbsent(acbd, k -> new ArrayList<>()).add(List.of(new Pair(ab, cd), new Pair(ad, bc)));
                } else {
                    int acbdIdx = idxes.get(new Pair(ac, bd)) + twosJoined.pointCount();
                    appendToLine(newLines, ac, acbdIdx);
                    appendToLine(newLines, bd, acbdIdx);
                    desiredOneFlags.computeIfAbsent(adbc, k -> new ArrayList<>()).add(List.of(new Pair(ab, cd), new Pair(ac, bd)));
                }
            }
        });
        desiredOneFlags.forEach((oldPt, newPts) -> {
            for (List<Pair> newPt : newPts) {
                int[] newLine = new int[newPt.size() + 1];
                newLine[newPt.size()] = oldPt;
                for (int i = 0; i < newPt.size(); i++) {
                    newLine[i] = idxes.get(newPt.get(i)) + twosJoined.pointCount();
                }
                Arrays.sort(newLine);
                newLines.add(newLine);
            }
        });
        int newPc = notInt.length + twosJoined.pointCount();
        FixBS[][] fanoLines = new FixBS[newPc][newPc];
        quads(twosJoined, twosJoined.pointCount(), 0).forEach(q -> {
            int ab = twosJoined.line(q.a, q.b);
            int cd = twosJoined.line(q.c, q.d);
            int ac = twosJoined.line(q.a, q.c);
            int bd = twosJoined.line(q.b, q.d);
            int ad = twosJoined.line(q.a, q.d);
            int bc = twosJoined.line(q.b, q.c);
            int abcdIdx = idxes.get(new Pair(ab, cd)) + twosJoined.pointCount();
            int acbdIdx = idxes.get(new Pair(ac, bd)) + twosJoined.pointCount();
            int adbcIdx = idxes.get(new Pair(ad, bc)) + twosJoined.pointCount();
            appendToLine(newLines, ab, abcdIdx);
            appendToLine(newLines, cd, abcdIdx);
            appendToLine(newLines, ac, acbdIdx);
            appendToLine(newLines, bd, acbdIdx);
            appendToLine(newLines, ad, adbcIdx);
            appendToLine(newLines, bc, adbcIdx);
            FixBS newLine = of(newPc, abcdIdx, acbdIdx, adbcIdx);
            fanoLines[abcdIdx][acbdIdx] = newLine;
            fanoLines[abcdIdx][adbcIdx] = newLine;
            fanoLines[acbdIdx][abcdIdx] = newLine;
            fanoLines[acbdIdx][adbcIdx] = newLine;
            fanoLines[adbcIdx][abcdIdx] = newLine;
            fanoLines[adbcIdx][acbdIdx] = newLine;
        });
        Set<FixBS> unique = Arrays.stream(fanoLines).flatMap(Arrays::stream).filter(Objects::nonNull).collect(Collectors.toSet());
        return new SimpleLiner(twosJoined.pointCount() + notInt.length, Stream.concat(newLines.stream(),
                unique.stream().map(bs -> bs.stream().toArray())).toArray(int[][]::new));
    }

    private static FixBS of(int v, int... values) {
        FixBS bs = new FixBS(v);
        IntStream.of(values).forEach(bs::set);
        return bs;
    }

    private static void join2Fano(int newPtIdx, int lastLineIdx, int ab, int cd, List<int[]> newLines) {
        appendToLine(newLines, ab, newPtIdx);
        appendToLine(newLines, cd, newPtIdx);
        appendToLine(newLines, lastLineIdx, newPtIdx);
    }

    private static void appendToLine(List<int[]> newLines, int line, int newPtIdx) {
        int[] oldLine = newLines.get(line);
        if (Arrays.binarySearch(oldLine, newPtIdx) >= 0) {
            return;
        }
        int[] newLine = Arrays.copyOf(oldLine, oldLine.length + 1);
        newLine[oldLine.length] = newPtIdx;
        Arrays.sort(newLine);
        newLines.set(line, newLine);
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

    private static Pair[] notIntersecting(SimpleLiner liner) {
        List<Pair> notIntersecting = new ArrayList<>();
        for (int a = 0; a < liner.lineCount(); a++) {
            for (int b = a + 1; b < liner.lineCount(); b++) {
                if (liner.intersection(a, b) < 0) {
                    notIntersecting.add(new Pair(a, b));
                }
            }
        }
        return notIntersecting.toArray(Pair[]::new);
    }

    private static List<Quad> quads(SimpleLiner liner, int cap) {
        List<Quad> result = new ArrayList<>();
        for (int a = 0; a < cap; a++) {
            for (int b = a + 1; b < cap; b++) {
                for (int c = b + 1; c < cap; c++) {
                    if (liner.collinear(a, b, c)) {
                        continue;
                    }
                    for (int d = c + 1; d < cap; d++) {
                        if (liner.collinear(a, b, d) || liner.collinear(a, c, d) || liner.collinear(b, c, d)) {
                            continue;
                        }
                        result.add(new Quad(a, b, c, d));
                    }
                }
            }
        }
        return result;
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

    private static boolean checkFano(Stream<Quad> quads, SimpleLiner liner) {
        return quads.allMatch(q -> liner.collinear(liner.intersection(liner.line(q.a, q.b), liner.line(q.c, q.d)),
                liner.intersection(liner.line(q.a, q.c), liner.line(q.b, q.d)),
                liner.intersection(liner.line(q.a, q.d), liner.line(q.b, q.c))));
    }

    public static void testCorrectness(SimpleLiner plane) {
        for (int p1 = 0; p1 < plane.pointCount(); p1++) {
            for (int p2 = p1 + 1; p2 < plane.pointCount(); p2++) {
                assertTrue(plane.line(p1, p2) >= 0, p1 + " " + p2);
            }
        }
    }
}
