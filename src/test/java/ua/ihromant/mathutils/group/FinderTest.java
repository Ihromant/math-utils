package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Automorphisms;
import ua.ihromant.mathutils.Liner;

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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FinderTest {
    @Test
    public void generateCom() throws IOException {
        String prefix = "com";
        int v = 15;
        int k = 3;
        DumpConfig conf = readLast(prefix, v, k);
        List<DesignData> liners = Arrays.stream(conf.partials()).map(part -> {
            boolean[][] frequencies = new boolean[v][v];
            for (int i = 0; i < v; i++) {
                frequencies[i][i] = true;
            }
            for (int[] block : part) {
                enhanceFrequencies(frequencies, block);
            }
            return new DesignData(part, frequencies);
        }).collect(Collectors.toList());
        long time = System.currentTimeMillis();
        int left = conf.left();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size());
        while (left > 0 && !liners.isEmpty()) {
            liners = nextStage(v, k, liners, l -> false);
            left--;
            dump(prefix, v, k, left, liners);
            System.out.println(left + " " + liners.size());
        }
        System.out.println(System.currentTimeMillis() - time);
    }

    private static void blocks(int prev, int[] curr, int moreNeeded, BitSet possible, boolean[][] frequencies, Consumer<int[]> sink) {
        for (int idx = possible.nextSetBit(prev + 1); idx >= 0; idx = possible.nextSetBit(idx + 1)) {
            int[] nextCurr = curr.clone();
            nextCurr[nextCurr.length - moreNeeded] = idx;
            if (moreNeeded == 1) {
                sink.accept(nextCurr);
                continue;
            }
            BitSet nextPossible = (BitSet) possible.clone();
            for (int i = idx + 1; i < frequencies[idx].length; i++) {
                if (frequencies[idx][i]) {
                    nextPossible.set(i, false);
                }
            }
            blocks(idx, nextCurr, moreNeeded - 1, nextPossible, frequencies, sink);
        }
    }

    @Test
    public void generateAP2() throws IOException {
        String prefix = "ap";
        int v = 28;
        int k = 4;
        DumpConfig conf = readLast(prefix, v, k);
        List<DesignData> liners = Arrays.stream(conf.partials()).map(part -> {
            boolean[][] frequencies = new boolean[v][v];
            for (int i = 0; i < v; i++) {
                frequencies[i][i] = true;
            }
            for (int[] block : part) {
                enhanceFrequencies(frequencies, block);
            }
            return new DesignData(part, frequencies);
        }).collect(Collectors.toList());
        long time = System.currentTimeMillis();
        int left = conf.left();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size());
        while (left > 0 && !liners.isEmpty()) {
            liners = nextStage(v, k, liners, FinderTest::checkAP);
            left--;
            dump(prefix, v, k, left, liners);
            System.out.println(left + " " + liners.size());
        }
        System.out.println(System.currentTimeMillis() - time);
    }

    private static void enhanceFrequencies(boolean[][] frequencies, int[] block) {
        for (int i = 0; i < block.length; i++) {
            for (int j = i + 1; j < block.length; j++) {
                int x = block[i];
                int y = block[j];
                frequencies[x][y] = true;
                frequencies[y][x] = true;
            }
        }
    }

    private record DesignData(int[][] partial, boolean[][] frequencies) {}

    private record DumpConfig(int v, int k, int left, int[][][] partials) {}

    private static void dump(String prefix, int v, int k, int left, List<DesignData> liners) throws IOException {
        try (FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/partials/" + prefix + "-" + v + "-" + k + ".txt", true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            ps.println(left + " blocks left");
            ps.println(liners.size() + " partials");
            for (DesignData l : liners) {
                for (int[] line : l.partial()) {
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

    private static boolean hasGaps(boolean[] arr) {
        for (boolean b : arr) {
            if (!b) {
                return true;
            }
        }
        return false;
    }

    private static List<DesignData> nextStage(int variants, int k, List<DesignData> partials, Predicate<Liner> filter) {
        BiFunction<Liner, Liner, int[]> iso = k >= 4 ? Automorphisms::isomorphism : Automorphisms::altIsomorphism;
        List<DesignData> nextList = new ArrayList<>();
        int cl = partials.getFirst().partial().length;
        List<Liner> nonIsomorphic = new ArrayList<>();
        for (DesignData data : partials) {
            int[][] partial = data.partial();
            boolean[][] frequencies = data.frequencies();
            int[] prev = partial[cl - 1];
            int prevFst = prev[0];
            int fst = IntStream.range(prevFst, variants - k + 1).filter(i -> hasGaps(frequencies[i])).findAny().orElseThrow();
            int[] initBlock = new int[k];
            initBlock[0] = fst;
            BitSet possible = new BitSet();
            boolean[] firstAssigned = frequencies[fst];
            for (int i = fst + 1; i < variants; i++) {
                if (!firstAssigned[i]) {
                    possible.set(i);
                }
            }
            int snd = possible.nextSetBit(fst + 1);
            if (snd < 0) {
                continue;
            }
            initBlock[1] = snd;
            boolean[] secondAssigned = frequencies[snd];
            possible.set(fst + 1, snd + 1, false);
            for (int i = snd + 1; i < variants; i++) {
                if (secondAssigned[i]) {
                    possible.set(i, false);
                }
            }
            Consumer<int[]> blockConsumer = block -> {
                int[][] nextPartial = new int[cl + 1][];
                System.arraycopy(partial, 0, nextPartial, 0, cl);
                nextPartial[cl] = block;
                Liner liner = new Liner(variants, Arrays.stream(nextPartial).map(FinderTest::of).toArray(BitSet[]::new));
                if (filter.test(liner) || nonIsomorphic.stream().anyMatch(l -> iso.apply(liner, l) != null)) {
                    return;
                }
                nonIsomorphic.add(liner);
                boolean[][] nextFrequencies = new boolean[variants][];
                for (int i = 0; i < nextFrequencies.length; i++) {
                    nextFrequencies[i] = frequencies[i].clone();
                }
                enhanceFrequencies(nextFrequencies, block);
                nextList.add(new DesignData(nextPartial, nextFrequencies));
            };
            blocks(snd, initBlock, k - 2, possible, frequencies, blockConsumer);
        }
        return nextList;
    }

    private static boolean checkAP(Liner liner) {
        int last = liner.lineCount() - 1;
        BitSet lLine = liner.line(last);
        for (int p = lLine.nextSetBit(0); p >= 0; p = lLine.nextSetBit(p + 1)) {
            BitSet lines = liner.point(p);
            for (int ol = lines.nextSetBit(0); ol >= 0; ol = lines.nextSetBit(ol + 1)) {
                if (ol == last) {
                    continue;
                }
                BitSet oLine = liner.line(ol);
                for (int pl1 = lLine.nextSetBit(0); pl1 >= 0; pl1 = lLine.nextSetBit(pl1+1)) {
                    if (pl1 == p) {
                        continue;
                    }
                    for (int pl2 = lLine.nextSetBit(pl1 + 1); pl2 >= 0; pl2 = lLine.nextSetBit(pl2 + 1)) {
                        if (pl2 == p) {
                            continue;
                        }
                        for (int po1 = oLine.nextSetBit(0); po1 >= 0; po1 = oLine.nextSetBit(po1 + 1)) {
                            if (po1 == p) {
                                continue;
                            }
                            int l1 = liner.line(pl1, po1);
                            int l2 = liner.line(pl2, po1);
                            if (l1 == -1 && l2 == -1) {
                                continue;
                            }
                            for (int po2 = oLine.nextSetBit(po1 + 1); po2 >= 0; po2 = oLine.nextSetBit(po2 + 1)) {
                                if (po2 == p) {
                                    continue;
                                }
                                int l3 = liner.line(pl1, po2);
                                int l4 = liner.line(pl2, po2);
                                if (l1 == -1 || l4 == -1) {
                                    continue;
                                } else {
                                    if (liner.intersection(l1, l4) >= 0) {
                                        return true;
                                    }
                                }
                                if (l2 != -1 && l3 != -1 && liner.intersection(l2, l3) >= 0) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet(values[values.length - 1] + 1);
        for (int v : values) {
            bs.set(v);
        }
        return bs;
    }
}
