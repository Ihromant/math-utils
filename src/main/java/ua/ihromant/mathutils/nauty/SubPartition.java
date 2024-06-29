package ua.ihromant.mathutils.nauty;

import lombok.Getter;

import java.util.Arrays;

@Getter
public class SubPartition {
    private final int[] cellMins;
    private final int[] idxes;
    private int size;

    public SubPartition(int cellCount, int[][] cells) {
        this.cellMins = new int[cells.length];
        this.idxes = new int[cells.length];
        Arrays.fill(idxes, -1);
        this.size = cellCount;
        for (int i = 0; i < cellCount; i++) {
            int min = cells[i][0];
            cellMins[i] = min;
            idxes[min] = i;
        }
    }

    public SubPartition(int maxLength, int singleton) {
        this.size = 1;
        this.cellMins = new int[maxLength];
        this.idxes = new int[maxLength];
        Arrays.fill(idxes, -1);
        cellMins[0] = singleton;
        idxes[singleton] = 0;
    }

    public void replace(int minIdx, int[][] split) {
        for (int i = size - 1; i > minIdx; --i) {
            int min = cellMins[i];
            idxes[min] += split.length - 1;
            cellMins[i + split.length - 1] = min;
        }
        for (int i = 0; i < split.length; i++) {
            int idx = minIdx + i;
            int min = split[i][0];
            cellMins[idx] = min;
            idxes[min] = idx;
        }
        size += split.length - 1;
    }

    public void addButLargest(DistinguishResult dist) {
        int[][] elms = dist.elms();
        for (int i = 0; i < elms.length; i++) {
            if (i == dist.largest()) {
                continue;
            }
            int min = elms[i][0];
            int idx = size++;
            cellMins[idx] = min;
            idxes[min] = idx;
        }
    }

    public int idxOf(int minimal) {
        return idxes[minimal];
    }

    public int remove() {
        int res = cellMins[--size];
        idxes[res] = -1;
        return res;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
