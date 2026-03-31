package ua.ihromant.mathutils.g;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;

public record NSState(int[] block, FixBS diffSet, OrbitFilter of) {
    public NSState acceptElem(GSpace1 sp, int x) {
        int v = sp.v();
        int[] newBlock = Arrays.copyOf(block, block.length + 1);
        newBlock[block.length] = x;
        FixBS newDiffSet = diffSet.copy();
        OrbitFilter newOf = of.copy();
        for (int b : block) {
            int bx = b * v + x;
            int dbx = sp.diffIdx(bx);
            if (newDiffSet.get(dbx)) {
                return null;
            } else {
                newDiffSet.set(dbx);
                newOf.or(sp.projection(dbx));
            }
            int xb = x * v + b;
            int dxb = sp.diffIdx(xb);
            if (newDiffSet.get(dxb)) {
                return null;
            } else {
                newDiffSet.set(dxb);
                newOf.or(sp.projection(dxb));
            }
        }
        return new NSState(newBlock, newDiffSet, newOf);
    }
}
