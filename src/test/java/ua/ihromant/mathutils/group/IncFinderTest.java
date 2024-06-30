package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Inc;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        int left = b - liners.getFirst().b();
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
        int v = 37;
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

    public static Inc beamBlocks(int v, int k) {
        int r = (v - 1) / (k - 1);
        BitSet inc = new BitSet(r * v + v);
        for (int i = 0; i < r; i++) {
            inc.set(i * v);
            for (int j = 0; j < k - 1; j++) {
                inc.set(i * v + 1 + i * (k - 1) + j);
            }
        }
        for (int i = 0; i < k; i++) {
            inc.set(r * v + 1 + (k - 1) * i);
        }
        return new Inc(inc, v, r + 1);
    }

    public static List<Inc> nextStageCanon(List<Inc> partials, AtomicLong cnt) {
        Map<BitSet, Inc> nonIsomorphic = new ConcurrentHashMap<>();
        partials.stream().parallel().forEach(partial -> {
            for (int[] block : partial.blocks()) {
                Inc liner = new Inc(partial, block);
                nonIsomorphic.putIfAbsent(liner.getCanonical(), liner);
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
                    nonIsomorphic.putIfAbsent(liner.getCanonical(), new Inc(liner.flags()));
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
                    Inc partial = new Inc(new BitSet(v * partialSize), v, partialSize);
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
            Inc[] partials = null;
            while ((line = br.readLine()) != null) {
                left = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                line = br.readLine();
                int partialsCount = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                int partialSize = lineCount - left;
                partials = new Inc[partialsCount];
                for (int i = 0; i < partialsCount; i++) {
                    Inc partial = new Inc(new BitSet(v * partialSize), v, partialSize);
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
        int process = 5;
        List<Inc> liners = Arrays.asList(conf.partials());
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + conf.left() + ", base size " + liners.size());
        long time = System.currentTimeMillis();
        Map<BitSet, Inc> iso = new ConcurrentHashMap<>();
        AtomicInteger ai = new AtomicInteger();
        IntStream.range(0, liners.size()).parallel().forEach(idx -> {
            Inc pl = liners.get(idx);
            PartialLiner partial = new PartialLiner(pl);
            partial.designs(process, l -> true, des -> {
                iso.putIfAbsent(des.getCanonical(), new Inc(des.flags()));
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
            Inc nextPartial = new Inc(partial, block);
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
            PartialLiner nextPartial = new PartialLiner(new Inc(partial, block));
            if (!filter.test(nextPartial)) {
                continue;
            }
            Inc nextInc = new Inc(nextPartial.flags());
            if (needed == 1) {
                cons.accept(new Inc(nextPartial.flags()));
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
        String prefix = "com";
        int v = 25;
        int k = 4;
        int process = 3;
        DumpConfig conf = readLast(prefix, v, k, () -> {throw new IllegalArgumentException();});
        Map<BitSet, Inc> nonIsomorphic = readList("ft/" + prefix, v, k, v * (v - 1) / k / (k - 1) - conf.left() + process)
                .stream().parallel().collect(Collectors.toMap(Inc::getCanonical, Function.identity(), (a, b) -> a, ConcurrentHashMap::new));
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
                designs(pl, process, des -> {
                    if (nonIsomorphic.putIfAbsent(des.getCanonical(), des) == null) {
                        ps.println(des.toLines());
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
        try (FileInputStream fis = new FileInputStream("/home/ihromant/maths/partials/" + prefix + "-" + v + "-" + k + ".txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            List<Inc> partials = new ArrayList<>();
            c: while (true) {
                boolean[][] incidence = new boolean[pl][v];
                for (int j = 0; j < pl; j++) {
                    String line = br.readLine();
                    if (line == null) {
                        break c;
                    }
                    String[] pts = line.split(" ");
                    for (int l = 0; l < k; l++) {
                        incidence[j][Integer.parseInt(pts[l])] = true;
                    }
                }
                partials.add(new Inc(incidence));
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

    private static List<Inc> nextStageAlt(List<Inc> partials, AtomicLong cnt) {
        Map<BitSet, Inc> nonIsomorphic = new HashMap<>();
        for (Inc inc : partials) {
            PartialLiner partial = new PartialLiner(inc);
            Consumer<int[]> blockConsumer = block -> {
                PartialLiner liner = new PartialLiner(partial, block.clone());
                cnt.incrementAndGet();
                nonIsomorphic.putIfAbsent(liner.getCanonical(), new Inc(liner.flags()));
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
                PartialLiner liner = new PartialLiner(partial, block.clone());
                if (filter.test(liner)) {
                    cnt.incrementAndGet();
                    nonIsomorphic.putIfAbsent(liner.getCanonical(), new Inc(liner.flags()));
                }
            };
            partial.altBlocks(blockConsumer);
        }
        return new ArrayList<>(nonIsomorphic.values());
    }

    private static List<Inc> nextStageAltConc(List<Inc> partials, AtomicLong cnt) {
        Map<BitSet, Inc> nonIsomorphic = new ConcurrentHashMap<>();
        partials.stream().parallel().forEach(inc -> {
            PartialLiner partial = new PartialLiner(inc);
            Consumer<int[]> blockConsumer = block -> {
                PartialLiner liner = new PartialLiner(partial, block.clone());
                cnt.incrementAndGet();
                nonIsomorphic.putIfAbsent(liner.getCanonical(), new Inc(liner.flags()));
            };
            partial.altBlocks(blockConsumer);
        });
        return new ArrayList<>(nonIsomorphic.values());
    }

    private static List<Inc> nextStageAltConc(List<Inc> partials, Predicate<PartialLiner> filter, AtomicLong cnt) {
        Map<BitSet, Inc> nonIsomorphic = new ConcurrentHashMap<>();
        partials.stream().parallel().forEach(inc -> {
            PartialLiner partial = new PartialLiner(inc);
            Consumer<int[]> blockConsumer = block -> {
                PartialLiner liner = new PartialLiner(partial, block.clone());
                if (filter.test(liner)) {
                    cnt.incrementAndGet();
                    nonIsomorphic.putIfAbsent(liner.getCanonical(), new Inc(liner.flags()));
                }
            };
            partial.altBlocks(blockConsumer);
        });
        return new ArrayList<>(nonIsomorphic.values());
    }

    @Test
    public void extract() throws IOException {
        DumpConfig conf = readExact("com", 41, 5, 63);
        dump("come", conf.v(), conf.k(), conf.left, Arrays.asList(conf.partials()));
    }
}
