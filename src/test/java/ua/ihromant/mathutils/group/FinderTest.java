package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Automorphisms;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.PartialLiner;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FinderTest {
    @Test
    public void generateCom() throws IOException {
        String prefix = "com";
        int v = 15;
        int k = 3;
        DumpConfig conf = readLast(prefix, v, k);
        List<PartialLiner> liners = Arrays.stream(conf.partials()).map(PartialLiner::new).collect(Collectors.toList());
        long time = System.currentTimeMillis();
        int left = conf.left();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size());
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStage(liners, PartialLiner::hasNext, PartialLiner::isomorphicSel, cnt);
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
        DumpConfig conf = readLast(prefix, v, k);
        List<PartialLiner> liners = Arrays.stream(conf.partials()).map(PartialLiner::new).collect(Collectors.toList());
        long time = System.currentTimeMillis();
        int left = conf.left();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size());
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStage(liners, l -> l.checkAP() && l.hasNext(PartialLiner::checkAP), PartialLiner::isomorphicSel, cnt);
            left--;
            dump(prefix, v, k, left, liners);
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

    private static DumpConfig readLast(String prefix, int v, int k) {
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
            return new DumpConfig(v, k, v * (v - 1) / k / (k - 1) - r - 1, new int[][][]{blocks});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<PartialLiner> nextStage(List<PartialLiner> partials, Predicate<PartialLiner> filter, BiPredicate<PartialLiner, PartialLiner> isoChecker, AtomicLong cnt) {
        List<PartialLiner> nonIsomorphic = new ArrayList<>();
        for (PartialLiner partial : partials) {
            Consumer<int[]> blockConsumer = block -> {
                PartialLiner liner = new PartialLiner(partial, block.clone());
                if (!filter.test(liner)) {
                    return;
                } else {
                    cnt.incrementAndGet();
                    if (nonIsomorphic.stream().anyMatch(l -> isoChecker.test(liner, l))) {
                        return;
                    }
                }
                nonIsomorphic.add(liner);
            };
            partial.blocks(blockConsumer);
        }
        return nonIsomorphic;
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
                    List<PartialLiner> liners = Arrays.stream(partials).map(PartialLiner::new).collect(Collectors.toList());
                    dump(newPrefix, v, k, left, liners);
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
        DumpConfig conf = readLast("perf", v, k);
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
            conf = readLast("perf", v, k);
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
    public void byPartials() {
        String prefix = "ap";
        int v = 28;
        int k = 4;
        DumpConfig conf = readLast(prefix, v, k);
        List<PartialLiner> liners = Arrays.stream(conf.partials()).map(PartialLiner::new).toList();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + conf.left() + ", base size " + liners.size());
        long time = System.currentTimeMillis();
        Predicate<PartialLiner> filter = PartialLiner::checkAP;
        liners.stream().forEach(pl -> designs(pl, conf.left(), filter, des -> {
            Liner l = new Liner(v, des.lines());
            System.out.println(l.hyperbolicIndex() + " " + Automorphisms.autCountOld(l) + " " + Arrays.deepToString(des.lines()));
        }));
        System.out.println("Finished, time elapsed " + (System.currentTimeMillis() - time));
    }

    private static void designs(PartialLiner partial, int needed, Predicate<PartialLiner> filter, Consumer<PartialLiner> cons) {
        Consumer<int[]> blockConsumer = block -> {
            PartialLiner nextPartial = new PartialLiner(partial, block.clone());
            if (!filter.test(nextPartial)) {
                return;
            }
            if (needed == 1) {
                cons.accept(nextPartial);
                return;
            }
            designs(nextPartial, needed - 1, filter, cons);
        };
        partial.blocks(blockConsumer);
    }

    @Test
    public void generateLimitedHulls() throws IOException {
        int cap = 15;
        String prefix = "hs" + cap;
        int v = 45;
        int k = 3;
        DumpConfig conf = readLast(prefix, v, k);
        List<PartialLiner> liners = Arrays.stream(conf.partials()).map(PartialLiner::new).collect(Collectors.toList());
        long time = System.currentTimeMillis();
        int left = conf.left();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size() + ", cap " + cap);
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStage(liners, l -> l.hullsUnderCap(cap) && l.hasNext(l1 -> l1.hullsUnderCap(cap)), PartialLiner::isomorphic, cnt);
            left--;
            dump(prefix, v, k, left, liners);
            System.out.println(left + " " + liners.size() + " " + cnt.get());
        }
        System.out.println(System.currentTimeMillis() - time);
    }

    @Test
    public void generateLimitedHullsAlt() throws IOException {
        int cap = 13;
        String prefix = "hsa" + cap;
        int v = 31;
        int k = 3;
        List<PartialLiner> liners;
        int left;
        if (new File("/home/ihromant/maths/partials/" + prefix + "-" + v + "-" + k + ".txt").exists()) {
            DumpConfig conf = readLast(prefix, v, k);
            liners = Arrays.stream(conf.partials()).map(PartialLiner::new).collect(Collectors.toList());
            left = conf.left();
        } else {
            DumpConfig common = readLast("com", cap, k);
            int bc = (cap - 1) / (k - 1);
            int lc = cap * (cap - 1) / k / (k - 1);
            liners = Arrays.stream(common.partials()).filter(part -> {
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
                return new PartialLiner(lines);
            }).toList();
            left = v * (v - 1) / k / (k - 1) - ((v - 1) / (k - 1) + lc - bc);
        }
        long time = System.currentTimeMillis();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size() + ", cap " + cap);
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStage(liners, l -> l.hullsUnderCap(cap) && l.hasNext(l1 -> l1.hullsUnderCap(cap)), PartialLiner::isomorphicSel, cnt);
            left--;
            dump(prefix, v, k, left, liners);
            System.out.println(left + " " + liners.size() + " " + cnt.get());
        }
        System.out.println(System.currentTimeMillis() - time);
    }
}
