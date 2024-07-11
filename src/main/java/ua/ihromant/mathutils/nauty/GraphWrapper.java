package ua.ihromant.mathutils.nauty;

import ua.ihromant.mathutils.Inc;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.PartialLiner;

import java.util.BitSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

public interface GraphWrapper {
    int size();

    int color(int idx);

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

    default Partition partition() {
        SortedMap<Integer, BitSet> colorDist = new TreeMap<>();
        IntStream.range(0, size()).forEach(i -> colorDist.computeIfAbsent(color(i), j -> new BitSet(size())).set(i));
        int[][] result = new int[colorDist.lastKey() + 1][0];
        colorDist.forEach((k, v) -> result[k] = v.stream().toArray());
        return new Partition(size(), result);
    }

    default long[] permutedIncidence(Partition partition) {
        int vc = size();
        long[] arr = new long[(vc * vc + 63) / 64];
        for (int l = 0; l < vc; l++) {
            int pl = partition.permute(l);
            for (int p = 0; p < vc; p++) {
                if (edge(l, p)) {
                    int pp = partition.permute(p);
                    int idx = pl * vc + pp;
                    arr[idx >> 6] |= (1L << idx);
                }
            }
        }
        return arr;
    }

    default long[] fragment(BitSet singulars, int[] permutation) {
        int vc = size();
        long[] arr = new long[(vc * vc + 63) / 64];
        for (int u = singulars.nextSetBit(0); u >= 0; u = singulars.nextSetBit(u + 1)) {
            int ut = permutation[u];
            for (int v = 0; v < size(); v++) {
                if (edge(u, v)) {
                    int vt = permutation[v];
                    int idx = ut * vc + vt;
                    arr[idx >> 6] |= (1L << idx);
                }
            }
        }
        return arr;
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
