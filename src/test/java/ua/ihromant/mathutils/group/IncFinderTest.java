package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Inc;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class IncFinderTest {
    @Test
    public void generateCom() throws IOException {
        String prefix = "com";
        int v = 15;
        int k = 3;
        int b = v * (v - 1) / k / (k - 1);
        int r = (v - 1) / (k - 1);
        DumpConfig conf = readLast(prefix, v, k, () -> new DumpConfig(v, k, b - r - 1, new Inc[]{(beamBlocks(v, k))}));
        List<Inc> liners = Arrays.asList(conf.partials);
        int left = conf.left();
        long time = System.currentTimeMillis();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size());
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStageAltConc(liners, cnt);
            left--;
            dump(prefix, v, k, left, liners);
            System.out.println(left + " " + liners.size() + " " + cnt.get());
        }
        System.out.println("Generated " + left + " " + liners.size());
        System.out.println(System.currentTimeMillis() - time);
    }

    @Test
    public void generateAP() throws IOException {
        String prefix = "ap";
        String altPrefix = "ap1";
        int v = 28;
        int k = 4;
        int b = v * (v - 1) / k / (k - 1);
        int r = (v - 1) / (k - 1);
        int dp = 4;
        DumpConfig conf = readLast(prefix, v, k, () -> k == 3
                ? new DumpConfig(v, k, b - r - 1, new Inc[]{(beamBlocks(v, k))})
                : readExact(altPrefix, v, k, b + 1 - 2 * r));
        List<Inc> liners = Arrays.asList(conf.partials);
        long time = System.currentTimeMillis();
        int left = conf.left();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size() + ", depth " + dp);
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStageAltConc(liners, PartialLiner::checkAP, cnt);
            left--;
            dump(prefix, v, k, left, liners);
            System.out.println(left + " " + liners.size() + " " + cnt.get());
        }
        System.out.println(System.currentTimeMillis() - time);
    }

    @Test
    public void generateResolvable() throws IOException {
        String prefix = "res";
        int v = 21;
        int k = 3;
        int dp = 4;
        int b = v * (v - 1) / k / (k - 1);
        int r = v / k;
        DumpConfig conf = readLast(prefix, v, k, () -> v == k * k
                ? new DumpConfig(v, k, b - 2 * r - 1, new Inc[]{squareBlocks(k)})
                : new DumpConfig(v, k, b - r - 1, new Inc[]{(resBlocks(v, k))}));
        List<Inc> liners = Arrays.asList(conf.partials);
        int left = conf.left();
        long time = System.currentTimeMillis();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size() + ", depth " + dp);
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            int depth = Math.min(left - 1, dp);
            liners = nextStageResolvableConc(liners, l -> l.hasNext(PartialLiner::blocksResolvable, depth), cnt);
            left--;
            dump(prefix, v, k, left, liners);
            System.out.println(left + " " + liners.size() + " " + cnt.get());
        }
        System.out.println(System.currentTimeMillis() - time);
    }

    public static Inc beamBlocks(int v, int k) {
        int r = (v - 1) / (k - 1);
        int b = r + 1;
        Inc res = Inc.empty(v, b);
        for (int l = 0; l < r; l++) {
            res.set(l, 0);
            for (int j = 0; j < k - 1; j++) {
                res.set(l, l * (k - 1) + j + 1);
            }
        }
        for (int i = 0; i < k; i++) {
            res.set(r, i * (k - 1) + 1);
        }
        return res;
    }

    private Inc resBlocks(int v, int k) {
        int r = v / k;
        int b = r + 1;
        Inc res = Inc.empty(v, b);
        for (int l = 0; l < r; l++) {
            for (int j = 0; j < k; j++) {
                res.set(l, l * k + j);
            }
        }
        for (int i = 0; i < k; i++) {
            res.set(r, k * i);
        }
        return res;
    }

    private Inc squareBlocks(int k) {
        int v = k * k;
        int b = 2 * k + 1;
        Inc res = Inc.empty(v, b);
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                res.set(i, i * k + j);
            }
        }
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                res.set(k + i, j * k + i);
            }
        }
        for (int i = 0; i < k; i++) {
            res.set(2 * k, k * i + i);
        }
        return res;
    }

    public static List<Inc> nextStageCanon(List<Inc> partials, AtomicLong cnt) {
        Map<BitSet, Inc> nonIsomorphic = new ConcurrentHashMap<>();
        partials.stream().parallel().forEach(partial -> {
            for (int[] block : partial.blocks()) {
                Inc liner = partial.addLine(block);
                nonIsomorphic.putIfAbsent(liner.getCanonicalOld(), liner);
                cnt.incrementAndGet();
            }
        });
        return new ArrayList<>(nonIsomorphic.values());
    }

    public static List<Inc> nextStageCanon(List<Inc> partials, Predicate<PartialLiner> filter, AtomicLong cnt) {
        Map<BitSet, Inc> nonIsomorphic = new ConcurrentHashMap<>();
        AtomicLong counter = new AtomicLong();
        partials.stream().parallel().forEach(inc -> {
            PartialLiner partial = new PartialLiner(inc);
            for (int[] block : partial.blocks()) {
                PartialLiner liner = new PartialLiner(partial, block);
                if (filter.test(liner)) {
                    cnt.incrementAndGet();
                    Inc next = liner.toInc();
                    nonIsomorphic.putIfAbsent(next.removeTwins().getCanonicalOld(), next);
                }
            }
            long val = counter.incrementAndGet();
            if (val % 1000 == 0) {
                System.out.println(val + " " + nonIsomorphic.size());
            }
        });
        return new ArrayList<>(nonIsomorphic.values());
    }

    private record DumpConfig(int v, int k, int left, Inc[] partials) {}

    private static void dump(String prefix, int v, int k, int left, List<Inc> liners) throws IOException {
        try (FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/partials/" + prefix + "-" + v + "-" + k + ".txt", true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            ps.println(left + " blocks left");
            ps.println(liners.size() + " partials");
            for (Inc l : liners) {
                IntStream.range(0, l.b()).forEach(line -> ps.println(IntStream.range(0, v)
                        .filter(p -> l.inc(line, p)).mapToObj(String::valueOf).collect(Collectors.joining(" "))));
                ps.println();
            }
        }
    }

    private static DumpConfig readLast(String prefix, int v, int k, Supplier<DumpConfig> fallback) {
        try (FileInputStream fis = new FileInputStream("/home/ihromant/maths/partials/" + prefix + "-" + v + "-" + k + ".txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            int left = Integer.MIN_VALUE;
            int lineCount = v * (v - 1) / k / (k - 1);
            Inc[] partials = null;
            while ((line = br.readLine()) != null) {
                left = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                line = br.readLine();
                int partialsCount = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                int partialSize = lineCount - left;
                partials = new Inc[partialsCount];
                for (int i = 0; i < partialsCount; i++) {
                    Inc partial = Inc.empty(v, partialSize);
                    for (int j = 0; j < partialSize; j++) {
                        String[] pts = br.readLine().split(" ");
                        for (int l = 0; l < k; l++) {
                            partial.set(j, Integer.parseInt(pts[l]));
                        }
                    }
                    partials[i] = partial;
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

    private static DumpConfig readExact(String prefix, int v, int k, int leftExpected) {
        try (FileInputStream fis = new FileInputStream("/home/ihromant/maths/partials/" + prefix + "-" + v + "-" + k + ".txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            int left;
            int lineCount = v * (v - 1) / k / (k - 1);
            Inc[] partials;
            while ((line = br.readLine()) != null) {
                left = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                line = br.readLine();
                int partialsCount = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                int partialSize = lineCount - left;
                partials = new Inc[partialsCount];
                for (int i = 0; i < partialsCount; i++) {
                    Inc partial = Inc.empty(v, partialSize);
                    for (int j = 0; j < partialSize; j++) {
                        String[] pts = br.readLine().split(" ");
                        for (int l = 0; l < k; l++) {
                            partial.set(j, Integer.parseInt(pts[l]));
                        }
                    }
                    partials[i] = partial;
                    br.readLine();
                }
                if (left == leftExpected) {
                    return new DumpConfig(v, k, left, partials);
                }
            }
            throw new IllegalStateException(String.valueOf(leftExpected));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void byPartials() throws IOException {
        String prefix = "come";
        int v = 25;
        int k = 4;
        DumpConfig conf = readLast(prefix, v, k, () -> {throw new IllegalArgumentException();});
        int process = conf.left();
        List<Inc> liners = Arrays.asList(conf.partials());
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + conf.left() + ", base size " + liners.size());
        long time = System.currentTimeMillis();
        Map<BitSet, Inc> iso = new ConcurrentHashMap<>();
        AtomicInteger ai = new AtomicInteger();
        IntStream.range(0, liners.size()).parallel().forEach(idx -> {
            Inc pl = liners.get(idx);
            PartialLiner partial = new PartialLiner(pl);
            partial.designs(process, l -> true, des -> {
                Inc next = des.toInc();
                iso.putIfAbsent(next.removeTwins().getCanonicalOld(), next);
            });
            int val = ai.incrementAndGet();
            if (val % 1 == 0) {
                System.out.println(val + " " + iso.size());
            }
        });
        dump("d" + prefix, v, k, conf.left() - process, new ArrayList<>(iso.values()));
        System.out.println("Finished, time elapsed " + (System.currentTimeMillis() - time));
    }

    private static int designs(Inc partial, int needed, Consumer<Inc> cons) {
        int res = needed;
        for (int[] block : partial.blocks()) {
            Inc nextPartial = partial.addLine(block);
            if (needed == 1) {
                cons.accept(nextPartial);
                res = 0;
            } else {
                int next = designs(nextPartial, needed - 1, cons);
                if (next < res) {
                    res = next;
                }
            }
        }
        return res;
    }

    private static int designs(Inc partial, int needed, Predicate<PartialLiner> filter, Consumer<Inc> cons) {
        int res = needed;
        for (int[] block : partial.blocks()) {
            PartialLiner nextPartial = new PartialLiner(partial.addLine(block));
            if (!filter.test(nextPartial)) {
                continue;
            }
            Inc nextInc = nextPartial.toInc();
            if (needed == 1) {
                cons.accept(nextInc);
                res = 0;
            } else {
                int next = designs(nextInc, needed - 1, filter, cons);
                if (next < res) {
                    res = next;
                }
            }
        }
        return res;
    }

    @Test
    public void generateFt() throws IOException {
        String prefix = "com1";
        int v = 61;
        int k = 6;
        int process = 1;
        DumpConfig conf = readLast(prefix, v, k, () -> {throw new IllegalArgumentException();});
        Map<BitSet, Inc> nonIsomorphic = readList("ft/" + prefix, v, k, v * (v - 1) / k / (k - 1) - conf.left() + process)
                .stream().parallel().collect(Collectors.toMap(l -> l.removeTwins().getCanonicalOld(), Function.identity(), (a, b) -> a, ConcurrentHashMap::new));
        List<Inc> liners = Arrays.asList(conf.partials());
        BitSet filter = readFilter("ft/" + prefix, v, k);
        try (FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/partials/ft/" + prefix + "-" + v + "-" + k + ".txt", true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos);
             FileOutputStream fos1 = new FileOutputStream("/home/ihromant/maths/partials/ft/" + prefix + "done-" + v + "-" + k + ".txt", true);
             BufferedOutputStream bos1 = new BufferedOutputStream(fos1);
             PrintStream ps1 = new PrintStream(bos1)) {
            long time = System.currentTimeMillis();
            System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + conf.left() + ", base size " + liners.size());
            AtomicInteger ai = new AtomicInteger(filter.cardinality());
            IntStream.range(0, liners.size()).parallel().forEach(idx -> {
                if (filter.get(idx)) {
                    return;
                }
                Inc pl = liners.get(idx);
                PartialLiner partial = new PartialLiner(pl);
                partial.designs(process, l -> true, des -> {
                    Inc res = des.toInc();
                    if (nonIsomorphic.putIfAbsent(res.removeTwins().getCanonicalOld(), res) == null) {
                        ps.println(res.toLines());
                        ps.flush();
                    }
                });
                System.out.println(ai.incrementAndGet() + " " + nonIsomorphic.size());
                ps1.println(idx);
                ps1.flush();
            });
            System.out.println(System.currentTimeMillis() - time);
        }
    }

    private static List<Inc> readList(String prefix, int v, int k, int pl) {
        List<Inc> partials = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream("/home/ihromant/maths/partials/" + prefix + "-" + v + "-" + k + ".txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            c: while (true) {
                Inc res = Inc.empty(v, pl);
                for (int ln = 0; ln < pl; ln++) {
                    String line = br.readLine();
                    if (line == null) {
                        break c;
                    }
                    String[] pts = line.split(" ");
                    for (int l = 0; l < k; l++) {
                        res.set(ln, Integer.parseInt(pts[l]));
                    }
                }
                partials.add(res);
                br.readLine();
            }
            return partials;
        } catch (NumberFormatException e) {
            if (e.getMessage().indexOf('\0') >= 0) {
                System.out.println("Zero row");
                return partials;
            }
            throw e;
        } catch (FileNotFoundException e) {
            return new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BitSet readFilter(String prefix, int v, int k) {
        try (FileInputStream fis = new FileInputStream("/home/ihromant/maths/partials/" + prefix + "done-" + v + "-" + k + ".txt");
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

    public static List<Inc> nextStageOld(List<Inc> partials, AtomicLong cnt) {
        List<PartialLiner> nonIsomorphic = new ArrayList<>();
        for (Inc inc : partials) {
            PartialLiner partial = new PartialLiner(inc);
            partial.altBlocks(block -> {
                PartialLiner liner = new PartialLiner(partial, block);
                cnt.incrementAndGet();
                if (nonIsomorphic.stream().noneMatch(liner::isomorphicSel)) {
                    nonIsomorphic.add(liner);
                }
            });
        }
        return nonIsomorphic.stream().map(PartialLiner::toInc).toList();
    }

    private static List<Inc> nextStageAlt(List<Inc> partials, AtomicLong cnt) {
        Map<BitSet, Inc> nonIsomorphic = new HashMap<>();
        for (Inc inc : partials) {
            PartialLiner partial = new PartialLiner(inc);
            Consumer<int[]> blockConsumer = block -> {
                Inc next = inc.addLine(block);
                cnt.incrementAndGet();
                nonIsomorphic.putIfAbsent(next.removeTwins().getCanonicalOld(), next);
            };
            partial.altBlocks(blockConsumer);
        }
        return new ArrayList<>(nonIsomorphic.values());
    }

    private static List<Inc> nextStageAlt(List<Inc> partials, Predicate<PartialLiner> filter, AtomicLong cnt) {
        Map<BitSet, Inc> nonIsomorphic = new HashMap<>();
        for (Inc inc : partials) {
            PartialLiner partial = new PartialLiner(inc);
            Consumer<int[]> blockConsumer = block -> {
                PartialLiner liner = new PartialLiner(partial, block);
                if (filter.test(liner)) {
                    cnt.incrementAndGet();
                    Inc next = liner.toInc();
                    nonIsomorphic.putIfAbsent(next.removeTwins().getCanonicalOld(), next);
                }
            };
            partial.altBlocks(blockConsumer);
        }
        return new ArrayList<>(nonIsomorphic.values());
    }

    private static List<Inc> nextStageResolvable(List<Inc> partials, Predicate<PartialLiner> filter, AtomicLong cnt) {
        Map<BitSet, Inc> nonIsomorphic = new HashMap<>();
        for (Inc inc : partials) {
            PartialLiner partial = new PartialLiner(inc);
            for (int[] block : partial.blocksResolvable()) {
                PartialLiner liner = new PartialLiner(partial, block);
                if (filter.test(liner)) {
                    cnt.incrementAndGet();
                    Inc next = liner.toInc();
                    nonIsomorphic.putIfAbsent(next.removeTwins().getCanonicalOld(), next);
                }
            }
        }
        return new ArrayList<>(nonIsomorphic.values());
    }

    private static List<Inc> nextStageResolvableConc(List<Inc> partials, Predicate<PartialLiner> filter, AtomicLong cnt) {
        Map<BitSet, Inc> nonIsomorphic = new ConcurrentHashMap<>();
        partials.stream().parallel().forEach(inc -> {
            PartialLiner partial = new PartialLiner(inc);
            for (int[] block : partial.blocksResolvable()) {
                PartialLiner liner = new PartialLiner(partial, block);
                if (filter.test(liner)) {
                    cnt.incrementAndGet();
                    Inc next = liner.toInc();
                    nonIsomorphic.putIfAbsent(next.removeTwins().getCanonicalOld(), next);
                }
            }
        });
        return new ArrayList<>(nonIsomorphic.values());
    }

    @Test
    public void randomizer1() {
        String prefix = "com";
        int v = 51;
        int k = 6;
        int cap = 54;
        DumpConfig conf = readLast(prefix, v, k, () -> {throw new IllegalArgumentException();});
        long[] freqs = new long[conf.left() - cap];
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + conf.left() + ", base size " + conf.partials.length);
        AtomicLong al = new AtomicLong();
        Predicate<PartialLiner> filter = l -> true;
        LongStream.range(0, Long.MAX_VALUE).parallel().forEach(l -> {
            PartialLiner pl = new PartialLiner(conf.partials[ThreadLocalRandom.current().nextInt(conf.partials.length)]);
            try {
                PartialLiner res = randomize(pl, filter, conf.left() - cap);
                res.designs(cap, filter, full -> System.out.println("Found " + Arrays.deepToString(full.lines())));
                freqs[0]++;
            } catch (IllegalStateException e) {
                freqs[Integer.parseInt(e.getMessage())]++;
            }
            if (al.incrementAndGet() % 1000 == 0) {
                System.out.println(Arrays.toString(freqs));
            }
        });
    }

    public PartialLiner randomize(PartialLiner partial, Predicate<PartialLiner> filter, int steps) {
        if (steps == 0) {
            return partial;
        }
        List<int[]> blocks = new ArrayList<>();
        partial.altBlocks(bl -> {
            PartialLiner pl = new PartialLiner(partial, bl);
            if (filter.test(pl)) {
                blocks.add(bl);
            }
        });
        if (blocks.isEmpty()) {
            throw new IllegalStateException(String.valueOf(steps));
        }
        PartialLiner next = new PartialLiner(partial, blocks.get(ThreadLocalRandom.current().nextInt(blocks.size())));
        return randomize(next, filter, steps - 1);
    }

    private static List<Inc> nextStageAltConc(List<Inc> partials, AtomicLong cnt) {
        Map<BitSet, Inc> nonIsomorphic = new ConcurrentHashMap<>();
        AtomicInteger ai = new AtomicInteger();
        partials.stream().parallel().forEach(inc -> {
            PartialLiner partial = new PartialLiner(inc);
            Consumer<int[]> blockConsumer = block -> {
                Inc liner = new PartialLiner(partial, block).toInc();
                cnt.incrementAndGet();
                nonIsomorphic.putIfAbsent(liner.removeTwins().getCanonicalOld(), liner);
            };
            partial.altBlocks(blockConsumer);
            //System.out.println(ai.incrementAndGet() + " " + nonIsomorphic.size());
        });
        return new ArrayList<>(nonIsomorphic.values());
    }

    private static List<Inc> nextStageAltConc(List<Inc> partials, Predicate<PartialLiner> filter, AtomicLong cnt) {
        Map<BitSet, Inc> nonIsomorphic = new ConcurrentHashMap<>();
        partials.stream().parallel().forEach(inc -> {
            PartialLiner partial = new PartialLiner(inc);
            Consumer<int[]> blockConsumer = block -> {
                PartialLiner liner = new PartialLiner(partial, block);
                if (filter.test(liner)) {
                    cnt.incrementAndGet();
                    Inc next = liner.toInc();
                    nonIsomorphic.putIfAbsent(next.removeTwins().getCanonicalOld(), next);
                }
            };
            partial.altBlocks(blockConsumer);
        });
        return new ArrayList<>(nonIsomorphic.values());
    }

    @Test
    public void generateLimitedHulls() throws IOException {
        int cap = 7;
        String prefix = "hs" + cap;
        int v = 27;
        int k = 3;
        int dp = 4;
        DumpConfig conf = readLast(prefix, v, k, () -> defaultHullsConfig(v, k, cap));
        List<Inc> liners = Arrays.asList(conf.partials);
        int left = conf.left();
        long time = System.currentTimeMillis();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size() + ", cap " + cap + ", depth " + dp);
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStageAlt(liners, l -> l.hullsUnderCap(cap), cnt);
            left--;
            dump(prefix, v, k, left, liners);
            System.out.println(left + " " + liners.size() + " " + cnt.get());
        }
        System.out.println(System.currentTimeMillis() - time);
    }

    private static DumpConfig defaultHullsConfig(int v, int k, int cap) {
        DumpConfig common = readLast("com", cap, k, () -> {throw new IllegalArgumentException();});
        int bc = (cap - 1) / (k - 1);
        int lc = cap * (cap - 1) / k / (k - 1);
        int[][][] liners = Arrays.stream(common.partials()).filter(inc -> {
            PartialLiner part = new PartialLiner(inc);
            Liner l = new Liner(cap, part.lines());
            return l.cardSubPlanes(true).nextSetBit(0) == cap;
        }).map(inc -> {
            PartialLiner partial = new PartialLiner(inc);
            int[][] part = partial.lines();
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
        return new DumpConfig(v, k, left, Arrays.stream(liners).map(PartialLiner::new).map(PartialLiner::toInc).toArray(Inc[]::new));
    }

    @Test
    public void extract() throws IOException {
        DumpConfig conf = readExact("com", 91, 10, 71);
        dump("come", conf.v(), conf.k(), conf.left, Arrays.asList(conf.partials()));
    }

    @Test
    public void extract1() throws IOException {
        int v = 21;
        int k = 3;
        int b = v * (v - 1) / k / (k - 1);
        int pl = 27;
        List<Inc> partials = readList("ft/ape", v, k, pl);
        dump("apee", v, k, b - pl, partials);
    }
}
