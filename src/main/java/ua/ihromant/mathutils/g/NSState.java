package ua.ihromant.mathutils.g;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;

public record NSState(int[] block, FixBS diffSet, FixBS whiteList) {
    public NSState acceptElem(GSpace sp, int x) {
        int v = sp.v();
        int[] newBlock = Arrays.copyOf(block, block.length + 1);
        newBlock[block.length] = x;
        FixBS newDiffSet = diffSet.copy();
        FixBS newWhiteList = whiteList.copy();
        for (int b : block) {
            int bx = b * v + x;
            int dbx = sp.diffIdx(bx);
            if (newDiffSet.get(dbx)) {
                return null;
            } else {
                newDiffSet.set(dbx);
                newWhiteList.andNot(sp.difference(dbx));
            }
            int xb = x * v + b;
            int dxb = sp.diffIdx(xb);
            if (newDiffSet.get(dxb)) {
                return null;
            } else {
                newDiffSet.set(dxb);
                newWhiteList.andNot(sp.difference(dxb));
            }
        }
        return new NSState(newBlock, newDiffSet, newWhiteList);
    }
}
