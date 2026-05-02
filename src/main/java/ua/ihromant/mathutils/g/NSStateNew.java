package ua.ihromant.mathutils.g;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;

public record NSStateNew(int[] block, OrbitFilter of, FixBS blackList) {
    public NSStateNew acceptElem(GSpace1 sp, int x) {
        int v = sp.v();
        int[] newBlock = Arrays.copyOf(block, block.length + 1);
        newBlock[block.length] = x;
        OrbitFilter newOf = of.copy();
        FixBS newBlackList = blackList.copy();
        for (int b : block) {
            int bx = b * v + x;
            int dbx = sp.diffIdx(bx);
            newOf.or(sp.projection(dbx));

            int xb = x * v + b;
            int dxb = sp.diffIdx(xb);
            newOf.or(sp.projection(dxb));

            newBlackList.or(sp.sameDist(b, x));
        }
        return new NSStateNew(newBlock, newOf, newBlackList);
    }
}
