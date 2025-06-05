package ua.ihromant.mathutils.g;

import lombok.Getter;
import lombok.experimental.Accessors;
import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

@Getter
@Accessors(fluent = true)
public class OrbitConfig {
    private final int v;
    private final int k;
    private final int orbitCount;
    private final int traceLength;
    private final boolean outer;
    private final int orbitSize;
    private final Integer infinity;
    private final List<FixBS> innerBlocks = new ArrayList<>();
    private final FixBS outerBlock;
    private final FixBS innerFilter;
    private final FixBS outerFilter;

    public OrbitConfig(int v, int k, int traceLength, boolean outer, int orbitCount) {
        if ((v - 1) % (k - 1) != 0 || (v * v - v) % (k * k - k) != 0 || v % orbitCount > 1) {
            throw new IllegalArgumentException();
        }
        this.v = v;
        this.k = k;
        this.orbitCount = orbitCount;
        this.traceLength = traceLength;
        this.outer = outer;
        this.orbitSize = v / orbitCount;
        this.infinity = v % orbitCount == 1 ? v - 1 : null;
        this.innerFilter = new FixBS(orbitSize);
        this.outerFilter = new FixBS(orbitSize);
        boolean infUsed = false;
        if (traceLength != 0) {
            if (orbitSize % traceLength != 0) {
                throw new IllegalArgumentException();
            }
            if (infinity != null && traceLength != k && traceLength != k - 1 || infinity == null && traceLength != k) {
                throw new IllegalArgumentException();
            }
            infUsed = infinity != null && traceLength == k - 1;
            for (int orb = 0; orb < orbitCount; orb++) {
                FixBS block = new FixBS(v);
                for (int i = 0; i < traceLength; i++) {
                    int val = orbitSize * i / traceLength;
                    block.set(val + orb * orbitSize);
                    if (i != 0) {
                        innerFilter.set(val);
                    }
                }
                if (infUsed) {
                    block.set(infinity);
                }
                innerBlocks.add(block);
            }
        }
        this.outerBlock = outer ? new FixBS(v) : null;
        if (outer) {
            int part = k / orbitCount;
            if (k % orbitCount > 1 || orbitSize % part != 0) {
                throw new IllegalArgumentException();
            }
            boolean inf = k % orbitCount == 1;
            if (infUsed && inf || inf && infinity == null) {
                throw new IllegalArgumentException();
            }
            infUsed = infUsed || inf;
            for (int i = 0; i < part; i++) {
                int val = orbitSize * i / part;
                for (int orb = 0; orb < orbitCount; orb++) {
                    outerBlock.set(val + orbitSize * orb);
                }
                outerFilter.set(val);
                if (i != 0) {
                    if (innerFilter.get(val)) {
                        throw new IllegalArgumentException();
                    }
                    innerFilter.set(val);
                }
            }
            if (inf) {
                outerBlock.set(infinity);
            }
        }
        if (orbitSize % 2 == 0 && innerBlocks.stream().noneMatch(bl -> bl.get(orbitSize / 2)) && (outerBlock == null || !outerBlock.get(orbitSize / 2))) {
            throw new IllegalArgumentException();
        }
        if (infinity != null && !infUsed) {
            throw new IllegalArgumentException();
        }
    }

    public OrbitConfig(int v, int k, int traceLength, boolean outer) {
        this(v, k, traceLength, outer, 2);
    }

    public OrbitConfig(int v, int k, int traceLength) {
        this(v, k, traceLength, false);
    }

    public OrbitConfig(int v, int k, boolean outer) {
        this(v, k, 0, outer);
    }

    public OrbitConfig(int v, int k) {
        this(v, k, 0, false);
    }

    @Override
    public String toString() {
        return (orbitCount == 2 ? "" : orbitCount + "-") + v + "-" + k + (traceLength == 0 ? "" : "-" + traceLength) + (outer ? "o" : "");
    }

    public Liner fromChunks(int[][][] chunks) {
        Set<BitSet> result = new HashSet<>();
        int ol = orbitSize();
        for (FixBS bl : innerBlocks) {
            for (int i = 0; i < orbitSize; i++) {
                BitSet block = new BitSet(v);
                for (int el = bl.nextSetBit(0); el >= 0; el = bl.nextSetBit(el + 1)) {
                    if (Objects.equals(el, infinity)) {
                        block.set(el);
                    } else {
                        int rest = el % orbitSize;
                        int base = el - rest;
                        block.set(base + ((rest + i) % orbitSize));
                    }
                }
                result.add(block);
            }
        }
        if (outerBlock != null) {
            for (int i = 0; i < orbitSize; i++) {
                BitSet block = new BitSet(v);
                for (int el = outerBlock.nextSetBit(0); el >= 0; el = outerBlock.nextSetBit(el + 1)) {
                    if (Objects.equals(el, infinity)) {
                        block.set(el);
                    } else {
                        int rest = el % orbitSize;
                        int base = el - rest;
                        block.set(base + ((rest + i) % orbitSize));
                    }
                }
                result.add(block);
            }
        }
        for (int[][] chunk : chunks) {
            for (int i = 0; i < ol; i++) {
                BitSet block = new BitSet(v);
                for (int orb = 0; orb < orbitCount; orb++) {
                    for (int p : chunk[orb]) {
                        block.set((p + i) % ol + orb * ol);
                    }
                }
                result.add(block);
            }
        }
        return new Liner(result.toArray(BitSet[]::new));
    }

    public int[][] firstSuitable() {
        return groupedSuitable().keySet().toArray(int[][]::new);
    }

    public Map<int[], List<int[][]>> groupedSuitable() {
        TreeMap<int[], List<int[][]>> result = new TreeMap<>(Combinatorics::compareArr);
        Arrays.stream(suitable()).forEach(arr -> {
            int[] fst = Arrays.stream(arr).mapToInt(pr -> pr[0]).toArray();
            result.computeIfAbsent(fst, k -> new ArrayList<>()).add(arr);
        });
        return result;
    }

    public int[][][] suitable() {
        return find().stream().map(l -> l.toArray(int[][]::new)).toArray(int[][][]::new);
    }

    private List<List<int[]>> find() {
        List<List<int[]>> res = new ArrayList<>();
        int[][] used = new int[orbitCount][orbitCount];
        for (int i = 0; i < orbitCount; i++) {
            for (int j = 0; j < orbitCount; j++) {
                used[i][j] = i == j ? innerFilter.cardinality() + 1 : outerFilter.cardinality();
            }
        }
        int[][] splits = generateSplits();
        find(used, new ArrayList<>(), splits, 0, res::add);
        return res;
    }

    private int[][] generateSplits() {
        List<int[]> res = new ArrayList<>();
        generateSplits(new int[orbitCount], 0, 0, res::add);
        return res.toArray(int[][]::new);
    }

    private void generateSplits(int[] curr, int idx, int sum, Consumer<int[]> cons) {
        for (int i = 0; i <= k - sum; i++) {
            int[] nextCurr = curr.clone();
            nextCurr[idx] = i;
            if (idx < curr.length - 2) {
                generateSplits(nextCurr, idx + 1, sum + i, cons);
            } else {
                nextCurr[curr.length - 1] = k - sum - i;
                cons.accept(nextCurr);
            }
        }
    }

    private void find(int[][] used, List<int[]> lst, int[][] splits, int idx, Consumer<List<int[]>> cons) {
        ex: for (int i = idx; i < splits.length; i++) {
            int[] split = splits[i];
            int[][] nextUsed = new int[orbitCount][orbitCount];
            boolean allSize = true;
            for (int j = 0; j < orbitCount; j++) {
                for (int k = 0; k < orbitCount; k++) {
                    int addition = j == k ? split[j] * (split[j] - 1) : split[j] * split[k];
                    int nextVal = used[j][k] + addition;
                    if (nextVal > orbitSize) {
                        continue ex;
                    }
                    allSize = allSize && nextVal == orbitSize;
                    nextUsed[j][k] = nextVal;
                }
            }
            List<int[]> nextLst = new ArrayList<>(lst);
            nextLst.add(split);
            if (allSize) {
                cons.accept(nextLst);
            } else {
                find(nextUsed, nextLst, splits, i, cons);
            }
        }
    }
}
