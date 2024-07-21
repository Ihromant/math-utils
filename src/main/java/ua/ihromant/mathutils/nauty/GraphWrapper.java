package ua.ihromant.mathutils.nauty;

import ua.ihromant.mathutils.Inc;
import ua.ihromant.mathutils.InversivePlane;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.PartialLiner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.BitSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

public interface GraphWrapper {
    int size();

    int color(int idx);

    int pointCount();

    int lineCount();

    boolean edge(int a, int b);

    static GraphWrapper forFull(Liner liner) {
        return new GraphWrapper() {
            @Override
            public int size() {
                return liner.pointCount() + liner.lineCount();
            }

            @Override
            public int color(int idx) {
                return idx < liner.pointCount() ? 0 : 1;
            }

            @Override
            public int pointCount() {
                return liner.pointCount();
            }

            @Override
            public int lineCount() {
                return liner.lineCount();
            }

            @Override
            public boolean edge(int a, int b) {
                int pc = liner.pointCount();
                if (a < pc) {
                    return b >= pc && liner.flag(b - pc, a);
                } else {
                    return b < pc && liner.flag(a - pc, b);
                }
            }
        };
    }

    static GraphWrapper forPartial(PartialLiner liner) {
        return new GraphWrapper() {
            @Override
            public int size() {
                return liner.pointCount() + liner.lineCount();
            }

            @Override
            public int color(int idx) {
                return idx < liner.pointCount() ? 0 : 1;
            }

            @Override
            public int pointCount() {
                return liner.pointCount();
            }

            @Override
            public int lineCount() {
                return liner.lineCount();
            }

            @Override
            public boolean edge(int a, int b) {
                int pc = liner.pointCount();
                if (a < pc) {
                    return b >= pc && liner.flag(b - pc, a);
                } else {
                    return b < pc && liner.flag(a - pc, b);
                }
            }
        };
    }

    static GraphWrapper byInc(Inc inc) {
        return new GraphWrapper() {
            @Override
            public int size() {
                return inc.v() + inc.b();
            }

            @Override
            public int color(int idx) {
                return idx < inc.v() ? 0 : 1;
            }

            @Override
            public int pointCount() {
                return inc.v();
            }

            @Override
            public int lineCount() {
                return inc.b();
            }

            @Override
            public boolean edge(int a, int b) {
                int pc = inc.v();
                if (a < pc) {
                    return b >= pc && inc.inc(b - pc, a);
                } else {
                    return b < pc && inc.inc(a - pc, b);
                }
            }
        };
    }

    static GraphWrapper forInversive(InversivePlane inv) {
        return new GraphWrapper() {
            @Override
            public int size() {
                return inv.pointCount() + inv.blockCount();
            }

            @Override
            public int color(int idx) {
                return idx < inv.pointCount() ? 0 : 1;
            }

            @Override
            public int pointCount() {
                return inv.pointCount();
            }

            @Override
            public int lineCount() {
                return inv.blockCount();
            }

            @Override
            public boolean edge(int a, int b) {
                int pc = inv.pointCount();
                if (a < pc) {
                    return b >= pc && inv.flag(b - pc, a);
                } else {
                    return b < pc && inv.flag(a - pc, b);
                }
            }
        };
    }

    default Partition partition() {
        SortedMap<Integer, BitSet> colorDist = new TreeMap<>();
        IntStream.range(0, size()).forEach(i -> colorDist.computeIfAbsent(color(i), j -> new BitSet(size())).set(i));
        int[][] result = new int[colorDist.lastKey() + 1][0];
        colorDist.forEach((k, v) -> result[k] = v.stream().toArray());
        return new Partition(size(), result);
    }

    default FixBS permutedIncidence(Partition partition) {
        int pc = pointCount();
        int lc = lineCount();
        int size = size();
        FixBS bs = new FixBS(pc * lc);
        for (int l = pc; l < size; l++) {
            int pl = partition.permute(l);
            for (int p = 0; p < pc; p++) {
                if (edge(l, p)) {
                    int pp = partition.permute(p);
                    int li = pl - pc;
                    bs.set(li * pc + pp);
                }
            }
        }
        return bs;
    }

    default FixBS fragment(BitSet singulars, int[] permutation) {
        int pc = pointCount();
        int lc = lineCount();
        FixBS res = new FixBS(pc * lc);
        for (int u = singulars.nextSetBit(0); u >= 0; u = singulars.nextSetBit(u + 1)) {
            int ut = permutation[u];
            for (int v = 0; v < size(); v++) {
                if (edge(u, v)) {
                    int vt = permutation[v];
                    int p = Math.min(ut, vt);
                    int l = Math.max(ut, vt);
                    res.set((l - pc) * pc + p);
                }
            }
        }
        return res;
    }

    default DistinguishResult distinguish(int[] cell, int[] w) {
        int cl = cell.length;
        BitSet singulars = new BitSet(size());
        if (cl == 1 || color(cell[0]) == color(w[0])) {
            return new DistinguishResult(new int[][]{cell}, 0, singulars); // TODO not true for not bipartite graphs
        }
        int[] numEdgesDist = new int[cl];
        for (int i = 0; i < cl; i++) {
            int el = cell[i];
            for (int o : w) {
                if (edge(el, o)) {
                    numEdgesDist[i]++;
                }
            }
        }
        BitSet nonZeros = new BitSet(w.length + 1);
        int[] numEdgesCnt = new int[w.length + 1];
        int maxCnt = 0;
        int maxIdx = 0;
        for (int i = 0; i < cl; i++) {
            int cnt = numEdgesDist[i];
            int val = ++numEdgesCnt[cnt];
            nonZeros.set(cnt);
            if (val > maxCnt || (val == maxCnt && cnt < maxIdx)) {
                maxCnt = val;
                maxIdx = cnt;
            }
        }
        int[] idxes = new int[w.length + 1];
        int idx = 0;
        for (int i = nonZeros.nextSetBit(0); i >= 0; i = nonZeros.nextSetBit(i + 1)) {
            idxes[i] = idx++;
        }
        int[][] result = new int[nonZeros.cardinality()][];
        for (int ix = 0; ix < cl; ix++) {
            int cnt = numEdgesDist[ix];
            int i = idxes[cnt];
            if (result[i] == null) {
                int size = numEdgesCnt[cnt];
                result[i] = new int[size];
                if (size == 1) {
                    singulars.set(cell[ix]);
                }
            }
            result[i][result[i].length - numEdgesCnt[cnt]--] = cell[ix];
        }
        int largest = idxes[maxIdx];
        return new DistinguishResult(result, largest, singulars);
    }
}
