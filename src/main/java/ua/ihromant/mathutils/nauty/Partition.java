package ua.ihromant.mathutils.nauty;

import ua.ihromant.jnauty.NautyGraph;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

public class Partition {
    private int cellCnt;
    private final int[] cellIdx;
    private final int[][] partition;

    public Partition(int maxCnt, int[][] partition) {
        this.partition = new int[maxCnt][];
        this.cellIdx = new int[maxCnt];
        this.cellCnt = partition.length;
        int idx = 0;
        for (int[] cell : partition) {
            this.partition[idx] = cell;
            for (int el : cell) {
                cellIdx[el] = idx++;
            }
        }
    }

    public Partition(Partition stack) {
        this.cellCnt = stack.cellCnt;
        this.partition = stack.partition.clone();
        this.cellIdx = stack.cellIdx.clone();
    }

    public static Partition partition(NautyGraph graph) {
        int sz = graph.vCount();
        SortedMap<Integer, FixBS> colorDist = new TreeMap<>();
        IntStream.range(0, sz).forEach(i -> colorDist.computeIfAbsent(graph.vColor(i), _ -> new FixBS(sz)).set(i));
        int[][] result = new int[colorDist.lastKey() + 1][0];
        colorDist.forEach((k, v) -> result[k] = v.toArray());
        return new Partition(sz, result);
    }

    private void replace(int idx, int[][] list) {
        for (int[] cell : list) {
            partition[idx] = cell;
            for (int el : cell) {
                cellIdx[el] = idx++;
            }
        }
        cellCnt = cellCnt + list.length - 1;
    }

    public boolean isDiscrete() {
        return cellCnt == partition.length;
    }

    public SubPartition subPartition() {
        return new SubPartition(partition);
    }

    public int[] cellByIdx(int idx) {
        return partition[idx];
    }

    public void refine(NautyGraph graph, SubPartition alpha, FixBS singulars) {
        while (!alpha.isEmpty() && !isDiscrete()) {
            int wMin = alpha.remove();
            int idx = 0;
            while (idx < partition.length) {
                int[] xCell = partition[idx];
                DistinguishResult dist = distinguish(graph, xCell, partition[cellIdx[wMin]]);
                int[][] elms = dist.elms();
                if (elms.length > 1) {
                    replace(idx, elms);
                }
                singulars.or(dist.singulars());
                idx = idx + xCell.length;
                int xIdx = alpha.idxOf(xCell[0]);
                if (xIdx >= 0) {
                    if (elms.length > 1) {
                        alpha.replace(xIdx, elms);
                    }
                } else {
                    alpha.addButLargest(elms, dist.largest());
                }
            }
        }
    }

    private static DistinguishResult distinguish(NautyGraph graph, int[] cell, int[] w) {
        int cl = cell.length;
        FixBS singulars = new FixBS(graph.vCount());
        if (cl == 1 || graph.vColor(cell[0]) == graph.vColor(w[0])) {
            return new DistinguishResult(new int[][]{cell}, 0, singulars); // TODO not true for not bipartite graphs
        }
        int[] numEdgesDist = new int[cl];
        for (int i = 0; i < cl; i++) {
            int el = cell[i];
            for (int o : w) {
                if (graph.edge(el, o)) {
                    numEdgesDist[i]++;
                }
            }
        }
        FixBS nonZeros = new FixBS(w.length + 1);
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

    public FixBS ort(NautyGraph g, int cellIdx, int shift) {
        int[] cell = partition[cellIdx];
        int v = cell[shift];
        SubPartition singleton = new SubPartition(partition.length, v);
        FixBS result = new FixBS(g.vCount());
        int[] butV = new int[cell.length - 1];
        System.arraycopy(cell, 0, butV, 0, shift);
        System.arraycopy(cell, shift + 1, butV, shift, cell.length - shift - 1);
        replace(cellIdx, new int[][]{{v}, butV});
        result.set(v);
        if (butV.length == 1) {
            result.set(butV[0]);
        }
        refine(g, singleton, result);
        return result;
    }

    public FixBS singulars() {
        FixBS result = new FixBS(partition.length);
        int idx = 0;
        while (idx < partition.length) {
            int[] cell = partition[idx];
            int cl = cell.length;
            if (cl == 1) {
                result.set(cell[0]);
            }
            idx = idx + cl;
        }
        return result;
    }

    public int firstNonTrivial() {
        int idx = 0;
        while (idx < partition.length) {
            int[] cell = partition[idx];
            int cl = cell.length;
            if (cl > 1) {
                return idx;
            }
            idx = idx + cl;
        }
        throw new IllegalStateException();
    }

    public int largestNonTrivial() {
        int res = -1;
        int maxL = Integer.MIN_VALUE;
        int idx = 0;
        while (idx < partition.length) {
            int[] cell = partition[idx];
            int cl = cell.length;
            if (cl > 1 && (res < 0 || maxL < cl)) {
                maxL = cl;
                res = idx;
            }
            idx = idx + cl;
        }
        return res;
    }

    public int permute(int v) {
        return cellIdx[v];
    }

    public int[] permutation() {
        return cellIdx;
    }

    public int[] reverse() {
        return Arrays.stream(partition).mapToInt(cell -> cell[0]).toArray();
    }

    public int[] cellSizes() {
        int[] result = new int[cellCnt];
        int idx = 0;
        int resIdx = 0;
        while (resIdx < result.length) {
            int len = partition[idx].length;
            result[resIdx++] = len;
            idx = idx + len;
        }
        return result;
    }

    public FixBS permutedIncidence(NautyGraph graph) {
        int size = graph.vCount();
        FixBS bs = new FixBS(size * size);
        for (int l = 0; l < size; l++) {
            int pl = cellIdx[l];
            for (int p = 0; p < size; p++) {
                if (graph.edge(l, p)) {
                    int pp = cellIdx[p];
                    bs.set(pl * size + pp);
                }
            }
        }
        return bs;
    }
}
