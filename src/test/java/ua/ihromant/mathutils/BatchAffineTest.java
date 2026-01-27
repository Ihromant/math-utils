package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.auto.Automorphisms;
import ua.ihromant.mathutils.auto.TernaryAutomorphisms;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.plane.AffinePlane;
import ua.ihromant.mathutils.plane.AffineTernaryRing;
import ua.ihromant.mathutils.plane.ProjectiveTernaryRing;
import ua.ihromant.mathutils.plane.Quad;
import ua.ihromant.mathutils.plane.TernaryRing;
import ua.ihromant.mathutils.util.FixBS;
import ua.ihromant.mathutils.vf2.IntPair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class BatchAffineTest {
    @Test
    public void testDilations() throws IOException {
        String name = "bbs4";
        int k = 16;
        Liner proj = readProj(k, name);
        for (int dl : dropped.getOrDefault(name, IntStream.range(0, k * k + k + 1).toArray())) {
            int[] infty = proj.line(dl);
            int[] partialPoints = new int[proj.pointCount()];
            Arrays.fill(partialPoints, -1);
            Arrays.stream(infty).forEach(i -> partialPoints[i] = i);
            int[] partialLines = new int[proj.lineCount()];
            Arrays.fill(partialLines, -1);
            partialLines[dl] = dl;
            int[][] dilations = Automorphisms.autArrayOld(proj, partialPoints, partialLines);
            PermutationGroup dilGr = new PermutationGroup(dilations);
            AffinePlane aff = new AffinePlane(proj, dl);
            PermutationGroup translations = new PermutationGroup(Arrays.stream(dilations).filter(dil -> PermutationGroup.identity(dil)
                    || IntStream.range(0, dil.length).filter(j -> !proj.flag(dl, j)).allMatch(j -> dil[j] != j)).toArray(int[][]::new));
            System.out.println(name + " dropped " + dl + " dilations size " + dilGr.order() + " comm dil " + dilGr.isCommutative()
                    + " translations size " + translations.order() + " comm trans " + translations.isCommutative()
                    + " orders " + IntStream.range(0, dilGr.order()).boxed().collect(Collectors.groupingBy(dilGr::order, Collectors.counting())));
            for (int i : aff.points()) {
                int[][] withFixed = Arrays.stream(dilations).filter(dil -> dil[i] == i).toArray(int[][]::new);
                PermutationGroup gr = new PermutationGroup(withFixed);
                if (gr.order() == 1) {
                    continue;
                }
                System.out.println("For point " + i + " group size " + gr.order() + " commutative " + gr.isCommutative() + " orders " + IntStream.range(0, gr.order()).boxed().collect(Collectors.groupingBy(gr::order, Collectors.counting())));
            }
        }
    }

    @Test
    public void testClassification() throws IOException {
        String name = "dhall9";
        int k = 9;
        System.out.println(name);
        Liner proj = readProj(k, name);
        int cnt = k * k + k + 1;
        int[] arr = IntStream.range(0, cnt).toArray();
        for (int dl : dropped.getOrDefault(name, arr)) {
            String[] ranks = Stream.<Supplier<String>>of(() -> translationRank(proj, k, dl), () -> shearRank(proj, k, dl), () -> centralRank(proj, k, dl),
                    () -> hyperScaleRank(proj, k, dl)).parallel().map(Supplier::get).toArray(String[]::new);
            System.out.println(dl + " " + ranks[0] + " " + ranks[1] + " " + ranks[2] + " " + ranks[3]);
        }
    }

    @Test
    public void testLenzBarlotti() throws IOException {
        int k = 25;
        for (File f : Objects.requireNonNull(new File("/home/ihromant/workspace/math-utils/src/test/resources/proj" + k).listFiles())) {
            Liner proj = readProj(k, f.getName());
            LenzBarlotti lb = lenzBarlotti(proj, k);
            System.out.println(f.getName().replace(".txt", "") + " Lenz points: " + lb.lenzPts.cardinality() + ", lenz lines: " + lb.lenzLns.cardinality() + ", barlotti points: " + lb.barPts.cardinality() + ", barlotti lines: " + lb.barLns.cardinality());
        }
    }

    private static final String uni = """
            5 7 8 11 15 17 18 21 29 31 32 35 37 40 41 50 51 57 58 63 66 67 69 71 76 86 88 89 93 95 102 124 131 153 160 162 166 167 169 179 184 186 188 189 192 197 198 204 205 214 215 218 220 223 224 226 234 237 238 240 244 247 248 250 267
            4 6 8 11 13 17 18 23 28 30 32 35 39 40 41 50 51 57 58 61 66 67 68 70 78 84 88 89 92 94 100 126 129 155 161 163 166 167 171 177 185 187 188 189 194 197 198 204 205 214 215 216 220 223 225 227 232 237 238 242 244 247 249 251 267
            2 3 10 12 14 18 21 23 29 30 32 50 51 53 54 56 72 80 90 91 93 94 98 100 102 106 107 117 118 122 125 127 129 131 135 142 143 144 147 152 154 159 173 181 184 187 190 191 197 208 211 214 215 221 230 231 233 235 239 240 242 247 248 251 267
            14 21 27 28 29 31 32 35 42 44 45 48 51 55 57 59 67 72 74 76 77 84 86 90 91 93 102 112 117 121 122 126 131 144 149 155 156 159 166 168 169 173 175 177 179 184 190 191 197 198 200 201 207 210 213 214 220 222 235 240 248 249 250 254 267
            """;

    @Test
    public void testMathon() throws IOException {
        int k = 16;
        int pc = k * k + k + 1;
        try (InputStream is = getClass().getResourceAsStream("/math.uni");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            br.readLine();
            br.readLine();
            String l;
            int[] idx = new int[pc];
            int[][] lns = new int[pc][k + 1];
            int rn = 0;
            while (!(l = br.readLine()).isEmpty()) {
                for (int i = 0; i < l.length(); i++) {
                    if (l.charAt(i) == '1') {
                        lns[i][idx[i]++] = rn;
                    }
                }
                rn++;
            }
            Liner mathon = new Liner(pc, lns);
            LenzBarlotti lb = lenzBarlotti(mathon, k);
            System.out.println(lb);
            int dl = lb.lenzLns.nextSetBit(0);
            System.out.println("Lenz line: " + Arrays.toString(mathon.line(dl)));
            List<int[]> auth = TernaryAutomorphisms.automorphismsAffine(mathon, dl);
            PermutationGroup mathonPg = new PermutationGroup(auth.toArray(int[][]::new));
            System.out.println(mathonPg.order());
            br.readLine();
            br.readLine();
            List<UnitalData> unitals = new ArrayList<>();
            while ((l = br.readLine()) != null) {
                int[] pts = Arrays.stream(l.split(" ")).mapToInt(Integer::parseInt).toArray();
                int[] reverse = new int[pc];
                Arrays.fill(reverse, -1);
                for (int i = 0; i < pts.length; i++) {
                    reverse[pts[i]] = i;
                }
                unitals.add(new UnitalData(mathon.subPlane(pts), pts, reverse));
            }
            UnitalData ud = unitals.getFirst();
            PermutationGroup unitalPg = ud.liner.automorphisms();
            System.out.println("Unital aut " + unitalPg.order());
            ex1: for (int i = 0; i < unitalPg.order(); i++) {
                int[] unitalPerm = unitalPg.permutation(i);
                ex: for (int j = 0; j < unitalPg.order(); j++) {
                    int[] mathonPerm = mathonPg.permutation(j);
                    for (int el = 0; el < ud.liner.pointCount(); el++) {
                        if (mathonPerm[ud.mapping[el]] != ud.mapping[unitalPerm[el]]) {
                            continue ex;
                        }
                    }
                    System.out.println(i + " auth to " + j);
                    continue ex1;
                }
            }
        }
    }

    private record UnitalData(Liner liner, int[] mapping, int[] reverse) {}

    private record LenzBarlotti(FixBS lenzPts, FixBS lenzLns, FixBS barPts, FixBS barLns) { }

    private static LenzBarlotti lenzBarlotti(Liner proj, int k) {
        int pc = k * k + k + 1;
        LenzBarlotti result = new LenzBarlotti(new FixBS(pc), new FixBS(pc), new FixBS(pc), new FixBS(pc));
        for (int p = 0; p < pc; p++) {
            for (int l = 0; l < pc; l++) {
                boolean inc = proj.flag(l, p);
                if (inc) {
                    int infL = l;
                    int v = p;
                    int h = Arrays.stream(proj.line(l)).filter(pt -> pt != v).findAny().orElseThrow();
                    int o = IntStream.range(0, pc).filter(pt -> !proj.flag(infL, pt)).findAny().orElseThrow();
                    int oh = proj.line(o, h);
                    int ov = proj.line(o, v);
                    int e = IntStream.range(0, pc).filter(pt -> !proj.flag(oh, pt) && !proj.flag(ov, pt) && !proj.flag(infL, pt)).findAny().orElseThrow();
                    int u = proj.intersection(oh, proj.line(v, e));
                    int w = proj.intersection(ov, proj.line(h, e));
                    TernaryRing rng = new ProjectiveTernaryRing("", proj, new Quad(o, u, w, e)).toMatrix();
                    if (rng.isLinear() && rng.addAssoc()) {
                        result.lenzPts.set(p);
                        result.lenzLns.set(l);
                    }
                } else {
                    int h = p;
                    int v = Arrays.stream(proj.line(l)).findAny().orElseThrow();
                    int infL = proj.line(v, h);
                    int o = Arrays.stream(proj.line(l)).filter(pt -> pt != v).findAny().orElseThrow();
                    int oh = proj.line(o, h);
                    int ov = proj.line(o, v);
                    int e = IntStream.range(0, pc).filter(pt -> !proj.flag(oh, pt) && !proj.flag(ov, pt) && !proj.flag(infL, pt)).findAny().orElseThrow();
                    int u = proj.intersection(oh, proj.line(v, e));
                    int w = proj.intersection(ov, proj.line(h, e));
                    TernaryRing rng = new ProjectiveTernaryRing("", proj, new Quad(o, u, w, e)).toMatrix();
                    if (rng.isLinear() && rng.mulAssoc()) {
                        result.barPts.set(p);
                        result.barLns.set(l);
                    }
                }
            }
        }
        return result;
    }

    private static String translationRank(Liner proj, int k, int dl) {
        int cnt = k * k + k + 1;
        BitSet result = new BitSet(cnt);
        for (int dir : proj.points(dl)) {
            int[] fixPts = new int[cnt];
            int[] fixLn = new int[cnt];
            Arrays.fill(fixPts, -1);
            Arrays.fill(fixLn, -1);
            for (int pt : proj.points(dl)) {
                fixPts[pt] = pt;
            }
            for (int l : proj.lines(dir)) {
                fixLn[l] = l;
            }
            int[][] auths = Automorphisms.autArrayOld(proj, fixPts, fixLn);
            BitSet forRemoval = new BitSet(cnt * cnt);
            for (int x = 0; x < cnt; x++) {
                if (proj.flag(dl, x)) {
                    continue;
                }
                int ln = proj.line(x, dir);
                for (int y : proj.points(ln)) {
                    if (y == dir) {
                        continue;
                    }
                    forRemoval.set(x * cnt + y);
                }
            }
            int idx = 0;
            while (idx < auths.length && !forRemoval.isEmpty()) {
                int[] aut = auths[idx++];
                for (int x = 0; x < cnt; x++) {
                    int y = aut[x];
                    forRemoval.clear(x * cnt + y);
                }
            }
            if (forRemoval.isEmpty()) {
                result.set(dir);
            }
        }
        return switch ((Integer) result.cardinality()) {
            case 0 -> "0";
            case 1 -> "1";
            case Integer i when i == k + 1 -> "2";
            default -> throw new IllegalStateException();
        };
    }

    private static String centralRank(Liner proj, int k, int dl) {
        int cnt = k * k + k + 1;
        BitSet result = new BitSet(cnt);
        for (int o = 0; o < cnt; o++) {
            if (proj.flag(dl, o)) {
                continue;
            }
            int[] fixPts = new int[cnt];
            int[] fixLn = new int[cnt];
            Arrays.fill(fixPts, -1);
            Arrays.fill(fixLn, -1);
            for (int pt : proj.points(dl)) {
                fixPts[pt] = pt;
            }
            for (int ln : proj.lines(o)) {
                fixLn[ln] = ln;
            }
            fixPts[o] = o;
            fixLn[dl] = dl;
            int[][] auths = Automorphisms.autArrayOld(proj, fixPts, fixLn);
            BitSet forRemoval = new BitSet(cnt * cnt);
            for (int x = 0; x < cnt; x++) {
                if (x == o || proj.flag(dl, x)) {
                    continue;
                }
                int ox = proj.line(o, x);
                for (int y : proj.points(ox)) {
                    if (o == y || proj.flag(dl, y)) {
                        continue;
                    }
                    forRemoval.set(x * cnt + y);
                }
            }
            int idx = 0;
            while (idx < auths.length && !forRemoval.isEmpty()) {
                int[] aut = auths[idx++];
                for (int x = 0; x < cnt; x++) {
                    int y = aut[x];
                    forRemoval.clear(x * cnt + y);
                }
            }
            if (forRemoval.isEmpty()) {
                result.set(o);
            }
        }
        return switch ((Integer) result.cardinality()) {
            case 0 -> "0";
            case 1 -> "1";
            case Integer i when i == k -> "2";
            case Integer i when i == k * k -> "3";
            default -> throw new IllegalStateException();
        };
    }

    private static String shearRank(Liner proj, int k, int dl) {
        int cnt = k * k + k + 1;
        BitSet result = new BitSet(cnt);
        for (int fixL = 0; fixL < cnt; fixL++) {
            if (fixL == dl) {
                continue;
            }
            int dir = proj.intersection(dl, fixL);
            int[] fixLPts = proj.points(fixL);
            int[] fixPts = new int[cnt];
            int[] fixLn = new int[cnt];
            Arrays.fill(fixPts, -1);
            Arrays.fill(fixLn, -1);
            for (int pt : fixLPts) {
                fixPts[pt] = pt;
            }
            fixLn[dl] = dl;
            fixLn[fixL] = fixL;
            int[][] auths = Automorphisms.autArrayOld(proj, fixPts, fixLn);
            BitSet forRemoval = new BitSet(cnt * cnt);
            for (int l : proj.lines(dir)) {
                if (l == fixL || l == dl) {
                    continue;
                }
                for (int x : proj.points(l)) {
                    if (x == dir) {
                        continue;
                    }
                    for (int y : proj.points(l)) {
                        if (y == dir) {
                            continue;
                        }
                        forRemoval.set(x * cnt + y);
                    }
                }
            }
            int idx = 0;
            while (idx < auths.length && !forRemoval.isEmpty()) {
                int[] aut = auths[idx++];
                for (int x = 0; x < cnt; x++) {
                    int y = aut[x];
                    forRemoval.clear(x * cnt + y);
                }
            }
            if (forRemoval.isEmpty()) {
                result.set(fixL);
            }
        }
        return switch ((Integer) result.cardinality()) {
            case 0 -> "0";
            case 1 -> "1";
            case Integer i when i == k -> "2par";
            case Integer i when i == k + 1 -> "2beam";
            case Integer i when i == k * k + k -> "3";
            default -> throw new IllegalStateException();
        };
    }

    private record HyperScale(int dir, int l) {}

    public static String hyperScaleRank(Liner proj, int k, int dl) {
        int cnt = k * k + k + 1;
        Set<HyperScale> result = new HashSet<>();
        for (int dir : proj.points(dl)) {
            for (int fixL = 0; fixL < cnt; fixL++) {
                if (fixL == dl || proj.flag(fixL, dir)) {
                    continue;
                }
                int[] fixLPts = proj.points(fixL);
                int[] fixPts = new int[cnt];
                int[] fixLn = new int[cnt];
                Arrays.fill(fixPts, -1);
                Arrays.fill(fixLn, -1);
                for (int pt : fixLPts) {
                    fixPts[pt] = pt;
                }
                fixLn[dl] = dl;
                fixLn[fixL] = fixL;
                fixPts[dir] = dir;
                int[][] auths = Automorphisms.autArrayOld(proj, fixPts, fixLn);
                BitSet forRemoval = new BitSet(cnt * cnt);
                for (int d : proj.lines(dir)) {
                    if (d == dl) {
                        continue;
                    }
                    for (int x : proj.points(d)) {
                        if (proj.flag(dl, x) || proj.flag(fixL, x)) {
                            continue;
                        }
                        for (int y : proj.points(d)) {
                            if (proj.flag(dl, y) || proj.flag(fixL, y)) {
                                continue;
                            }
                            forRemoval.set(x * cnt + y);
                        }
                    }
                }
                int idx = 0;
                while (idx < auths.length && !forRemoval.isEmpty()) {
                    int[] aut = auths[idx++];
                    for (int x = 0; x < cnt; x++) {
                        int y = aut[x];
                        forRemoval.clear(x * cnt + y);
                    }
                }
                if (forRemoval.isEmpty()) {
                    result.add(new HyperScale(dir, fixL));
                }
            }
        }
        BitSet dirs = result.stream().mapToInt(HyperScale::dir).collect(BitSet::new, BitSet::set, BitSet::or);
        BitSet lines = result.stream().mapToInt(HyperScale::l).collect(BitSet::new, BitSet::set, BitSet::or);
        String dirRank = switch ((Integer) dirs.cardinality()) {
            case 0 -> "0";
            case 1 -> "1";
            case Integer i when i == k || i == k + 1 -> "2";
            default -> "Number " + dirs.cardinality();
        };
        String lineRank = switch ((Integer) lines.cardinality()) {
            case 0 -> "0";
            case 1 -> "1";
            case Integer i when i == k -> "2par";
            case Integer i when i == k + 1 -> "2beam";
            case Integer i when i == k * k + k -> "3";
            default -> "Number " + lines.cardinality();
        };
        return dirRank + " " + lineRank;
    }

    @Test
    public void testTriangles() throws IOException {
        String name = "dhall9";
        int k = 9;
        Liner proj = readProj(k, name);
        for (int dl : dropped.getOrDefault(name, IntStream.range(0, k * k + k + 1).toArray())) {
            Liner liner = new AffinePlane(proj, dl).toLiner();
            long time = System.currentTimeMillis();
            PermutationGroup permGroup = liner.automorphisms();
            System.out.println(name + " dropped " + dl + " time " + (System.currentTimeMillis() - time) + " count " + permGroup.order());
            int[] triangles = new int[liner.triangleCount()];
            Arrays.fill(triangles, -1);
            for (int i = 0; i < triangles.length; i++) {
                if (triangles[i] >= 0) {
                    continue;
                }
                Triangle tr = liner.trOf(i);
                for (int j = 0; j < permGroup.order(); j++) {
                    int[] perm = permGroup.permutation(j);
                    Triangle applied = new Triangle(perm[tr.o()], perm[tr.u()], perm[tr.w()]);
                    triangles[liner.trIdx(applied)] = i;
                }
            }
            Map<Integer, Long> counts = Arrays.stream(triangles).boxed().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            System.out.println(counts);
        }
    }

    @Test
    public void calculateCentralMatrix() throws IOException {
        int k = 16;
        for (File f : Objects.requireNonNull(new File("/home/ihromant/workspace/math-utils/src/test/resources/proj" + k).listFiles())) {
            String name = f.getName();
            if ("desarg.txt".equals(f.getName())) {
                continue;
            }
            Liner proj = readProj(k, name);
            int[][] central = new int[proj.pointCount()][proj.lineCount()];
            IntStream.range(0, proj.pointCount()).parallel().forEach(o -> {
                for (int l = 0; l < proj.lineCount(); l++) {
                    central[o][l]++;
                    int a = findA(proj, o, l);
                    int oa = proj.line(o, a);
                    ex:
                    for (int a1 : proj.line(oa)) {
                        if (a == a1 || o == a1 || proj.flag(l, a1)) {
                            continue;
                        }
                        for (int ob : proj.lines(o)) {
                            if (ob == l || ob == oa) {
                                continue;
                            }
                            for (int oc : proj.lines(o)) {
                                if (oc == l || oc == oa || oc == ob) {
                                    continue;
                                }
                                for (int b : proj.points(ob)) {
                                    if (b == o || proj.flag(l, b)) {
                                        continue;
                                    }
                                    for (int c : proj.points(oc)) {
                                        if (c == o || proj.flag(l, c)) {
                                            continue;
                                        }
                                        int b1 = proj.intersection(ob, proj.line(proj.intersection(proj.line(a, b), l), a1));
                                        int c1 = proj.intersection(oc, proj.line(proj.intersection(proj.line(a, c), l), a1));
                                        if (!proj.flag(l, proj.intersection(proj.line(b, c), proj.line(b1, c1)))) {
                                            continue ex;
                                        }
                                    }
                                }
                            }
                        }
                        central[o][l]++;
                    }
                }
            });
            System.out.println(name);
            //calculateParameters(proj, central);
            Arrays.stream(central).forEach(ln -> System.out.println(Arrays.stream(ln).mapToObj(i -> Integer.toString(i, 36)).collect(Collectors.joining())));
        }
    }

    private static void calculateParameters(Liner proj, int[][] central) {
        Map<Integer, List<Pr>> grouped = new HashMap<>(64);
        for (int p = 0; p < proj.pointCount(); p++) {
            for (int l = 0; l < proj.lineCount(); l++) {
                int val = central[p][l];
                grouped.computeIfAbsent(val, ky -> new ArrayList<>()).add(new Pr(p, l));
            }
        }
        System.out.println("Spectrum: " + grouped.keySet());
        System.out.println("LevelSize: " + grouped.entrySet().stream().map(e -> e.getValue().size() + "=" + e.getKey()).collect(Collectors.joining(" ")));
        StringBuilder builder = new StringBuilder("Central multirank: ");
        for (Map.Entry<Integer, List<Pr>> e : grouped.entrySet()) {
            Set<Integer> pts = e.getValue().stream().map(Pr::p).collect(Collectors.toSet());
            Set<Integer> lns = e.getValue().stream().map(Pr::l).collect(Collectors.toSet());
            int ptRank;
            int lnRank;
            if (pts.size() == 1) {
                ptRank = 1;
            } else {
                ptRank = 2;
                Set<Integer> unL = new HashSet<>();
                ex: for (int pt1 : pts) {
                    for (int pt2 : pts) {
                        if (pt1 == pt2) {
                            continue;
                        }
                        unL.add(proj.line(pt1, pt2));
                        if (unL.size() >= 2) {
                            ptRank = 3;
                            break ex;
                        }
                    }
                }
            }
            if (lns.size() == 1) {
                lnRank = 1;
            } else {
                lnRank = 2;
                Set<Integer> unP = new HashSet<>();
                ex: for (int ln1 : lns) {
                    for (int ln2 : lns) {
                        if (ln1 == ln2) {
                            continue;
                        }
                        unP.add(proj.intersection(ln1, ln2));
                        if (unP.size() >= 2) {
                            lnRank = 3;
                            break ex;
                        }
                    }
                }
            }
            builder.append(ptRank).append(lnRank).append("=").append(e.getKey()).append(", ");
        }
        System.out.println(builder);
    }

    private record Pr(int p, int l) {}

    private static int findA(Liner proj, int o, int l) {
        return IntStream.range(0, proj.pointCount()).filter(a -> a != o && !proj.flag(l, a)).findAny().orElseThrow();
    }

    @Test
    public void testBiloopCorrectness() throws IOException {
        String name = "dhall9";
        int k = 9;
        Liner proj = readProj(k, name);
        int dl = 1;
        Liner liner = new AffinePlane(proj, dl).toLiner();
        for (int triangle : uniqueTriangles.get(name + "-" + dl + "-" + k)) {
            TernaryRing tr = new AffineTernaryRing(liner, liner.trOf(triangle));
            for (int x : tr.elements()) {
                for (int b : tr.elements()) {
                    assertEquals(b, tr.op(x, 0, b));
                    assertEquals(b, tr.op(0, x, b));
                }
                assertEquals(x, tr.op(x, 1, 0));
                assertEquals(x, tr.op(1, x, 0));
            }
            for (int a : tr.elements()) {
                for (int x : tr.elements()) {
                    for (int y : tr.elements()) {
                        int b = IntStream.range(0, tr.order()).filter(c -> tr.op(x, a, c) == y).findAny().orElseThrow();
                        for (int c : tr.elements()) {
                            if (b == c) {
                                continue;
                            }
                            if (tr.op(x, a, c) == y) {
                                fail();
                            }
                        }
                    }
                }
            }
            for (int a : tr.elements()) {
                for (int b : tr.elements()) {
                    for (int c : tr.elements()) {
                        if (c == a) {
                            continue;
                        }
                        for (int d : tr.elements()) {
                            int x = IntStream.range(0, tr.order()).filter(y -> tr.op(y, a, b) == tr.op(y, c, d)).findAny().orElseThrow();
                            for (int y : tr.elements()) {
                                if (x == y) {
                                    continue;
                                }
                                if (tr.op(y, a, b) == tr.op(y, c, d)) {
                                    fail();
                                }
                            }
                        }
                    }
                }
            }
            for (int x1 : tr.elements()) {
                for (int y1 : tr.elements()) {
                    for (int x2 : tr.elements()) {
                        if (x1 == x2) {
                            continue;
                        }
                        for (int y2 : tr.elements()) {
                            IntPair ab = null;
                            for (int a : tr.elements()) {
                                for (int b : tr.elements()) {
                                    if (tr.op(x1, a, b) == y1 && tr.op(x2, a, b) == y2) {
                                        ab = new IntPair(a, b);
                                    }
                                }
                            }
                            assertNotNull(ab);
                            for (int a1 : tr.elements()) {
                                for (int b1 : tr.elements()) {
                                    if (a1 == ab.fst() && b1 == ab.snd()) {
                                        continue;
                                    }
                                    if (tr.op(x1, a1, b1) == y1 && tr.op(x2, a1, b1) == y2) {
                                        fail();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testGolden() throws IOException {
        String name = "hughes9";
        int k = 9;
        Liner proj = readProj(k, name);
        int dl = 3;
        System.out.println(name + "-" + dl + "-" + k);
        Liner liner = new AffinePlane(proj, dl).toLiner();
        for (int triangle : uniqueTriangles.get(name + "-" + dl + "-" + k)) {
            TernaryRing tr = new AffineTernaryRing(liner, liner.trOf(triangle));
            TreeMap<Integer, Boolean> elems = new TreeMap<>();
            for (int el : tr.elements()) {
                if (tr.mul(el, el) != tr.add(1, el)) {
                    continue;
                }
                boolean leftDistr = true;
                ex:
                for (int x : tr.elements()) {
                    for (int y : tr.elements()) {
                        if (tr.mul(el, tr.add(x, y)) != tr.add(tr.mul(el, x), tr.mul(el, y))) {
                            leftDistr = false;
                            break ex;
                        }
                    }
                }
                elems.put(el, leftDistr);
            }
            if (!elems.isEmpty()) {
                System.out.println("Triangle " + triangle + " " + elems);
            }
        }
    }

    @Test
    public void testEquality() throws IOException {
        String name = "dhall9";
        int k = 9;
        int[][] hBijections = Combinatorics.permutations(IntStream.range(0, k).toArray()).toArray(int[][]::new);
        Liner proj = readProj(k, name);
        Liner liner = new AffinePlane(proj, 0).toLiner();
        TernaryRing tr0 = new AffineTernaryRing(liner, liner.trOf(0));
        TernaryRing tr1 = new AffineTernaryRing(liner, liner.trOf(1));
        assertFalse(tr0.trEquals(tr1));
        assertFalse(tr0.isotopic(tr1, hBijections));
        TernaryRing tr2 = new AffineTernaryRing(liner, liner.trOf(2));
        assertTrue(tr1.trEquals(tr2));
        assertTrue(tr1.isotopic(tr2, hBijections));
        assertEquals(tr0.characteristic(), tr1.characteristic());
        TernaryRing tr18 = new AffineTernaryRing(liner, liner.trOf(18));
        TernaryRing tr24 = new AffineTernaryRing(liner, liner.trOf(24));
        assertTrue(!tr18.trEquals(tr0) && !tr18.trEquals(tr1));
        assertTrue(!tr24.trEquals(tr0) && !tr24.trEquals(tr1) && !tr24.trEquals(tr18));
        TernaryRing tr17 = new AffineTernaryRing(liner, liner.trOf(17));
        assertTrue(tr0.trEquals(tr17) && !tr1.trEquals(tr17));
        assertTrue(tr0.isotopic(tr17, hBijections) && !tr1.isotopic(tr17, hBijections));
        assertEquals(tr0.characteristic(), tr17.characteristic());
    }

    @Test
    public void testTRAutomorphisms() throws IOException {
        String name = "hughes9-3-9";
        String[] tokens = name.split("-");
        String plName = tokens[0];
        int dl = Integer.parseInt(tokens[1]);
        int k = Integer.parseInt(tokens[2]);
        Liner proj = readProj(k, plName);
        Liner liner = new AffinePlane(proj, dl).toLiner();
        for (int triangle : uniqueTriangles.get(name)) {
            Triangle tr = liner.trOf(triangle);
            int[] fixedPoints = new int[liner.pointCount()];
            Arrays.fill(fixedPoints, -1);
            fixedPoints[tr.o()] = tr.o();
            fixedPoints[tr.u()] = tr.u();
            fixedPoints[tr.w()] = tr.w();
            int[] fixedLines = new int[liner.lineCount()];
            Arrays.fill(fixedLines, -1);
            fixedLines[liner.line(tr.o(), tr.u())] = liner.line(tr.o(), tr.u());
            fixedLines[liner.line(tr.o(), tr.w())] = liner.line(tr.o(), tr.w());
            fixedLines[liner.line(tr.u(), tr.w())] = liner.line(tr.u(), tr.w());
            PermutationGroup perm = new PermutationGroup(Automorphisms.autArrayOld(liner, fixedPoints, fixedLines));
            TernaryRing ring = new AffineTernaryRing(liner, tr);
            System.out.println(triangle + " " + perm.order() + " " + perm.isCommutative() + " " + ring.isLinear());
        }
    }

    @Test
    public void testBiloopEquality() throws IOException {
        String name = "hughes9-3-9";
        String[] tokens = name.split("-");
        String plName = tokens[0];
        int dl = Integer.parseInt(tokens[1]);
        int k = Integer.parseInt(tokens[2]);
        Liner proj = readProj(k, plName);
        Liner liner = new AffinePlane(proj, dl).toLiner();
        Map<Integer, List<TernaryRing>> byIso = new HashMap<>();
        ex: for (int triangle : uniqueTriangles.get(name)) {
            Triangle tr = liner.trOf(triangle);
            TernaryRing ring = new AffineTernaryRing(liner, tr);
            for (Map.Entry<Integer, List<TernaryRing>> e : byIso.entrySet()) {
                if (e.getValue().getFirst().biLoopEquals(ring, true, true)) {
                    e.getValue().add(ring);
                    continue ex;
                }
            }
            List<TernaryRing> nl = new ArrayList<>();
            nl.add(ring);
            byIso.put(triangle, nl);
        }
        System.out.println(byIso);
    }

    @Test
    public void testBooleanProperties() throws IOException {
        String name = "hall9-0-9";
        String[] tokens = name.split("-");
        String plName = tokens[0];
        int dl = Integer.parseInt(tokens[1]);
        int k = Integer.parseInt(tokens[2]);
        Liner proj = readProj(k, plName);
        Liner liner = new AffinePlane(proj, dl).toLiner();
        System.out.println(name + " dropped line " + dl);
        for (int triangle : uniqueTriangles.get(name)) {
            TernaryRing ring = new AffineTernaryRing(liner, liner.trOf(triangle));
            System.out.println("triangle:" + String.format("%8d", triangle)
                    + ", lftd:" + (ring.isLeftDistributive() ? 1 : 0)
                    + ", rgtd:" + (ring.isRightDistributive() ? 1 : 0)
                    + ", addass:" + (ring.addAssoc() ? 1 : 0)
                    + ", mulass:" + (ring.mulAssoc() ? 1 : 0)
                    + ", addcom:" + (ring.addComm() ? 1 : 0)
                    + ", mulcom:" + (ring.mulComm() ? 1 : 0)
                    + ", addTSI:" + (ring.addTwoSidedInverse() ? 1 : 0)
                    + ", mulTSI:" + (ring.mulTwoSidedInverse() ? 1 : 0)
                    + ", lAddInv:" + (ring.addLeftInverse() ? 1 : 0)
                    + ", rAddInv:" + (ring.addRightInverse() ? 1 : 0)
                    + ", lMulInv:" + (ring.mulLeftInverse() ? 1 : 0)
                    + ", rMulInv:" + (ring.mulRightInverse() ? 1 : 0)
                    + ", linear:" + (ring.isLinear() ? 1 : 0)
                    + ", 1comm:" + (ring.oneComm() ? 1 : 0)
                    + ", addPAss:" + (ring.addPowerAssoc() ? 1 : 0)
                    + ", mulPAss:" + (ring.mulPowerAssoc() ? 1 : 0)
            );
        }
    }

    @Test
    public void testBooleanNotFano() throws IOException {
        String name = "dhall-80-16";
        String[] tokens = name.split("-");
        String plName = tokens[0];
        int dl = Integer.parseInt(tokens[1]);
        int k = Integer.parseInt(tokens[2]);
        Liner proj = readProj(k, plName);
        Liner liner = new AffinePlane(proj, dl).toLiner();
        boolean notFanoFound = false;
        boolean linearFound = false;
        for (int a = 0; a < liner.pointCount(); a++) {
            for (int b = a + 1; b < liner.pointCount(); b++) {
                int ab = liner.line(a, b);
                for (int c = b + 1; c < liner.pointCount(); c++) {
                    if (liner.collinear(a, b, c)) {
                        continue;
                    }
                    AffineTernaryRing ring = new AffineTernaryRing(liner, new Triangle(a, b, c));
                    if (!linearFound && ring.isLinear()) {
                        linearFound = true;
                        printLinearTables("dhall-80-16", ring);
                    }
                    int ac = liner.line(a, c);
                    int bc = liner.line(b, c);
                    for (int d = c + 1; d < liner.pointCount(); d++) {
                        if (liner.collinear(a, b, d) || liner.collinear(a, c, d) || liner.collinear(b, c, d)) {
                            continue;
                        }
                        int ad = liner.line(a, d);
                        int bd = liner.line(b, d);
                        int cd = liner.line(c, d);
                        int cnt = 0;
                        int abcd = liner.intersection(ab, cd);
                        if (abcd >= 0) {
                            cnt++;
                        }
                        int acbd = liner.intersection(ac, bd);
                        if (acbd >= 0) {
                            cnt++;
                        }
                        int adbc = liner.intersection(ad, bc);
                        if (adbc >= 0) {
                            cnt++;
                        }
                        if (cnt == 1) {
                            System.out.println("Not Boolean");
                        }
                        if (!notFanoFound && cnt == 3 && !liner.collinear(abcd, acbd, adbc)) {
                            notFanoFound = true;
                            System.out.println("Not Fano");
                        }
                    }
                }
            }
        }
    }

    private static void printBooleanProperties(String name, AffineTernaryRing ring) {
        System.out.println("triangle:" + String.format("%8d", ring.trIdx())
                + ", lftd:" + (ring.isLeftDistributive() ? 1 : 0)
                + ", rgtd:" + (ring.isRightDistributive() ? 1 : 0)
                + ", addass:" + (ring.addAssoc() ? 1 : 0)
                + ", mulass:" + (ring.mulAssoc() ? 1 : 0)
                + ", addcom:" + (ring.addComm() ? 1 : 0)
                + ", mulcom:" + (ring.mulComm() ? 1 : 0)
                + ", addTSI:" + (ring.addTwoSidedInverse() ? 1 : 0)
                + ", mulTSI:" + (ring.mulTwoSidedInverse() ? 1 : 0)
                + ", lAddInv:" + (ring.addLeftInverse() ? 1 : 0)
                + ", rAddInv:" + (ring.addRightInverse() ? 1 : 0)
                + ", lMulInv:" + (ring.mulLeftInverse() ? 1 : 0)
                + ", rMulInv:" + (ring.mulRightInverse() ? 1 : 0)
                + ", linear:" + (ring.isLinear() ? 1 : 0)
                + ", 1comm:" + (ring.oneComm() ? 1 : 0)
        );
    }

    private static void printLinearTables(String name, AffineTernaryRing ring) {
        if (!ring.isLinear()) {
            return;
        }
        System.out.println(name + " triangle " + ring.trIdx());
        int[][] additionMatrix = ring.addMatrix();
        int[][] mulMatrix = ring.mulMatrix();
        System.out.println("Addition");
        Arrays.stream(additionMatrix).forEach(arr -> System.out.println(Arrays.toString(arr)));
        System.out.println("Multiplication");
        Arrays.stream(mulMatrix).forEach(arr -> System.out.println(Arrays.toString(arr)));
        System.out.println();
    }

    @Test
    public void batchBooleanProperties() throws IOException {
        EqualityProcessor processor = new EqualityProcessor(false, true);
        //BiConsumer<String, AffineTernaryRing> processor = BatchAffineTest::printLinearTables;
        for (String plName : dropped.keySet()) {
            int k = 9;
            Liner proj = readProj(k, plName);
            for (int dl : dropped.get(plName)) {
                Liner liner = new AffinePlane(proj, dl).toLiner();
                String name = plName + "-" + dl + "-" + k;
                System.out.println(name + " dropped line " + dl);
                for (int triangle : uniqueTriangles.get(name)) {
                    AffineTernaryRing ring = new AffineTernaryRing(liner, liner.trOf(triangle));
                    processor.accept(name, ring);
                }
                System.out.println();
            }
        }
        processor.finish();
    }

    private static class EqualityProcessor implements BiConsumer<String, AffineTernaryRing> {
        private final Map<String, SequencedMap<String, AffineTernaryRing>> grouped = new LinkedHashMap<>();
        private final boolean incAdd;
        private final boolean incMul;

        public EqualityProcessor(boolean incAdd, boolean incMul) {
            this.incAdd = incAdd;
            this.incMul = incMul;
        }

        @Override
        public void accept(String name, AffineTernaryRing ring) {
            for (Map.Entry<String, SequencedMap<String, AffineTernaryRing>> e : grouped.entrySet()) {
                if (e.getValue().firstEntry().getValue().biLoopEquals(ring, incAdd, incMul)) {
                    e.getValue().put(name + "-" + ring.trIdx(), ring);
                    return;
                }
            }
            SequencedMap<String, AffineTernaryRing> map = new LinkedHashMap<>();
            map.put(name + "-" + ring.trIdx(), ring);
            grouped.put(name + "-" + ring.trIdx(), map);
        }

        public void finish() {
            for (Map.Entry<String, SequencedMap<String, AffineTernaryRing>> e : grouped.entrySet()) {
                if (incAdd && incMul && e.getValue().size() == 1) {
                    continue;
                }
                System.out.println("Unique biloop by " + (incAdd ? "addition" + (incMul ? " and " : "") : "")
                        + (incMul ? "multiplication" : "") + ": " + e.getKey());
                AffineTernaryRing ring = e.getValue().firstEntry().getValue();
                if (incAdd) {
                    System.out.println("Addition");
                    Arrays.stream(ring.addMatrix()).forEach(arr -> System.out.println(Arrays.toString(arr)));
                }
                if (incMul) {
                    System.out.println("Multiplication");
                    Arrays.stream(ring.mulMatrix()).forEach(arr -> System.out.println(Arrays.toString(arr)));
                }
                System.out.println("Equal triangles " + e.getValue().size() + ": " + String.join(" ", e.getValue().keySet()));
            }
        }
    }

    @Test
    public void isotopicTest() throws IOException {
        int k = 9;
        IsotopyProcessor processor = new IsotopyProcessor(k);
        for (String plName : dropped.keySet()) {
            Liner proj = readProj(k, plName);
            for (int dl : dropped.get(plName)) {
                Liner liner = new AffinePlane(proj, dl).toLiner();
                String name = plName + "-" + dl + "-" + k;
                System.out.println(name + " dropped line " + dl);
                for (int triangle : uniqueTriangles.get(name)) {
                    AffineTernaryRing ring = new AffineTernaryRing(liner, liner.trOf(triangle));
                    processor.accept(name, ring);
                }
                System.out.println();
            }
        }
        processor.finish();
    }

    @Test
    public void charTest() throws IOException {
        int k = 16;
        int pc = k * k + k + 1;
        //IsotopyProcessor processor = new IsotopyProcessor(k);
        //for (String plName : dropped.keySet()) {
        Set<TernaryRing.TerChar> set = new HashSet<>();
        String plName = "dbbh2";
        Liner proj = readProj(k, plName);
        for (int dl : new int[]{0, 1, 21, 41, 233}) {
            //Liner liner = new AffinePlane(proj, dl).toLiner();
            String name = plName + "-" + dl + "-" + k;
            System.out.println(name + " dropped line " + dl);
            for (int o = 0; o < pc; o++) {
                if (proj.flag(dl, o)) {
                    continue;
                }
                for (int u = 0; u < pc; u++) {
                    if (u == o || proj.flag(dl, u)) {
                        continue;
                    }
                    int ou = proj.line(o, u);
                    for (int w = 0; w < pc; w++) {
                        if (w == o || w == u || proj.flag(dl, w) || proj.flag(ou, w)) {
                            continue;
                        }
                        int e = proj.intersection(proj.line(w, proj.intersection(ou, dl)),
                                proj.line(u, proj.intersection(proj.line(o, w), dl)));
                        Quad quad = new Quad(o, u, w, e);
                        ProjectiveTernaryRing ptr = new ProjectiveTernaryRing("", proj, quad);
                        if (ptr.isLinear()) {
                            TernaryRing.TerChar tcr = ptr.terChar();
                            if (set.add(tcr)) {
                                System.out.println(quad + " " + ptr.terChar());
                            }
                        }
                    }
                }
            }
        }
        //}
        //processor.finish();
    }

    @Test
    public void testFanoChar() throws IOException {
        int k = 16;
        int pc = k * k + k + 1;
        for (File f : Objects.requireNonNull(new File("/home/ihromant/workspace/math-utils/src/test/resources/proj" + k).listFiles())) {
            String name = f.getName();
            if (name.equals("desarg.txt")) {
                continue;
            }
            Liner proj = readProj(k, name);
            int tl = TernaryAutomorphisms.findTranslationLine(proj);
            System.out.println(name);
            Map<Integer, Integer> quantities = new ConcurrentHashMap<>();
            IntStream.range(0, pc).parallel().forEach(dl -> {
                if (dl == tl) {
                    return;
                }
                int cnt = 0;
                for (int o = 0; o < pc; o++) {
                    if (proj.flag(dl, o)) {
                        continue;
                    }
                    for (int u = 0; u < pc; u++) {
                        if (u == o || proj.flag(dl, u)) {
                            continue;
                        }
                        int ou = proj.line(o, u);
                        for (int w = 0; w < pc; w++) {
                            if (w == o || w == u || proj.flag(dl, w) || proj.flag(ou, w)) {
                                continue;
                            }
                            int ow = proj.line(o, w);
                            int e = proj.intersection(proj.line(w, proj.intersection(ou, dl)),
                                    proj.line(u, proj.intersection(ow, dl)));
                            int ea = proj.intersection(proj.line(e, proj.intersection(ou, dl)), proj.line(e, proj.intersection(ow, dl)));
                            int lao = proj.line(o, ea);
                            int ob = proj.intersection(ow, proj.line(e, proj.intersection(ou, dl)));
                            int lab = proj.line(ob, proj.intersection(lao, dl));
                            int xy = proj.intersection(lab, proj.line(e, proj.intersection(ow, dl)));
                            int y = proj.intersection(proj.line(o, e), proj.line(xy, proj.intersection(ou, dl)));
                            if (y == o) {
                                cnt++;
                            }
                        }
                    }
                }
                quantities.compute(cnt, (_, v) -> v == null ? 1 : v + 1);
            });
            System.out.println(quantities);
        }
    }

    private static class IsotopyProcessor implements BiConsumer<String, AffineTernaryRing> {
        private final Map<Map<Map<Integer, Integer>, Integer>, Map<String, SequencedMap<String, AffineTernaryRing>>> grouped = new HashMap<>();
        private final int[][] hBijections;

        private IsotopyProcessor(int ord) {
            this.hBijections = Combinatorics.permutations(IntStream.range(0, ord).toArray()).toArray(int[][]::new);
        }

        @Override
        public void accept(String name, AffineTernaryRing ring) {
            Map<Map<Integer, Integer>, Integer> ch = ring.characteristic();
            Map<String, SequencedMap<String, AffineTernaryRing>> gr = grouped.computeIfAbsent(ch, k -> new HashMap<>());
            Optional<SequencedMap<String, AffineTernaryRing>> vals = new ArrayList<>(gr.values()).stream().parallel()
                    .filter(val -> val.firstEntry().getValue().isotopic(ring, hBijections))
                    .findAny();
            if (vals.isPresent()) {
                vals.get().put(name + "-" + ring.trIdx(), ring);
            } else {
                SequencedMap<String, AffineTernaryRing> map = new LinkedHashMap<>();
                map.put(name + "-" + ring.trIdx(), ring);
                gr.put(name + "-" + ring.trIdx(), map);
            }
        }

        public void finish() {
            System.out.println("Isotopic size: " + grouped.values().stream().mapToInt(Map::size).sum());
            for (Map<String, SequencedMap<String, AffineTernaryRing>> gr : grouped.values()) {
                for (Map.Entry<String, SequencedMap<String, AffineTernaryRing>> e : gr.entrySet()) {
                    if (e.getValue().size() == 1) {
                        continue;
                    }
                    System.out.println("Isotopic triangles " + e.getValue().size() + ": " + String.join(" ", e.getValue().keySet()));
                }
            }
        }
    }

    public static Liner readProj(int k, String name) throws IOException {
        name = name.indexOf('.') < 0 ? name + ".txt" : name;
        String s = Files.readString(Path.of(Objects.requireNonNull(
                BatchAffineTest.class.getResource("/proj" + k + "/" + name)).getPath()));
        List<int[]> lines = new ArrayList<>();
        String[] lns = s.lines().toArray(String[]::new);
        IntList lst = new IntList(k + 1);
        for (String ln : lns) {
            int[] ints = Arrays.stream(ln.trim().split(" ")).mapToInt(Integer::parseInt).toArray();
            for (int pt : ints) {
                lst.add(pt);
            }
            if (lst.size() == k + 1) {
                lines.add(lst.toArray());
                lst = new IntList(k + 1);
            }
        }
        return new Liner(lines.toArray(int[][]::new));
    }

    @Test
    public void centralAuths() throws IOException {
        int k = 9;
        String name = "hughes9";
        int dl = 3;
        Liner proj = readProj(k, name);
        List<int[]> auths = new ArrayList<>();
        auths.add(IntStream.range(0, proj.pointCount()).toArray());
        for (int o = 0; o < proj.pointCount(); o++) {
            int[] lines;
            // translation - point on infinite, line infinite
            // shear - point infinite and on line which is not infinite
            // homotety - point finite, line infinite
            // hyperscale - point infinite, line finite, point does not belong to line
            if (proj.flag(dl, o)) { // point in infinity
                //lines = new int[]{dl}; // translation
                //lines = IntStream.of(proj.lines(o)).filter(l -> l != dl).toArray(); // shear
                lines = proj.lines(o); // translation + shear
                //lines = IntStream.range(0, proj.pointCount()).toArray(); // translation + shear + hyperscale
            } else {
                lines = new int[]{}; // nothing
                //lines = new int[]{dl}; // homotety
            }
            for (int l : lines) {
                int a = findA(proj, o, l);
                int oa = proj.line(o, a);
                ex: for (int a1 : proj.line(oa)) {
                    int[] map = new int[proj.pointCount()];
                    Arrays.fill(map, -1);
                    map[o] = o;
                    map[a] = a1;
                    for (int pt : proj.points(l)) {
                        map[pt] = pt;
                    }
                    if (a == a1 || o == a1 || proj.flag(l, a1)) {
                        continue;
                    }
                    for (int ob : proj.lines(o)) {
                        if (ob == l || ob == oa) {
                            continue;
                        }
                        for (int oc : proj.lines(o)) {
                            if (oc == l || oc == oa || oc == ob) {
                                continue;
                            }
                            for (int b : proj.points(ob)) {
                                if (b == o || proj.flag(l, b)) {
                                    continue;
                                }
                                for (int c : proj.points(oc)) {
                                    if (c == o || proj.flag(l, c)) {
                                        continue;
                                    }
                                    int b1 = proj.intersection(ob, proj.line(proj.intersection(proj.line(a, b), l), a1));
                                    int c1 = proj.intersection(oc, proj.line(proj.intersection(proj.line(a, c), l), a1));
                                    if (!proj.flag(l, proj.intersection(proj.line(b, c), proj.line(b1, c1)))) {
                                        continue ex;
                                    }
                                    map[b] = b1;
                                    map[c] = c1;
                                    map[proj.intersection(oa, proj.line(b, c))] = proj.intersection(oa, proj.line(b1, c1));
                                }
                            }
                        }
                    }
                    auths.add(map);
                }
            }
        }
        Set<int[]> all = new TreeSet<>(Combinatorics::compareArr);
        all.addAll(auths);
        while (true) {
            Set<int[]> next = new TreeSet<>(Combinatorics::compareArr);
            for (int[] fst : all) {
                for (int[] snd : all) {
                    int[] comb = new int[proj.pointCount()];
                    for (int i = 0; i < proj.pointCount(); i++) {
                        comb[i] = fst[snd[i]];
                    }
                    next.add(comb);
                }
            }
            if (next.size() == all.size()) {
                break;
            } else {
                all = next;
            }
        }
        PermutationGroup gr = new PermutationGroup(all.toArray(int[][]::new));
        System.out.println(gr.order());
        int pc = proj.pointCount();
        int ts = proj.pointCount() * proj.pointCount() * proj.pointCount();
        QuickFind qf = new QuickFind(ts);
        for (int el = 0; el < gr.order(); el++) {
            int[] perm = gr.permutation(el);
            for (int i = 0; i < ts; i++) {
                int[] abc = to(i, pc);
                int[] mapped = new int[]{perm[abc[0]], perm[abc[1]], perm[abc[2]]};
                qf.union(i, from(mapped, pc));
            }
        }
        Set<FixBS> comps = new HashSet<>(qf.components());
        comps.removeIf(l -> {
            int st = l.nextSetBit(0);
            int[] abc = to(st, pc);
            return abc[0] == abc[1] || abc[1] == abc[2] || abc[0] == abc[2]
                    || proj.flag(dl, abc[0]) || proj.flag(dl, abc[1]) || proj.flag(dl, abc[2])
                    || proj.collinear(abc[0], abc[1], abc[2]);
        });
        System.out.println(comps.size());
        for (int i = 0; i < ts; i++) {
            int[] abc = to(i, pc);
            int[] acb = new int[]{abc[0], abc[2], abc[1]};
            int[] bac = new int[]{abc[1], abc[0], abc[2]};
            int[] bca = new int[]{abc[1], abc[2], abc[0]};
            int[] cab = new int[]{abc[2], abc[0], abc[1]};
            int[] cba = new int[]{abc[2], abc[1], abc[0]};
            qf.union(i, from(acb, pc));
            qf.union(i, from(bac, pc));
            qf.union(i, from(bca, pc));
            qf.union(i, from(cab, pc));
            qf.union(i, from(cba, pc));
        }
        Set<FixBS> comps1 = new HashSet<>(qf.components());
        comps1.removeIf(l -> {
            int st = l.nextSetBit(0);
            int[] abc = to(st, pc);
            return abc[0] == abc[1] || abc[1] == abc[2] || abc[0] == abc[2]
                    || proj.flag(dl, abc[0]) || proj.flag(dl, abc[1]) || proj.flag(dl, abc[2])
                    || proj.collinear(abc[0], abc[1], abc[2]);
        });
        System.out.println(comps1.size());
        Map<FixBS, List<FixBS>> multiplicities = new HashMap<>();
        for (FixBS comp : comps) {
            for (FixBS comp1 : comps1) {
                if (comp.intersects(comp1)) {
                    multiplicities.computeIfAbsent(comp1, uu -> new ArrayList<>()).add(comp);
                    break;
                }
            }
        }
        System.out.println(Arrays.toString(multiplicities.values().stream().mapToInt(List::size).sorted().toArray()));
        //System.out.println(GroupIndex.identify(gr));
    }

    @Test
    public void testBoolean() throws IOException {
        int k = 32;
        for (File f : Objects.requireNonNull(new File("/home/ihromant/workspace/math-utils/src/test/resources/proj" + k).listFiles())) {
            String name = f.getName();
            Liner proj = readProj(k, name);
            System.out.println(name);
            for (int dl = 0; dl < proj.lineCount(); dl++) {
                if (TernaryAutomorphisms.isAffineTranslation(proj, dl)) {
                    continue;
                }
                testBoolean(proj, dl);
            }
        }
    }

    private static void testBoolean(Liner proj, int dl) {
        int pc = proj.pointCount();
        for (int a = 0; a < pc; a++) {
            if (proj.flag(dl, a)) {
                continue;
            }
            for (int b = a + 1; b < pc; b++) {
                if (proj.flag(dl, b)) {
                    continue;
                }
                int ab = proj.line(a, b);
                for (int c = b + 1; c < pc; c++) {
                    if (proj.flag(dl, c) || proj.flag(ab, c)) {
                        continue;
                    }
                    int ac = proj.line(a, c);
                    int bc = proj.line(b, c);
                    int abInf = proj.intersection(ab, dl);
                    int acInf = proj.intersection(ac, dl);
                    int d = proj.intersection(proj.line(c, abInf), proj.line(b, acInf));
                    int ad = proj.line(a, d);
                    int adbc = proj.intersection(ad, bc);
                    if (!proj.flag(dl, adbc)) {
                        return;
                    }
                }
            }
        }
        System.out.println("Boolean " + dl);
    }

    private static final int fCap = 15 * 14 * 12 * 8;

    @Test
    public void testGrundhofer() throws IOException {
        int k = 32;
        for (File f : Objects.requireNonNull(new File("/home/ihromant/workspace/math-utils/src/test/resources/proj" + k).listFiles())) {
            String name = f.getName();
            Liner proj = readProj(k, name);
            System.out.println(name);
            int pc = proj.pointCount();
            IntStream.range(0, pc).parallel().forEach(dl -> {
                if (TernaryAutomorphisms.isAffineTranslation(proj, dl)) {
                    return;
                }
                for (int l = 0; l < pc; l++) {
                    if (l == dl) {
                        continue;
                    }
                    int lDl = proj.intersection(l, dl);
                    int[] pts = proj.line(l);
                    int[] idxes = new int[pc];
                    int idx = 0;
                    for (int pt : pts) {
                        if (pt == lDl) {
                            continue;
                        }
                        idxes[pt] = idx++;
                    }
                    if (initialCheck(dl, pts, lDl, proj, l, k, idxes) && advancedCheck(dl, pts, lDl, proj, l, k, idxes)) {
                        System.out.println("Found");
                    }
                }
            });
        }
    }

    private static final int sCap = 16 * 15 * 14 * 12 * 8;

    private boolean initialCheck(int dl, int[] pts, int lDl, Liner proj, int l, int k, int[] idxes) {
        int o = pts[0] == lDl ? pts[1] : pts[0];
        for (int u : proj.line(dl)) {
            if (lDl == u) {
                continue;
            }
            for (int v : proj.line(dl)) {
                if (lDl == v || u == v) {
                    continue;
                }
                Set<ArrWrap> gens = new HashSet<>();
                for (int l1 : proj.lines(o)) {
                    if (l1 == l || proj.flag(l1, u) || proj.flag(l1, v)) {
                        continue;
                    }
                    int[] perm = new int[k];
                    for (int pt : pts) {
                        if (pt == lDl) {
                            continue;
                        }
                        int l1Pt = proj.intersection(l1, proj.line(u, pt));
                        int mapPt = proj.intersection(l, proj.line(v, l1Pt));
                        perm[idxes[pt]] = idxes[mapPt];
                    }
                    gens.add(new ArrWrap(perm));
                }
                Set<ArrWrap> awp = getClosureAlt(gens,  sCap + 1);
                if (awp != null && awp.size() <= sCap) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean advancedCheck(int dl, int[] pts, int lDl, Liner proj, int l, int k, int[] idxes) {
        for (int u : proj.line(dl)) {
            if (lDl == u) {
                continue;
            }
            for (int v : proj.line(dl)) {
                if (lDl == v || u == v) {
                    continue;
                }
                IntList lst = new IntList(2000);
                for (int l1 = 0; l1 < proj.pointCount(); l1++) {
                    if (l1 == l || proj.flag(l1, u) || proj.flag(l1, v)) {
                        continue;
                    }
                    lst.add(l1);
                }
                Set<ArrWrap> gens = new HashSet<>();
                for (int i = 0; i < lst.size(); i++) {
                    int l1 = lst.get(i);
                    int[] perm = new int[k];
                    for (int pt : pts) {
                        if (pt == lDl) {
                            continue;
                        }
                        int l1Pt = proj.intersection(l1, proj.line(u, pt));
                        int mapPt = proj.intersection(l, proj.line(v, l1Pt));
                        perm[idxes[pt]] = idxes[mapPt];
                    }
                    gens.add(new ArrWrap(perm));
                }
                Set<ArrWrap> awp = getClosureAlt(gens,  fCap + 1);
                if (awp != null && awp.size() <= fCap) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void testPickert() throws IOException {
        int k = 16;
        for (File f : Objects.requireNonNull(new File("/home/ihromant/workspace/math-utils/src/test/resources/proj" + k).listFiles())) {
            String name = f.getName();
            Liner proj = readProj(k, name);
            System.out.println(name);
            int pc = proj.pointCount();
            for (int dl = 0; dl < pc; dl++) {
                if (TernaryAutomorphisms.isAffineTranslation(proj, dl)) {
                    continue;
                }
                int infL = dl;
                for (int o = 0; o < pc; o++) {
                    if (proj.flag(dl, o)) {
                        continue;
                    }
                    for (int h : proj.line(dl)) {
                        int oh = proj.line(o, h);
                        for (int v : proj.line(dl)) {
                            if (v == h) {
                                continue;
                            }
                            int ov = proj.line(o, v);
                            int e = IntStream.range(0, pc).filter(pt -> !proj.flag(oh, pt) && !proj.flag(ov, pt) && !proj.flag(infL, pt)).findAny().orElseThrow();
                            int u = proj.intersection(oh, proj.line(v, e));
                            int w = proj.intersection(ov, proj.line(h, e));
                            Quad q = new Quad(o, u, w, e);
                            TernaryRing rng = new ProjectiveTernaryRing("", proj, q).toMatrix();
                            Set<ArrWrap> basic = new HashSet<>();
                            for (int i = 1; i < rng.order(); i++) {
                                for (int j = 0; j < rng.order(); j++) {
                                    int[] arr = new int[k];
                                    for (int x = 0; x < rng.order(); x++) {
                                        arr[x] = rng.op(x, i, j);
                                    }
                                    basic.add(new ArrWrap(arr));
                                }
                            }
                            Set<ArrWrap> closure = getClosure(basic, 362880);
                            System.out.println(dl + " " + q + " " + closure.size());
                        }
                    }
                }
            }
        }
    }

    private Set<ArrWrap> getClosureAlt(Set<ArrWrap> gens, int cap) {
        Set<ArrWrap> result = new HashSet<>();
        ArrWrap fst = gens.iterator().next();
        result.add(new ArrWrap(IntStream.range(0, fst.map.length).toArray()));
        while (!gens.isEmpty()) {
            ArrWrap gen = gens.iterator().next();
            boolean added;
            do {
                added = false;
                for (ArrWrap el : result.toArray(ArrWrap[]::new)) {
                    ArrWrap xy = new ArrWrap(combine(gen.map, el.map));
                    ArrWrap yx = new ArrWrap(combine(el.map, gen.map));
                    if (xy.map == null || yx.map == null) {
                        return null;
                    }
                    added = result.add(xy) || added;
                    added = result.add(yx) || added;
                }
                if (result.size() >= cap) {
                    return result;
                }
            } while (added);
            gens.remove(gen);
        }
        return result;
    }

    private Set<ArrWrap> getClosure(Set<ArrWrap> basePerms, int cap) {
        Set<ArrWrap> closure = new HashSet<>(basePerms);
        Set<ArrWrap> additional = new HashSet<>(closure);
        while (!(additional = additional(closure, additional, cap)).isEmpty()) {}
        return closure;
    }

    @Test
    public void testPickertFor9() throws IOException {
        int k = 9;
        for (File f : Objects.requireNonNull(new File("/home/ihromant/workspace/math-utils/src/test/resources/proj" + k).listFiles())) {
            String name = f.getName();
            Liner proj = readProj(k, name);
            name = name.substring(0, name.indexOf('.'));
            System.out.println(name);
            for (int dl : dropped.get(name)) {
                String aName = name + "-" + dl + "-" + k;
                System.out.println(name + " dropped line " + dl);
                Liner liner = new AffinePlane(proj, dl).toLiner();
                Arrays.stream(uniqueTriangles.get(aName)).parallel().forEach(triangle -> {
                    AffineTernaryRing rng = new AffineTernaryRing(liner, liner.trOf(triangle));
                    Set<ArrWrap> basic = new HashSet<>();
                    for (int i = 1; i < rng.order(); i++) {
                        for (int j = 0; j < rng.order(); j++) {
                            int[] arr = new int[k];
                            for (int x = 0; x < rng.order(); x++) {
                                arr[x] = rng.op(x, i, j);
                            }
                            basic.add(new ArrWrap(arr));
                        }
                    }
                    boolean even = basic.stream().allMatch(a -> even(a.map()));
                    Set<ArrWrap> closure = getClosure(basic, even ? 181440 : 362880);
                    System.out.println(aName + " " + triangle + " " + closure.size());
                });
            }
        }
    }

    private Set<ArrWrap> additional(Set<ArrWrap> currGroup, Set<ArrWrap> addition, int cap) {
        Set<ArrWrap> result = new HashSet<>();
        for (ArrWrap x : currGroup) {
            for (ArrWrap y : addition) {
                ArrWrap xy = new ArrWrap(combine(x.map, y.map));
                ArrWrap yx = new ArrWrap(combine(y.map, x.map));
                if (!currGroup.contains(xy)) {
                    result.add(xy);
                }
                if (!currGroup.contains(yx)) {
                    result.add(yx);
                }
                if (currGroup.size() + result.size() >= cap) {
                    currGroup.addAll(result);
                    return Set.of();
                }
            }
        }
        currGroup.addAll(result);
        return result;
    }

    private static int[] combine(int[] a, int[] b) {
        int[] result = new int[a.length];
        int fixed = 0;
        for (int i = 0; i < a.length; i++) {
            int c = a[b[i]];
            result[i] = c;
            if (c == i) {
                fixed++;
            }
        }
        if ((fixed & (fixed - 1)) != 0) {
            return null;
        }
        return result;
    }

    private static boolean even(int[] perm) {
        int cnt = 0;
        for (int i = 0; i < perm.length; i++) {
            for (int j = i + 1; j < perm.length; j++) {
                if (perm[i] > perm[j]) {
                    cnt++;
                }
            }
        }
        return cnt % 2 == 0;
    }

    private record ArrWrap(int[] map) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ArrWrap(int[] map1))) return false;
            return Arrays.equals(map, map1);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(map) >>> 1;
        }
    }

    @Test
    public void parallelTriangles() throws IOException {
        int k = 16;
        String name = "dhall";
        int dl = 0;
        Liner proj = readProj(k, name);
        int pc = proj.pointCount();
        int ts = pc * pc * pc;
        QuickFind qf = new QuickFind(ts);
        for (int a = 0; a < pc; a++) {
            for (int b = 0; b < pc; b++) {
                qf.union(from(new int[]{a, a, b}, pc), from(new int[]{a, a, a}, pc));
                qf.union(from(new int[]{a, a, b}, pc), from(new int[]{a, b, b}, pc));
                qf.union(from(new int[]{a, a, b}, pc), from(new int[]{a, b, a}, pc));
                qf.union(from(new int[]{a, a, b}, pc), from(new int[]{b, a, a}, pc));
                for (int dp : proj.line(dl)) {
                    qf.union(from(new int[]{a, a, a}, pc), from(new int[]{a, b, dp}, pc));
                    qf.union(from(new int[]{a, a, a}, pc), from(new int[]{a, dp, b}, pc));
                    qf.union(from(new int[]{a, a, a}, pc), from(new int[]{dp, a, b}, pc));
                }
                if (a == b) {
                    continue;
                }
                int ab = proj.line(a, b);
                for (int c : proj.line(ab)) {
                    qf.union(from(new int[]{a, a, a}, pc), from(new int[]{a, b, c}, pc));
                }
            }
        }
        for (int a = 0; a < pc; a++) {
            if (proj.flag(dl, a)) {
                continue;
            }
            for (int b = 0; b < pc; b++) {
                if (proj.flag(dl, b) || a == b) {
                    continue;
                }
                int ab = proj.line(a, b);
                int inf = proj.intersection(dl, ab);
                for (int c = 0; c < pc; c++) {
                    if (proj.flag(dl, c)) {
                        continue;
                    }
                    //qf.union(from(new int[]{a, b, c}, pc), from(new int[]{b, c, a}, pc));
                    //qf.union(from(new int[]{a, b, c}, pc), from(new int[]{c, a, b}, pc));
                    int cd = proj.line(c, inf);
                    for (int d : proj.line(cd)) {
                        if (proj.flag(dl, d)) {
                            continue;
                        }
                        qf.union(from(new int[]{a, b, c}, pc), from(new int[]{a, b, d}, pc));
                        qf.union(from(new int[]{a, c, b}, pc), from(new int[]{a, d, b}, pc));
                        qf.union(from(new int[]{b, a, c}, pc), from(new int[]{b, a, d}, pc));
                        qf.union(from(new int[]{b, c, a}, pc), from(new int[]{b, d, a}, pc));
                        qf.union(from(new int[]{c, a, b}, pc), from(new int[]{d, a, b}, pc));
                        qf.union(from(new int[]{c, b, a}, pc), from(new int[]{d, b, a}, pc));
                    }
                }
            }
        }
        Set<FixBS> comps = new HashSet<>(qf.components());
        comps.removeIf(l -> {
            int st = l.nextSetBit(0);
            int[] abc = to(st, pc);
            return abc[0] == abc[1] || abc[0] == abc[2] || abc[1] == abc[2]
                    || proj.flag(dl, abc[0]) || proj.flag(dl, abc[1]) || proj.flag(dl, abc[2])
                    || proj.collinear(abc[0], abc[1], abc[2]);
        });
        System.out.println(comps.size());
        for (int i = 0; i < ts; i++) {
            int[] abc = to(i, pc);
            int[] acb = new int[]{abc[0], abc[2], abc[1]};
            int[] bac = new int[]{abc[1], abc[0], abc[2]};
            int[] bca = new int[]{abc[1], abc[2], abc[0]};
            int[] cab = new int[]{abc[2], abc[0], abc[1]};
            int[] cba = new int[]{abc[2], abc[1], abc[0]};
            qf.union(i, from(acb, pc));
            qf.union(i, from(bac, pc));
            qf.union(i, from(bca, pc));
            qf.union(i, from(cab, pc));
            qf.union(i, from(cba, pc));
        }
        Set<FixBS> comps1 = new HashSet<>(qf.components());
        comps1.removeIf(l -> {
            int st = l.nextSetBit(0);
            int[] abc = to(st, pc);
            return abc[0] == abc[1] || abc[0] == abc[2] || abc[1] == abc[2]
                    || proj.flag(dl, abc[0]) || proj.flag(dl, abc[1]) || proj.flag(dl, abc[2])
                    || proj.collinear(abc[0], abc[1], abc[2]);
        });
        System.out.println(comps1.size());
        Map<FixBS, List<FixBS>> multiplicities = new HashMap<>();
        for (FixBS comp : comps) {
            for (FixBS comp1 : comps1) {
                if (comp.intersects(comp1)) {
                    multiplicities.computeIfAbsent(comp1, uu -> new ArrayList<>()).add(comp);
                    break;
                }
            }
        }
        System.out.println(Arrays.toString(multiplicities.values().stream().mapToInt(List::size).sorted().toArray()));
        //System.out.println(GroupIndex.identify(gr));
    }

    private int from(int[] abc, int pc) {
        return abc[0] * pc * pc + abc[1] * pc + abc[2];
    }

    private int[] to(int p, int pc) {
        return new int[]{p / pc / pc, p / pc % pc, p % pc};
    }

    private static final Map<String, int[]> dropped = Map.of(
            "pg29", new int[]{0},
            "dhall9", new int[]{0, 1},
            "hall9", new int[]{0, 81},
            "hughes9", new int[]{0, 3}
//            "bbh1", new int[]{0, 192, 193, 269},
//            "bbh2", new int[]{0, 28},
//            "dbbh2", new int[]{0, 1, 21},
//            "bbs4", new int[]{0, 108, 270},
//            "dbbs4", new int[]{0, 228, 241}
            //"", new int[]{}
    );

    private static final Map<String, int[]> uniqueTriangles = Map.of("pg29-0-9", new int[]{0},
            "dhall9-0-9", new int[]{0, 1, 18, 24},
            "dhall9-1-9", Arrays.stream(new int[]{0, 1, 53250, 51972, 52742, 52743, 53256, 52488, 52745, 52489, 9, 52746, 51978,
                    52747, 52748, 52750, 52751, 52752, 52753, 52754, 52755, 51990, 53274, 51996, 53280, 53286, 53802,
                    53292, 7226, 7227, 59708, 53823, 7233, 7234, 7235, 53316, 53829, 53317, 53318, 53320, 53321, 53322,
                    7242, 53835, 53323, 53324, 6732, 53326, 53327, 53328, 7248, 59729, 53841, 53330, 82, 53332, 84, 53334,
                    7254, 53335, 53336, 52569, 53338, 90, 53339, 53340, 7260, 59229, 53853, 59742, 53343, 53344, 53345,
                    53346, 52578, 7266, 53347, 52579, 52580, 53350, 53351, 53352, 7272, 53354, 52586, 52587, 53356, 53357,
                    7278, 53359, 53362, 53363, 53364, 53365, 53368, 53369, 53370, 53371, 53374, 53375, 53377, 648, 51849,
                    649, 51850, 51851, 53137, 53138, 53139, 53145, 53147, 59811, 59817, 52650, 6570, 52651, 52659, 52660,
                    59829, 52661, 59835, 59841, 59073, 59847, 53705, 53706, 59853, 53714, 53720, 53721, 7129, 51930, 7131,
                    51932, 53218, 51938, 738, 53219, 51939, 740, 53221, 53222, 53223, 53224, 53225, 59626, 53226, 51946,
                    746, 51947, 747, 53229, 53230, 53231, 53232, 53233, 51953, 753, 53234, 51954, 53235, 51960, 53241, 53243, 51966}).sorted().toArray(),
            "hall9-0-9", Arrays.stream(new int[]{1536, 0, 1, 6407, 7, 8, 9, 10, 11, 12, 13, 14, 15, 5904, 16, 17, 5906, 18, 19, 6420,
                    20, 21, 22, 23, 5912, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 38, 39, 40, 41, 42, 43, 44,
                    45, 46, 47, 48, 49, 50, 51, 52, 53, 1334, 54, 460080, 567, 55, 568, 56, 569, 57, 570, 58, 1595, 571,
                    59, 572, 60, 829, 573, 61, 1342, 574, 62, 460088, 575, 63, 64, 65, 460101, 66, 67, 68, 69, 1350, 70,
                    6471, 71, 72, 73, 74, 75, 76, 77, 1358, 78, 79, 80, 1364, 1621, 1370, 1628, 1377, 1634, 866, 1388,
                    460152, 3969, 648, 3977, 656, 659, 666, 669, 672, 675, 4006, 1702, 1703, 1704, 1706, 685, 688, 1458,
                    1720, 698, 1468, 1215, 1228, 1741, 720, 1492, 726, 728, 729, 1506, 1513, 1775, 1521, 1781, 1528, 6399}).sorted().toArray(),
            "hall9-81-9", Arrays.stream(new int[]{256, 0, 257, 258, 2, 3, 73, 9, 266, 10, 81, 23, 6553, 235, 171, 1900, 172, 1782, 185, 316, 252, 253, 254, 1791, 255}).sorted().toArray(),
            "hughes9-0-9", Arrays.stream(new int[]{38912, 0, 38913, 1, 38914, 2, 38915, 20483, 3, 4, 20485, 5, 6, 38919, 7, 8, 38921,
                    2057, 9, 10, 38923, 11, 12, 13, 14, 15, 2064, 16, 38929, 2065, 17, 38930, 2066, 18, 2067, 19, 2068, 20,
                    2069, 38934, 22, 38935, 23, 24, 25, 26, 27, 38941, 29, 38942, 30, 31, 38944, 32, 33, 34, 35, 36, 37, 39,
                    40, 41, 42, 43, 44, 45, 46, 48, 49, 50, 51, 52, 54, 55, 56, 57, 2106, 58, 38971, 59, 38972, 60, 61, 38974,
                    62, 63, 38976, 64, 38977, 65, 66, 67, 68, 69, 70, 72, 38985, 73, 38987, 75, 76, 38990, 78, 38991, 79, 80,
                    38993, 81, 38994, 82, 83, 38996, 84, 85, 86, 87, 88, 89, 39002, 90, 91, 39004, 92, 93, 39006, 94, 95,
                    39008, 96, 97, 98, 99, 100, 39013, 101, 102, 39015, 103, 104, 105, 39018, 106, 107, 108, 109, 110, 111,
                    112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 39034, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131,
                    132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152,
                    153, 154, 155, 156, 157, 158, 159, 160, 161, 165, 166, 167, 168, 169, 170, 2222, 20655, 20656, 20657,
                    20658, 20665, 20666, 20669, 2247, 2255, 243, 244, 245, 246, 252, 253, 254, 255, 256, 257, 258, 259, 260,
                    261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271, 272, 273, 20754, 274, 275, 276, 277, 278, 279,
                    280, 281, 20762, 282, 283, 284, 285, 286, 287, 20768, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297,
                    298, 299, 300, 39213, 301, 2350, 302, 39215, 20783, 2351, 303, 39216, 2352, 304, 39217, 2353, 305, 39218,
                    2354, 306, 2355, 307, 39220, 2356, 308, 39221, 2357, 309, 39222, 310, 311, 312, 39225, 313, 314, 39227,
                    315, 316, 317, 39230, 318, 39231, 319, 39232, 320, 20801, 321, 39234, 322, 39235, 2371, 323, 39236, 324,
                    39237, 325, 39238, 20806, 326, 327, 328, 329, 39242, 330, 39243, 331, 332, 39245, 333, 334, 39247, 335,
                    6480, 336, 39249, 6481, 337, 6482, 338, 6483, 339, 20820, 6484, 340, 39253, 6485, 341, 39254, 6486, 342,
                    6487, 6488, 344, 39257, 345, 39258, 346, 347, 349, 350, 351, 352, 39265, 20833, 354, 39267, 355, 39268,
                    356, 357, 39270, 358, 359, 362, 363, 365, 367, 369, 373, 374, 377, 378, 379, 385, 386, 387, 388, 390,
                    405, 406, 407, 408, 409, 410, 411, 412, 413, 6561, 6562, 6564, 6565, 6567, 6568, 6569, 6574, 41391,
                    41392, 41395, 41400, 41404, 20925, 41406, 41407, 41410, 20930, 41414, 6606, 2511, 6618, 20956, 20963,
                    486, 487, 2536, 488, 489, 6634, 490, 491, 20972, 492, 493, 494, 495, 496, 20977, 497, 6642, 498, 6643,
                    499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 510, 511, 512, 513, 514, 515, 516, 517, 518, 519,
                    520, 521, 522, 523, 524, 525, 526, 527, 528, 529, 530, 531, 532, 533, 534, 535, 536, 537, 538, 539, 540,
                    541, 542, 543, 2592, 544, 545, 546, 547, 548, 549, 550, 551, 552, 553, 554, 555, 556, 557, 558, 559, 560,
                    561, 2610, 562, 563, 564, 565, 566, 567, 568, 569, 570, 571, 572, 573, 574, 2623, 575, 576, 577, 578,
                    6723, 579, 6724, 580, 6725, 581, 6726, 582, 6727, 2631, 583, 584, 6729, 585, 6730, 2635, 587, 2636, 588,
                    589, 590, 592, 593, 594, 595, 597, 598, 599, 600, 601, 602, 605, 606, 608, 610, 612, 616, 617, 620, 621,
                    622, 2673, 628, 629, 630, 631, 633, 648, 6809, 6810, 6811, 6812, 12960, 12961, 12962, 12963, 12964, 6820,
                    12965, 12966, 12967, 12968, 6825, 4779, 6833, 6837, 39609, 39610, 39612, 6845, 39615, 2754, 6853, 4806,
                    6855, 4814, 6864, 729, 2781, 738, 739, 740, 6885, 21222, 6886, 742, 21224, 6888, 6889, 13041, 13042, 13043,
                    13044, 13045, 13046, 13047, 13048, 13049, 39690, 39691, 39694, 39695, 39700, 39701, 39702, 39703, 39704,
                    39705, 39706, 39712, 39713, 39715, 39718, 8998, 9002, 810, 819, 820, 821, 6966, 6967, 823, 6969, 6970,
                    6972, 6973, 39744, 4928, 13122, 39747, 13123, 13124, 13125, 13126, 13127, 13128, 13129, 13130, 4941, 4942,
                    4943, 2895, 4944, 4945, 4946, 21331, 4947, 4948, 21333, 4949, 21335, 2903, 4963, 4964, 2916, 39783, 39784,
                    21352, 39785, 4978, 21368, 2939, 891, 21375, 902, 903, 904, 905, 7058, 13203, 13204, 13205, 13206, 13207,
                    13208, 13209, 13210, 13211, 7067, 7079, 39852, 39853, 39854, 39855, 39856, 39857, 39858, 39862, 39863,
                    7101, 972, 981, 982, 983, 13284, 13285, 13286, 5094, 13287, 13288, 13289, 13290, 13291, 13292, 19440,
                    19441, 19442, 19443, 3059, 19444, 19445, 19446, 19447, 19448, 19449, 19450, 19451, 19452, 39933, 19453,
                    3069, 19454, 19455, 19456, 39937, 39938, 39942, 19462, 39943, 19463, 39944, 39945, 19465, 39946, 19466,
                    39947, 39949, 19470, 19472, 19474, 9236, 19479, 39960, 19481, 19483, 39965, 19489, 19490, 19494, 39976,
                    19502, 19504, 13365, 13366, 13367, 13368, 13369, 13370, 13371, 13372, 13373, 19521, 19522, 19523, 19524,
                    19525, 19526, 19527, 19528, 19529, 19531, 19534, 19535, 19537, 19538, 19539, 19541, 19542, 19543, 19544,
                    19545, 19547, 19548, 40029, 19549, 19551, 19553, 19554, 19555, 19556, 19557, 40038, 19558, 19559, 19560,
                    19562, 19563, 19565, 19566, 19568, 19571, 19572, 19573, 19575, 19578, 19579, 19582, 19583, 40067, 19588,
                    19589, 13446, 19591, 13447, 19592, 13448, 19593, 13449, 19594, 13450, 13451, 19596, 13452, 13453, 19598,
                    13454, 19599, 19601, 19602, 19603, 19604, 13527, 13528, 13529, 13530, 13531, 13532, 13533, 13534, 13535,
                    19683, 19684, 19685, 19686, 19687, 19688, 19689, 19690, 19691, 19692, 19693, 19695, 19698, 19699, 19700,
                    19701, 19702, 19704, 19705, 19709, 19710, 19711, 19713, 19714, 19715, 19716, 19719, 19720, 19721, 19722,
                    19723, 19724, 19726, 19727, 19728, 19729, 19731, 19734, 19737, 19738, 19740, 19741, 19742, 19747, 19748,
                    19749, 19750, 19752, 19753, 19754, 19755, 19756, 19757, 19758, 19761, 19762, 19763, 5427, 19769, 19770,
                    19771, 19772, 5444, 3402, 5454, 5462, 1377, 3427, 5482, 3434, 5487, 3441, 3442, 3443, 3444, 19845, 19846,
                    19847, 19848, 19849, 19850, 40344, 3483, 40350, 3508, 3515, 3522, 3523, 3524, 3525, 3526, 3527, 5578,
                    5583, 1489, 1491, 19926, 19927, 19928, 19929, 19930, 19931, 19932, 19933, 19934, 1506, 1519, 1531, 1534,
                    1538, 40451, 1539, 22033, 22035, 40471, 22042, 20007, 22056, 20008, 20009, 20010, 40491, 20011, 22060,
                    20012, 20013, 20014, 20015, 1649, 5751, 1656, 1665, 5774, 1683, 1686, 1687, 5788, 1698, 1702, 1703, 1704,
                    1705, 1706, 1707, 1708, 1709, 1710, 1711, 1712, 1713, 1714, 1715, 1716, 5813, 1717, 1718, 1719, 1720,
                    1721, 20169, 20170, 20172, 20178, 1782, 1783, 1784, 1785, 1786, 1787, 1788, 1789, 1790, 1791, 1792, 1793,
                    1794, 1796, 1801, 1803, 1804, 1805, 1806, 1809, 1811, 1812, 1813, 1815, 20250, 1818, 20259, 1827, 20260,
                    20261, 1830, 1831, 20264, 20266, 1836, 1839, 26416, 1841, 1842, 1844, 20278, 20280, 1850, 20283, 26429,
                    26431, 1855, 1857, 20290, 20291, 26442, 20302, 20307, 20308, 20318, 26465, 20321, 1889, 26467, 1891, 1893,
                    20328, 20331, 26476, 1900, 20334, 26480, 20340, 20342, 1910, 1912, 20345, 20347, 1915, 1917, 26498, 1922,
                    1925, 1928, 26507, 1935, 26518, 26519, 1944, 26541, 20429, 20438, 20448, 20455, 38889, 38890, 38891, 38892,
                    38893, 38894, 38895, 20463, 38896, 38897, 38898, 38902, 38903, 38905, 38906, 38907, 38910, 38911}).sorted().toArray(),
            "hughes9-3-9", Arrays.stream(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 3081, 9, 10, 11, 12, 13, 3086, 14, 15, 16, 17, 3090,
                    18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 1566, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 1577,
                    41, 1578, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 567, 55, 568, 56, 57, 570, 58, 571, 59,
                    572, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 84, 85, 86, 89,
                    46686, 648, 649, 650, 652, 653, 654, 655, 658, 659, 661, 664, 666, 667, 668, 673, 675, 678, 171, 172,
                    173, 687, 175, 178, 180, 181, 182, 697, 185, 187, 189, 1215, 191, 192, 193, 195, 201, 211, 730, 731,
                    733, 737, 738, 739, 740, 741, 742, 743, 745, 746, 747, 750, 753, 754, 755, 243, 756, 244, 246, 759,
                    760, 761, 765, 766, 46335, 767, 46336, 768, 772, 774, 777, 779, 780, 786, 790, 801, 1325, 45360, 45361,
                    45362, 45364, 1332, 45368, 45369, 45370, 45373, 1341, 829, 45378, 45379, 45380, 45383, 329, 45387, 332,
                    45390, 45391, 45393, 1361, 45399, 45409, 45927, 46440, 45928, 45931, 2432, 2433, 45444, 45445, 45446, 2439,
                    45449, 1460, 1464, 1466, 1472, 1475, 46576}).sorted().toArray());
}
