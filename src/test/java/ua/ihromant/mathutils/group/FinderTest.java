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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FinderTest {
    @Test
    public void generateByPartial2() {
        int v = 63;
        int k = 3;
        int[][] pr = new int[][] {
                {0, 3, 5},
                {0, 4, 6},
                {1, 3, 4},
                {1, 5, 6},
                {2, 3, 6},
                {2, 4, 5}
        };
        int[][] base = Stream.concat(IntStream.range(0, 10).map(i -> i * 4).boxed().flatMap(sh -> Arrays.stream(pr).map(arr -> {
            int[] res = new int[3];
            for (int i = 0; i < 3; i++) {
                res[i] = arr[i] < 3 ? arr[i] : arr[i] + sh;
            }
            return res;
        })), Stream.of(new int[]{0, 1, 2})).toArray(int[][]::new);
        BitSet[] blocks = Arrays.stream(base).map(FinderTest::of).toArray(BitSet[]::new);
        BitSet[] frequencies = IntStream.range(0, v).mapToObj(i -> new BitSet()).toArray(BitSet[]::new);
        Arrays.stream(blocks).forEach(line -> enhanceFrequencies(frequencies, line));
        designs(v, k, blocks, v * (v - 1) / k / (k - 1) - base.length, frequencies)
                .forEach(arr -> {
                    Liner pl = new Liner(arr);
                    BitSet csp = pl.cardSubPlanes(false);
                    if (!csp.get(v) && csp.cardinality() == 1) {
                        System.out.println(csp + " " + pl.cardSubSpaces(true) + " " + Arrays.toString(arr));
                    }
                });
    }

    @Test
    public void generate() {
        int v = 15;
        int k = 3;
        int r = (v - 1) / (k - 1);
        BitSet[] frequencies = IntStream.range(0, v).mapToObj(i -> new BitSet()).toArray(BitSet[]::new);
        BitSet[] blocks = new BitSet[r + 1];
        IntStream.range(0, r).forEach(i -> {
            BitSet block = of(IntStream.concat(IntStream.of(0), IntStream.range(0, k - 1).map(j -> 1 + i * (k - 1) + j)).toArray());
            enhanceFrequencies(frequencies, block);
            blocks[i] = block;
        });
        BitSet initial = of(IntStream.range(0, k).map(i -> 1 + (k - 1) * i).toArray());
        enhanceFrequencies(frequencies, initial);
        blocks[r] = initial;
        long time = System.currentTimeMillis();
        List<Liner> liners = new ArrayList<>();
        designs(v, k, blocks, v * (v - 1) / k / (k - 1) - r - 1, frequencies)
                .forEach(arr -> {
                    Liner pl = new Liner(arr);
                    BitSet csp = pl.cardSubPlanes(false);
                    if (liners.stream().noneMatch(l -> Automorphisms.isomorphism(l, pl) != null)) {
                        liners.add(pl);
                        System.out.println(csp + " " + Arrays.toString(arr));
                    }
                });
        System.out.println(System.currentTimeMillis() - time);
    }

    @Test
    public void generateCom() throws IOException {
        String prefix = "com";
        int v = 15;
        int k = 3;
        DumpConfig conf = readLast(prefix, v, k);
        List<DesignData> liners = conf.partials().stream().map(part -> {
            BitSet[] frequencies = IntStream.range(0, v).mapToObj(i -> new BitSet()).toArray(BitSet[]::new);
            for (BitSet block : part) {
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

    private static Stream<BitSet> blocks(int prev, BitSet curr, int needed, BitSet possible, BitSet[] frequencies) {
        return possible.stream().boxed().mapMulti((idx, sink) -> {
            BitSet nextCurr = (BitSet) curr.clone();
            nextCurr.set(idx);
            if (needed == 1) {
                sink.accept(nextCurr);
                return;
            }
            BitSet nextPossible = (BitSet) possible.clone();
            nextPossible.set(prev + 1, idx + 1, false);
            BitSet fr = frequencies[idx];
            for (int i = fr.nextSetBit(idx); i >= 0; i = fr.nextSetBit(i + 1)) {
                nextPossible.set(i, false);
            }
            blocks(prev, nextCurr, needed - 1, nextPossible, frequencies).forEach(sink);
        });
    }

    private static Stream<BitSet[]> designs(int variants, int k, BitSet[] curr, int needed, BitSet[] frequencies) {
        int blockNeeded = k - 1;
        int cl = curr.length;
        BitSet prev = curr[cl - 1];
        int prevFst = prev.nextSetBit(0);
        int fst = IntStream.range(prevFst, variants - blockNeeded).filter(i -> frequencies[i].cardinality() + 1 != variants).findAny().orElse(variants);
        BitSet base = of(fst);
        BitSet possible = (BitSet) frequencies[fst].clone();
        if (prevFst == fst) {
            int second = prev.nextSetBit(fst + 1);
            possible.set(0, second + 1, false);
            possible.flip(second + 1, variants);
        } else {
            possible.set(0, fst, false);
            possible.flip(fst + 1, variants);
        }
        return blocks(fst, base, blockNeeded, possible, frequencies).mapMulti((block, sink) -> {
            BitSet[] nextCurr = new BitSet[cl + 1];
            System.arraycopy(curr, 0, nextCurr, 0, cl);
            nextCurr[cl] = block;
            if (needed == 1) {
                sink.accept(nextCurr);
                return;
            }
            BitSet[] nextFrequencies = Arrays.stream(frequencies).map(bs -> (BitSet) bs.clone()).toArray(BitSet[]::new);
            enhanceFrequencies(nextFrequencies, block);
            designs(variants, k, nextCurr, needed - 1, nextFrequencies).forEach(sink);
        });
    }

    @Test
    public void generateAP2() throws IOException {
        String prefix = "ap";
        int v = 28;
        int k = 4;
        DumpConfig conf = readLast(prefix, v, k);
        List<DesignData> liners = conf.partials().stream().map(part -> {
            BitSet[] frequencies = IntStream.range(0, v).mapToObj(i -> new BitSet()).toArray(BitSet[]::new);
            for (BitSet block : part) {
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

    private record DesignData(BitSet[] partial, BitSet[] frequencies) {}

    private record DumpConfig(int v, int k, int left, List<BitSet[]> partials) {}

    private static void dump(String prefix, int v, int k, int left, List<DesignData> liners) throws IOException {
        try (FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/partials/" + prefix + "-" + v + "-" + k + ".txt", true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            ps.println(left + " blocks left");
            ps.println(liners.size() + " partials");
            for (DesignData l : liners) {
                for (BitSet bs : l.partial()) {
                    ps.println(bs.stream().mapToObj(String::valueOf).collect(Collectors.joining(" ")));
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
            List<BitSet[]> partials = new ArrayList<>();
            int lineCount = v * (v - 1) / k / (k - 1);
            while ((line = br.readLine()) != null) {
                partials.clear();
                left = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                line = br.readLine();
                int partialsCount = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                for (int i = 0; i < partialsCount; i++) {
                    int partialSize = lineCount - left;
                    BitSet[] partial = new BitSet[partialSize];
                    for (int j = 0; j < partialSize; j++) {
                        partial[j] = of(Arrays.stream(br.readLine().split(" ")).mapToInt(Integer::parseInt).toArray());
                    }
                    br.readLine();
                    partials.add(partial);
                }
            }
            return new DumpConfig(v, k, left, partials);
        } catch (FileNotFoundException e) {
            int r = (v - 1) / (k - 1);
            BitSet[] blocks = new BitSet[r + 1];
            IntStream.range(0, r).forEach(i -> {
                BitSet block = of(IntStream.concat(IntStream.of(0), IntStream.range(0, k - 1).map(j -> 1 + i * (k - 1) + j)).toArray());
                blocks[i] = block;
            });
            BitSet initial = of(IntStream.range(0, k).map(i -> 1 + (k - 1) * i).toArray());
            blocks[r] = initial;
            return new DumpConfig(v, k, v * (v - 1) / k / (k - 1) - r - 1, List.<BitSet[]>of(blocks));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<DesignData> nextStage(int variants, int k, List<DesignData> partials, Predicate<Liner> filter) {
        List<DesignData> nextList = new ArrayList<>();
        int cl = partials.getFirst().partial().length;
        int blockNeeded = k - 1;
        List<Liner> nonIsomorphic = new ArrayList<>();
        for (DesignData data : partials) {
            BitSet[] partial = data.partial();
            BitSet[] frequencies = data.frequencies();
            BitSet prev = partial[cl - 1];
            int prevFst = prev.nextSetBit(0);
            int fst = IntStream.range(prevFst, variants - blockNeeded).filter(i -> frequencies[i].cardinality() + 1 != variants).findAny().orElse(variants);
            BitSet base = of(fst);
            BitSet possible = (BitSet) frequencies[fst].clone();
            if (prevFst == fst) {
                int second = prev.nextSetBit(fst + 1);
                possible.set(0, second + 1, false);
                possible.flip(second + 1, variants);
            } else {
                possible.set(0, fst, false);
                possible.flip(fst + 1, variants);
            }
            blocks(fst, base, blockNeeded, possible, frequencies).forEach(block -> {
                BitSet[] nextPartial = new BitSet[cl + 1];
                System.arraycopy(partial, 0, nextPartial, 0, cl);
                nextPartial[cl] = block;
                Liner liner = new Liner(variants, nextPartial);
                if (filter.test(liner) || nonIsomorphic.stream().anyMatch(l -> Automorphisms.isomorphism(liner, l) != null)) {
                    return;
                }
                nonIsomorphic.add(liner);
                BitSet[] nextFrequencies = Arrays.stream(frequencies).map(bs -> (BitSet) bs.clone()).toArray(BitSet[]::new);
                enhanceFrequencies(nextFrequencies, block);
                nextList.add(new DesignData(nextPartial, nextFrequencies));
            });
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
                            if (l1 == -1 || l2 == -1) {
                                continue;
                            }
                            for (int po2 = oLine.nextSetBit(po1 + 1); po2 >= 0; po2 = oLine.nextSetBit(po2 + 1)) {
                                if (po2 == p) {
                                    continue;
                                }
                                int l3 = liner.line(pl1, po2);
                                int l4 = liner.line(pl2, po2);
                                if (l3 == -1 || l4 == -1) {
                                    continue;
                                }
                                if (liner.intersection(l1, l4) >= 0 || liner.intersection(l2, l3) >= 0) {
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

    private static void enhanceFrequencies(BitSet[] frequencies, BitSet block) {
        for (int x = block.nextSetBit(0); x >= 0; x = block.nextSetBit(x + 1)) {
            for (int y = block.nextSetBit(x + 1); y >= 0; y = block.nextSetBit(y + 1)) {
                frequencies[x].set(y);
                frequencies[y].set(x);
            }
        }
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet(values[values.length - 1] + 1);
        for (int v : values) {
            bs.set(v);
        }
        return bs;
    }
}
