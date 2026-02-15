package ua.ihromant.mathutils;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import ua.ihromant.jnauty.GraphData;
import ua.ihromant.mathutils.auto.AutoAlgo;
import ua.ihromant.mathutils.fuzzy.Pair;
import ua.ihromant.mathutils.g.GSpace;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.FinderTest;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.group.SpecialLinear;
import ua.ihromant.mathutils.group.SubGroup;
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
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BatchLinerTest {
    private static ZipInputStream getZis(InputStream is) throws IOException {
        ZipInputStream zis = new ZipInputStream(Objects.requireNonNull(is));
        zis.getNextEntry();
        return zis;
    }

    public static List<Liner> readPlanes(int v, int k) throws IOException {
        try (InputStream is = BatchLinerTest.class.getResourceAsStream("/inc/S2-" + k + "-" + v + ".inc.zip");
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

    private static Liner readPlane(int v, int b, String next, BufferedReader br) throws IOException {
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

    private static final String base66 = "[[0, 2, 13, 37, 51, 52], [0, 5, 10, 15, 60, 65], [0, 6, 12, 32, 39, 40], [0, 21, 22, 38, 43, 55], [0, 25, 34, 41, 54, 57], [2, 3, 21, 33, 49, 60], [6, 9, 22, 41, 52, 60], [20, 22, 24, 26, 28, 65], [20, 25, 30, 35, 60, 61], [40, 42, 44, 46, 48, 65], [40, 45, 50, 55, 60, 62]]";

    @Test
    public void test66_6() throws IOException {
        List<Liner> planes = readPlanes(66, 6);
        assertEquals(3, planes.size());
        planes.forEach(p -> System.out.println(p.hyperbolicFreq()));
        Set<BitSet> blocks = new HashSet<>();
        for (int[] bbl : new ObjectMapper().readValue(base66, int[][].class)) {
            for (int el = 0; el < 20; el++) {
                BitSet actRes = new BitSet(66);
                for (int x : bbl) {
                    if (x < 20) {
                        actRes.set(op(el, x));
                    } else if (x < 40) {
                        actRes.set(op(el, x - 20) + 20);
                    } else if (x < 60) {
                        actRes.set(op(el, x - 40) + 40);
                    } else if (x < 65) {
                        actRes.set(op5(el, x - 60) + 60);
                    } else {
                        actRes.set(x);
                    }
                }
                blocks.add(actRes);
            }
        }
        Liner lnr = new Liner(blocks.toArray(BitSet[]::new));
        System.out.println(lnr.hyperbolicFreq());
    }

    private static int op(int el, int x) {
        if (el < 10 && x < 10 || el >= 10 && x >= 10) {
            return (el + x) % 10;
        } else {
            return 10 + ((el + x) % 10);
        }
    }

    private static int op5(int x, int y) {
        return (x + y) % 5;
    }

    @Test
    public void test65_5() throws IOException {
        List<Liner> planes = readPlanes(65, 5);
        assertEquals(1777, planes.size());
        assertEquals(of(3), planes.getFirst().hyperbolicIndex());
        assertEquals(FixBS.of(planes.getFirst().pointCount() + 1, 65), planes.getFirst().cardSubPlanes(true));
        Liner liner = planes.get(7);
        assertEquals(Map.of(1, 7200, 2, 100800, 3, 640800), liner.hyperbolicFreq());
        List<Liner> para = liner.paraModifications();
        assertEquals(228, para.size());
        Map<FixBS, Liner> unique = new ConcurrentHashMap<>();
        para.stream().parallel().forEach(lnr -> {
            GraphData gd = lnr.graphData();
            unique.putIfAbsent(new FixBS(gd.canonical()), lnr);
        });
        assertEquals(17, unique.size());
        List<Liner> paraAlt = liner.paraModificationsAlt();
        assertEquals(228, paraAlt.size());
        Map<FixBS, Liner> uniqueAlt = new ConcurrentHashMap<>();
        paraAlt.stream().parallel().forEach(lnr -> {
            GraphData gd = lnr.graphData();
            uniqueAlt.putIfAbsent(new FixBS(gd.canonical()), lnr);
        });
        assertEquals(17, uniqueAlt.size());
    }

    @Test
    public void test41_5() throws IOException {
        List<Liner> planes = readPlanes(41, 5);
        assertEquals(15, planes.size());
        IntStream.range(0, planes.size()).parallel().forEach(i -> {
            orbits(planes.get(i), i);
        });
    }

    @Test
    public void test45_5() throws IOException {
        List<Liner> planes = getUpdated45();
        assertEquals(1072, planes.size());
        IntStream.range(0, planes.size()).parallel().forEach(i -> {
            orbits(planes.get(i), i);
        });
    }

    public static List<Liner> getUpdated45() throws IOException {
        String s = Files.readString(Path.of("/home/ihromant/workspace/math-utils/src/test/resources/2-45-5-1.des"));
        ObjectMapper om = new ObjectMapper();
        List<Liner> planes = new ArrayList<>();
        while (true) {
            int fIdx = s.indexOf("blocks :=");
            if (fIdx < 0) {
                break;
            }
            int lIdx = s.indexOf("isBinary");
            String bl = s.substring(fIdx + "blocks :=".length(), lIdx);
            bl = bl.substring(0, bl.lastIndexOf(','));
            int[][] lines = om.readValue(bl, int[][].class);
            Arrays.stream(lines).forEach(b -> {
                for (int i = 0; i < 5; i++) {
                    b[i]--;
                }
            });
            planes.add(new Liner(45, lines));
            s = s.substring(lIdx + 20);
        }
        return planes;
    }

    @Test
    public void test37_4() throws IOException {
        List<Liner> planes = readPlanes(37, 4);
        assertEquals(51402, planes.size());
    }

    @Test
    public void test28_4() throws IOException {
        int v = 28;
        List<Liner> planes = readPlanes(v, 4);
        assertEquals(4466, planes.size());
        Liner plane = planes.get(1001); // 1001 classical, 976 Ree
        assertEquals(of(2), plane.hyperbolicIndex());
        assertEquals(FixBS.of(plane.pointCount() + 1, v), plane.cardSubPlanes(true));

        Liner ree = planes.get(976);
        PermutationGroup pg = ree.automorphisms();
        Group table = pg.asTable();
        List<SubGroup> subGroups = table.subGroups();
        System.out.println(subGroups.stream().collect(Collectors.groupingBy(SubGroup::order, Collectors.counting())));
        for (SubGroup g : table.subGroups()) {
            if (g.order() % v != 0) {
                continue;
            }
            QuickFind pts = new QuickFind(ree.pointCount());
            for (int a = 0; a < g.order(); a++) {
                int[] arr = pg.permutation(g.arr()[a]);
                for (int p1 = 0; p1 < ree.pointCount(); p1++) {
                    pts.union(p1, arr[p1]);
                }
            }
            System.out.println(g.order() + " elems " + g.elems() + " points " + pts.components());
        }
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
        List<Liner> designs = linersByStrings(25, 4);
        for (int i = 0; i < designs.size(); i++) {
            Liner p = designs.get(i);
            assertEquals(25, p.pointCount());
            assertEquals(50, p.lineCount());
            assertEquals(of(4), p.playfairIndex());
            assertEquals(i == 0 ? of(1, 2) : of(0, 1, 2), p.hyperbolicIndex()); // first is hyperaffine
            assertEquals(FixBS.of(p.pointCount() + 1, 25), p.cardSubPlanes(true));
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

    public static List<Liner> linersByStrings(int v, int k) throws IOException {
        List<Liner> result = new ArrayList<>();
        try (InputStream is = BatchLinerTest.class.getResourceAsStream("/S(2," + k + "," + v + ").txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] design = new String[k];
                design[0] = line;
                for (int i = 1; i < k; i++) {
                    design[i] = br.readLine();
                }
                result.add(Liner.byStrings(design));
            }
        }
        return result;
    }

    private void cdfForGroup(Group group, int k, Consumer<Liner> cons) throws IOException {
        int v = group.order();
        if (group.isCommutative()) {
            try (InputStream is = new FileInputStream("/home/ihromant/maths/diffSets/beg/" + k + "-" + group.name() + ".txt");
                 InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
                 BufferedReader br = new BufferedReader(isr)) {
                br.lines().parallel().filter(l -> l.contains("{{") || l.contains("[[")).map(line -> {
                    String[] split = line.substring(2, line.length() - 2).split("], \\[|}, \\{");
                    int[][] des = Stream.concat(Arrays.stream(split).map(part -> Arrays.stream(part.split(", ")).mapToInt(Integer::parseInt).toArray()),
                            v % k == 0 ? Stream.of(group.elements().filter(e -> k % group.order(e) == 0).toArray()) : Stream.empty()).toArray(int[][]::new);
                    return Liner.byDiffFamily(group, des);
                }).forEach(cons);
            }
        } else {
            try (InputStream is = new FileInputStream("/home/ihromant/maths/g-spaces/bunch/" + k + "-" + group.name() + ".txt");
                 InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
                 BufferedReader br = new BufferedReader(isr)) {
                br.lines().parallel().filter(l -> l.contains("{{") || l.contains("[[")).map(line -> {
                    String[] split = line.substring(2, line.length() - 2).split("], \\[|}, \\{");
                    int[][] des = Arrays.stream(split).map(part -> Arrays.stream(part.split(", ")).mapToInt(Integer::parseInt).toArray()).toArray(int[][]::new);
                    int[][] blocks = Arrays.stream(des).flatMap(bl -> blocks(bl, v, group)).toArray(int[][]::new);
                    return new Liner(v, blocks);
                }).forEach(cons);
            }
        }
    }

    private static Stream<int[]> blocks(int[] block, int v, Group gr) {
        int ord = gr.order();
        Set<FixBS> set = new HashSet<>(ord);
        List<int[]> res = new ArrayList<>();
        for (int i = 0; i < ord; i++) {
            FixBS fbs = new FixBS(v);
            for (int el : block) {
                fbs.set(el == ord ? ord : gr.op(i, el));
            }
            if (set.add(fbs)) {
                res.add(fbs.toArray());
            }
        }
        return res.stream();
    }

    private static Stream<int[]> blocksRight(int[] block, int v, Group gr) {
        int ord = gr.order();
        Set<FixBS> set = new HashSet<>(ord);
        List<int[]> res = new ArrayList<>();
        for (int i = 0; i < ord; i++) {
            FixBS fbs = new FixBS(v);
            for (int el : block) {
                fbs.set(el == ord ? ord : gr.op(el, i));
            }
            if (set.add(fbs)) {
                res.add(fbs.toArray());
            }
        }
        return res.stream();
    }

    @Test
    public void testDesigns() throws IOException {
        Group gr = new SemiDirectProduct(new CyclicGroup(37), new CyclicGroup(3));
        cdfForGroup(gr, 6, l -> System.out.println(l.hyperbolicFreq()));
    }

    @Test
    public void test_ap_19_3() {
        int v = 19;
        int[][][] liners = readLast(getClass().getResourceAsStream("/ap-19-3.txt"), v, 3);
        for (int i = 0; i < liners.length; i++) {
            Liner liner = new Liner(v, liners[i]);
            long ac = AutoAlgo.autCountOld(liner);
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
        int[][][] liners = linersByStrings(25, 4).stream().map(Liner::lines).toArray(int[][][]::new);
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
            System.out.println(unique.size() + " " + uniquePairs.size() + " " + uniqueTriples.size() + " " + AutoAlgo.autCountOld(new Liner(v, full)));
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
        FixBS[] ex = linersByStrings(25, 4).stream().map(Liner::getCanonical).toArray(FixBS[]::new);
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

    @Test
    public void test15_3() throws IOException {
        List<Liner> lnrs = linersByStrings(15, 3);
        for (int i = 0; i < lnrs.size(); i++) {
            System.out.println(i + " " + Arrays.deepToString(lnrs.get(i).resolutions()));
        }
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

    private static final int[][][] BASES = {
            {{0, 1, 2}, {0, 3, 10}, {0, 4, 18}, {0, 5, 20}, {0, 7, 8}, {0, 15, 24}},
            {{0, 1, 3}, {0, 2, 9}, {0, 4, 10}, {0, 5, 20}, {0, 7, 8}, {0, 15, 24}},
            {{0, 1, 2}, {0, 3, 14}, {0, 4, 18}, {0, 5, 20}, {0, 7, 8}, {0, 15, 24}},
            {{0, 1, 2}, {0, 3, 19}, {0, 4, 18}, {0, 5, 20}, {0, 7, 8}, {0, 15, 24}},
            {{0, 1, 3}, {0, 2, 22}, {0, 4, 19}, {0, 7, 8}, {0, 9, 16}, {0, 15, 24}},
            {{0, 1, 3}, {0, 2, 22}, {0, 4, 23}, {0, 5, 19}, {0, 9, 12}, {0, 15, 24}},
            {{0, 1, 3}, {0, 2, 22}, {0, 4, 23}, {0, 5, 20}, {0, 9, 14}, {0, 15, 24}},
    };

    private static final int[][][] GAP_BASES = {
            {{0, 1, 2}, {0, 3, 13}, {0, 4, 24}, {0, 7, 20}, {0, 8, 18}, {0, 15, 21}},
            {{0, 1, 5}, {0, 2, 3}, {0, 4, 24}, {0, 6, 12}, {0, 7, 20}, {0, 8, 19}},
            {{0, 1, 5}, {0, 2, 3}, {0, 4, 24}, {0, 6, 14}, {0, 12, 16}, {0, 15, 21}},
            {{0, 1, 2}, {0, 3, 16}, {0, 4, 24}, {0, 6, 23}, {0, 8, 22}, {0, 15, 21}},
            {{0, 1, 5}, {0, 2, 3}, {0, 4, 24}, {0, 6, 20}, {0, 8, 12}, {0, 15, 21}},
            {{0, 1, 5}, {0, 2, 6}, {0, 3, 17}, {0, 4, 24}, {0, 7, 20}, {0, 8, 22}},
            {{0, 1, 2}, {0, 3, 19}, {0, 4, 24}, {0, 6, 23}, {0, 7, 20}, {0, 8, 15}}
    };

    @Test
    public void testRot25_3() throws IOException {
        SpecialLinear sl = new SpecialLinear(2, new GaloisField(3));
        int v = 25;
        int[][] burattiBase = new int[][]{
                {sl.asElem(new int[][]{{1, 0}, {0, 1}}), sl.asElem(new int[][]{{2, 0}, {0, 2}}), 24},
                {sl.asElem(new int[][]{{1, 0}, {0, 1}}), sl.asElem(new int[][]{{1, 1}, {0, 1}}), sl.asElem(new int[][]{{1, 2}, {0, 1}})},
                {sl.asElem(new int[][]{{1, 0}, {0, 1}}), sl.asElem(new int[][]{{1, 0}, {2, 1}}), sl.asElem(new int[][]{{1, 0}, {1, 1}})},
                {sl.asElem(new int[][]{{1, 0}, {0, 1}}), sl.asElem(new int[][]{{1, 1}, {1, 2}}), sl.asElem(new int[][]{{0, 2}, {1, 0}})},
                {sl.asElem(new int[][]{{1, 0}, {0, 1}}), sl.asElem(new int[][]{{2, 1}, {2, 0}}), sl.asElem(new int[][]{{2, 0}, {2, 2}})},
                {sl.asElem(new int[][]{{1, 0}, {0, 1}}), sl.asElem(new int[][]{{2, 2}, {0, 2}}), sl.asElem(new int[][]{{0, 1}, {2, 1}})},
        };
        Arrays.stream(burattiBase).forEach(Arrays::sort);
        Liner burattiLiner = new Liner(v, Arrays.stream(burattiBase)
                .flatMap(bl -> blocksRight(bl, v, sl)).toArray(int[][]::new));
        System.out.println("Buratti liner, auths " + GroupIndex.identify(burattiLiner.automorphisms()));
        System.out.println("Subdesigns: " + burattiLiner.cardSubPlanes(true) + " fp: " + burattiLiner.hyperbolicFreq());
        FixBS burattiCanon = burattiLiner.getCanonicalOld();
        Group gap = GroupIndex.group(24, 3);
        Liner[] designs = Arrays.stream(GAP_BASES).map(base -> new Liner(v, Arrays.stream(base)
                .flatMap(bl -> blocks(bl, v, gap)).toArray(int[][]::new))).toArray(Liner[]::new);
        FixBS[] canon = Arrays.stream(designs).map(Liner::getCanonicalOld).toArray(FixBS[]::new);
        for (int i = 0; i < designs.length; i++) {
            for (int j = i + 1; j < designs.length; j++) {
                if (canon[i].equals(canon[j])) {
                    System.out.println(i + "<->" + j);
                }
            }
            Liner p = designs[i];
            orbits(p, i);
        }
        System.out.println("Buratti liner is isomorphic to liner " + IntStream.range(0, canon.length).filter(i -> canon[i].equals(burattiCanon)).findAny().orElseThrow());
    }

    @SneakyThrows
    private static void orbits(Liner p, int i) {
        PermutationGroup aut = p.automorphisms();
        FixBS elems = new FixBS(aut.order());
        elems.set(0, aut.order());
        orbits(p, new SubGroup(aut, elems), i);
    }

    @SneakyThrows
    public static void orbits(Liner p, SubGroup gr, int i) {
        PermutationGroup perm = (PermutationGroup) gr.group();
        FixBS elems = gr.elems();
        QuickFind pts = new QuickFind(p.pointCount());
        for (int a = elems.nextSetBit(0); a >= 0; a = elems.nextSetBit(a + 1)) {
            int[] arr = perm.permutation(a);
            for (int p1 = 0; p1 < p.pointCount(); p1++) {
                pts.union(p1, arr[p1]);
            }
        }
        List<FixBS> components = pts.components();
        System.out.println("Liner " + i + " auths " + gr.order() + " " + GroupIndex.groupId(gr) + " " + GroupIndex.identify(gr) + " orbits " + components.size() + " " + components);
    }

    private static final String lns = """
            [[0, 1, 4, 16, 23, 64, 74], [0, 2, 8, 32, 37, 46, 57], [0, 13, 26, 39, 52, 65, 78]]
            [[0, 1, 4, 16, 23, 64, 74], [0, 2, 36, 47, 56, 61, 85], [0, 13, 26, 39, 52, 65, 78]]
            """;

    @Test
    public void testOrbits() {
        ObjectMapper om = new ObjectMapper();
        Arrays.stream(lns.split("\n")).parallel().forEach(l -> {
            int[][] base = om.readValue(l, int[][].class);
            Liner lnr = Liner.byDiffFamily(91, base);
            PermutationGroup aut = lnr.automorphisms();
            Group table = aut.asTable();
            Map<Integer, List<SubGroup>> gr = table.groupedSubGroups();
            for (List<SubGroup> sgs : gr.values()) {
                for (SubGroup sg : sgs) {
                    orbits(lnr, new SubGroup(aut, sg.elems()), l.length() % 2);
                }
            }
        });
    }

    private static final String lns175 = "[[0, 7, 22, 39, 55, 151, 174], [0, 8, 14, 88, 97, 109, 139], [0, 10, 45, 49, 72, 116, 156], [0, 17, 37, 100, 122, 133, 158], [0, 1, 2, 3, 4, 5, 6]]";

    @Test
    public void generateAuts() throws IOException {
        ObjectMapper om = new ObjectMapper();
        int[][] base = om.readValue(lns175, int[][].class);
        Liner lnr = Liner.byDiffFamily(new CyclicProduct(5, 5, 7), base);
        System.out.println(lnr.hyperbolicFreq());
        PermutationGroup aut = lnr.automorphisms();
        try (FileOutputStream fos = new FileOutputStream(new File("/home/ihromant/maths/g-spaces", "auths-5-5-7.txt"));
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            ps.println(Arrays.deepToString(lnr.lines()));
            for (int i = 0; i < aut.order(); i++) {
                ps.println(Arrays.toString(aut.permutation(i)));
            }
        }
    }

    @Test
    public void orbitsByFile() throws IOException {
        ObjectMapper om = new ObjectMapper();
        try (FileInputStream fis = new FileInputStream(new File("/home/ihromant/maths/g-spaces", "auths-5-5-7.txt"));
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            Liner lnr = new Liner(175, om.readValue(br.readLine(), int[][].class));
            List<int[]> aut = new ArrayList<>();
            String l;
            while ((l = br.readLine()) != null) {
                aut.add(om.readValue(l, int[].class));
            }
            PermutationGroup group = new PermutationGroup(aut.toArray(int[][]::new));
            Group table = group.asTable();
            System.out.println(lnr.hyperbolicFreq());
            Map<Integer, List<SubGroup>> gr = table.groupedSubGroups();
            for (List<SubGroup> sgs : gr.values()) {
                for (SubGroup sg : sgs) {
                    orbits(lnr, new SubGroup(group, sg.elems()), 0);
                }
            }
        }
    }

    @Test
    public void testParallel() throws IOException {
//        List<Liner> planes = readPlanes(66, 6);
//        for (int i = 0; i < planes.size(); i++) {
//            parallelTriangles(planes.get(i), i);
//        }
        Liner denniston = HyperbolicPlaneTest.dennistonArc(16, 8);
        parallelTriangles(denniston, 0);
        Liner proj = new Liner(new GaloisField(5).generatePlane());
        //parallelTriangles(proj, 0);
        AffinePlane pl = new AffinePlane(proj, 0);
        Liner lnrAff = pl.toLiner();
        parallelTriangles(lnrAff, 0);
    }

    private static void parallelTriangles(Liner lnr, int idx) throws IOException {
        int pc = lnr.pointCount();
        int lc = lnr.lineCount();
        int ts = pc * pc * pc;
        System.out.println("Liner " + idx);
        QuickFind qf = new QuickFind(ts);
        for (int a = 0; a < pc; a++) {
            for (int b = 0; b < pc; b++) {
                qf.union(from(new int[]{a, a, b}, pc), from(new int[]{a, a, a}, pc));
                qf.union(from(new int[]{a, a, b}, pc), from(new int[]{a, b, b}, pc));
                qf.union(from(new int[]{a, a, b}, pc), from(new int[]{a, b, a}, pc));
                qf.union(from(new int[]{a, a, b}, pc), from(new int[]{b, a, a}, pc));
                if (a == b) {
                    continue;
                }
                int ab = lnr.line(a, b);
                for (int c : lnr.line(ab)) {
                    qf.union(from(new int[]{a, a, a}, pc), from(new int[]{a, b, c}, pc));
                }
            }
        }
        for (int ln1 = 0; ln1 < lc; ln1++) {
            for (int ln2 = 0; ln2 < lc; ln2++) {
                if (lnr.intersection(ln1, ln2) >= 0) {
                    continue;
                }
                for (int a : lnr.line(ln1)) {
                    for (int b : lnr.line(ln1)) {
                        if (a == b) {
                            continue;
                        }
                        for (int c : lnr.line(ln2)) {
                            for (int d : lnr.line(ln2)) {
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
            }
        }
        Set<FixBS> comps = new HashSet<>(qf.components());
        comps.removeIf(l -> {
            int st = l.nextSetBit(0);
            int[] abc = to(st, pc);
            return abc[0] == abc[1] || abc[0] == abc[2] || abc[1] == abc[2]
                    || lnr.collinear(abc[0], abc[1], abc[2]);
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
                    || lnr.collinear(abc[0], abc[1], abc[2]);
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

    private static int from(int[] abc, int pc) {
        return abc[0] * pc * pc + abc[1] * pc + abc[2];
    }

    private static int[] to(int p, int pc) {
        return new int[]{p / pc / pc, p / pc % pc, p % pc};
    }

    @Test
    public void refine() throws IOException {
        Stream<String> lns = Files.lines(Path.of("/home/ihromant/maths/g-spaces/bunch/6-96-1,19,19,19,19,19.txt"));
        ObjectMapper om = new ObjectMapper();
        Group g = new SemiDirectProduct(new CyclicGroup(19), new CyclicGroup(3));
        GSpace space = new GSpace(6, g, false, 57, 3, 3, 3, 3, 3);
        Map<FixBS, Liner> lnrs = new ConcurrentHashMap<>();
        lns.parallel().forEach(l -> {
            if (!l.contains("[{")) {
                return;
            }
            String s = l.substring(l.indexOf("[{")).replace('{', '[').replace('}', ']');
            int[][] base = om.readValue(s, int[][].class);
            Liner lnr = new Liner(space.v(), Arrays.stream(base).flatMap(bl -> space.blocks(FixBS.of(space.v(), bl))).toArray(int[][]::new));
            Map<Integer, Integer> freq = lnr.hyperbolicFreq();
            GraphData gd = lnr.graphData();
            Liner existing = lnrs.putIfAbsent(new FixBS(gd.canonical()), lnr);
            if (existing == null) {
                try {
                    String name;
                    if (gd.autCount() != g.order()) {
                        Group aut = new PermutationGroup(permutationClosure(gd.autGens())).asTable();
                        name = GroupIndex.identify(aut) + " " + GroupIndex.groupId(aut);
                    } else {
                        name = g.name();
                    }
                    System.out.println(gd.autCount() + " " + name + " " + freq + " " + Arrays.deepToString(lnr.lines()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        lns.close();
    }

    private int[][] permutationClosure(int[][] generators) {
        Set<ArrWrap> result = new HashSet<>();
        result.add(new ArrWrap(IntStream.range(0, generators[0].length).toArray()));
        boolean added;
        do {
            added = false;
            for (ArrWrap el : result.toArray(ArrWrap[]::new)) {
                for (int[] gen : generators) {
                    ArrWrap xy = new ArrWrap(combine(gen, el.map));
                    ArrWrap yx = new ArrWrap(combine(el.map, gen));
                    added = result.add(xy) || added;
                    added = result.add(yx) || added;
                }
            }
        } while (added);
        return result.stream().map(ArrWrap::map).toArray(int[][]::new);
    }

    private static int[] combine(int[] a, int[] b) {
        int[] result = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[b[i]];
        }
        return result;
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
    public void filterFolder() throws IOException {
        ObjectMapper om = new ObjectMapper();
        Map<FixBS, Pr> lnrs = new ConcurrentHashMap<>();
        int v = 65;
        int k = 5;
        String folder = "/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/";
        for (File f : Objects.requireNonNull(new File(folder).listFiles())) {
            System.out.println("Reading " + f.getName());
            String content = Files.readString(f.toPath());
            content.lines().parallel().forEach(l -> {
                if (!l.contains("[[")) {
                    return;
                }
                int[][] lines = om.readValue(l.substring(l.indexOf("[[")), int[][].class);
                if (lines.length != (v * v - v) / (k * k - k)) {
                    System.out.println("Wrong " + Arrays.deepToString(lines));
                    return;
                }
                Liner lnr = new Liner(lines);
                GraphData gd = lnr.graphData();
                lnrs.putIfAbsent(new FixBS(gd.canonical()), new Pr(gd, lnr));
            });
        }
        StringBuilder sb = new StringBuilder();
        Map<Long, Long> quantities = new HashMap<>();
        for (Pr pr : lnrs.values()) {
            quantities.compute(pr.gd.autCount(), (_, ct) -> ct == null ? 1 : ct + 1);
            sb.append(pr.gd.autCount()).append(' ').append(pr.lnr.hyperbolicFreq()).append(' ')
                    .append(Arrays.deepToString(pr.lnr.lines())).append('\n');
        }
        System.out.println(quantities);
        Files.writeString(Path.of(folder, "final.txt"), sb);
    }

    private record Pr(GraphData gd, Liner lnr) {}
}
