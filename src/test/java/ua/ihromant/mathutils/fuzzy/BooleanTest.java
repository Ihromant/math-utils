package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.QuickFind;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BooleanTest {
    @Test
    public void test() {
        int[][] lines = new int[][]{{0, 1, 6, 7}, {0, 2, 3, 4}, {0, 5}, {1, 2, 5}, {1, 3}, {1, 4}, {3, 5, 6}, {4, 5, 7},
                {2, 6}, {2, 7}, {3, 7}, {4, 6}};
        Liner lnr = new Liner(8, lines);
        Liner land0 = landStep(lnr);
        System.out.println(land0.pointCount() + " " + land0.lineCount() + " " + checkBoolean(land0) + " " + checkThales(land0));
        Liner sky0 = skyStep(land0);
        System.out.println(sky0.pointCount() + " " + sky0.lineCount() + " " + checkBoolean(sky0) + " " + checkThales(sky0));
        Liner land1 = landStep(sky0);
        System.out.println(land1.pointCount() + " " + land1.lineCount() + " " + checkThales(land1));
        Liner sky1 = skyStep(land1);
        System.out.println(sky1.pointCount() + " " + sky1.lineCount() + " " + checkThales(sky1));
    }

    @Test
    public void test1() {
        int[][] lines = new int[][]{{3, 4, 5}, {0, 1, 2, 3}, {0, 4}, {1, 4}, {2, 4}, {0, 5}, {1, 5}, {2, 5}};
        Liner lnr = new Liner(6, lines);
        Liner land0 = landStep(lnr);
        System.out.println(land0.pointCount() + " " + land0.lineCount() + " " + checkBoolean(land0) + " " + checkThales(land0));
        Liner sky0 = skyStep(land0);
        System.out.println(sky0.pointCount() + " " + sky0.lineCount() + " " + checkBoolean(sky0) + " " + checkThales(sky0));
        Liner land1 = landStep(sky0);
        System.out.println(land1.pointCount() + " " + land1.lineCount() + " " + checkThales(land1));
        Liner sky1 = skyStep(land1);
        System.out.println(sky1.pointCount() + " " + sky1.lineCount() + " " + checkThales(sky1));
    }

    private static Liner landStep(Liner lnr) {
        QuickFind qf = new QuickFind(lnr.lineCount() * lnr.lineCount());
        for (int i = 0; i < lnr.pointCount(); i++) {
            int[] lns = lnr.lines(i);
            int pr = lns[0] * lnr.lineCount() + lns[1];
            for (int j = 0; j < lns.length; j++) {
                int a = lns[j];
                for (int k = j + 1; k < lns.length; k++) {
                    int b = lns[k];
                    qf.union(pr, a * lnr.lineCount() + b);
                    qf.union(pr, b * lnr.lineCount() + a);
                }
            }
        }
        for (int i = 0; i < lnr.pointCount(); i++) {
            if (lnr.flag(0, i)) {
                continue;
            }
            for (int j = i + 1; j < lnr.pointCount(); j++) {
                if (lnr.flag(0, j)) {
                    continue;
                }
                int ij = lnr.line(i, j);
                int ijInf = lnr.intersection(ij, 0);
                for (int k = j + 1; k < lnr.pointCount(); k++) {
                    if (lnr.flag(0, k) || lnr.flag(ij, k)) {
                        continue;
                    }
                    int ik = lnr.line(i, k);
                    int jk = lnr.line(j, k);
                    int ikInf = lnr.intersection(ik, 0);
                    int jkInf = lnr.intersection(jk, 0);
                    int ijk = lnr.line(ijInf, k);
                    int ikj = lnr.line(ikInf, j);
                    int jki = lnr.line(jkInf, i);
                    int fst = ijk * lnr.lineCount() + ikj;
                    qf.union(fst, ikj * lnr.lineCount() + ijk);
                    qf.union(fst, ijk * lnr.lineCount() + jki);
                    qf.union(fst, jki * lnr.lineCount() + ijk);
                    qf.union(fst, ikj * lnr.lineCount() + jki);
                    qf.union(fst, jki * lnr.lineCount() + ikj);
                }
            }
        }
        for (int i = 0; i < lnr.lineCount(); i++) {
            for (int j = i + 1; j < lnr.lineCount(); j++) {
                qf.union(i * lnr.lineCount() + j, j * lnr.lineCount() + i);
            }
        }
        for (int i = 0; i < lnr.lineCount(); i++) {
            for (int j = 0; j < lnr.lineCount(); j++) {
                if (i == j) {
                    continue;
                }
                for (int k = 0; k < lnr.lineCount(); k++) {
                    if (i == k || j == k) {
                        continue;
                    }
                    int prIj = i * lnr.lineCount() + j;
                    int prJk = j * lnr.lineCount() + k;
                    int prIk = i * lnr.lineCount() + k;
                    if (qf.root(prIj) == qf.root(prJk)) {
                        qf.union(prIj, prIk);
                    }
                }
            }
        }
        List<FixBS> comps = qf.components();
        int ctr = lnr.pointCount();
        Map<Integer, Set<Integer>> adds = new HashMap<>();
        for (FixBS comp : comps) {
            if (comp.cardinality() == 1) {
                continue;
            }
            int[] pairArr = comp.toArray();
            Set<Integer> existing = new HashSet<>();
            List<Integer> unknown = new ArrayList<>();
            for (int pr : pairArr) {
                int fst = pr / lnr.lineCount();
                int snd = pr % lnr.lineCount();
                int inter = lnr.intersection(fst, snd);
                if (inter < 0) {
                    unknown.add(pr);
                } else {
                    existing.add(inter);
                }
            }
            if (existing.size() > 2) {
                throw new IllegalStateException("Impossible situation");
            }
            int addition = existing.isEmpty() ? ctr++ : existing.iterator().next();
            for (int pr : unknown) {
                int fst = pr / lnr.lineCount();
                int snd = pr % lnr.lineCount();
                if (addition >= lnr.pointCount() || !lnr.flag(fst, addition)) {
                    adds.computeIfAbsent(fst, _ -> new HashSet<>()).add(addition);
                }
                if (addition >= lnr.pointCount() || !lnr.flag(snd, addition)) {
                    adds.computeIfAbsent(snd, _ -> new HashSet<>()).add(addition);
                }
            }
        }
        int[][] newLines = new int[lnr.lineCount()][];
        for (int i = 0; i < lnr.lineCount(); i++) {
            newLines[i] = IntStream.concat(Arrays.stream(lnr.line(i)), adds.getOrDefault(i, Set.of()).stream().mapToInt(Integer::intValue).sorted()).toArray();
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
                                //System.out.println("Union " + Arrays.toString(new int[]{ac, ad, bc, bd}) + " " + acbd + " " + Arrays.toString(lnr.line(acbd)) + " " + adbc + " " + Arrays.toString(lnr.line(adbc)));
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
        newLines.add(Arrays.stream(infs).skip(1).distinct().sorted().toArray());
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

    private static boolean checkBoolean(Liner lnr) {
        int[] infLine = lnr.line(0);
        for (int i = 0; i < infLine.length; i++) {
            int[] xLns = lnr.lines(infLine[i]);
            for (int j = i + 1; j < infLine.length; j++) {
                int[] yLns = lnr.lines(infLine[j]);
                for (int a : xLns) {
                    if (a == 0) {
                        continue;
                    }
                    for (int b : xLns) {
                        if (a >= b) {
                            continue;
                        }
                        for (int c : yLns) {
                            if (c == 0) {
                                continue;
                            }
                            for (int d : yLns) {
                                if (c >= d) {
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
                                if (acbd < 0 || adbc < 0) {
                                    continue;
                                }
                                int intr = lnr.intersection(acbd, adbc);
                                if (intr < 0) {
                                    continue;
                                }
                                if (!lnr.flag(0, intr)) {
                                    System.out.println("Not Boolean");
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Boolean");
        return true;
    }

    private static boolean checkThales(Liner lnr) {
        int[] inf = lnr.line(0);
        for (int hor : inf) {
            int[] hl = lnr.lines(hor);
            for (int ver : inf) {
                if (hor == ver) {
                    continue;
                }
                for (int uDiag : inf) {
                    if (uDiag == hor || uDiag == ver) {
                        continue;
                    }
                    for (int lDiag : inf) {
                        if (lDiag == uDiag || lDiag == ver || lDiag == hor) {
                            continue;
                        }
                        for (int top : hl) {
                            if (top == 0) {
                                continue;
                            }
                            int[] tLine = lnr.line(top);
                            for (int bot : hl) {
                                if (bot == 0 || bot == top) {
                                    continue;
                                }
                                for (int fst : tLine) {
                                    if (lnr.flag(0, fst)) {
                                        continue;
                                    }
                                    for (int snd : tLine) {
                                        if (fst == snd || lnr.flag(0, snd)) {
                                            continue;
                                        }
                                        int vll = lnr.line(ver, fst);
                                        int vlr = lnr.line(ver, snd);
                                        if (vll < 0 || vlr < 0) {
                                            continue;
                                        }
                                        int bl = lnr.intersection(vll, bot);
                                        int br = lnr.intersection(vlr, bot);
                                        if (bl < 0 || br < 0) {
                                            continue;
                                        }
                                        int ldu = lnr.line(bl, uDiag);
                                        int rdu = lnr.line(br, uDiag);
                                        int ldb = lnr.line(fst, lDiag);
                                        int rdb = lnr.line(snd, lDiag);
                                        if (ldu < 0 || rdu < 0 || ldb < 0 || rdb < 0) {
                                            continue;
                                        }
                                        int fm = lnr.intersection(ldu, ldb);
                                        int sm = lnr.intersection(ldb, rdb);
                                        if (fm < 0 || sm < 0) {
                                            continue;
                                        }
                                        int mid = lnr.line(fm, sm);
                                        if (mid < 0) {
                                            continue;
                                        }
                                        int infMid = lnr.intersection(0, mid);
                                        if (infMid < 0) {
                                            continue;
                                        }
                                        if (infMid != hor) {
                                            System.out.println("Not Thales");
                                            return false;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Thales");
        return true;
    }
}
