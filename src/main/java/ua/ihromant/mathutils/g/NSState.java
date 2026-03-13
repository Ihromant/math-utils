package ua.ihromant.mathutils.g;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;

public record NSState(int[] block, FixBS whiteList) {
    public NSState acceptElem(GSpace sp, int x) {
        int v = sp.v();
        int[] newBlock = Arrays.copyOf(block, block.length + 1);
        newBlock[block.length] = x;
        FixBS newWhiteList = whiteList.copy();
        for (int b : block) {
            int bx = b * v + x;
            if (newWhiteList.get(bx)) {
                newWhiteList.andNot(sp.difference(sp.diffIdx(bx)));
            } else {
                return null;
            }
            int xb = x * v + b;
            if (newWhiteList.get(xb)) {
                newWhiteList.andNot(sp.difference(sp.diffIdx(xb)));
            } else {
                return null;
            }
        }
        return new NSState(newBlock, newWhiteList);
    }
}
