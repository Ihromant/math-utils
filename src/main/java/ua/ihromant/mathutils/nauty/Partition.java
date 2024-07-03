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
                cellIdx[el] = idx++;
            }
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

    private Partition mul(int cellIdx, int shift) {
        int[] cell = partition[cellIdx];
        Partition copy = new Partition(this);
        if (cell.length == 1) {
            return copy;
        }
        int[] butV = new int[cell.length - 1];
        System.arraycopy(cell, 0, butV, 0, shift);
        System.arraycopy(cell, shift + 1, butV, shift, cell.length - shift - 1);
        copy.replace(cellIdx, new int[][]{{cell[shift]}, butV});
        return copy;
    }

    public Partition ort(GraphWrapper g, int cellIdx, int shift) {
        int[] cell = partition[cellIdx];
        int v = cell[shift];
        SubPartition singleton = new SubPartition(partition.length, v);
        Partition mul = mul(cellIdx, shift);
        mul.refine(g, singleton);
        return mul;
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
}
