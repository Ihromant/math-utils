package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.nauty.Partition;
import ua.ihromant.mathutils.util.FixBS;
import ua.ihromant.mathutils.vf2.IntPair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class BatchAffineTest {
    @Test
    public void testDilations() throws IOException {
        String name = "bbs4";
        int k = 16;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
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
    }

    @Test
    public void testAutomorphisms() throws IOException {
        String name = "hall9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
            for (int dl : dropped.getOrDefault(name, IntStream.range(0, k * k + k + 1).toArray())) {
                long time = System.currentTimeMillis();
                System.out.println(name + " dropped " + dl + " count " + new AffinePlane(proj, dl).toLiner().autCountNew() + " time " + (System.currentTimeMillis() - time));
            }
        }
    }

    @Test
    public void testVectors() throws IOException {
        String name = "math";
        int k = 16;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
            for (int dl : dropped.getOrDefault(name, IntStream.range(0, k * k + k + 1).toArray())) {
                AffinePlane aff = new AffinePlane(proj, dl);
                List<Set<Pair>> vectors = aff.vectors();
                System.out.println(name + " dropped " + dl + " vectors " + Arrays.toString(vectors.stream().mapToInt(Set::size).sorted().toArray()));
            }
        }
    }

    @Test
    public void testFuncVectors() throws IOException {
        String name = "dbbs4";
        int k = 16;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
            for (int dl : dropped.getOrDefault(name, IntStream.range(0, k * k + k + 1).toArray())) {
                AffinePlane aff = new AffinePlane(proj, dl);
                int base = aff.points().iterator().next();
                BitSet vectorSub = new BitSet();
                vectorSub.set(base);
                br: for (int end : aff.points()) {
                    if (base == end) {
                        continue;
                    }
                    int l = aff.line(base, end);
                    for (int l1 : aff.parallel(l)) {
                        if (l1 == l) {
                            continue;
                        }
                        for (int l2 : aff.parallel(l)) {
                            if (l2 == l1 || l2 == l) {
                                continue;
                            }
                            for (int b1 : aff.points(l1)) {
                                for (int b2 : aff.points(l2)) {
                                    int e1 = aff.parallelogram(base, end, b1);
                                    int e2 = aff.parallelogram(b1, e1, b2);
                                    int altEnd = aff.parallelogram(b2, e2, base);
                                    if (end != altEnd) {
                                        continue br;
                                    }
                                }
                            }
                        }
                    }
                    vectorSub.set(end);
                }
                Liner subliner = proj.subPlane(vectorSub.stream().toArray());
                if (vectorSub.cardinality() == 1) {
                    continue;
                }
                System.out.println("Dropped " + dl + " size: " + vectorSub.cardinality() + " points " + vectorSub);
            }
        }
    }

    @Test
    public void testNonIsomorphic() throws IOException {
        String name = "hughes9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
            Map<FixBS, Integer> nonIsomorphic = new HashMap<>();
            for (int dl : IntStream.range(0, k * k + k + 1).toArray()) {
                Partition partition = new Partition(proj.pointCount() + proj.lineCount(), new int[][]{
                        IntStream.range(0, proj.pointCount()).toArray(),
                        {dl + proj.pointCount()},
                        IntStream.range(0, proj.lineCount()).filter(l -> l != dl).map(i -> i + proj.pointCount()).toArray()
                });
                Integer v = nonIsomorphic.putIfAbsent(proj.getCanonical(partition), dl);
                System.out.println(dl + " " + (v == null ? "Unique" : "Non Unique"));
            }
            System.out.println("Non isomorphic " + nonIsomorphic.values());
        }
    }

    @Test
    public void testClassification() throws IOException {
        String name = "dhall9";
        int k = 9;
        System.out.println(name);
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
            int cnt = k * k + k + 1;
            int[] arr = IntStream.range(0, cnt).toArray();
            for (int dl : dropped.getOrDefault(name, arr)) {
                String shear = shearRank(proj, k, dl);
                String central = centralRank(proj, k, dl);
                String translation = translationRank(proj, k, dl);
                String hyperScale = hyperScaleRank(proj, k, dl);
                System.out.println(dl + " " + translation + " " + shear + " " + central + " " + hyperScale);
            }
        }
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
            default -> throw new IllegalStateException();
        };
        String lineRank = switch ((Integer) lines.cardinality()) {
            case 0 -> "0";
            case 1 -> "1";
            case Integer i when i == k -> "2par";
            case Integer i when i == k + 1 -> "2beam";
            case Integer i when i == k * k + k -> "3";
            default -> throw new IllegalStateException();
        };
        return dirRank + " " + lineRank;
    }

    @Test
    public void testTriangles() throws IOException {
        String name = "dhall9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
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
    }

    @Test
    public void testBiloopCorrectness() throws IOException {
        String name = "dhall9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
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
    }

    @Test
    public void testEquality() throws IOException {
        String name = "dhall9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
            Liner liner = new AffinePlane(proj, 0).toLiner();
            TernaryRing tr0 = new AffineTernaryRing(liner, liner.trOf(0));
            TernaryRing tr1 = new AffineTernaryRing(liner, liner.trOf(1));
            assertFalse(tr0.trEquals(tr1));
            TernaryRing tr2 = new AffineTernaryRing(liner, liner.trOf(2));
            assertTrue(tr1.trEquals(tr2));
            TernaryRing tr18 = new AffineTernaryRing(liner, liner.trOf(18));
            TernaryRing tr24 = new AffineTernaryRing(liner, liner.trOf(24));
            assertTrue(!tr18.trEquals(tr0) && !tr18.trEquals(tr1));
            assertTrue(!tr24.trEquals(tr0) && !tr24.trEquals(tr1) && !tr24.trEquals(tr18));
            TernaryRing tr17 = new AffineTernaryRing(liner, liner.trOf(17));
            assertTrue(tr0.trEquals(tr17) && !tr1.trEquals(tr17));
        }
    }

    @Test
    public void testTRAutomorphisms() throws IOException {
        String name = "hughes9-3-9";
        String[] tokens = name.split("-");
        String plName = tokens[0];
        int dl = Integer.parseInt(tokens[1]);
        int k = Integer.parseInt(tokens[2]);
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + plName + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
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
    }

    @Test
    public void testBiloopEquality() throws IOException {
        String name = "hughes9-3-9";
        String[] tokens = name.split("-");
        String plName = tokens[0];
        int dl = Integer.parseInt(tokens[1]);
        int k = Integer.parseInt(tokens[2]);
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + plName + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
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
    }

    @Test
    public void testBooleanProperties() throws IOException {
        String name = "hall9-0-9";
        String[] tokens = name.split("-");
        String plName = tokens[0];
        int dl = Integer.parseInt(tokens[1]);
        int k = Integer.parseInt(tokens[2]);
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + plName + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
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
            try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + plName + ".txt");
                 InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
                 BufferedReader br = new BufferedReader(isr)) {
                Liner proj = readTxt(br);
                HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
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
                System.out.println("Equal triangles " + e.getValue().keySet().size() + ": " + String.join(" ", e.getValue().keySet()));
            }
        }
    }

    public Liner readTxt(BufferedReader br) throws IOException {
        List<BitSet> list = new ArrayList<>();
        String line = br.readLine();
        while (line != null) {
            BitSet bs = new BitSet();
            do {
                Arrays.stream(line.trim().split(" ")).mapToInt(Integer::parseInt).forEach(bs::set);
                line = br.readLine();
            } while (line != null && line.charAt(0) == ' ');
            list.add(bs);
        }
        return new Liner(list.toArray(BitSet[]::new));
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet();
        IntStream.of(values).forEach(bs::set);
        return bs;
    }

    private static final Map<String, int[]> dropped = Map.of(
            "pg29", new int[]{0},
            "dhall9", new int[]{0, 1},
            "hall9", new int[]{0, 81},
            "hughes9", new int[]{0, 3},
            "bbh1", new int[]{0, 192, 193, 269},
            "bbh2", new int[]{0, 28},
            "dbbh2", new int[]{0, 1, 21},
            "bbs4", new int[]{0, 108, 270},
            "dbbs4", new int[]{0, 228, 241}
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
