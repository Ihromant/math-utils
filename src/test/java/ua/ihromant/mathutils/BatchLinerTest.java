package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.vf2.IntPair;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class BatchLinerTest {
    private ZipInputStream getZis(InputStream is) throws IOException {
        ZipInputStream zis = new ZipInputStream(Objects.requireNonNull(is));
        zis.getNextEntry();
        return zis;
    }

    private List<Liner> readPlanes(int v, int k) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/inc/S2-" + k + "-" + v + ".inc.zip");
             ZipInputStream zis = getZis(is);
             InputStreamReader isr = new InputStreamReader(zis);
             BufferedReader br = new BufferedReader(isr)) {
            String[] first = br.readLine().trim().split(" ");
            assertEquals(Integer.parseInt(first[0]), v);
            int b = Integer.parseInt(first[1]);
            assertEquals(b, v * (v - 1) / k / (k - 1));
            List<Liner> result = new ArrayList<>();
            while (true) {
                String next = br.readLine();
                if (next == null) {
                    zis.getNextEntry();
                    return result;
                }
                while (next.trim().isEmpty()) {
                    next = br.readLine();
                }
                result.add(readPlane(v, b, next, br));
            }
        }
    }

    private Liner readPlane(int v, int b, String next, BufferedReader br) throws IOException {
        BitSet[] lines = IntStream.range(0, b).mapToObj(i -> new BitSet()).toArray(BitSet[]::new);
        for (int i = 0; i < v; i++) {
            char[] chars = next.trim().toCharArray();
            for (int j = 0; j < b; j++) {
                if (chars[j] == '1') {
                    lines[j].set(i);
                }
            }
            next = br.readLine();
        }
        return new Liner(lines);
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet();
        IntStream.of(values).forEach(bs::set);
        return bs;
    }

    @Test
    public void test217_7() throws IOException {
        List<Liner> planes = readPlanes(217, 7);
        assertEquals(4, planes.size());
        planes.forEach(p -> HyperbolicPlaneTest.testCorrectness(p, of(7)));
        assertEquals(of(1, 2, 3, 4, 5), planes.get(0).hyperbolicIndex());
        assertEquals(of(1, 2, 3, 4, 5), planes.get(1).hyperbolicIndex());
        assertEquals(of(1, 2, 3, 4, 5), planes.get(2).hyperbolicIndex());
        assertEquals(of(2, 3, 4, 5), planes.get(3).hyperbolicIndex());
    }

    @Test
    public void test175_7() throws IOException {
        List<Liner> planes = readPlanes(175, 7);
        assertEquals(2, planes.size());
        planes.forEach(p -> HyperbolicPlaneTest.testCorrectness(p, of(7)));
        assertEquals(of(2, 3, 4, 5), planes.get(0).hyperbolicIndex());
        assertEquals(of(1, 2, 3, 4, 5), planes.get(1).hyperbolicIndex());
    }

    @Test
    public void test66_6() throws IOException {
        List<Liner> planes = readPlanes(66, 6);
        assertEquals(3, planes.size());
        planes.forEach(p -> assertEquals(of(0, 1, 2, 3, 4), p.hyperbolicIndex()));
    }

    @Test
    public void test65_5() throws IOException {
        List<Liner> planes = readPlanes(65, 5);
        assertEquals(1777, planes.size());
        HyperbolicPlaneTest.testCorrectness(planes.getFirst(), of(5));
        assertEquals(of(3), planes.getFirst().hyperbolicIndex());
        assertEquals(of(65), planes.getFirst().cardSubPlanes(true));
    }

    @Test
    public void test41_5() throws IOException {
        List<Liner> planes = readPlanes(41, 5);
        assertEquals(15, planes.size());
    }

    @Test
    public void test45_5() throws IOException {
        List<Liner> planes = readPlanes(45, 5);
        assertEquals(30, planes.size());
    }

    @Test
    public void test37_4() throws IOException {
        List<Liner> planes = readPlanes(37, 4);
        assertEquals(51402, planes.size());
    }

    @Test
    public void test28_4() throws IOException {
        List<Liner> planes = readPlanes(28, 4);
        assertEquals(4466, planes.size());
        Liner plane = planes.get(1001);
        assertEquals(of(2), plane.hyperbolicIndex());
        assertEquals(of(28), plane.cardSubPlanes(true));
        HyperbolicPlaneTest.testCorrectness(plane, of(4));

        String[] design = printDesign(planes.get(3429));
        Arrays.stream(design).forEach(System.out::println);

        for (int i = 2600; i < planes.size(); i++) {
            Liner p1 = planes.get(i);
            Set<BitSet> subaffines = new HashSet<>();
            for (int t0 = 0; t0 < p1.pointCount(); t0++) {
                for (int t1 = t0 + 1; t1 < p1.pointCount(); t1++) {
                    for (int t3 = t1 + 1; t3 < p1.pointCount(); t3++) {
                        if (p1.collinear(t0, t1, t3)) {
                            continue;
                        }
                        for (int t2 : p1.points(p1.line(t0, t1))) {
                            if (t2 == t0 || t2 == t1) {
                                continue;
                            }
                            for (int t6 : p1.points(p1.line(t0, t3))) {
                                if (t6 == t0 || t6 == t3) {
                                    continue;
                                }
                                for (int t4 : p1.points(p1.line(t2, t6))) {
                                    if (t2 == t4 || t6 == t4) {
                                        continue;
                                    }
                                    for (int t8 : p1.points(p1.line(t0, t4))) {
                                        if (t8 == t0 || t8 == t4 || !p1.collinear(t1, t3, t8)) {
                                            continue;
                                        }
                                        for (int t7 : p1.points(p1.line(t6, t8))) {
                                            if (t7 == t6 || t7 == t8 || !p1.collinear(t2, t3, t7) || !p1.collinear(t1, t4, t7)) {
                                                continue;
                                            }
                                            for (int t5 : p1.points(p1.line(t2, t8))) {
                                                if (t5 == t2 || t5 == t8 || !p1.collinear(t3, t4, t5) || !p1.collinear(t0, t5, t7) || !p1.collinear(t1, t5, t6)) {
                                                    continue;
                                                }
                                                BitSet aff = of(t0, t1, t2, t3, t4, t5, t6, t7, t8);
                                                if (subaffines.add(aff)) {
                                                    System.out.println(i + " " + aff);
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
        }
    }

    private String[] printDesign(Liner plane) {
        return IntStream.range(0, plane.line(0).length).mapToObj(i -> IntStream.range(0, plane.lineCount())
                .mapToObj(plane::line).map(bs -> String.valueOf(Character.forDigit(Arrays.stream(bs)
                        .skip(i).findAny().orElseThrow(), 36))).collect(Collectors.joining())).toArray(String[]::new);
    }

    @Test
    public void testFixed() throws IOException {
        String name = "semi4";
        try (InputStream is = getClass().getResourceAsStream("/proj/" + name + ".uni");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            Liner projective = readUni(br);
            for (int dl = 0; dl < projective.lineCount(); dl++) {
                System.out.println(Arrays.toString(projective.line(dl)) + " " + testThalesVectors(projective, dl));
            }
        }
    }

    private static int parallel(Liner pl, int dl, int line, int p) {
        return pl.line(p, pl.intersection(line, dl));
    }

    private static BitSet testThalesVectors(Liner pl, int droppedLine) {
        BitSet result = new BitSet();
        int base = IntStream.range(0, pl.pointCount()).filter(p -> !pl.flag(droppedLine, p)).findFirst().orElseThrow();
        for (int end = base + 1; end < pl.pointCount(); end++) {
            if (!pl.flag(droppedLine, end) && testThales(pl, droppedLine, base, end)) {
                result.set(end);
            }
        }
        return result;
    }

    private static boolean testThales(Liner pl, int droppedLine, int base, int end) {
        int infty = pl.intersection(droppedLine, pl.line(base, end));
        int bl = pl.line(base, infty);
        for (int fst = 0; fst < pl.pointCount(); fst++) {
            if (pl.flag(droppedLine, fst) || pl.flag(bl, fst)) {
                continue;
            }
            int fstLine = pl.line(fst, infty);
            int fstEnd = pl.intersection(parallel(pl, droppedLine, bl, fst), parallel(pl, droppedLine, pl.line(base, fst), end));
            for (int snd = 0; snd < pl.pointCount(); snd++) {
                if (pl.flag(droppedLine, snd) || pl.flag(bl, snd) || pl.flag(fstLine, snd)) {
                    continue;
                }
                int sndEnd = pl.intersection(parallel(pl, droppedLine, bl, snd), parallel(pl, droppedLine, pl.line(base, snd), end));
                int fstEndAlt = pl.intersection(parallel(pl, droppedLine, pl.line(snd, sndEnd), fst), parallel(pl, droppedLine, pl.line(fst, snd), sndEnd));
                if (fstEndAlt != fstEnd) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    public void testProjectiveAndUnitals7() throws IOException {
        String name = "semi4";
        try (InputStream is = getClass().getResourceAsStream("/proj/" + name + ".uni");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            Liner projective = readUni(br);
            String next;
            HyperbolicPlaneTest.testCorrectness(projective, of(17));
            int ub = Integer.parseInt(br.readLine());
            br.readLine();
            while ((next = br.readLine()) != null) {
                String[] numbers = next.split(" ");
                assertEquals(ub, numbers.length);
                BitSet points = Arrays.stream(numbers).mapToInt(Integer::parseInt).collect(BitSet::new, BitSet::set, BitSet::or);
                BitSet[] lines = IntStream.range(0, projective.lineCount()).mapToObj(l -> {
                    BitSet result = new BitSet();
                    Arrays.stream(projective.line(l)).forEach(result::set);
                    result.and(points);
                    return result;
                }).filter(l -> l.cardinality() > 1).toArray(BitSet[]::new);
                int[] pointArray = points.stream().toArray();
                lines = Arrays.stream(lines).map(l -> l.stream()
                        .map(p -> Arrays.binarySearch(pointArray, p)).collect(BitSet::new, BitSet::set, BitSet::or)).toArray(BitSet[]::new);
                Liner p = new Liner(lines);
                HyperbolicPlaneTest.testCorrectness(p, of(5));
                System.out.println(p.hyperbolicIndex());
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

    @Test
    public void testAffinePappus() throws IOException {
        String name = "fig";
        int k = 64;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            Liner projective = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(projective, of(k + 1));
            for (int dl = 0; dl < projective.lineCount(); dl++) {
                AffinePlane aff = new AffinePlane(projective, dl);
                boolean paraPappus = aff.isParaPappus();
                System.out.println("Dropped " + dl + " ParaPappus " + paraPappus);
                if (paraPappus) {
                    boolean paraDesargues = aff.isParaDesargues();
                    System.out.println("ParaDesargues " + paraDesargues);
                }
            }
        }
    }

    @Test
    public void testCubeDesargues() throws IOException {
        String name = "dhall9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            Liner projective = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(projective, of(k + 1));
            for (int dl = 0; dl < projective.lineCount(); dl++) {
                AffinePlane aff = new AffinePlane(projective, dl);
                System.out.println("Dropped " + dl);
                System.out.println("ParaDesargues " + aff.isParaDesargues());
                System.out.println("Cube " + aff.isCubeDesargues());
            }
        }
    }

    @Test
    public void testZigZag() throws IOException {
        String name = "dhall9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
            Set<Set<Pair>> configs = new HashSet<>();
            for (int dl = 0; dl < proj.lineCount(); dl++) {
                AffinePlane aff = new AffinePlane(proj, dl);
                Set<Pair> pairs = new HashSet<>();
                boolean pappus = aff.isParaPappus();
                boolean diagonal = aff.isDiagonal();
                for (int o : aff.points()) {
                    for (int x : aff.points()) {
                        if (x == o) {
                            continue;
                        }
                        for (int y : aff.points()) {
                            if (y == o || y == x || aff.line(y, o) == aff.line(y, x)) {
                                continue;
                            }
                            Pair p = new Pair(aff.zigZagNumber(o, x, y), aff.zigZagNumber(o, y, x));
                            pairs.add(p);
                        }
                    }
                }
                if (configs.add(pairs)) {
                    System.out.println("Dropped " + dl + " diagonal " + diagonal + " pappus " + pappus + " config " + pairs);
                }
            }
        }
    }

    @Test
    public void testCharacteristics() throws IOException {
        String name = "hall9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
            for (int dl = 0; dl < proj.lineCount(); dl++) {
                AffinePlane aff = new AffinePlane(proj, dl);
                System.out.println("Dropped " + dl + " chars " + aff.getCharacteristics());
            }
        }
    }

    @Test
    public void testClosure() throws IOException {
        String name = "hughes9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
            for (int dl = 0; dl < proj.lineCount(); dl++) {
                AffinePlane aff = new AffinePlane(proj, dl);
                Set<Integer> closures = new HashSet<>();
                for (int a : aff.points()) {
                    for (int b : aff.points()) {
                        if (b <= a) {
                            continue;
                        }
                        for (int c : aff.points()) {
                            if (c <= b || aff.line(a, c) == aff.line(b, c)) {
                                continue;
                            }
                            BitSet base = of(a, b, c);
                            BitSet closure = aff.closure(base);
                            if (closures.add(closure.cardinality())) {
                                System.out.println(closure.cardinality());
                            }
                        }
                    }
                }
                System.out.println("Dropped " + dl + " closures " + closures);
            }
        }
    }

    @Test
    public void filterIsomorphic() throws IOException {
        int v = 121;
        int k = 6;
        try (InputStream fis = new FileInputStream(new File("/home/ihromant/maths/diffSets/", "list-" + v + "-" + k + ".txt"));
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(fis));
             BufferedReader br = new BufferedReader(isr);
             FileOutputStream fos = new FileOutputStream(new File("/home/ihromant/maths/diffSets/unique", k + "-" + v + "?.txt"));
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            String line = br.readLine();
            CyclicGroup gr = new CyclicGroup(v);
            int[][] auths = gr.auth();
            Set<Set<BitSet>> unique = new HashSet<>();
            long time = System.currentTimeMillis();
            AtomicLong counter = new AtomicLong();
            ps.println(v + " " + k);
            while ((line = br.readLine()) != null) {
                if (line.length() < 20) {
                    continue;
                }
                String cut = line.replace("}}", "");
                String[] arrays = Arrays.stream(cut.split("\\}, \\{")).map(s -> s.substring(s.indexOf("={") + 2)).toArray(String[]::new);
                int[][] diffSet = Stream.concat(Arrays.stream(arrays).map(s -> Arrays.stream(s.split(", ")).mapToInt(Integer::parseInt)
                        .toArray()), v % k == 0 ? Stream.of(IntStream.range(0, k).map(i -> i * v / k).toArray()) : Stream.empty()).toArray(int[][]::new);
                IntStream.range(0, 1 << (diffSet.length - (v % k == 0 ? 2 : 1))).forEach(comb -> {
                    int[][] diffs = IntStream.range(0, diffSet.length)
                            .mapToObj(i -> ((1 << i) & comb) == 0 ? diffSet[i].clone() : mirrorTuple(gr, diffSet[i]))
                            .map(arr -> minimalTuple(arr, v)).toArray(int[][]::new);
                    if (Arrays.stream(auths).noneMatch(auth -> {
                        Set<BitSet> result = new HashSet<>();
                        for (int[] arr : diffs) {
                            result.add(of(minimalTuple(applyAuth(arr, auth), v)));
                        }
                        return unique.contains(result);
                    })) {
                        unique.add(Arrays.stream(diffs).map(BatchLinerTest::of).collect(Collectors.toSet()));
                        ps.println(Arrays.stream(diffs).map(arr -> of(arr).toString())
                                .collect(Collectors.joining(", ", "{", "}")));
                        counter.incrementAndGet();
                    }
                });
            }
            System.out.println(counter.get() + " " + (System.currentTimeMillis() - time));
        }
    }

    private static int[] mirrorTuple(Group g, int[] tuple) {
        return IntStream.concat(IntStream.of(tuple[0], tuple[1]), IntStream.range(2, tuple.length).map(i -> g.op(tuple[1], g.inv(tuple[i])))).toArray();
    }

    private static int[] minimalTuple(int[] arr, int v) {
        Arrays.sort(arr);
        int l = arr.length;
        int[] diffs = new int[l];
        for (int i = 0; i < l; i++) {
            diffs[i] = diff(arr[i], arr[(l + i + 1) % l], v);
        }
        int minIdx = IntStream.range(0, l).boxed().max(Comparator.comparing(i -> diffs[i])).orElseThrow();
        int val = arr[minIdx];
        int[] res = Arrays.stream(arr).map(i -> i >= val ? i - val : v + i - val).toArray();
        Arrays.sort(res);
        return res;
    }

    private static int diff(int a, int b, int size) {
        int d = Math.abs(a - b);
        return Math.min(d, size - d);
    }

    private static int[] applyAuth(int[] arr, int[] auth) {
        return Arrays.stream(arr).map(i -> auth[i]).toArray();
    }

    private Liner readUni(BufferedReader br) throws IOException {
        String[] first = br.readLine().trim().split(" ");
        int v = Integer.parseInt(first[0]);
        int b = Integer.parseInt(first[1]);
        br.readLine();
        String next = br.readLine();
        return readPlane(v, b, next, br);
    }

    @Test
    public void test25_4() throws IOException {
        String[][] designs = new String[18][4];
        try (InputStream is = getClass().getResourceAsStream("/S(2,4,25).txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            int counter = 0;
            while ((line = br.readLine()) != null) {
                designs[counter][0] = line;
                designs[counter][1] = br.readLine();
                designs[counter][2] = br.readLine();
                designs[counter][3] = br.readLine();
                counter++;
            }
        }
        for (int i = 0; i < designs.length; i++) {
            Liner p = Liner.byStrings(designs[i]);
            assertEquals(25, p.pointCount());
            assertEquals(50, p.lineCount());
            HyperbolicPlaneTest.testCorrectness(p, of(4));
            assertEquals(of(4), p.playfairIndex());
            assertEquals(i == 0 ? of(1, 2) : of(0, 1, 2), p.hyperbolicIndex()); // first is hyperaffine
            assertEquals(of(25), p.cardSubPlanes(true));
        }
    }

    private static final Map<String, int[]> dropped = Map.of(
            "pg29", new int[]{0},
            "dhall9", new int[]{0, 1},
            "hall9", new int[]{0, 81},
            "hughes9", new int[]{0, 3},
            "bbh1", new int[]{0, 192, 193, 269},
      //      "bbh2", new int[]{0, 28},
            "", new int[]{});

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
    public void testAutomorphisms2() throws IOException {
        String name = "dhall9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
            for (int dl : dropped.getOrDefault(name, IntStream.range(0, k * k + k + 1).toArray())) {
                long time = System.currentTimeMillis();
                System.out.println(name + " dropped " + dl + " count " + Automorphisms.autCountOld(new AffinePlane(proj, dl).toLiner()) + " time " + (System.currentTimeMillis() - time));
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
            Map<Integer, Liner> nonIsomorphic = new HashMap<>();
            br: for (int dl : IntStream.range(0, k * k + k + 1).toArray()) {
                System.out.println(dl);
                Liner aff = new AffinePlane(proj, dl).toLiner();
                for (Liner l : nonIsomorphic.values()) {
                    if (Automorphisms.isomorphism(l, aff) != null) {
                        continue br;
                    }
                }
                nonIsomorphic.put(dl, aff);
            }
            System.out.println("Non isomorphic " + nonIsomorphic.keySet());
        }
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
                    Triangle tr = liner.ofIdx(i);
                    for (int j = 0; j < permGroup.order(); j++) {
                        int[] perm = permGroup.permutation(j);
                        Triangle applied = new Triangle(perm[tr.o()], perm[tr.u()], perm[tr.w()]);
                        triangles[liner.toIdx(applied)] = i;
                    }
                }
                Map<Integer, Long> counts = Arrays.stream(triangles).boxed().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                System.out.println(counts);
            }
        }
    }

    @Test
    public void testCorrectness() throws IOException {
        String name = "dhall9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
            Liner liner = new AffinePlane(proj, 0).toLiner();
            TernaryRing tr = new AffineTernaryRing(liner, liner.ofIdx(0));
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
                            int x = IntStream.range(0, tr.order()).filter(y -> tr.op(a, y, b) == tr.op(c, y, d)).findAny().orElseThrow();
                            for (int y : tr.elements()) {
                                if (x == y) {
                                    continue;
                                }
                                if (tr.op(a, y, b) == tr.op(c, y, d)) {
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
    public void testEquality() throws IOException {
        String name = "dhall9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
            Liner liner = new AffinePlane(proj, 0).toLiner();
            TernaryRing tr0 = new AffineTernaryRing(liner, liner.ofIdx(0));
            TernaryRing tr1 = new AffineTernaryRing(liner, liner.ofIdx(1));
            assertFalse(tr0.trEquals(tr1));
            TernaryRing tr2 = new AffineTernaryRing(liner, liner.ofIdx(2));
            assertTrue(tr1.trEquals(tr2));
            TernaryRing tr18 = new AffineTernaryRing(liner, liner.ofIdx(18));
            TernaryRing tr24 = new AffineTernaryRing(liner, liner.ofIdx(24));
            assertTrue(!tr18.trEquals(tr0) && !tr18.trEquals(tr1));
            assertTrue(!tr24.trEquals(tr0) && !tr24.trEquals(tr1) && !tr24.trEquals(tr18));
            TernaryRing tr17 = new AffineTernaryRing(liner, liner.ofIdx(17));
            assertTrue(tr0.trEquals(tr17) && !tr1.trEquals(tr17));
        }
    }

    private static final Map<String, int[]> uniqueTriangles = Map.of("dhall9-0-9", new int[]{0, 1, 18, 24},
            "hughes9-3-9", new int[]{46686});

    @Test
    public void testTRAutomorphisms() throws IOException {
        String name = "dhall9-0-9";
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
                Triangle tr = liner.ofIdx(triangle);
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
}
