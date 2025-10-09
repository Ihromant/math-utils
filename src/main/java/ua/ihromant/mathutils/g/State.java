package ua.ihromant.mathutils.g;

import ua.ihromant.mathutils.IntList;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Objects;

public record State(FixBS block, FixBS stabilizer, FixBS diffSet, IntList[] diffs, int size) {
    public static State fromBlock(GSpace space, FixBS block) {
        int fst = block.nextSetBit(0);
        int snd = block.nextSetBit(fst + 1);
        State result = space.forInitial(fst, snd);
        for (int el = block.nextSetBit(snd + 1); el >= 0; el = block.nextSetBit(el + 1)) {
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
        IntList[] newDiffs = new IntList[diffs.length];
        for (int i = 0; i < diffs.length; i++) {
            IntList lst = diffs[i];
            if (lst != null) {
                newDiffs[i] = lst.copy();
            }
        }
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
                int compXb = gSpace.diffIdx(xb);
                IntList existingDiffs = newDiffs[compBx];
                if (existingDiffs == null) {
                    existingDiffs = (newDiffs[compBx] = new IntList(k * (k - 1)));
                }
                existingDiffs.add(bx);
                for (int i = 0; i < existingDiffs.size(); i++) {
                    int diff = existingDiffs.get(i);
                    int fst = diff / v;
                    int snd = diff % v;
                    stabExt.or(gSpace.preImage(b, fst).intersection(gSpace.preImage(x, snd)));
                }
                newDiffSet.set(compBx);

                existingDiffs = newDiffs[compXb];
                if (existingDiffs == null) {
                    existingDiffs = (newDiffs[compXb] = new IntList(k * (k - 1)));
                }
                existingDiffs.add(xb);
                for (int i = 0; i < existingDiffs.size(); i++) {
                    int diff = existingDiffs.get(i);
                    int fst = diff / v;
                    int snd = diff % v;
                    stabExt.or(gSpace.preImage(x, fst).intersection(gSpace.preImage(b, snd)));
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
        for (int i = 0; i < diffs.length; i++) {
            if (diffs[i] != null) {
                filter.or(space.difference(i));
            }
        }
    }
}
