package ua.ihromant.mathutils.g;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.Objects;

public record State(FixBS block, FixBS stabilizer, FixBS diffSet, int[][] diffs, int size) {
    public static State fromBlock(GSpace space, FixBS block) {
        int fst = block.nextSetBit(0);
        int snd = block.nextSetBit(fst + 1);
        int trd = block.nextSetBit(snd + 1);
        State result = space.forInitial(fst, snd, trd);
        for (int el = block.nextSetBit(trd + 1); el >= 0; el = block.nextSetBit(el + 1)) {
            if (result.block().get(el)) {
                continue;
            }
            result = Objects.requireNonNull(result.acceptElem(space, space.emptyFilter(), el));
        }
        return result;
    }

    public State acceptElem(GSpace gSpace, FixBS globalFilter, int val) {
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
                if (globalFilter.get(bx) || globalFilter.get(xb)) {
                    return null;
                }

                int compBx = gSpace.diffIdx(bx);
                int[] exBx = newDiffs[compBx];
                stabExt.or(gSpace.preImage(b, b).intersection(gSpace.preImage(x, x)));
                if (exBx != null) {
                    for (int diff : exBx) {
                        int fst = diff / v;
                        int snd = diff % v;
                        stabExt.or(gSpace.preImage(b, fst).intersection(gSpace.preImage(x, snd)));
                    }
                    int[] newExDiffs = Arrays.copyOf(exBx, exBx.length + 1);
                    newExDiffs[exBx.length] = bx;
                    newDiffs[compBx] = newExDiffs;
                } else {
                    newDiffs[compBx] = new int[]{bx};
                }
                newDiffSet.set(compBx);

                int compXb = gSpace.diffIdx(xb);
                int[] exXb = newDiffs[compXb];
                stabExt.or(gSpace.preImage(x, x).intersection(gSpace.preImage(b, b)));
                if (exXb != null) {
                    for (int diff : exXb) {
                        int fst = diff / v;
                        int snd = diff % v;
                        stabExt.or(gSpace.preImage(x, fst).intersection(gSpace.preImage(b, snd)));
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
        return new State(newBlock, newStabilizer, newDiffSet, newDiffs, sz);
    }

    public void updateFilter(FixBS filter, GSpace space) {
        for (int i = diffSet.nextSetBit(0); i >= 0; i = diffSet.nextSetBit(i + 1)) {
            filter.or(space.difference(i));
        }
    }
}
