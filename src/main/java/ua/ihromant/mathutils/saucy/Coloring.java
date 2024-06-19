package ua.ihromant.mathutils.saucy;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Coloring {
    private int[] lab;
    private int[] unLab;
    private int[] cFront;
    private int[] cLen;

    public int doFindMin(int t) {
        return SaucyAlgo.minIdx(lab, t, cLen[t]) + t;
    }

    public void setLabel(int index, int value) {
        lab[index] = value;
        unLab[value] = index;
    }

    public void swapLabels(int a, int b) {
        int tmp = lab[a];
        setLabel(a, lab[b]);
        setLabel(b, tmp);
    }

    public void fixFronts(int cf, int ff) {
        int end = cf + cLen[cf];
        for (int i = ff; i <= end; ++i) {
            cFront[lab[i]] = cf;
        }
    }

    public boolean inCellRange(int ff, int cf) {
        return cf <= ff && ff < cf + cLen[cf];
    }

    public void splitColor(int cf, int ff) {
        int fb = ff - 1;
        int cb = cf + cLen[cf];
        cLen[cf] = fb - cf;
        cLen[ff] = cb - ff;

        fixFronts(ff, ff);
    }
}
