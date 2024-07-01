package ua.ihromant.mathutils.nauty;

import java.util.Arrays;

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
                cellIdx[el] = idx;
            }
            idx = idx + cell.length;
        }
    }

    public Partition(Partition stack) {
        this.cellCnt = stack.cellCnt;
        this.partition = stack.partition.clone();
        this.cellIdx = stack.cellIdx.clone();
    }

    private void replace(int idx, int[][] list) {
        for (int[] cell : list) {
            partition[idx] = cell;
            for (int el : cell) {
                cellIdx[el] = idx;
            }
            idx = idx + cell.length;
        }
        cellCnt = cellCnt + list.length - 1;
    }

    public boolean isDiscrete() {
        return cellCnt == partition.length;
    }

    public SubPartition subPartition() {
        return new SubPartition(partition);
    }

    public void refine(GraphWrapper graph, SubPartition alpha) {
        while (!alpha.isEmpty() && !isDiscrete()) {
            int wMin = alpha.remove();
            int idx = 0;
            while (idx < partition.length) {
                int[] xCell = partition[idx];
                DistinguishResult dist = graph.distinguish(xCell, partition[cellIdx[wMin]]);
                int[][] elms = dist.elms();
                replace(idx, elms);
                idx = idx + xCell.length;
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
        SubPartition singleton = new SubPartition(partition.length, v);
        Partition mul = mul(v);
        mul.refine(g, singleton);
        return mul;
    }

    public int[] smallestNonTrivial() {
        int[] res = null;
        int idx = 0;
        while (idx < partition.length) {
            int[] cell = partition[idx];
            int cl = cell.length;
            if (cl > 1 && (res == null || cl > res.length)) {
                res = cell;
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
}
