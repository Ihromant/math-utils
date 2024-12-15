package ua.ihromant.mathutils.plane;

import ua.ihromant.mathutils.Liner;

import java.util.Arrays;

public class NumeratedAffinePlane {
    private final Liner proj;
    private final int dl;
    private final int[] lnMap;
    private final int[] lnRev;
    private final int[] ptMap;
    private final int[] ptRev;
    private final int[][] lines;
    private final int[][] lineIdxes;
    private final int[][] pairs;
    private final int[][] pairRev;
    private final int[][] triples;
    private final int[][][] trRev;

    public NumeratedAffinePlane(Liner proj, int dl) {
        this.proj = proj;
        this.dl = dl;
        int order = proj.line(0).length - 1;
        this.ptMap = new int[proj.pointCount() - order - 1];
        this.ptRev = new int[proj.pointCount()];
        int idx = 0;
        for (int i = 0; i < proj.pointCount(); i++) {
            if (proj.flag(dl, i)) {
                ptRev[i] = -1;
                continue;
            }
            ptRev[i] = idx;
            ptMap[idx++] = i;
        }
        idx = 0;
        this.lnMap = new int[proj.lineCount() - 1];
        this.lnRev = new int[proj.lineCount()];
        this.lines = new int[proj.lineCount() - 1][order];
        this.lineIdxes = new int[proj.lineCount() - 1][ptMap.length];
        for (int i = 0; i < proj.lineCount(); i++) {
            if (i == dl) {
                lnRev[i] = -1;
                continue;
            }
            lnRev[i] = idx;
            Arrays.fill(lineIdxes[idx], -1);
            int ix = 0;
            for (int pt : proj.line(i)) {
                if (proj.flag(dl, pt)) {
                    continue;
                }
                lineIdxes[idx][ptRev[pt]] = ix;
                lines[idx][ix++] = ptRev[pt];
            }
            lnMap[idx++] = i;
        }
        this.pairs = new int[order * (order - 1)][2];
        this.pairRev = new int[order][order];
        this.triples = new int[order * (order - 1) * (order - 2)][3];
        this.trRev = new int[order][order][order];
        idx = 0;
        int pairIdx = 0;
        for (int i = 0; i < order; i++) {
            for (int j = 0; j < order; j++) {
                if (i == j) {
                    continue;
                }
                pairs[pairIdx][0] = i;
                pairs[pairIdx][1] = j;
                pairRev[i][j] = pairIdx++;
                for (int k = 0; k < order; k++) {
                    if (i == k || j == k) {
                        continue;
                    }
                    triples[idx][0] = i;
                    triples[idx][1] = j;
                    triples[idx][2] = k;
                    trRev[i][j][k] = idx++;
                }
            }
        }
    }

    public int intersection(int l1, int l2) {
        int inter = proj.intersection(lnMap[l1], lnMap[l2]);
        if (proj.flag(dl, inter)) {
            return -1;
        }
        return ptRev[inter];
    }

    public int line(int p1, int p2) {
        return lnRev[proj.line(ptMap[p1], ptMap[p2])];
    }

    public int parallel(int l, int p) {
        return lnRev[proj.line(ptMap[p], proj.intersection(lnMap[l], dl))];
    }

    public int[] line(int l) {
        return lines[l];
    }

    public int trIdx(int i, int j, int k) {
        return trRev[i][j][k];
    }

    public int[][] triples() {
        return triples;
    }

    public int pairIdx(int i, int j) {
        return pairRev[i][j];
    }

    public int[][] pairs() {
        return pairs;
    }

    public int idxOf(int line, int pt) {
        return lineIdxes[line][pt];
    }

    public int lineCount() {
        return lines.length;
    }
}
