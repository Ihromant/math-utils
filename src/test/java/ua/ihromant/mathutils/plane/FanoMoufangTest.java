package ua.ihromant.mathutils.plane;

import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Liner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
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
    public void generateFanoNotMoulton() {
        Liner base = new Liner(13, new int[][]{
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {0, 7, 8, 10, 11},
                {1, 3, 7},
                {1, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9},
                {4, 6, 9},
                {0, 9, 12},
                {1, 4, 10},
                {1, 6, 11},
                {1, 9},
                {2, 3, 10},
                {2, 5, 11},
                {2, 9},
                {3, 6, 12},
                {3, 8},
                {3, 11},
                {4, 5, 12},
                {4, 8},
                {4, 11},
                {5, 7},
                {5, 10},
                {6, 7},
                {6, 10},
                {7, 9},
                {8, 9},
                {9, 10},
                {9, 11},
                {1, 12},
                {2, 12},
                {7, 12},
                {8, 12},
                {10, 12},
                {11, 12}
        });
        int counter = 0;
        int prev = 0;
        while (true) {
            System.out.println("Checking liner " + counter++);
            int tmp = base.pointCount();
            base = generateNext(base, prev);
            prev = tmp;
        }
    }

    private static Liner generateNext(Liner base, int prevPts) {
        testCorrectness(base);
        System.out.println("Pts: " + base.pointCount() + ", lines: " + base.lineCount());
        assertTrue(checkFano(quads(base, prevPts), base));
        List<Quad> quads = quads(base, base.pointCount());
        Pair[] notInt = notIntersecting(base);
        Map<Pair, Integer> idxes = IntStream.range(0, notInt.length).boxed().collect(Collectors.toMap(i -> notInt[i], Function.identity()));
        System.out.println("Quads: " + quads.size() + ", not inter: " + notInt.length);
        Map<Integer, List<Quad>> grouped = quads.stream().collect(Collectors.groupingBy(q -> {
            int cnt = 0;
            if (base.intersection(base.line(q.a, q.b), base.line(q.c, q.d)) >= 0) {
                cnt++;
            }
            if (base.intersection(base.line(q.a, q.c), base.line(q.b, q.d)) >= 0) {
                cnt++;
            }
            if (base.intersection(base.line(q.a, q.d), base.line(q.b, q.c)) >= 0) {
                cnt++;
            }
            return cnt;
        }));
        System.out.println("QuadDist: " + grouped.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().size()).collect(Collectors.joining(", ")));
        List<int[]> newLines = Arrays.stream(base.lines()).collect(Collectors.toList());
        grouped.getOrDefault(2, List.of()).forEach(q -> {
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
                int newIdx = idxes.get(new Pair(ab, cd));
                join2Fano(newIdx  + base.pointCount(), base.line(acbd, adbc), ab, cd, newLines);
            } else {
                if (acbd < 0) {
                    int newIdx = idxes.get(new Pair(ac, bd));
                    join2Fano(newIdx  + base.pointCount(), base.line(abcd, adbc), ac, bd, newLines);
                } else {
                    int newIdx = idxes.get(new Pair(ad, bc));
                    join2Fano(newIdx  + base.pointCount(), base.line(abcd, acbd), ad, bc, newLines);
                }
            }
        });
        Map<Integer, List<List<Pair>>> desiredOneFlags = new HashMap<>();
        grouped.get(1).forEach(q -> {
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
                int acbdIdx = idxes.get(new Pair(ac, bd)) + base.pointCount();
                int adbcIdx = idxes.get(new Pair(ad, bc)) + base.pointCount();
                appendToLine(newLines, ac, acbdIdx);
                appendToLine(newLines, bd, acbdIdx);
                appendToLine(newLines, ad, adbcIdx);
                appendToLine(newLines, bc, adbcIdx);
                desiredOneFlags.computeIfAbsent(abcd, k -> new ArrayList<>()).add(List.of(new Pair(ac, bd), new Pair(ad, bc)));
            } else {
                int abcdIdx = idxes.get(new Pair(ab, cd)) + base.pointCount();
                appendToLine(newLines, ab, abcdIdx);
                appendToLine(newLines, cd, abcdIdx);
                if (acbd >= 0) {
                    int adbcIdx = idxes.get(new Pair(ad, bc)) + base.pointCount();
                    appendToLine(newLines, ad, adbcIdx);
                    appendToLine(newLines, bc, adbcIdx);
                    desiredOneFlags.computeIfAbsent(acbd, k -> new ArrayList<>()).add(List.of(new Pair(ab, cd), new Pair(ad, bc)));
                } else {
                    int acbdIdx = idxes.get(new Pair(ac, bd)) + base.pointCount();
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
                    newLine[i] = idxes.get(newPt.get(i)) + base.pointCount();
                }
                Arrays.sort(newLine);
                newLines.add(newLine);
            }
        });
        BitSet[][] fanoLines = new BitSet[notInt.length + base.pointCount()][notInt.length + base.pointCount()];
        grouped.get(0).forEach(q -> {
            int ab = base.line(q.a, q.b);
            int cd = base.line(q.c, q.d);
            int ac = base.line(q.a, q.c);
            int bd = base.line(q.b, q.d);
            int ad = base.line(q.a, q.d);
            int bc = base.line(q.b, q.c);
            int abcdIdx = idxes.get(new Pair(ab, cd)) + base.pointCount();
            int acbdIdx = idxes.get(new Pair(ac, bd)) + base.pointCount();
            int adbcIdx = idxes.get(new Pair(ad, bc)) + base.pointCount();
            appendToLine(newLines, ab, abcdIdx);
            appendToLine(newLines, cd, abcdIdx);
            appendToLine(newLines, ac, acbdIdx);
            appendToLine(newLines, bd, acbdIdx);
            appendToLine(newLines, ad, adbcIdx);
            appendToLine(newLines, bc, adbcIdx);
            BitSet newLine = of(abcdIdx, acbdIdx, adbcIdx);
            fanoLines[abcdIdx][acbdIdx] = newLine;
            fanoLines[abcdIdx][adbcIdx] = newLine;
            fanoLines[acbdIdx][abcdIdx] = newLine;
            fanoLines[acbdIdx][adbcIdx] = newLine;
            fanoLines[adbcIdx][abcdIdx] = newLine;
            fanoLines[adbcIdx][acbdIdx] = newLine;
        });
        Set<BitSet> unique = Arrays.stream(fanoLines).flatMap(Arrays::stream).filter(Objects::nonNull).collect(Collectors.toSet());
        Liner l = new Liner(base.pointCount() + notInt.length, Stream.concat(newLines.stream(),
                unique.stream().map(bs -> bs.stream().toArray())).toArray(int[][]::new));
        Pair[] notJoined = notJoined(l);
        return new Liner(l.pointCount(), Stream.concat(Arrays.stream(l.lines()), Arrays.stream(notJoined).map(p -> new int[]{p.f, p.s})).toArray(int[][]::new));
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet(values[values.length - 1] + 1);
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


    @EqualsAndHashCode
    private static class Pair {
        private final int f;
        private final int s;
        public Pair(int f, int s) {
            this.f = Math.min(f, s);
            this.s = Math.max(f, s);
        }

        @Override
        public String toString() {
            return "P(" + f + "," + s + ")";
        }
    }

    private record Quad(int a, int b, int c, int d) {}

    private static Pair[] notJoined(Liner liner) {
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

    private static Pair[] notIntersecting(Liner liner) {
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

    private static List<Quad> quads(Liner liner, int cap) {
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

    private static boolean checkFano(List<Quad> list, Liner liner) {
        return list.stream().allMatch(q -> liner.collinear(liner.intersection(liner.line(q.a, q.b), liner.line(q.c, q.d)),
                liner.intersection(liner.line(q.a, q.c), liner.line(q.b, q.d)),
                liner.intersection(liner.line(q.a, q.d), liner.line(q.b, q.c))));
    }

    public static void testCorrectness(Liner plane) {
        for (int p1 = 0; p1 < plane.pointCount(); p1++) {
            for (int p2 = p1 + 1; p2 < plane.pointCount(); p2++) {
                assertTrue(plane.line(p1, p2) >= 0, p1 + " " + p2);
            }
        }
    }
}
