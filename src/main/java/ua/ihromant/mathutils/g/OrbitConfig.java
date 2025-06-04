package ua.ihromant.mathutils.g;

import lombok.Getter;
import lombok.experimental.Accessors;
import ua.ihromant.mathutils.IntList;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

@Getter
@Accessors(fluent = true)
public class OrbitConfig {
    private final int v;
    private final int k;
    private final int traceLength;
    private final boolean outer;
    private final int orbitSize;
    private final Integer infinity;
    private final List<FixBS> innerBlocks = new ArrayList<>();
    private final FixBS outerBlock;
    private final FixBS innerFilter;
    private final FixBS outerFilter;

    public OrbitConfig(int v, int k, int traceLength, boolean outer) {
        if ((v - 1) % (k - 1) != 0 || (v * v - v) % (k * k - k) != 0) {
            throw new IllegalArgumentException();
        }
        this.v = v;
        this.k = k;
        this.traceLength = traceLength;
        this.outer = outer;
        this.orbitSize = v / 2;
        this.infinity = v % 2 == 1 ? v - 1 : null;
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
            FixBS left = new FixBS(v);
            FixBS right = new FixBS(v);
            for (int i = 0; i < traceLength; i++) {
                int val = orbitSize * i / traceLength;
                left.set(val);
                right.set(val + orbitSize);
                if (i != 0) {
                    innerFilter.set(val);
                }
            }
            if (infUsed) {
                left.set(infinity);
                right.set(infinity);
            }
            innerBlocks.add(left);
            innerBlocks.add(right);
        }
        this.outerBlock = outer ? new FixBS(v) : null;
        if (outer) {
            boolean inf = k % 2 == 1;
            if (infUsed && inf || inf && infinity == null) {
                throw new IllegalArgumentException();
            }
            infUsed = infUsed || inf;
            int half = k / 2;
            for (int i = 0; i < half; i++) {
                int val = orbitSize * i / half;
                outerBlock.set(val);
                outerBlock.set(val + orbitSize);
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
        return v + "-" + k + (traceLength == 0 ? "" : "-" + traceLength) + (infinity != null ? "o" : "");
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
            int[] left = chunk[0];
            int[] right = chunk[1];
            for (int i = 0; i < ol; i++) {
                BitSet block = new BitSet(v);
                for (int l : left) {
                    block.set((l + i) % ol);
                }
                for (int r : right) {
                    block.set((r + i) % ol + ol);
                }
                result.add(block);
            }
        }
        return new Liner(result.toArray(BitSet[]::new));
    }

    public int[][] getSuitable() {
        List<IntList> res = new ArrayList<>();
        find(innerFilter.cardinality() + 1, innerFilter.cardinality() + 1, outerFilter.cardinality(), new IntList(orbitSize), res::add);
        return res.stream().map(IntList::toArray).toArray(int[][]::new);
    }

    private void find(int left, int right, int inter, IntList lst, Consumer<IntList> cons) {
        if (left == orbitSize && right == orbitSize && inter == orbitSize) {
            cons.accept(lst);
            return;
        }
        if (left > orbitSize || right > orbitSize || inter > orbitSize) {
            return;
        }
        int prev = lst.isEmpty() ? 0 : lst.get(lst.size() - 1);
        for (int i = prev; i <= k; i++) {
            IntList nextLst = lst.copy();
            nextLst.add(i);
            find(left + i * (i - 1), right + (k - i) * (k - i - 1), inter + i * (k - i), nextLst, cons);
        }
    }
}
