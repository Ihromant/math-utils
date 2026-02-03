package ua.ihromant.mathutils.nauty;

import ua.ihromant.mathutils.util.FixBS;

import java.util.BitSet;

public interface GraphWrapper {
    int size();

    int color(int idx);

    boolean edge(int a, int b);

    default FixBS permutedIncidence(Partition partition) {
        int size = size();
        FixBS bs = new FixBS(size * size);
        for (int l = 0; l < size; l++) {
            int pl = partition.permute(l);
            for (int p = 0; p < size; p++) {
                if (edge(l, p)) {
                    int pp = partition.permute(p);
                    bs.set(pl * size + pp);
                }
            }
        }
        return bs;
    }

    default FixBS fragment(BitSet singulars, int[] permutation) {
        int vc = size();
        FixBS res = new FixBS(vc * vc);
        for (int u = singulars.nextSetBit(0); u >= 0; u = singulars.nextSetBit(u + 1)) {
            int ut = permutation[u];
            for (int v = 0; v < vc; v++) {
                if (edge(u, v)) {
                    int vt = permutation[v];
                    res.set(ut * vc + vt);
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
