package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.HyperbolicPlane;

import java.util.Arrays;
import java.util.BitSet;
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
                    HyperbolicPlane pl = new HyperbolicPlane(arr);
                    BitSet csp = pl.cardSubPlanes(false);
                    if (!csp.get(v) && csp.cardinality() == 1) {
                        System.out.println(csp + " " + pl.cardSubSpaces(true) + " " + Arrays.toString(arr));
                    }
                });
    }

    @Test
    public void generate() {
        int v = 31;
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
        designs(v, k, blocks, v * (v - 1) / k / (k - 1) - r - 1, frequencies)
                .forEach(arr -> {
                    HyperbolicPlane pl = new HyperbolicPlane(arr);
                    BitSet csp = pl.cardSubPlanes(false);
                    if (!csp.get(v) && csp.cardinality() == 1) {
                        System.out.println(csp + " " + pl.cardSubSpaces(true) + " " + Arrays.toString(arr));
                    }
                });
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
