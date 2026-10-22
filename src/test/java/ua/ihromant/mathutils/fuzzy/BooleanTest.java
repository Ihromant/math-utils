package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.QuickFind;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BooleanTest {
    @Test
    public void test() {
        int[][] lines = new int[][]{{0, 1, 6, 7}, {0, 2, 3, 4}, {0, 5}, {1, 2, 5}, {1, 3}, {1, 4}, {3, 5, 6}, {4, 5, 7},
                {2, 6}, {2, 7}, {3, 7}, {4, 6}};
        Liner lnr = new Liner(8, lines);
        Liner land0 = landStep(lnr);
        Liner sky0 = skyStep(land0);
        Liner land1 = landStep(sky0);
        Liner sky1 = skyStep(land1);
        System.out.println();
    }

    private static Liner landStep(Liner lnr) {
        List<int[]> intersections = new ArrayList<>();
        int ctr = lnr.pointCount();
        for (int i = 0; i < lnr.pointCount(); i++) {
            for (int j = i + 1; j < lnr.pointCount(); j++) {
                if (lnr.intersection(i, j) < 0) {
                    intersections.add(new int[]{ctr++, i, j});
                }
            }
        }
        Map<Integer, List<Integer>> adds = new HashMap<>();
        for (int[] intersection : intersections) {
            adds.computeIfAbsent(intersection[1], _ -> new ArrayList<>()).add(intersection[0]);
            adds.computeIfAbsent(intersection[2], _ -> new ArrayList<>()).add(intersection[0]);
        }
        int[][] newLines = new int[lnr.lineCount()][];
        for (int i = 0; i < lnr.lineCount(); i++) {
            newLines[i] = IntStream.concat(Arrays.stream(lnr.line(i)), adds.getOrDefault(i, List.of()).stream().mapToInt(Integer::intValue)).toArray();
        }
        Liner next = new Liner(ctr, newLines);
        List<int[]> prs = new ArrayList<>();
        for (int i = 0; i < next.pointCount(); i++) {
            if (next.flag(0, i)) {
                continue;
            }
            for (int j = i + 1; j < next.pointCount(); j++) {
                if (next.flag(0, j)) {
                    continue;
                }
                if (next.line(i, j) < 0) {
                    prs.add(new int[]{i, j});
                }
            }
        }
        return new Liner(ctr, Stream.concat(Arrays.stream(next.lines()), prs.stream()).toArray(int[][]::new));
    }

    private static Liner skyStep(Liner lnr) {
        QuickFind qf = new QuickFind(lnr.lineCount());
        int[] inf = lnr.line(0);
        for (int i = 0; i < inf.length; i++) {
            int[] iBeam = lnr.lines(inf[i]);
            for (int j = i + 1; j < inf.length; j++) {
                int[] jBeam = lnr.lines(inf[j]);
                for (int a : iBeam) {
                    if (a == 0) {
                        continue;
                    }
                    for (int b : iBeam) {
                        if (b == 0 || a >= b) {
                            continue;
                        }
                        for (int c : jBeam) {
                            if (c == 0) {
                                continue;
                            }
                            for (int d : jBeam) {
                                if (d == 0 || c >= d) {
                                    continue;
                                }
                                int ac = lnr.intersection(a, c);
                                int ad = lnr.intersection(a, d);
                                int bc = lnr.intersection(b, c);
                                int bd = lnr.intersection(b, d);
                                if (ac < 0 || ad < 0 || bc < 0 || bd < 0) {
                                    continue;
                                }
                                int acbd = lnr.line(ac, bd);
                                int adbc = lnr.line(ad, bc);
                                qf.union(acbd, adbc);
                                System.out.println("Union " + Arrays.toString(new int[]{ac, ad, bc, bd}) + " " + acbd + " " + Arrays.toString(lnr.line(acbd)) + " " + adbc + " " + Arrays.toString(lnr.line(adbc)));
                            }
                        }
                    }
                }
            }
        }
        int[] infs = new int[lnr.lineCount()];
        int ctr = lnr.pointCount();
        for (FixBS component : qf.components()) {
            if (component.get(0)) {
                continue;
            }
            int[] arr = component.toArray();
            int[] inter = Arrays.stream(arr).map(i -> lnr.intersection(0, i)).filter(i -> i >= 0).distinct().toArray();
            if (inter.length > 1) {
                throw new IllegalStateException("Something wrong");
            }
            int pt = inter.length == 0 ? ctr++ : inter[0];
            for (int i : arr) {
                infs[i] = pt;
            }
        }
        List<int[]> newLines = new ArrayList<>();
        newLines.add(Arrays.stream(infs).distinct().sorted().toArray());
        for (int i = 1; i < lnr.lineCount(); i++) {
            int[] line = lnr.line(i);
            int ip = infs[i];
            if (Arrays.binarySearch(line, ip) >= 0) {
                if (line.length > 2) {
                    newLines.add(line);
                }
            } else {
                int[] newLine = new int[line.length + 1];
                System.arraycopy(line, 0, newLine, 0, line.length);
                newLine[line.length] = ip;
                Arrays.sort(newLine);
                newLines.add(newLine);
            }
        }
        Liner next = new Liner(ctr, newLines.toArray(int[][]::new));
        List<int[]> prs = new ArrayList<>();
        for (int i : newLines.getFirst()) {
            for (int j = 0; j < next.pointCount(); j++) {
                if (!next.flag(0, j) && next.line(i, j) < 0) {
                    prs.add(i < j ? new int[]{i, j} : new int[]{j, i});
                }
            }
        }
        return new Liner(ctr, Stream.concat(Arrays.stream(next.lines()), prs.stream()).toArray(int[][]::new));
    }
}
