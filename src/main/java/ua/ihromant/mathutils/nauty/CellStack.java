package ua.ihromant.mathutils.nauty;

import java.util.Arrays;
import java.util.BitSet;

public class CellStack {
    private int cellCnt;
    private final int[][] partition;

    public CellStack(int maxCnt, int[][] partition) {
        this.partition = new int[maxCnt][];
        this.cellCnt = partition.length;
        System.arraycopy(partition, 0, this.partition, 0, partition.length);
    }

    public CellStack(CellStack stack) {
        this.cellCnt = stack.cellCnt;
        this.partition = new int[stack.partition.length][];
        System.arraycopy(stack.partition, 0, partition, 0, cellCnt);
    }

    public void replace(int idx, int[][] list) {
        System.arraycopy(partition, idx + 1, partition, idx + list.length, cellCnt - idx - 1);
        System.arraycopy(list, 0, partition, idx, list.length);
        cellCnt = cellCnt + list.length - 1;
    }

    public int idxOf(int[] elem) {
        for (int i = 0; i < cellCnt; i++) {
            if (Arrays.equals(elem, partition[i])) {
                return i;
            }
        }
        return -1;
    }

    public void addButLargest(DistinguishResult dist) {
        int[][] elms = dist.elms();
        for (int i = 0; i < elms.length; i++) {
            if (i == dist.largest()) {
                continue;
            }
            partition[cellCnt++] = elms[i];
        }
    }

    public boolean isDiscrete() {
        return cellCnt == partition.length;
    }

    public boolean isEmpty() {
        return cellCnt == 0;
    }

    public int[] remove() {
        return partition[--cellCnt];
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
            if (val > maxCnt) {
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
        return new DistinguishResult(result, idxes[maxIdx]);
    }

    public void refine(GraphWrapper graph, CellStack alpha) {
        while (!alpha.isEmpty() && !isDiscrete()) {
            int[] w = alpha.remove();
            int idx = 0;
            while (idx < cellCnt) {
                int[] xCell = partition[idx];
                DistinguishResult dist = distinguish(graph, idx, w);
                int[][] elms = dist.elms();
                replace(idx, elms);
                idx = idx + elms.length;
                int xIdx = alpha.idxOf(xCell);
                if (xIdx >= 0) {
                    alpha.replace(xIdx, elms);
                } else {
                    alpha.addButLargest(dist);
                }
            }
        }
    }
}
