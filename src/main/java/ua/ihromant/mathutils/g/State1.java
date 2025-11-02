package ua.ihromant.mathutils.g;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;

public record State1(FixBS block, FixBS stabilizer, FixBS diffSet, int[][] diffs, int size) {
    public State1 acceptElem(GSpace1 gSpace, OrbitFilter globalFilter, int val) {
        if (block.get(val)) {
            return null;
        }
        int v = gSpace.v();
        int k = gSpace.k();
        FixBS newBlock = block.copy();
        FixBS queue = new FixBS(v);
        queue.set(val);
        int sz = size;
        FixBS newStabilizer = stabilizer.copy();
        FixBS newDiffSet = diffSet.copy();
        int[][] newDiffs = diffs.clone();
        while (!queue.isEmpty()) {
            if (++sz > k) {
                return null;
            }
            int x = queue.nextSetBit(0);
            if (x < val) {
                return null;
            }
            FixBS stabExt = new FixBS(gSpace.gOrd());
            for (int b = newBlock.nextSetBit(0); b >= 0; b = newBlock.nextSetBit(b + 1)) {
                int bx = b * v + x;
                int xb = x * v + b;
                int compBx = gSpace.diffIdx(bx);
                int compXb = gSpace.diffIdx(xb);
                if (globalFilter.intersects(gSpace.projection(compBx)) || globalFilter.intersects(gSpace.projection(compXb))) {
                    return null;
                }

                int[] exBx = newDiffs[compBx];
                stabExt.or(gSpace.prImage(b, b).intersection(gSpace.prImage(x, x)));
                if (exBx != null) {
                    for (int diff : exBx) {
                        int fst = diff / v;
                        int snd = diff % v;
                        stabExt.or(gSpace.prImage(b, fst).intersection(gSpace.prImage(x, snd)));
                    }
                    int[] newExDiffs = Arrays.copyOf(exBx, exBx.length + 1);
                    newExDiffs[exBx.length] = bx;
                    newDiffs[compBx] = newExDiffs;
                } else {
                    newDiffs[compBx] = new int[]{bx};
                }
                newDiffSet.set(compBx);

                int[] exXb = newDiffs[compXb];
                stabExt.or(gSpace.prImage(x, x).intersection(gSpace.prImage(b, b)));
                if (exXb != null) {
                    for (int diff : exXb) {
                        int fst = diff / v;
                        int snd = diff % v;
                        stabExt.or(gSpace.prImage(x, fst).intersection(gSpace.prImage(b, snd)));
                    }
                    int[] newExDiffs = Arrays.copyOf(exXb, exXb.length + 1);
                    newExDiffs[exXb.length] = xb;
                    newDiffs[compXb] = newExDiffs;
                } else {
                    newDiffs[compXb] = new int[]{xb};
                }
                newDiffSet.set(compXb);
            }
            newBlock.set(x);
            stabExt.andNot(newStabilizer);
            for (int st = newStabilizer.nextSetBit(1); st >= 0; st = newStabilizer.nextSetBit(st + 1)) {
                queue.set(gSpace.apply(st, x));
            }
            for (int st = stabExt.nextSetBit(1); st >= 0; st = stabExt.nextSetBit(st + 1)) {
                for (int b = newBlock.nextSetBit(0); b >= 0; b = newBlock.nextSetBit(b + 1)) {
                    queue.set(gSpace.apply(st, b));
                }
            }
            newStabilizer.or(stabExt);
            queue.andNot(newBlock);
        }
        return new State1(newBlock, newStabilizer, newDiffSet, newDiffs, sz);
    }

    public void updateFilter(OrbitFilter filter, GSpace1 space) {
        for (int i = diffSet.nextSetBit(0); i >= 0; i = diffSet.nextSetBit(i + 1)) {
            filter.or(space.projection(i));
        }
    }
}
