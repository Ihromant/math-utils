package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.BSInc;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.PartialLiner;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FinderTest {
    @Test
    public void generateCom() throws IOException {
        String prefix = "com";
        int v = 15;
        int k = 3;
        int dp = 3;
        DumpConfig conf = readLast(prefix, v, k, () -> defaultBeamConfig(v, k));
        List<PartialLiner> liners = Arrays.stream(conf.partials()).map(PartialLiner::new).collect(Collectors.toList());
        int left = conf.left();
        long time = System.currentTimeMillis();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size() + ", depth " + dp);
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            int depth = Math.min(left - 1, dp);
            liners = nextStage(liners, l -> l.hasNext(depth), PartialLiner::isomorphicSel, cnt);
            left--;
            dump(prefix, v, k, left, liners);
            System.out.println(left + " " + liners.size() + " " + cnt.get());
        }
        System.out.println(System.currentTimeMillis() - time);
    }

    @Test
    public void generateAP() throws IOException {
        String prefix = "ap";
        int v = 28;
        int k = 4;
        int dp = 4;
        DumpConfig conf = readLast(prefix, v, k, () -> defaultBeamConfig(v, k));
        List<BSInc> liners = Arrays.stream(conf.partials()).map(PartialLiner::new).map(l -> new BSInc(l.flags())).collect(Collectors.toList());
        long time = System.currentTimeMillis();
        int left = conf.left();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size() + ", depth " + dp);
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            int depth = Math.min(left - 1, dp);
            liners = nextStageCanonWithConv(liners, l -> l.hasNext(PartialLiner::checkAP, depth), PartialLiner::isomorphicSel, cnt).toList();
            left--;
            dump(prefix, v, k, left, liners.size(), liners.stream());
            System.out.println(left + " " + liners.size() + " " + cnt.get());
        }
        System.out.println(System.currentTimeMillis() - time);
    }

    private record DumpConfig(int v, int k, int left, int[][][] partials) {}

    private static void dump(String prefix, int v, int k, int left, List<PartialLiner> liners) throws IOException {
        try (FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/partials/" + prefix + "-" + v + "-" + k + ".txt", true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            ps.println(left + " blocks left");
            ps.println(liners.size() + " partials");
            for (PartialLiner l : liners) {
                for (int[] line : l.lines()) {
                    ps.println(Arrays.stream(line).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
                }
                ps.println();
            }
        }
    }

    private static void dump(String prefix, int v, int k, int left, int size, Stream<BSInc> liners) throws IOException {
        try (FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/partials/" + prefix + "-" + v + "-" + k + ".txt", true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            ps.println(left + " blocks left");
            ps.println(size + " partials");
            liners.forEach(i -> {
                PartialLiner l = new PartialLiner(i);
                for (int[] line : l.lines()) {
                    ps.println(Arrays.stream(line).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
                }
                ps.println();
            });
        }
    }

    private static DumpConfig readLast(String prefix, int v, int k, Supplier<DumpConfig> fallback) {
        try (FileInputStream fis = new FileInputStream("/home/ihromant/maths/partials/" + prefix + "-" + v + "-" + k + ".txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            int left = Integer.MIN_VALUE;
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
            return new DumpConfig(v, k, left, partials);
        } catch (FileNotFoundException e) {
            return fallback.get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<PartialLiner> nextStageConc(List<PartialLiner> partials, Predicate<PartialLiner> filter, BiPredicate<PartialLiner, PartialLiner> isoChecker, AtomicLong cnt) {
        List<PartialLiner> nonIsomorphic = new CopyOnWriteArrayList<>();
        AtomicInteger ai = new AtomicInteger();
        partials.stream().parallel().forEach(partial -> {
            for (int[] block : partial.blocks()) {
                PartialLiner liner = new PartialLiner(partial, block);
                if (!filter.test(liner)) {
                    continue;
                } else {
                    cnt.incrementAndGet();
                    if (nonIsomorphic.stream().parallel().anyMatch(l -> isoChecker.test(liner, l))) {
                        continue;
                    }
                }
                nonIsomorphic.add(liner);
            }
            System.out.println(ai.incrementAndGet() + " " + nonIsomorphic.size());
        });
        return new ArrayList<>(nonIsomorphic);
    }

    public static List<PartialLiner> nextStage(List<PartialLiner> partials, Predicate<PartialLiner> filter, BiPredicate<PartialLiner, PartialLiner> isoChecker, AtomicLong cnt) {
        List<PartialLiner> nonIsomorphic = new ArrayList<>();
        for (PartialLiner partial : partials) {
            for (int[] block : partial.blocks()) {
                PartialLiner liner = new PartialLiner(partial, block);
                if (!filter.test(liner)) {
                    continue;
                } else {
                    cnt.incrementAndGet();
                    if (nonIsomorphic.stream().anyMatch(l -> isoChecker.test(liner, l))) {
                        continue;
                    }
                }
                nonIsomorphic.add(liner);
            }
        }
        return nonIsomorphic;
    }

    public static List<PartialLiner> nextStageCanon(List<PartialLiner> partials, Predicate<PartialLiner> filter, BiPredicate<PartialLiner, PartialLiner> isoChecker, AtomicLong cnt) {
        Map<BitSet, PartialLiner> nonIsomorphic = new ConcurrentHashMap<>();
        AtomicLong counter = new AtomicLong();
        partials.stream().parallel().forEach(partial -> {
            for (int[] block : partial.blocks()) {
                PartialLiner liner = new PartialLiner(partial, block);
                if (filter.test(liner)) {
                    cnt.incrementAndGet();
                    nonIsomorphic.putIfAbsent(liner.getCanonical(), liner);
                }
            }
            long val = counter.incrementAndGet();
            if (val % 100 == 0) {
                System.out.println(val + " " + nonIsomorphic.size());
            }
        });
        return new ArrayList<>(nonIsomorphic.values());
    }

    public static Stream<BSInc> nextStageCanonWithConv(List<BSInc> partials, Predicate<PartialLiner> filter, BiPredicate<PartialLiner, PartialLiner> isoChecker, AtomicLong cnt) {
        Map<BitSet, BSInc> nonIsomorphic = new ConcurrentHashMap<>();
        AtomicLong counter = new AtomicLong();
        partials.stream().parallel().forEach(inc -> {
            PartialLiner partial = new PartialLiner(inc);
            for (int[] block : partial.blocks()) {
                PartialLiner liner = new PartialLiner(partial, block);
                if (filter.test(liner)) {
                    cnt.incrementAndGet();
                    nonIsomorphic.putIfAbsent(liner.getCanonical(), new BSInc(liner.flags()));
                }
            }
            long val = counter.incrementAndGet();
            if (val % 1000 == 0) {
               System.out.println(val + " " + nonIsomorphic.size());
            }
        });
        return nonIsomorphic.values().stream();
    }

    @Test
    public void readByLeft() {
        String prefix = "ap";
        String newPrefix = "ap1";
        int v = 19;
        int k = 3;
        int leftNeeded = 35;
        try (FileInputStream fis = new FileInputStream("/home/ihromant/maths/partials/" + prefix + "-" + v + "-" + k + ".txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            int lineCount = v * (v - 1) / k / (k - 1);
            while ((line = br.readLine()) != null) {
                int left = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                line = br.readLine();
                int partialsCount = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                int partialSize = lineCount - left;
                int[][][] partials = new int[partialsCount][partialSize][k];
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
                if (left == leftNeeded) {
                    List<PartialLiner> liners = Arrays.stream(partials).map(PartialLiner::new).toList();
                    dump(newPrefix, v, k, left, liners.size(), liners.stream().map(l -> new BSInc(l.flags())));
                    return;
                }
            }
            System.out.println("Not found");
        } catch (IOException e) {
            System.out.println("Something wrong happened");
            e.printStackTrace();
        }
    }

    @Test
    public void performance() {
        testPerformance(15, 3, 79, false);
        testPerformance(52, 4, 38, true);
        testPerformance(25, 4, 1, false);
        testPerformance(45, 3, 8, true);
    }

    private static void testPerformance(int v, int k, int cnt, boolean skipLines) {
        DumpConfig conf = readLast("perf", v, k, () -> {throw new IllegalArgumentException();});
        List<PartialLiner> dataSet = Arrays.stream(conf.partials()).map(PartialLiner::new).toList();
        long time = System.currentTimeMillis();
        List<PartialLiner> nonIsomorphic = new ArrayList<>();
        for (PartialLiner data : dataSet) {
            if (nonIsomorphic.stream().anyMatch(data::isomorphic)) {
                continue;
            }
            nonIsomorphic.add(data);
        }
        assertEquals(cnt, nonIsomorphic.size());
        System.out.println(v + " " + k + " iso points time " + (System.currentTimeMillis() - time));
        if (!skipLines) {
            conf = readLast("perf", v, k, () -> {throw new IllegalArgumentException();});
            dataSet = Arrays.stream(conf.partials()).map(PartialLiner::new).toList();
            time = System.currentTimeMillis();
            nonIsomorphic.clear();
            for (PartialLiner data : dataSet) {
                if (nonIsomorphic.stream().anyMatch(data::isomorphicL)) {
                    continue;
                }
                nonIsomorphic.add(data);
            }
            assertEquals(cnt, nonIsomorphic.size());
            System.out.println(v + " " + k + " iso lines time " + (System.currentTimeMillis() - time));
        }
        time = System.currentTimeMillis();
        nonIsomorphic.clear();
        for (PartialLiner data : dataSet) {
            if (nonIsomorphic.stream().anyMatch(data::isomorphicSel)) {
                continue;
            }
            nonIsomorphic.add(data);
        }
        assertEquals(cnt, nonIsomorphic.size());
        System.out.println(v + " " + k + " iso sel time " + (System.currentTimeMillis() - time));
//        nonIsomorphic.clear();
//        if (!vf2) {
//            return;
//        }
//        List<LinerWrapper> wrappers = new ArrayList<>();
//        VF2IsomorphismTester tester = new VF2IsomorphismTester();
//        time = System.currentTimeMillis();
//        for (PartialLiner data : dataSet) {
//            LinerWrapper liner = new LinerWrapper(data);
//            if (wrappers.stream().anyMatch(l -> tester.areIsomorphic(liner, l))) {
//                continue;
//            }
//            wrappers.add(liner);
//        }
//        assertEquals(cnt, wrappers.size());
//        System.out.println(prefix + " " + v + " " + k + " vf2 time " + (System.currentTimeMillis() - time));
    }

    @Test
    public void byPartials() throws IOException {
        String prefix = "ap";
        int v = 37;
        int k = 4;
        DumpConfig conf = readLast(prefix, v, k, () -> {throw new IllegalArgumentException();});
        int process = conf.left();
        List<BSInc> liners = Arrays.stream(conf.partials()).map(PartialLiner::new).map(l -> new BSInc(l.flags())).toList();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + conf.left() + ", base size " + liners.size());
        long time = System.currentTimeMillis();
        Predicate<PartialLiner> filter = PartialLiner::checkAP;
        Map<BitSet, BSInc> iso = new ConcurrentHashMap<>();
        AtomicInteger ai = new AtomicInteger();
        IntStream.range(0, liners.size()).parallel().forEach(idx -> {
            PartialLiner pl = new PartialLiner(liners.get(idx));
            designs(pl, process, filter, des -> {
                iso.putIfAbsent(des.getCanonical(), new BSInc(des.flags()));
                System.out.println(Arrays.toString(des.lines()));
            });
            int val = ai.incrementAndGet();
            if (iso.size() % 10 == 0) {
                System.out.println(val + " " + iso.size());
            }
        });
        dump("d" + prefix, v, k, conf.left() - process, iso.size(), iso.values().stream());
        System.out.println("Finished, time elapsed " + (System.currentTimeMillis() - time));
    }

    @Test
    public void byPartialsWithStorage1() throws IOException {
        String prefix = "ap";
        int v = 19;
        int k = 3;
        int process = 2;
        DumpConfig conf = readLast(prefix, v, k, () -> {throw new IllegalArgumentException();});
        List<BSInc> liners = Arrays.stream(conf.partials()).map(PartialLiner::new).map(l -> new BSInc(l.flags())).toList();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + conf.left() + ", base size " + liners.size());
        long time = System.currentTimeMillis();
        Predicate<PartialLiner> filter = l -> true;
        Map<BitSet, BSInc> iso = new ConcurrentHashMap<>();
        AtomicInteger ai = new AtomicInteger();
        try (FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/partials/ft/" + prefix + "-" + v + "-" + k + ".txt", true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            IntStream.range(0, liners.size()).parallel().forEach(idx -> {
                PartialLiner pl = new PartialLiner(liners.get(idx));
                designs(pl, process, filter, des -> {
                    if (iso.putIfAbsent(des.getCanonical(), new BSInc(des.flags())) == null) {
                        ps.println(Arrays.stream(des.lines()).map(line -> Arrays.stream(line).mapToObj(String::valueOf)
                                .collect(Collectors.joining(" "))).collect(Collectors.joining("\n")) + "\n");
                        ps.flush();
                        if (iso.size() % 1000 == 0) {
                            System.out.println(iso.size());
                        }
                    }
                });
                int val = ai.incrementAndGet();
                System.out.println(val + " " + iso.size());
            });
            dump("d" + prefix, v, k, conf.left() - process, iso.size(), iso.values().stream());
            System.out.println("Finished, time elapsed " + (System.currentTimeMillis() - time));
        }
    }

    private static void designs(PartialLiner partial, int needed, Predicate<PartialLiner> filter, Consumer<PartialLiner> cons) {
        for (int[] block : partial.blocks()) {
            PartialLiner nextPartial = new PartialLiner(partial, block);
            if (!filter.test(nextPartial)) {
                continue;
            }
            if (needed == 1) {
                cons.accept(nextPartial);
                continue;
            }
            designs(nextPartial, needed - 1, filter, cons);
        }
    }

    public static int[][] beamBlocks(int v, int k) {
        int r = (v - 1) / (k - 1);
        int[][] blocks = new int[r + 1][k];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < k - 1; j++) {
                blocks[i][j + 1] = 1 + i * (k - 1) + j;
            }
        }
        for (int i = 0; i < k; i++) {
            blocks[r][i] = 1 + (k - 1) * i;
        }
        return blocks;
    }

    private static DumpConfig defaultBeamConfig(int v, int k) {
        int[][] blocks = beamBlocks(v, k);
        return new DumpConfig(v, k, v * (v - 1) / k / (k - 1) - blocks.length, new int[][][]{blocks});
    }

    private static DumpConfig defaultHullsConfig(int v, int k, int cap) {
        DumpConfig common = readLast("com", cap, k, () -> {throw new IllegalArgumentException();});
        int bc = (cap - 1) / (k - 1);
        int lc = cap * (cap - 1) / k / (k - 1);
        int[][][] liners = Arrays.stream(common.partials()).filter(part -> {
            Liner l = new Liner(cap, part);
            return l.cardSubPlanes(true).nextSetBit(0) == cap;
        }).map(part -> {
            int[][] lines = new int[(v - 1) / (k - 1) + lc - bc][k];
            System.arraycopy(part, 0, lines, 0, part.length);
            for (int o = part.length; o < lines.length; o++) {
                int i = o - lc + bc;
                for (int j = 0; j < k - 1; j++) {
                    lines[o][j + 1] = 1 + i * (k - 1) + j;
                }
            }
            int[] tmp = lines[bc];
            lines[bc] = lines[lines.length - 1];
            lines[lines.length - 1] = tmp;
            return lines;
        }).toArray(int[][][]::new);
        int left = v * (v - 1) / k / (k - 1) - ((v - 1) / (k - 1) + lc - bc);
        return new DumpConfig(v, k, left, liners);
    }

    private DumpConfig defaultResConfig(int v, int k) {
        int r = v / k;
        int[][] blocks = new int[r + 1][k];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < k; j++) {
                blocks[i][j] = i * k + j;
            }
        }
        for (int i = 0; i < k; i++) {
            blocks[r][i] = k * i;
        }
        return new DumpConfig(v, k, v * (v - 1) / k / (k - 1) - r - 1, new int[][][]{blocks});
    }

    @Test
    public void generateLimitedHulls() throws IOException {
        int cap = 13;
        String prefix = "hsa" + cap;
        int v = 31;
        int k = 3;
        int dp = 4;
        DumpConfig conf = readLast(prefix, v, k, () -> defaultHullsConfig(v, k, cap));
        List<PartialLiner> liners = Arrays.stream(conf.partials()).map(PartialLiner::new).collect(Collectors.toList());
        int left = conf.left();
        long time = System.currentTimeMillis();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size() + ", cap " + cap + ", depth " + dp);
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            int depth = Math.min(left - 1, dp);
            liners = nextStageCanon(liners, l -> l.hasNext((Predicate<PartialLiner>)  l1 -> l1.hullsUnderCap(cap), depth), PartialLiner::isomorphicSel, cnt);
            left--;
            dump(prefix, v, k, left, liners.size(), liners.stream().map(l -> new BSInc(l.flags())));
            System.out.println(left + " " + liners.size() + " " + cnt.get());
        }
        System.out.println(System.currentTimeMillis() - time);
    }

    @Test
    public void generateResolvable() throws IOException {
        String prefix = "res";
        int v = 15;
        int k = 3;
        int dp = 4;
        DumpConfig conf = readLast(prefix, v, k, () -> defaultResConfig(v, k));
        List<PartialLiner> liners = Arrays.stream(conf.partials()).map(PartialLiner::new).collect(Collectors.toList());
        int left = conf.left();
        long time = System.currentTimeMillis();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size() + ", depth " + dp);
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            int depth = Math.min(left - 1, dp);
            liners = nextStageResolvable(liners, l -> l.hasNext(PartialLiner::blocksResolvable, depth), PartialLiner::isomorphicSel, cnt);
            left--;
            dump(prefix, v, k, left, liners.size(), liners.stream().map(p -> new BSInc(p.flags())));
            System.out.println(left + " " + liners.size() + " " + cnt.get());
        }
        System.out.println(System.currentTimeMillis() - time);
    }

    private static List<PartialLiner> nextStageResolvable(List<PartialLiner> partials, Predicate<PartialLiner> filter, BiPredicate<PartialLiner, PartialLiner> isoChecker, AtomicLong cnt) {
        List<PartialLiner> nonIsomorphic = new ArrayList<>();
        for (PartialLiner partial : partials) {
            for (int[] block : partial.blocksResolvable()) {
                PartialLiner liner = new PartialLiner(partial, block);
                if (!filter.test(liner)) {
                    continue;
                } else {
                    cnt.incrementAndGet();
                    if (nonIsomorphic.stream().anyMatch(l -> isoChecker.test(liner, l))) {
                        continue;
                    }
                }
                nonIsomorphic.add(liner);
            }
        }
        return nonIsomorphic;
    }

    @Test
    public void generateFt() throws IOException {
        String prefix = "com3";
        int v = 51;
        int k = 6;
        int dp = 3;
        DumpConfig conf = readLast(prefix, v, k, () -> defaultBeamConfig(v, k));
        Map<BitSet, PartialLiner> nonIsomorphic = readList(prefix, v, k, v * (v - 1) / k / (k - 1) - conf.left() + 1)
                .stream().collect(Collectors.toMap(PartialLiner::getCanonical, Function.identity(), (a, b) -> a, ConcurrentHashMap::new));
        List<PartialLiner> liners = Arrays.stream(conf.partials()).map(PartialLiner::new).toList();
        BitSet filter = readFilter(prefix, v, k);
        try (FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/partials/ft/" + prefix + "-" + v + "-" + k + ".txt", true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos);
             FileOutputStream fos1 = new FileOutputStream("/home/ihromant/maths/partials/ft/" + prefix + "done-" + v + "-" + k + ".txt", true);
             BufferedOutputStream bos1 = new BufferedOutputStream(fos1);
             PrintStream ps1 = new PrintStream(bos1)) {
            long time = System.currentTimeMillis();
            System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + conf.left() + ", base size " + liners.size() + ", depth " + dp);
            AtomicLong cnt = new AtomicLong();
            AtomicInteger ai = new AtomicInteger(filter.cardinality());
            IntStream.range(0, liners.size()).parallel().forEach(idx -> {
                if (filter.get(idx)) {
                    return;
                }
                PartialLiner partial = liners.get(idx);
                for (int[] block : partial.blocks()) {
                    PartialLiner liner = new PartialLiner(partial, block);
                    if (!liner.hasNext(dp)) {
                        continue;
                    } else {
                        cnt.incrementAndGet();
                    }
                    if (nonIsomorphic.putIfAbsent(liner.getCanonical(), liner) == null) {
                        ps.println(Arrays.stream(liner.lines()).map(line -> Arrays.stream(line).mapToObj(String::valueOf)
                                .collect(Collectors.joining(" "))).collect(Collectors.joining("\n")) + "\n");
                        ps.flush();
                    }
                }
                System.out.println(ai.incrementAndGet() + " " + nonIsomorphic.size());
                ps1.println(idx);
                ps1.flush();
            });
            System.out.println(System.currentTimeMillis() - time);
        }
    }

    private static List<PartialLiner> readList(String prefix, int v, int k, int pl) {
        try (FileInputStream fis = new FileInputStream("/home/ihromant/maths/partials/ft/" + prefix + "-" + v + "-" + k + ".txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            List<PartialLiner> partials = new ArrayList<>();
            c: while (true) {
                int[][] partial = new int[pl][k];
                for (int j = 0; j < pl; j++) {
                    String line = br.readLine();
                    if (line == null) {
                        break c;
                    }
                    String[] pts = line.split(" ");
                    for (int l = 0; l < k; l++) {
                        partial[j][l] = Integer.parseInt(pts[l]);
                    }
                }
                partials.add(new PartialLiner(v, partial));
                br.readLine();
            }
            return partials;
        } catch (FileNotFoundException e) {
            return new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BitSet readFilter(String prefix, int v, int k) {
        try (FileInputStream fis = new FileInputStream("/home/ihromant/maths/partials/ft/" + prefix + "done-" + v + "-" + k + ".txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            BitSet result = new BitSet();
            while (true) {
                String l = br.readLine();
                if (l == null) {
                    break;
                }
                result.set(Integer.parseInt(l));
            }
            return result;
        } catch (FileNotFoundException e) {
            return new BitSet();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void filterDuplicates() throws IOException {
        String prefix = "com3";
        int v = 51;
        int k = 6;
        //DumpConfig conf = readLast(prefix, v, k, () -> defaultBeamConfig(v, k));
        int full = v * (v - 1) / k / (k - 1);
        int size = full - 77;
        List<PartialLiner> list = readList(prefix, v, k, size);
        List<PartialLiner> filtered = Collections.synchronizedList(new ArrayList<>());
        System.out.println("Started filtering for v = " + v + ", k = " + k + ", blocks left " + 77 + ", base size " + list.size());
//        IntStream.range(0, list.size()).parallel().forEach(idx -> {
//            PartialLiner part = list.get(idx);
//            if (IntStream.range(Math.max(0, idx - 30), idx).parallel().noneMatch(i -> part.isomorphicSel(list.get(i)))) {
//                filtered.add(part);
//            }
//        });
        dump("ft/" + prefix + "a", v, k, full - size, list.size(), list.stream().map(p -> new BSInc(p.flags())));
        System.out.println("After filtering " + filtered.size());
    }
}
