package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Inc;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IncFinderTest {
    @Test
    public void generateCom() throws IOException {
        String prefix = "com";
        int v = 25;
        int k = 4;
        int b = v * (v - 1) / k / (k - 1);
        int r = (v - 1) / (k - 1);
        DumpConfig conf = readLast(prefix, v, k, () -> new DumpConfig(v, k, b - r - 1, new Inc[]{(beamBlocks(v, k))}));
        List<Inc> liners = Arrays.asList(conf.partials);
        int left = b - liners.getFirst().b();
        long time = System.currentTimeMillis();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size());
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStageCanon(liners, l -> true, cnt);
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
        int v = 28;
        int k = 4;
        int b = v * (v - 1) / k / (k - 1);
        int r = (v - 1) / (k - 1);
        int dp = 4;
        DumpConfig conf = readLast(prefix, v, k, () -> new DumpConfig(v, k, b - r - 1, new Inc[]{(beamBlocks(v, k))}));
        List<Inc> liners = Arrays.asList(conf.partials);
        long time = System.currentTimeMillis();
        int left = conf.left();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size() + ", depth " + dp);
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStageCanon(liners, Inc::checkAP, cnt);
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

    public static List<Inc> nextStageCanon(List<Inc> partials, Predicate<Inc> filter, AtomicLong cnt) {
        Map<BitSet, Inc> nonIsomorphic = new ConcurrentHashMap<>();
        partials.stream().parallel().filter(filter).forEach(partial -> {
            for (int[] block : partial.blocks()) {
                Inc liner = new Inc(partial, block);
                nonIsomorphic.putIfAbsent(liner.getCanonical(), liner);
                cnt.incrementAndGet();
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
                    Inc partial = new Inc(new BitSet(v * lineCount), v, lineCount);
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

    @Test
    public void byPartials() throws IOException {
        String prefix = "com";
        int v = 25;
        int k = 4;
        int process = 3;
        DumpConfig conf = readLast(prefix, v, k, () -> {throw new IllegalArgumentException();});
        List<Inc> liners = Arrays.asList(conf.partials());
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + conf.left() + ", base size " + liners.size());
        long time = System.currentTimeMillis();
        Predicate<Inc> filter = l -> true;
        Map<BitSet, Inc> iso = new ConcurrentHashMap<>();
        AtomicInteger ai = new AtomicInteger();
        IntStream.range(0, liners.size()).parallel().forEach(idx -> {
            Inc pl = liners.get(idx);
            designs(pl, process, filter, des -> iso.putIfAbsent(des.getCanonical(), des));
            int val = ai.incrementAndGet();
            if (val % 1000 == 0) {
                System.out.println(val + " " + iso.size());
            }
        });
        dump("d" + prefix, v, k, conf.left() - process, new ArrayList<>(iso.values()));
        System.out.println("Finished, time elapsed " + (System.currentTimeMillis() - time));
    }

    private static void designs(Inc partial, int needed, Predicate<Inc> filter, Consumer<Inc> cons) {
        for (int[] block : partial.blocks()) {
            Inc nextPartial = new Inc(partial, block);
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
}