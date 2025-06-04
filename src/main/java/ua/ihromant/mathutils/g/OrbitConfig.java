package ua.ihromant.mathutils.g;

import lombok.Getter;
import lombok.experimental.Accessors;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
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
            if (k % orbitCount > 1) {
                throw new IllegalArgumentException();
            }
            boolean inf = k % orbitCount == 1;
            if (infUsed && inf || inf && infinity == null) {
                throw new IllegalArgumentException();
            }
            infUsed = infUsed || inf;
            int part = k / orbitCount;
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

    public int[][] getSuitable() {
        return Arrays.stream(suitable()).map(arr -> Arrays.stream(arr).mapToInt(pr -> pr[0]).toArray()).toArray(int[][]::new);
    }

    private int[][][] suitable() {
        List<List<int[]>> res = new ArrayList<>();
        find(innerFilter.cardinality() + 1, innerFilter.cardinality() + 1, outerFilter.cardinality(), new ArrayList<>(), res::add);
        return res.stream().map(l -> l.toArray(int[][]::new)).toArray(int[][][]::new);
    }

    private void find(int left, int right, int inter, List<int[]> lst, Consumer<List<int[]>> cons) {
        if (left == orbitSize && right == orbitSize && inter == orbitSize) {
            cons.accept(lst);
            return;
        }
        if (left > orbitSize || right > orbitSize || inter > orbitSize) {
            return;
        }
        int[] prev = lst.isEmpty() ? new int[]{0} : lst.getLast();
        for (int i = prev[0]; i <= k; i++) {
            List<int[]> nextLst = new ArrayList<>(lst);
            nextLst.add(new int[]{i, k - i});
            find(left + i * (i - 1), right + (k - i) * (k - i - 1), inter + i * (k - i), nextLst, cons);
        }
    }

    private record Triple(int left, int mid, int right) {
        @Override
        public String toString() {
            return "(" + left + "," + mid + "," + right + ")";
        }
    }

    public static void main(String[] args) {
        List<List<Triple>> result = new ArrayList<>();
        find(30, 7, 5, 5, 5, 0, 0, 0, new ArrayList<>(), result::add);
        result.forEach(System.out::println);
    }

    private static void find(int v, int k, int left, int mid, int right, int interLeftRight, int interLeftMid, int interMidRight, List<Triple> lst, Consumer<List<Triple>> cons) {
        if (left == v - 1 && right == v - 1 && mid == v - 1 && interLeftRight == v && interLeftMid == v && interMidRight == v) {
            cons.accept(lst);
            return;
        }
        if (left >= v || mid >= v || right >= v || interLeftRight > v || interLeftMid > v || interMidRight > v) {
            return;
        }
        Triple prev = lst.isEmpty() ? null : lst.getLast();
        int leftLast = prev == null ? 0 : prev.left();
        for (int l = leftLast; l <= k; l++) {
            int midLast = !lst.isEmpty() && l == leftLast ? prev.mid() : 0;
            for (int m = midLast; m <= k - l; m++) {
                List<Triple> nextLst = new ArrayList<>(lst);
                int r = k - l - m;
                nextLst.add(new Triple(l, m, r));
                find(v, k, left + l * (l - 1), mid + m * (m - 1), right + r * (r - 1), interLeftRight + l * r, interLeftMid + l * m, interMidRight + m * r, nextLst, cons);
            }
        }
    }
}
