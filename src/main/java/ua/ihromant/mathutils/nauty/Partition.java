package ua.ihromant.mathutils.nauty;

import java.util.Arrays;
import java.util.BitSet;

public class Partition {
    private int cellCnt;
    private final int[] cellIdx;
    private final int[][] partition;

    public Partition(int maxCnt, int[][] partition) {
        this.partition = new int[maxCnt][];
        this.cellIdx = new int[maxCnt];
        this.cellCnt = partition.length;
        System.arraycopy(partition, 0, this.partition, 0, partition.length);
        for (int i = 0; i < cellCnt; i++) {
            for (int el : partition[i]) {
                cellIdx[el] = i;
            }
        }
    }

    public Partition(Partition stack) {
        this.cellCnt = stack.cellCnt;
        this.partition = stack.partition.clone();
        this.cellIdx = stack.cellIdx.clone();
    }

    public void replace(int idx, int[][] list) {
        for (int i = cellCnt - 1; i > idx; i--) {
            int[] cell = partition[i];
            int shifted = i + list.length - 1;
            partition[shifted] = cell;
            for (int el : cell) {
                cellIdx[el] = shifted;
            }
        }
        for (int i = 0; i < list.length; i++) {
            int[] cell = list[i];
            int shifted = idx + i;
            partition[shifted] = cell;
            for (int el : cell) {
                cellIdx[el] = shifted;
            }
        }
        cellCnt = cellCnt + list.length - 1;
    }

    public boolean isDiscrete() {
        return cellCnt == partition.length;
    }

    public DistinguishResult distinguish(GraphWrapper graph, int cellIdx, int[] w) {
        int[] cell = partition[cellIdx];
        int[] numEdgesDist = new int[graph.size()];
        for (int el : cell) {
            for (int o : w) {
                if (graph.edge(el, o)) {
                    numEdgesDist[el]++;
                }
            }
        }
        BitSet nonZeros = new BitSet(w.length + 1);
        int[] numEdgesCnt = new int[w.length + 1];
        int maxCnt = 0;
        int maxIdx = 0;
        for (int i : cell) {
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
        for (int x : cell) {
            int cnt = numEdgesDist[x];
            int i = idxes[cnt];
            if (result[i] == null) {
                result[i] = new int[numEdgesCnt[cnt]];
            }
            result[i][result[i].length - numEdgesCnt[cnt]--] = x;
        }
        int largest = idxes[maxIdx];
        return new DistinguishResult(result, largest);
    }

    public SubPartition subPartition() {
        return new SubPartition(cellCnt, partition);
    }

    public void refine(GraphWrapper graph, SubPartition alpha) {
        while (!alpha.isEmpty() && !isDiscrete()) {
            int wMin = alpha.remove();
            int idx = 0;
            while (idx < cellCnt) {
                int[] xCell = partition[idx];
                DistinguishResult dist = distinguish(graph, idx, partition[cellIdx[wMin]]);
                int[][] elms = dist.elms();
                replace(idx, elms);
                idx = idx + elms.length;
                int xIdx = alpha.idxOf(xCell[0]);
                if (xIdx >= 0) {
                    alpha.replace(xIdx, elms);
                } else {
                    alpha.addButLargest(dist);
                }
            }
        }
    }

    public Partition mul(int v) {
        int idx = cellIdx[v];
        int[] cell = partition[idx];
        Partition copy = new Partition(this);
        if (cell.length == 1) {
            return copy;
        }
        int[] butV = new int[cell.length - 1];
        int vIdx = Arrays.binarySearch(cell, v);
        System.arraycopy(cell, 0, butV, 0, vIdx);
        System.arraycopy(cell, vIdx + 1, butV, vIdx, cell.length - vIdx - 1);
        copy.replace(idx, new int[][]{{v}, butV});
        return copy;
    }

    public Partition ort(GraphWrapper g, int v) {
        SubPartition singleton = new SubPartition(cellIdx.length, v);
        Partition mul = mul(v);
        mul.refine(g, singleton);
        return mul;
    }

    public int[] smallestNonTrivial() {
        int[] res = null;
        for (int i = 0; i < cellCnt; i++) {
            int[] elem = partition[i];
            if (elem.length == 1) {
                continue;
            }
            if (res == null || res.length > elem.length) {
                res = elem;
            }
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
}
