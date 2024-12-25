package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.fuzzy.Pair;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.FinderTest;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupProduct;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.plane.AffinePlane;
import ua.ihromant.mathutils.util.FixBS;

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
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(of(1, 2, 3, 4, 5), planes.get(0).hyperbolicIndex());
        assertEquals(of(1, 2, 3, 4, 5), planes.get(1).hyperbolicIndex());
        assertEquals(of(1, 2, 3, 4, 5), planes.get(2).hyperbolicIndex());
        assertEquals(of(2, 3, 4, 5), planes.get(3).hyperbolicIndex());
    }

    @Test
    public void test175_7() throws IOException {
        List<Liner> planes = readPlanes(175, 7);
        assertEquals(2, planes.size());
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
        Liner[] designs = getLiners25();
        for (int i = 0; i < designs.length; i++) {
            Liner p = designs[i];
            assertEquals(25, p.pointCount());
            assertEquals(50, p.lineCount());
            assertEquals(of(4), p.playfairIndex());
            assertEquals(i == 0 ? of(1, 2) : of(0, 1, 2), p.hyperbolicIndex()); // first is hyperaffine
            assertEquals(of(25), p.cardSubPlanes(true));
            PermutationGroup perm = p.automorphisms();
            QuickFind pts = new QuickFind(p.pointCount());
            QuickFind lns = new QuickFind(p.lineCount());
            int ll = p.line(0).length;
            QuickFind flags = new QuickFind(p.lineCount() * ll);
            for (int a = 0; a < perm.order(); a++) {
                int[] arr = perm.permutation(a);
                int[] lineMap = new int[p.lineCount()];
                for (int p1 = 0; p1 < p.pointCount(); p1++) {
                    pts.union(p1, arr[p1]);
                    for (int p2 = p1 + 1; p2 < p.pointCount(); p2++) {
                        lineMap[p.line(p1, p2)] = p.line(arr[p1], arr[p2]);
                    }
                }
                for (int ln = 0; ln < p.lineCount(); ln++) {
                    int ml = lineMap[ln];
                    lns.union(ln, ml);
                    int[] line = p.line(ln);
                    int[] mappedLine = p.line(ml);
                    for (int pt = 0; pt < line.length; pt++) {
                        flags.union(ln * ll + pt, lineMap[ln] * ll + Arrays.binarySearch(mappedLine, arr[line[pt]]));
                    }
                }
            }
            System.out.println("Liner " + i + " auths " + perm.order());
            System.out.println("Points " + pts.components());
            System.out.println("Lines " + lns.components());
            System.out.println("Flags " + flags.components().stream().map(bs -> bs.stream().mapToObj(fl -> "(" + fl / ll + ", " + p.line(fl / ll)[fl % ll] + ")")
                    .collect(Collectors.joining(", ", "[", "]"))).collect(Collectors.joining(", ", "[", "]")));
        }
    }

    private Liner[] getLiners25() throws IOException {
        Liner[] designs = new Liner[18];
        try (InputStream is = getClass().getResourceAsStream("/S(2,4,25).txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            int counter = 0;
            while ((line = br.readLine()) != null) {
                String[] design = new String[4];
                design[0] = line;
                design[1] = br.readLine();
                design[2] = br.readLine();
                design[3] = br.readLine();
                designs[counter++] = Liner.byStrings(design);
            }
        }
        return designs;
    }

    private List<Liner> cdfForGroup(Group group, int k) throws IOException {
        try (InputStream is = new FileInputStream("/home/ihromant/maths/diffSets/beg/" + k + "-" + group.name() + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            return br.lines().filter(l -> l.indexOf('{') >= 0 || l.indexOf('[') >= 0).map(line -> {
                String[] split = line.substring(2, line.length() - 2).split("], \\[|}, \\{");
                int[][] des = Arrays.stream(split).map(part -> Arrays.stream(part.split(", ")).mapToInt(Integer::parseInt).toArray()).toArray(int[][]::new);
                return Liner.byDiffFamily(group, des);
            }).toList();
        }
    }

    @Test
    public void testDesigns() throws IOException {
        Group gr = new GroupProduct(11, 11);
        List<Liner> liners = cdfForGroup(gr, 5);
        liners.forEach(l -> System.out.println(l.hyperbolicIndex()));
    }

    @Test
    public void test_ap_19_3() {
        int v = 19;
        int[][][] liners = readLast(getClass().getResourceAsStream("/ap-19-3.txt"), v, 3);
        for (int i = 0; i < liners.length; i++) {
            Liner liner = new Liner(v, liners[i]);
            long ac = Automorphisms.autCountOld(liner);
            if (ac != 1) {
                System.out.println(i + " " + liner.hyperbolicIndex() + " " + ac);
            }
        }
    }

    private static int[][][] readLast(InputStream is, int v, int k) {
        try (InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            int left;
            int lineCount = v * (v - 1) / k / (k - 1);
            int[][][] partials = null;
            while ((line = br.readLine()) != null) {
                left = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                line = br.readLine();
                int partialsCount = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                int partialSize = lineCount - left;
                partials = new int[partialsCount][partialSize][k];
                for (int i = 0; i < partialsCount; i++) {
                    int[][] partial = partials[i];
                    for (int j = 0; j < partialSize; j++) {
                        String[] pts = br.readLine().split(" ");
                        for (int l = 0; l < k; l++) {
                            partial[j][l] = Integer.parseInt(pts[l]);
                        }
                    }
                    br.readLine();
                }
            }
            return partials;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test_com_13_3() throws IOException {
        int v = 25;
        int k = 4;
        //int[][][] liners = readLast(getClass().getResourceAsStream("/como-" + v + "-" + k + ".txt"), v, k);
        int[][][] liners = Arrays.stream(getLiners25()).map(Liner::lines).toArray(int[][][]::new);
        Map<FixBS, PartialLiner> un = new HashMap<>();
        Map<FixBS, PartialLiner> unP = new HashMap<>();
        Map<FixBS, PartialLiner> unT = new HashMap<>();
        for (int[][] full : liners) {
            Map<FixBS, PartialLiner> unique = new HashMap<>();
            Map<FixBS, PartialLiner> uniquePairs = new HashMap<>();
            Map<FixBS, PartialLiner> uniqueTriples = new HashMap<>();
            Liner lnr = new Liner(v, full);
            for (int[] line : full) {
                BitSet set = of(line);
                PartialLiner liner = new PartialLiner(v, Arrays.stream(full).filter(l -> Arrays.stream(l).anyMatch(set::get)).toArray(int[][]::new));
                unique.putIfAbsent(liner.getCanonical(), liner);
            }
            for (int i : IntStream.range(0, v).toArray()) {
                for (int j : IntStream.range(i + 1, v).toArray()) {
                    PartialLiner liner = new PartialLiner(v, Arrays.stream(full).filter(l -> Arrays.stream(l).anyMatch(pt -> pt == i || pt == j)).toArray(int[][]::new));
                    uniquePairs.putIfAbsent(liner.getCanonical(), liner);
                }
            }
            for (int i : IntStream.range(0, v).toArray()) {
                for (int j : IntStream.range(i + 1, v).toArray()) {
                    for (int m : IntStream.range(j + 1, v).toArray()) {
                        if (lnr.flag(lnr.line(i, j), m)) {
                            continue;
                        }
                        PartialLiner liner = new PartialLiner(v, Arrays.stream(full).filter(l -> Arrays.stream(l).anyMatch(pt -> pt == i || pt == j || pt == m)).toArray(int[][]::new));
                        uniqueTriples.putIfAbsent(liner.getCanonical(), liner);
                    }
                }
            }
            System.out.println(unique.size() + " " + uniquePairs.size() + " " + uniqueTriples.size() + " " + Automorphisms.autCountOld(new Liner(v, full)));
            un.putAll(unique);
            unP.putAll(uniquePairs);
            unT.putAll(uniqueTriples);
        }
        System.out.println(un.size() + " " + unP.size() + " " + unT.size());
        int r = (v - 1) / (k - 1);
        int b = v * (v - 1) / k / (k - 1);
        FinderTest.dump("come", v, k, b - 3 * (r - 1), new ArrayList<>(unT.values()));
    }

    @Test
    public void test_com_25_4() throws IOException {
        int v = 25;
        int k = 4;
        int[][][] liners = readLast(getClass().getResourceAsStream("/com1-" + v + "-" + k + ".txt"), v, k);
        FixBS[] ex = Arrays.stream(getLiners25()).map(Liner::getCanonical).toArray(FixBS[]::new);
        for (int[][] full : liners) {
            Liner pl = new Liner(v, full);
            FixBS can = pl.getCanonical();
            System.out.println(IntStream.range(0, ex.length).filter(i -> ex[i].equals(can)).findAny().orElseThrow());
        }
    }

    @Test
    public void checkMatrices() {
        int v = 25;
        int k = 4;
        int[][][] liners = readLast(getClass().getResourceAsStream("/com-" + v + "-" + k + ".txt"), v, k);
        Arrays.stream(liners)
                .map(arr -> {
                    PartialLiner lnr = new PartialLiner(v, arr);
                    FixInc fi = (FixInc) lnr.toInc();
                    Matrix mt = fi.sqrInc().sqr();
                    Map<Integer, Integer> fr = new TreeMap<>();
                    for (int i = 0; i < v; i++) {
                        for (int j = i + 1; j < v; j++) {
                            if (lnr.line(i, j) < 0) {
                                fr.compute(mt.vals()[i][j], (a, b) -> b == null ? 1 : b + 1);
                            }
                        }
                    }
                    return fr;
//                    int[][] vals = Arrays.stream(mt.vals()).map(int[]::clone).toArray(int[][]::new);
//                    for (int i = 0; i < v; i++) {
//                        for (int j = 0; j < v; j++) {
//                            if (i == j || lnr.line(i, j) >= 0) {
//                                vals[i][j] = 0;
//                            }
//                        }
//                    }
//                    return new Matrix(vals);
                })
                .forEach(mt -> System.out.println(mt + "\n"));
    }

    private Liner[] getLiners15() throws IOException {
        Liner[] designs = new Liner[80];
        try (InputStream is = getClass().getResourceAsStream("/S(2,3,15).txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            int counter = 0;
            while ((line = br.readLine()) != null) {
                String[] design = new String[3];
                design[0] = line;
                design[1] = br.readLine();
                design[2] = br.readLine();
                designs[counter++] = Liner.byStrings(design);
            }
        }
        return designs;
    }

    private static Liner[] readUnique(int v, int k) throws IOException {
        try (InputStream fis = new FileInputStream(new File("/home/ihromant/maths/diffSets/unique", k + "-" + v + ".txt"));
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(fis));
             BufferedReader br = new BufferedReader(isr)) {
            return br.lines().skip(1).map(line -> {
                String cut = line.replace("[[", "").replace("]]", "")
                        .replace("{{", "").replace("}}", "")
                        .replace("[{", "").replace("}]", "");
                String[] arrays = cut.split("\\], \\[|\\}, \\{");
                int[][] diffSet = Arrays.stream(arrays).map(s -> Arrays.stream(s.split(", ")).mapToInt(Integer::parseInt).toArray()).toArray(int[][]::new);
                return Liner.byDiffFamily(v, diffSet);
            }).toArray(Liner[]::new);
        }
    }

    @Test
    public void testBooleanThalesian() throws IOException {
        Liner[] liners = readUnique(15, 3);
        for (int i = 0; i < liners.length; i++) {
            Liner l = liners[i];
            System.out.println(l.cardSubPlanes(true));
            ex: for (int a = 0; a < l.pointCount(); a++) {
                for (int b = a + 1; b < l.pointCount(); b++) {
                    for (int c = a + 1; c < l.pointCount(); c++) {
                        if (l.collinear(a, b, c)) {
                            continue;
                        }
                        for (int d = c + 1; d < l.pointCount(); d++) {
                            if (l.collinear(a, b, d) || l.collinear(a, c, d) || l.collinear(b, c, d)) {
                                continue;
                            }
                            if (IntStream.of(l.intersection(l.line(a, b), l.line(c, d)),
                                    l.intersection(l.line(a, c), l.line(b, d)),
                                    l.intersection(l.line(a, d), l.line(b, c))).filter(j -> j >= 0).count() == 1) {
                                System.out.println(i + " Not boolean");
                                break ex;
                            }
                        }
                    }
                }
            }
            ex2: for (int l1 = 0; l1 < l.lineCount(); l1++) {
                for (int l2 = 0; l2 < l.lineCount(); l2++) {
                    if (l1 == l2 || l.intersection(l1, l2) >= 0) {
                        continue;
                    }
                    for (int a : l.line(l1)) {
                        for (int b : l.line(l1)) {
                            if (a == b) {
                                continue;
                            }
                            for (int c : l.line(l1)) {
                                if (c == b || c == a) {
                                    continue;
                                }
                                for (int a1 : l.line(l2)) {
                                    for (int b1 : l.line(l2)) {
                                        if (a1 == b1) {
                                            continue;
                                        }
                                        for (int c1 : l.line(l2)) {
                                            if (c1 == b1 || c1 == a1) {
                                                continue;
                                            }
                                            if (l.intersection(l.line(a, b1), l.line(a1, b)) < 0
                                                    && l.intersection(l.line(b, c1), l.line(b1, c)) < 0
                                                    && l.intersection(l.line(a, c1), l.line(a1, c)) >= 0) {
                                                System.out.println(i + " Not para-Pappus " + a + " " + b + " " + c + " " + a1 + " " + b1 + " " + c1);
                                                break ex2;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ex1: for (int l1 = 0; l1 < l.lineCount(); l1++) {
                for (int l2 = 0; l2 < l.lineCount(); l2++) {
                    if (l2 == l1 || l.intersection(l1, l2) >= 0) {
                        continue;
                    }
                    for (int l3 = 0; l3 < l.lineCount(); l3++) {
                        if (l3 == l2 || l3 == l1 || l.intersection(l3, l2) >= 0 || l.intersection(l3, l1) >= 0) {
                            continue;
                        }
                        for (int a : l.line(l1)) {
                            for (int a1 : l.line(l1)) {
                                if (a == a1) {
                                    continue;
                                }
                                for (int b : l.line(l2)) {
                                    for (int b1 : l.line(l2)) {
                                        if (b == b1) {
                                            continue;
                                        }
                                        for (int c : l.line(l3)) {
                                            for (int c1 : l.line(l3)) {
                                                if (c == c1) {
                                                    continue;
                                                }
                                                if (!l.flag(l.line(a, c), b1)) {
                                                    continue;
                                                }
                                                if (l.intersection(l.line(a, b), l.line(a1, b1)) >= 0) {
                                                    continue;
                                                }
                                                if (l.intersection(l.line(b, c), l.line(b1, c1)) >= 0) {
                                                    continue;
                                                }
                                                int inter = l.intersection(l.line(a, c), l.line(a1, c1));
                                                if (inter >= 0) {
                                                    System.out.println(i + " Not near-Thalesian");
                                                    System.out.println(Arrays.toString(l.line(l1)) + " " + Arrays.toString(l.line(l2)) + " " + Arrays.toString(l.line(l3)));
                                                    System.out.println(a + " " + a1 + " " + b + " " + b1 + " " + c + " " + c1 + " " + inter);
                                                    break ex1;
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
}
